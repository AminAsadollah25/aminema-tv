package com.amin.tvos.browser

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.amin.tvos.data.model.CatalogKind
import com.amin.tvos.data.model.SearchResult
import com.amin.tvos.data.model.StreamingService
import com.amin.tvos.data.model.UserAgentMode
import org.json.JSONArray
import org.json.JSONObject

/**
 * Runs a query through each website's **own** search, inside the signed-in WebView.
 *
 * Nothing is scraped beyond what the site's own results page shows: title, poster and the
 * normal detail-page link. No media URL, stream URL, DRM value or token is read.
 *
 * The two services differ in what they can match, which the UI surfaces honestly:
 *  - the international site indexes both English and Persian titles;
 *  - the Iranian site only matches Persian titles, so a Latin query returns nothing there.
 */
class SiteSearchEngine(
    context: Context,
    private val onResults: (serviceId: String, results: List<SearchResult>) -> Unit,
    private val onFailure: (serviceId: String, reason: String) -> Unit
) {

    private var pendingService: StreamingService? = null
    private var pendingQuery = ""
    private var scriptStarted = false

    @SuppressLint("SetJavaScriptEnabled")
    private val webView = WebView(context).apply {
        alpha = 0f
        setBackgroundColor(Color.TRANSPARENT)
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            @Suppress("DEPRECATION")
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            allowFileAccess = false
            allowContentAccess = false
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        }
    }

    val view: WebView get() = webView

    init {
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }
        webView.addJavascriptInterface(Bridge(), "AminSearch")
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                val service = pendingService ?: return
                if (scriptStarted) return
                if (!Uri.parse(url).host.orEmpty()
                        .equals(Uri.parse(service.url).host.orEmpty(), true)
                ) return
                scriptStarted = true
                view.postDelayed({ view.evaluateJavascript(script(service, pendingQuery), null) }, 900L)
            }
        }
    }

    /** Loads the service origin once, then queries it from inside that origin. */
    fun search(service: StreamingService, query: String) {
        pendingService = service
        pendingQuery = query
        scriptStarted = false
        val mode = service.userAgent
            ?.let { value -> UserAgentMode.entries.firstOrNull { it.name == value } }
        webView.settings.userAgentString =
            mode?.value ?: WebSettings.getDefaultUserAgent(webView.context)
        webView.loadUrl(service.url)
    }

    fun destroy() {
        webView.stopLoading()
        webView.destroy()
    }

    private inner class Bridge {
        @JavascriptInterface
        fun results(serviceId: String?, payload: String?) {
            val service = pendingService ?: return
            if (serviceId != service.id) return
            onResults(service.id, parse(service, payload.orEmpty()))
        }

        @JavascriptInterface
        fun failed(serviceId: String?, reason: String?) {
            val service = pendingService ?: return
            if (serviceId != service.id) return
            onFailure(service.id, reason.orEmpty().take(80))
        }
    }

    /** Page output is untrusted: only same-host links matching the service pattern survive. */
    private fun parse(service: StreamingService, payload: String): List<SearchResult> {
        val adapter = ServiceAdapter(service)
        val host = Uri.parse(service.url).host
        val array = runCatching { JSONArray(payload) }.getOrNull() ?: return emptyList()
        return buildList {
            for (index in 0 until minOf(array.length(), MAX_RESULTS)) {
                val item = array.optJSONObject(index) ?: continue
                val contentUrl = item.optString("contentUrl").take(2_000)
                if (!adapter.isContentUrl(contentUrl)) continue
                if (!Uri.parse(contentUrl).host.equals(host, true)) continue
                add(
                    SearchResult(
                        title = item.optString("title").trim().take(140)
                            .ifBlank { service.name },
                        kind = if (item.optString("kind").equals("SERIES", true)) {
                            CatalogKind.SERIES
                        } else {
                            CatalogKind.MOVIE
                        },
                        contentUrl = contentUrl,
                        posterUrl = item.optString("posterUrl").take(2_000)
                            .takeIf { it.startsWith("http", true) }.orEmpty(),
                        serviceId = service.id
                    )
                )
            }
        }.distinctBy { it.contentUrl }
    }

    private fun script(service: StreamingService, query: String): String {
        val encoded = JSONObject.quote(query)
        return if (service.id == PARSI_ID) parsiScript(encoded) else filmRoozScript(encoded)
    }

    private companion object {
        const val PARSI_ID = "parsiflix"
        const val MAX_RESULTS = 24

        /** The account's own catalog endpoint, matched on title. Persian titles only. */
        fun parsiScript(query: String) = """
            (async function() {
              try {
                var token = localStorage.getItem('accessToken');
                if (!token) { AminSearch.failed('parsiflix', 'Login required'); return; }
                try { token = JSON.parse(token); } catch (_) {}
                var response = await fetch(
                  'https://api.parsiflix.com/medias?title=' +
                    encodeURIComponent($query) + '&page=1&size=24',
                  {
                    headers: {
                      Authorization: 'Bearer ' + token,
                      appVersion: '1.0.0',
                      Accept: 'application/json, text/plain, */*'
                    }
                  }
                );
                if (!response.ok) {
                  AminSearch.failed('parsiflix', 'Login expired');
                  return;
                }
                var data = await response.json();
                var items = (data.elements || []).map(function(item) {
                  var kind = String(item.type || '').toUpperCase() === 'SERIES'
                    ? 'SERIES' : 'MOVIE';
                  return {
                    title: String(item.title || '').slice(0, 140),
                    kind: kind,
                    contentUrl: location.origin + '/medias/' +
                      (kind === 'SERIES' ? 'series' : 'movies') + '/' + item.id,
                    posterUrl: item.coverLink || item.thumbnailLink || ''
                  };
                });
                AminSearch.results('parsiflix', JSON.stringify(items));
              } catch (error) {
                AminSearch.failed('parsiflix', 'Service unavailable');
              }
            })();
        """.trimIndent()

        /** The site's own `?s=` results page. Matches English and Persian titles. */
        fun filmRoozScript(query: String) = """
            (async function() {
              try {
                var response = await fetch(
                  location.origin + '/?s=' + encodeURIComponent($query),
                  {credentials: 'include'}
                );
                if (!response.ok) {
                  AminSearch.failed('filmrooz', 'Service unavailable');
                  return;
                }
                var doc = new DOMParser().parseFromString(
                  await response.text(), 'text/html'
                );
                var seen = {};
                var items = [];
                Array.from(doc.querySelectorAll('a[href*="/post/"]')).forEach(
                  function(anchor) {
                    var href = anchor.getAttribute('href') || '';
                    if (!href) return;
                    var url = new URL(href, location.origin);
                    if (url.origin !== location.origin) return;
                    var match = url.pathname.match(/^\/post\/(film|series)\/(\d+)\//);
                    if (!match || seen[url.pathname]) return;
                    seen[url.pathname] = true;
                    var card = anchor.closest('.postMeta') || anchor.parentElement;
                    var titleLink = card
                      ? Array.from(card.querySelectorAll('a[href*="/post/"]')).find(
                          function(a) {
                            var text = (a.textContent || '').trim();
                            if (text.length < 2 || /^قسمت/.test(text)) return false;
                            var target = new URL(
                              a.getAttribute('href') || '', location.origin
                            );
                            return target.pathname === url.pathname;
                          }
                        )
                      : null;
                    var title = titleLink ? titleLink.textContent.trim() : '';
                    if (!title) {
                      var slug = url.pathname.split('/').filter(Boolean)[3] || '';
                      title = decodeURIComponent(slug)
                        .replace(/[-_]+/g, ' ')
                        .replace(/\b[a-z]/g, function(c) { return c.toUpperCase(); })
                        .trim();
                    }
                    var image = anchor.querySelector('img');
                    var candidate = image
                      ? (image.getAttribute('data-src') || image.getAttribute('src') || '')
                      : '';
                    var poster = candidate && !/^data:/i.test(candidate)
                      ? new URL(candidate, location.origin).href : '';
                    items.push({
                      title: title.slice(0, 140),
                      kind: match[1] === 'series' ? 'SERIES' : 'MOVIE',
                      contentUrl: url.origin + url.pathname,
                      posterUrl: poster
                    });
                  }
                );
                AminSearch.results('filmrooz', JSON.stringify(items.slice(0, 24)));
              } catch (error) {
                AminSearch.failed('filmrooz', 'Service unavailable');
              }
            })();
        """.trimIndent()
    }
}
