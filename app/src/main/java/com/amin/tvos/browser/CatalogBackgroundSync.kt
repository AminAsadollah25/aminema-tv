package com.amin.tvos.browser

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.amin.tvos.AminTvApp
import com.amin.tvos.data.model.CatalogItem
import com.amin.tvos.data.model.CatalogKind
import com.amin.tvos.data.model.CatalogSection
import com.amin.tvos.data.model.PlaybackSession
import com.amin.tvos.data.model.ResumeStrategy
import com.amin.tvos.data.model.StreamingService
import com.amin.tvos.data.model.UserAgentMode
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * Refreshes one provider rail at a time without opening a modal Activity.
 *
 * Tiny off-screen WebViews reuse the user's normal signed-in browser session. Cached cards stay
 * visible and Home remains fully interactive while a provider independently refreshes.
 */
class CatalogBackgroundSync(
    private val activity: ComponentActivity,
    private val app: AminTvApp
) {
    private data class Slot(
        val webView: WebView,
        val timeout: Runnable
    )

    private val handler = Handler(Looper.getMainLooper())
    private val slots = mutableMapOf<String, Slot>()

    fun refresh(serviceId: String) {
        if (serviceId !in SUPPORTED_IDS || slots.containsKey(serviceId)) return
        val service = app.servicesRepository.findById(serviceId)
        if (service == null) {
            activity.lifecycleScope.launch {
                app.catalogRepository.recordError(serviceId, "سرویس تنظیم نشده است")
            }
            return
        }

        app.catalogRepository.setRefreshing(serviceId, true)
        createWebView(service)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(service: StreamingService) {
        val serviceId = service.id
        val serviceHost = Uri.parse(service.url).host.orEmpty()
        var scriptStarted = false
        val webView = WebView(activity).apply {
            alpha = 0.01f
            isFocusable = false
            isFocusableInTouchMode = false
            isClickable = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            overScrollMode = View.OVER_SCROLL_NEVER
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                @Suppress("DEPRECATION")
                databaseEnabled = true
                cacheMode = WebSettings.LOAD_DEFAULT
                allowFileAccess = false
                allowContentAccess = false
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                val mode = service.userAgent
                    ?.let { value -> UserAgentMode.entries.firstOrNull { it.name == value } }
                userAgentString = mode?.value ?: WebSettings.getDefaultUserAgent(activity)
            }
        }
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }
        webView.addJavascriptInterface(CatalogBridge(serviceId, webView), "AminCatalog")
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                CookieManager.getInstance().flush()
                if (
                    scriptStarted ||
                    !Uri.parse(url).host.equals(serviceHost, ignoreCase = true)
                ) return
                scriptStarted = true
                val delay = if (serviceId == PARSI_ID) 1_500L else 1_200L
                view.postDelayed({
                    if (slots[serviceId]?.webView === view) {
                        view.evaluateJavascript(scriptFor(serviceId), null)
                    }
                }, delay)
            }
        }

        val timeout = Runnable { fail(serviceId, webView, "timeout") }
        slots[serviceId] = Slot(webView, timeout)
        activity.addContentView(
            webView,
            FrameLayout.LayoutParams(2, 2, Gravity.BOTTOM or Gravity.END)
        )
        handler.postDelayed(timeout, TIMEOUT_MS)
        webView.loadUrl(
            if (serviceId == FILMROOZ_ID) {
                service.url.trimEnd('/') + "/user/panel/"
            } else {
                service.url
            }
        )
    }

    private inner class CatalogBridge(
        private val expectedServiceId: String,
        private val source: WebView
    ) {
        @JavascriptInterface
        fun section(serviceId: String?, payload: String?) {
            if (serviceId != expectedServiceId) return
            handler.post {
                if (slots[expectedServiceId]?.webView !== source) return@post
                val service = app.servicesRepository.findById(expectedServiceId)
                if (service == null) {
                    fail(expectedServiceId, source, "سرویس تنظیم نشده است")
                    return@post
                }
                val section = parseSection(service, payload.orEmpty())
                val accountSessions = parseAccountSessions(service, payload.orEmpty())
                activity.lifecycleScope.launch {
                    app.catalogRepository.save(section)
                    // `null` means the account row was unavailable, so stale Continue data
                    // is preserved. An explicit empty array is authoritative for this service.
                    accountSessions?.let {
                        app.libraryRepository.syncAccountSessions(service.id, it)
                    }
                    complete(expectedServiceId, source)
                }
            }
        }

        @JavascriptInterface
        fun failed(serviceId: String?, reason: String?) {
            if (serviceId != expectedServiceId) return
            handler.post {
                fail(expectedServiceId, source, reason.orEmpty().take(80))
            }
        }
    }

    private fun fail(serviceId: String, source: WebView, reason: String) {
        if (slots[serviceId]?.webView !== source) return
        activity.lifecycleScope.launch {
            app.catalogRepository.recordError(
                serviceId,
                reason.ifBlank { "بروزرسانی انجام نشد" }
            )
            complete(serviceId, source)
        }
    }

    private fun complete(serviceId: String, source: WebView) {
        val slot = slots[serviceId] ?: return
        if (slot.webView !== source) return
        slots.remove(serviceId)
        handler.removeCallbacks(slot.timeout)
        app.catalogRepository.setRefreshing(serviceId, false)
        source.apply {
            stopLoading()
            removeJavascriptInterface("AminCatalog")
            (parent as? ViewGroup)?.removeView(this)
            destroy()
        }
    }

    fun destroy() {
        slots.toMap().forEach { (serviceId, slot) ->
            complete(serviceId, slot.webView)
        }
        handler.removeCallbacksAndMessages(null)
    }

    /**
     * Every bridge value is untrusted page output. Only normal same-host content pages survive.
     */
    private fun parseSection(service: StreamingService, payload: String): CatalogSection {
        val adapter = ServiceAdapter(service)
        val serviceHost = Uri.parse(service.url).host
        val root = runCatching { JSONObject(payload) }.getOrNull()
            ?: return CatalogSection(serviceId = service.id, error = "پاسخ نامعتبر")

        fun list(key: String): List<CatalogItem> {
            val array = root.optJSONArray(key) ?: return emptyList()
            return buildList {
                for (index in 0 until minOf(array.length(), MAX_ITEMS)) {
                    val entry = array.optJSONObject(index) ?: continue
                    val contentUrl = entry.optString("contentUrl").take(2_000)
                    if (!adapter.isContentUrl(contentUrl)) continue
                    if (!Uri.parse(contentUrl).host.equals(serviceHost, true)) continue
                    val poster = entry.optString("posterUrl").take(2_000)
                        .takeIf { it.startsWith("http", ignoreCase = true) }
                        .orEmpty()
                    val kind = if (
                        entry.optString("kind").equals("SERIES", true)
                    ) CatalogKind.SERIES else CatalogKind.MOVIE
                    add(
                        CatalogItem(
                            title = entry.optString("title").trim().take(140)
                                .ifBlank { service.name },
                            kind = kind,
                            contentUrl = contentUrl,
                            posterUrl = poster,
                            serviceId = service.id,
                            episodeLabel = entry.optString("episodeLabel")
                                .replace(Regex("""\s+"""), " ")
                                .trim()
                                .take(72),
                            summary = entry.optString("summary")
                                .replace(Regex("""\s+"""), " ")
                                .trim()
                                .take(420),
                            year = entry.optString("year")
                                .replace(Regex("""[^0-9۰-۹]"""), "")
                                .take(4),
                            genres = entry.optJSONArray("genres")
                                ?.let { genres ->
                                    buildList {
                                        for (genreIndex in 0 until minOf(genres.length(), 4)) {
                                            genres.optString(genreIndex)
                                                .replace(Regex("""\s+"""), " ")
                                                .trim()
                                                .replace(
                                                    Regex(
                                                        """^(?:ژانر|Genre)\s*:\s*""",
                                                        RegexOption.IGNORE_CASE
                                                    ),
                                                    ""
                                                )
                                                .take(28)
                                                .takeIf { it.isNotBlank() }
                                                ?.let(::add)
                                        }
                                    }
                                }
                                .orEmpty(),
                            rating = entry.optString("rating")
                                .replace(Regex("""[^0-9۰-۹.٫]"""), "")
                                .replace('٫', '.')
                                .take(5),
                            runtime = entry.optString("runtime")
                                .replace(Regex("""\s+"""), " ")
                                .trim()
                                .take(24)
                        )
                    )
                }
            }.distinctBy { it.contentUrl }
        }

        return CatalogSection(
            serviceId = service.id,
            all = list("all"),
            movies = list("movies"),
            series = list("series"),
            popularSeries = list("popularSeries"),
            syncedAt = System.currentTimeMillis(),
            error = ""
        )
    }

    /**
     * Parses only normal account Continue/Recent detail links bundled with a catalog refresh.
     * No playback URL, media URL, token or protected stream value crosses the JS bridge.
     */
    private fun parseAccountSessions(
        service: StreamingService,
        payload: String
    ): List<PlaybackSession>? {
        val root = runCatching { JSONObject(payload) }.getOrNull() ?: return null
        if (!root.has("accountItems")) return null
        val array: JSONArray = root.optJSONArray("accountItems") ?: return emptyList()
        val adapter = ServiceAdapter(service)
        val serviceHost = Uri.parse(service.url).host
        val now = System.currentTimeMillis()
        val actionPatterns = if (service.id == FILMROOZ_ID) {
            listOf("پخش آنلاین", "پخش انلاین", "تماشای آنلاین", "Watch Online")
        } else {
            emptyList()
        }
        return buildList {
            for (index in 0 until minOf(array.length(), MAX_ACCOUNT_ITEMS)) {
                val item = array.optJSONObject(index) ?: continue
                val contentUrl = item.optString("contentUrl").take(2_000)
                if (!adapter.isContentUrl(contentUrl)) continue
                if (!Uri.parse(contentUrl).host.equals(serviceHost, true)) continue
                add(
                    PlaybackSession(
                        id = "",
                        title = item.optString("title").trim().take(140)
                            .ifBlank { service.name },
                        subtitle = "Synced from account",
                        posterUrl = item.optString("posterUrl").take(2_000),
                        serviceId = service.id,
                        serviceName = service.name,
                        contentUrl = contentUrl,
                        playbackUrl = "",
                        lastPlayed = now - index,
                        resumePosition = item.optLong("resumePosition")
                            .coerceIn(0L, 86_400_000L),
                        duration = 0L,
                        resumeStrategy = ResumeStrategy.CLICK_SITE_CONTINUE,
                        actionButtonTextPatterns = actionPatterns,
                        syncedFromAccount = true
                    )
                )
            }
        }
    }

    private companion object {
        const val PARSI_ID = "parsiflix"
        const val FILMROOZ_ID = "filmrooz"
        const val MAX_ITEMS = 24
        const val MAX_ACCOUNT_ITEMS = 20
        const val TIMEOUT_MS = 35_000L
        val SUPPORTED_IDS = setOf(PARSI_ID, FILMROOZ_ID)

        fun scriptFor(serviceId: String): String =
            if (serviceId == PARSI_ID) PARSI_SCRIPT else FILMROOZ_SCRIPT

        val PARSI_SCRIPT = """
            (async function() {
              function map(item) {
                var kind = String(item.type || '').toUpperCase() === 'SERIES'
                  ? 'SERIES' : 'MOVIE';
                var path = kind === 'SERIES' ? 'series' : 'movies';
                function text(value) {
                  if (value == null) return '';
                  if (typeof value === 'string' || typeof value === 'number') {
                    return String(value);
                  }
                  return String(value.name || value.title || value.label || '');
                }
                var rawGenres = item.genres || item.genre || item.categories || [];
                if (!Array.isArray(rawGenres)) rawGenres = [rawGenres];
                var published = text(
                  item.releaseYear || item.year || item.publishedAt || item.published
                );
                var yearMatch = published.match(/(?:19|20)\d{2}/);
                return {
                  title: String(item.title || '').slice(0, 140),
                  kind: kind,
                  contentUrl: location.origin + '/medias/' + path + '/' + item.id,
                  posterUrl: item.coverLink || item.thumbnailLink || '',
                  episodeLabel: '',
                  summary: text(
                    item.description || item.summary || item.overview || item.plot
                  ).replace(/\s+/g, ' ').slice(0, 420),
                  year: yearMatch ? yearMatch[0] : '',
                  genres: rawGenres.map(text).filter(Boolean).slice(0, 4),
                  rating: text(
                    item.imdbRating || item.rating || item.rate
                  ).replace(/[^0-9.]/g, '').slice(0, 4),
                  runtime: text(item.runtime || item.duration || '')
                };
              }
              try {
                var token = localStorage.getItem('accessToken');
                if (!token) { AminCatalog.failed('parsiflix', 'Login required'); return; }
                try { token = JSON.parse(token); } catch (_) {}
                var headers = {
                  Authorization: 'Bearer ' + token,
                  appVersion: '1.0.0',
                  Accept: 'application/json, text/plain, */*'
                };
                var homeResponse = await fetch('https://api.parsiflix.com/app/home', {
                  headers: headers
                });
                if (!homeResponse.ok) {
                  AminCatalog.failed('parsiflix', 'Login expired');
                  return;
                }
                var home = await homeResponse.json();
                var latest = (home.sections || []).find(function(section) {
                  return /جدیدترین/.test(section.title || '');
                });
                var all = ((latest && latest.items) || []).slice(0, 24).map(map);
                var accountSection = (home.sections || []).find(function(section) {
                  return /ادامه\s*تماشا|continue\s*watching/i.test(section.title || '');
                });
                var accountItems = ((accountSection && accountSection.items) || [])
                  .slice(0, 20).map(function(item) {
                    var kind = String(item.type || '').toUpperCase() === 'MOVIE'
                      ? 'movies' : 'series';
                    return {
                      title: String(item.title || '').slice(0, 140),
                      contentUrl: location.origin + '/medias/' + kind + '/' + item.id,
                      posterUrl: item.coverLink || item.thumbnailLink || '',
                      resumePosition: 0
                    };
                  });

                async function typed(type) {
                  var response = await fetch(
                    'https://api.parsiflix.com/medias?type=' + type + '&page=1&size=24',
                    {headers: headers}
                  );
                  if (!response.ok) return [];
                  var data = await response.json();
                  return (data.elements || []).slice(0, 24).map(map);
                }
                var movies = await typed('MOVIE');
                var series = await typed('SERIES');
                if (!all.length) {
                  all = movies.slice(0, 12).concat(series.slice(0, 12));
                }
                AminCatalog.section('parsiflix', JSON.stringify({
                  all: all,
                  movies: movies,
                  series: series,
                  popularSeries: [],
                  accountItems: accountItems
                }));
              } catch (error) {
                AminCatalog.failed('parsiflix', 'Service unavailable');
              }
            })();
        """.trimIndent()

        val FILMROOZ_SCRIPT = """
            (async function() {
              function parse(html, expectedKind) {
                var doc = new DOMParser().parseFromString(html, 'text/html');
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
                    var kind = match[1] === 'series' ? 'SERIES' : 'MOVIE';
                    if (expectedKind && kind !== expectedKind) return;
                    seen[url.pathname] = true;
                    var card = anchor.closest('.postMeta') || anchor.parentElement;
                    var links = card
                      ? Array.from(card.querySelectorAll('a[href*="/post/"]')) : [];
                    var titleLink = links.find(function(a) {
                      var text = (a.textContent || '').replace(/\s+/g, ' ').trim();
                      if (text.length < 2 || /^قسمت/.test(text)) return false;
                      var target = new URL(a.getAttribute('href') || '', location.origin);
                      return target.pathname === url.pathname;
                    });
                    var title = titleLink ? titleLink.textContent.trim() : '';
                    if (!title) {
                      var slug = url.pathname.split('/').filter(Boolean)[3] || '';
                      title = decodeURIComponent(slug)
                        .replace(/[-_]+/g, ' ')
                        .replace(/\b[a-z]/g, function(c) { return c.toUpperCase(); })
                        .trim();
                    }
                    // FilmRooz renders the publication status as plain text beside the
                    // title, not as a link. Read only this card — never a parent rail —
                    // so one show's episode cannot leak onto another show's card.
                    var status = card
                      ? (card.textContent || '').replace(/\s+/g, ' ').trim() : '';
                    var episodeMatch = status.match(
                      /قسمت\s*[۰-۹0-9]+\s*(?:فصل\s+[^\s،؛\-]+)?/
                    );
                    var image = anchor.querySelector('img');
                    var candidate = image
                      ? (image.getAttribute('data-src') || image.getAttribute('src') || '')
                      : '';
                    var poster = candidate && !/^data:/i.test(candidate)
                      ? new URL(candidate, location.origin).href : '';
                    var yearMatch = title.match(/(?:19|20)\d{2}/) ||
                      status.match(/(?:19|20)\d{2}/);
                    function field(pattern) {
                      return card ? Array.from(card.querySelectorAll('.col-12')).find(
                        function(node) {
                          return pattern.test((node.textContent || '').trim());
                        }
                      ) : null;
                    }
                    var ratingNode = field(/از\s*۱۰|IMDb/i);
                    var ratingMatch = ratingNode
                      ? (ratingNode.textContent || '').match(
                          /([۰-۹0-9]+(?:[.٫][۰-۹0-9]+)?)\s*از\s*۱۰/
                        )
                      : null;
                    var runtimeNode = field(/دقیقه|\bmin\b/i);
                    var runtimeMatch = runtimeNode
                      ? (runtimeNode.textContent || '').match(
                          /([۰-۹0-9]{2,3}\s*(?:دقیقه|min))/i
                        )
                      : null;
                    var genreNode = field(/^(?:ژانر|Genre)\s*:/i);
                    var genres = [];
                    if (genreNode) {
                      var genreHtml = genreNode.innerHTML.replace(
                        /<spl[^>]*><\/spl>/gi, '|'
                      );
                      var holder = document.createElement('div');
                      holder.innerHTML = genreHtml;
                      genres = (holder.textContent || '')
                        .replace(/\s+/g, ' ').trim()
                        .replace(/^(?:ژانر|Genre)\s*:\s*/i, '')
                        .split('|')
                        .map(function(value) {
                          return value.replace(/\s+/g, ' ').trim();
                        }).filter(Boolean).slice(0, 4);
                    }
                    var summary = '';
                    if (card) {
                      var summaryNode = card.querySelector(
                        '.text-justify.mt-2.p-2,.postExcerpt,.excerpt,.summary,' +
                        '[class*="excerpt" i],[class*="summary" i]'
                      );
                      var candidateSummary = summaryNode
                        ? (summaryNode.textContent || '').replace(/\s+/g, ' ').trim()
                        : '';
                      if (candidateSummary.length >= 35) {
                        summary = candidateSummary.slice(0, 420);
                      }
                    }
                    items.push({
                      title: title.slice(0, 140),
                      kind: kind,
                      contentUrl: url.origin + url.pathname,
                      posterUrl: poster,
                      episodeLabel: episodeMatch ? episodeMatch[0].trim() : '',
                      summary: summary,
                      year: yearMatch ? yearMatch[0] : '',
                      genres: genres,
                      rating: ratingMatch ? ratingMatch[1] : '',
                      runtime: runtimeMatch ? runtimeMatch[1] : ''
                    });
                  }
                );
                return items.slice(0, 24);
              }
              async function page(path, kind) {
                try {
                  var response = await fetch(location.origin + path, {
                    credentials: 'include'
                  });
                  if (!response.ok) return [];
                  return parse(await response.text(), kind);
                } catch (_) { return []; }
              }
              function unique(items) {
                var seen = {};
                return items.filter(function(item) {
                  if (seen[item.contentUrl]) return false;
                  seen[item.contentUrl] = true;
                  return true;
                });
              }
              async function recentAccountItems() {
                function findBox() {
                  var heading = Array.from(
                    document.querySelectorAll('h1,h2,h3,h4,h5')
                  ).find(function(el) {
                    return /مشاهدات\s*اخیر|recent/i.test(el.innerText || '');
                  });
                  return heading && heading.parentElement
                    ? heading.parentElement.nextElementSibling : null;
                }
                var box = findBox();
                for (var attempt = 0; !box && attempt < 4; attempt++) {
                  await new Promise(function(resolve) { setTimeout(resolve, 900); });
                  box = findBox();
                }
                if (!box) return null;
                var seen = {};
                return Array.from(box.querySelectorAll('a[href*="/post/"]'))
                  .map(function(anchor) {
                    var url = new URL(anchor.href, location.href);
                    if (seen[url.pathname]) return null;
                    seen[url.pathname] = true;
                    var parts = url.pathname.split('/').filter(Boolean);
                    var contentId = parts.length > 2 ? parts[2] : '';
                    var slug = parts.length > 3 ? decodeURIComponent(parts[3]) : '';
                    var title = slug.replace(/[-_]+/g, ' ').replace(
                      /\b[a-z]/g, function(letter) {
                        return letter.toUpperCase();
                      }
                    ).trim();
                    var image = anchor.querySelector('img');
                    var candidate = image ? (
                      image.dataset.src ||
                      image.dataset.lazySrc ||
                      image.dataset.original ||
                      image.currentSrc ||
                      image.src ||
                      ''
                    ) : '';
                    var poster = candidate && !/^data:/i.test(candidate)
                      ? new URL(candidate, location.href).href : '';
                    var resumeKey = Object.keys(localStorage).find(function(key) {
                      return key.indexOf('rvd_') === 0 &&
                        contentId && key.endsWith('_' + contentId);
                    });
                    var seconds = resumeKey
                      ? Number(localStorage.getItem(resumeKey)) : 0;
                    return {
                      title: title || 'فیلم خارجی',
                      contentUrl: url.origin + url.pathname,
                      posterUrl: poster,
                      resumePosition: isFinite(seconds)
                        ? Math.max(0, Math.round(seconds * 1000)) : 0
                    };
                  }).filter(Boolean).slice(0, 20);
              }
              try {
                if (!document.querySelector('a[href*="/user/panel"]')) {
                  AminCatalog.failed('filmrooz', 'Login required');
                  return;
                }
                // Reconcile account history and public catalog metadata in the same
                // invisible provider pass. Home never gets replaced by a sync screen.
                var accountItems = await recentAccountItems();
                var movies = await page('/archive/category/new-films/', 'MOVIE');
                // Release-ordered: old series return to the top whenever a new episode lands.
                var series = await page('/archive/series/', 'SERIES');
                // Curated/trending, intentionally separate from new episode releases.
                var popularSeries = await page(
                  '/archive/playlist/show/most-popular-tv-shows/', 'SERIES'
                );
                if (!movies.length && !series.length && !popularSeries.length) {
                  AminCatalog.failed('filmrooz', 'Empty catalog');
                  return;
                }
                var all = [];
                for (var i = 0; i < Math.max(movies.length, series.length); i++) {
                  if (movies[i]) all.push(movies[i]);
                  if (series[i]) all.push(series[i]);
                }
                var payload = {
                  all: unique(all).slice(0, 24),
                  movies: unique(movies),
                  series: unique(series),
                  popularSeries: unique(popularSeries)
                };
                if (accountItems !== null) payload.accountItems = accountItems;
                AminCatalog.section('filmrooz', JSON.stringify(payload));
              } catch (error) {
                AminCatalog.failed('filmrooz', 'Service unavailable');
              }
            })();
        """.trimIndent()
    }
}
