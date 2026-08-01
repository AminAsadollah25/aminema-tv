package com.amin.tvos.ui.spotlight

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import com.amin.tvos.data.PublicTitleMetadataEnricher
import com.amin.tvos.data.model.CatalogKind
import com.amin.tvos.data.model.PersonRef
import com.amin.tvos.data.model.SpotlightItem
import com.amin.tvos.data.model.TitleMetadata
import org.json.JSONArray
import org.json.JSONObject

/**
 * Completes missing Iranian credits from Sheyda's ordinary public title UI.
 *
 * Sheyda's public HTML is only an SPA shell, so a plain HTTP parser cannot see the same
 * director/cast fields that a visitor sees. This tiny off-screen WebView performs one normal
 * public title search, accepts only an exact title + movie/series match, then reads the visible
 * detail labels. It never signs in, opens a player or reads a media URL.
 */
class SheydaMetadataLoader(
    private val activity: ComponentActivity,
    private val onLoaded: (TitleMetadata) -> Unit,
    private val onFailed: () -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private var webView: WebView? = null
    private var item: SpotlightItem? = null
    private var delivered = false
    private val timeout = Runnable { finish(null) }

    @SuppressLint("SetJavaScriptEnabled")
    fun load(sourceItem: SpotlightItem) {
        destroy()
        if (!isIranian(sourceItem) || sourceItem.title.isBlank()) {
            onFailed()
            return
        }
        item = sourceItem
        delivered = false

        val view = WebView(activity).apply {
            alpha = 0.01f
            isFocusable = false
            isFocusableInTouchMode = false
            isClickable = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            overScrollMode = View.OVER_SCROLL_NEVER
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                cacheMode = WebSettings.LOAD_DEFAULT
                allowFileAccess = false
                allowContentAccess = false
                mediaPlaybackRequiresUserGesture = true
                userAgentString = WebSettings.getDefaultUserAgent(activity)
            }
        }
        webView = view
        view.addJavascriptInterface(Bridge(view), BRIDGE_NAME)
        view.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean = !request.url.host.equals(HOST, ignoreCase = true)

            override fun onPageFinished(source: WebView, url: String) {
                if (source !== webView || delivered) return
                val uri = Uri.parse(url)
                if (!uri.host.equals(HOST, true)) return
                when {
                    uri.path == "/search" -> source.postDelayed(
                        { if (source === webView && !delivered) search(source) },
                        1_250L
                    )
                    uri.path.orEmpty().startsWith("/p/") -> source.postDelayed(
                        { if (source === webView && !delivered) extract(source) },
                        1_650L
                    )
                }
            }
        }
        activity.addContentView(
            view,
            FrameLayout.LayoutParams(2, 2, Gravity.BOTTOM or Gravity.END)
        )
        handler.postDelayed(timeout, TIMEOUT_MS)
        view.loadUrl(SEARCH_URL)
    }

    private inner class Bridge(private val source: WebView) {
        @JavascriptInterface
        fun open(path: String?) {
            handler.post {
                if (source !== webView || delivered) return@post
                val uri = Uri.parse(path.orEmpty())
                val safeUrl = when {
                    uri.isRelative && uri.path.orEmpty().startsWith("/p/") ->
                        "https://$HOST${uri.path}"
                    uri.host.equals(HOST, true) && uri.path.orEmpty().startsWith("/p/") ->
                        uri.toString()
                    else -> ""
                }
                if (safeUrl.isBlank()) finish(null) else source.loadUrl(safeUrl)
            }
        }

        @JavascriptInterface
        fun result(payload: String?) {
            handler.post {
                if (source !== webView || delivered) return@post
                finish(parse(payload.orEmpty()))
            }
        }

        @JavascriptInterface
        fun failed() {
            handler.post { if (source === webView && !delivered) finish(null) }
        }
    }

    private fun search(source: WebView) {
        val current = item ?: return finish(null)
        val script = SEARCH_SCRIPT
            .replace("__TITLE__", JSONObject.quote(cleanTitle(current.title)))
            .replace("__KIND__", JSONObject.quote(current.kind.name))
        source.evaluateJavascript(script, null)
    }

    private fun extract(source: WebView) {
        source.evaluateJavascript(EXTRACT_SCRIPT, null)
    }

    private fun parse(payload: String): TitleMetadata? {
        val current = item ?: return null
        val root = runCatching { JSONObject(payload) }.getOrNull() ?: return null
        if (titleScore(current.title, root.optString("title")) < 0.92) return null

        fun people(key: String): List<PersonRef> {
            val values = root.optJSONArray(key) ?: JSONArray()
            return buildList {
                for (index in 0 until minOf(values.length(), 10)) {
                    val value = values.optJSONObject(index)
                    val name = value?.optString("name")
                        .orEmpty()
                        .replace(Regex("""\s+"""), " ")
                        .trim(' ', '،')
                        .take(80)
                    if (name.isBlank()) continue
                    add(
                        PersonRef(
                            name = name,
                            providerId = value?.optString("providerId").orEmpty().take(80),
                            profileUrl = value?.optString("profileUrl")
                                .orEmpty()
                                .takeIf { Uri.parse(it).host.equals(HOST, true) }
                                .orEmpty()
                        )
                    )
                }
            }.distinctBy { it.name }
        }

        val year = root.optString("year").filter(Char::isDigit).take(4)
        val genres = root.optJSONArray("genres") ?: JSONArray()
        val now = System.currentTimeMillis()
        return TitleMetadata(
            contentUrl = current.contentUrl,
            backdropUrl = root.optString("backdropUrl")
                .takeIf { Uri.parse(it).host.equals(STATIC_HOST, true) }
                .orEmpty(),
            summary = root.optString("summary").clean(620),
            year = year.takeIf { it.toIntOrNull() in 1300..1499 }.orEmpty(),
            genres = buildList {
                for (index in 0 until minOf(genres.length(), 4)) {
                    genres.optString(index).clean(35).takeIf { it.isNotBlank() }?.let(::add)
                }
            }.distinct(),
            country = "ایران",
            language = "فارسی",
            directors = people("directors").take(3),
            cast = people("cast").take(10),
            externalLookupAt = now,
            externalLookupVersion = PublicTitleMetadataEnricher.LOOKUP_VERSION,
            fetchedAt = now
        ).takeIf {
            it.summary.isNotBlank() || it.directors.isNotEmpty() || it.cast.isNotEmpty()
        }
    }

    private fun String.clean(limit: Int): String =
        replace(Regex("""\s+"""), " ").trim().take(limit)

    private fun cleanTitle(value: String): String = value
        .replace(Regex("""\s*[（(]\s*\d{4}\s*[)）]\s*$"""), "")
        .replace(Regex("""\s+"""), " ")
        .trim()
        .take(120)

    private fun titleScore(expected: String, candidate: String): Double {
        val left = normalize(expected)
        val right = normalize(candidate)
        if (left.isBlank() || right.isBlank()) return 0.0
        if (left == right) return 1.0
        if (left.contains(right) || right.contains(left)) return 0.94
        return 0.0
    }

    private fun normalize(value: String): String = cleanTitle(value)
        .lowercase()
        .replace('ي', 'ی')
        .replace('ك', 'ک')
        .replace(Regex("""(?:فیلم|سریال|مجموعه)"""), " ")
        .replace(Regex("""[^\p{L}\p{N}]+"""), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()

    private fun isIranian(sourceItem: SpotlightItem): Boolean =
        sourceItem.serviceId.equals("parsiflix", true) ||
            sourceItem.country.contains("ایران") || sourceItem.country.contains("Iran", true)

    private fun finish(metadata: TitleMetadata?) {
        if (delivered) return
        delivered = true
        handler.removeCallbacks(timeout)
        if (metadata == null) onFailed() else onLoaded(metadata)
        releaseWebView()
    }

    fun destroy() {
        handler.removeCallbacksAndMessages(null)
        delivered = true
        releaseWebView()
    }

    private fun releaseWebView() {
        webView?.apply {
            stopLoading()
            removeJavascriptInterface(BRIDGE_NAME)
            (parent as? ViewGroup)?.removeView(this)
            destroy()
        }
        webView = null
    }

    private companion object {
        const val HOST = "www.sheyda.com"
        const val STATIC_HOST = "static-s.sheyda.com"
        const val SEARCH_URL = "https://www.sheyda.com/search"
        const val BRIDGE_NAME = "AminSheyda"
        const val TIMEOUT_MS = 24_000L

        val SEARCH_SCRIPT = """
            (function() {
              if (window.__aminemaSearchStarted) return;
              window.__aminemaSearchStarted = true;
              var wanted = __TITLE__;
              var kind = __KIND__;
              function clean(value) {
                return String(value || '')
                  .replace(/[ي]/g, 'ی').replace(/[ك]/g, 'ک')
                  .replace(/(?:فیلم|سریال|مجموعه)/g, ' ')
                  .replace(/[^\p{L}\p{N}]+/gu, ' ')
                  .replace(/\s+/g, ' ').trim().toLowerCase();
              }
              var input = document.querySelector('input[placeholder*="جستجو"]');
              if (!input) { AminSheyda.failed(); return; }
              var setter = Object.getOwnPropertyDescriptor(
                window.HTMLInputElement.prototype, 'value'
              ).set;
              setter.call(input, wanted);
              input.dispatchEvent(new Event('input', {bubbles: true}));
              input.dispatchEvent(new Event('change', {bubbles: true}));
              input.dispatchEvent(new KeyboardEvent('keydown', {
                key: 'Enter', code: 'Enter', keyCode: 13, which: 13, bubbles: true
              }));
              input.dispatchEvent(new KeyboardEvent('keyup', {
                key: 'Enter', code: 'Enter', keyCode: 13, which: 13, bubbles: true
              }));

              var attempts = 0;
              var timer = setInterval(function() {
                attempts += 1;
                var wantedClean = clean(wanted);
                var links = Array.from(document.querySelectorAll('a[href^="/p/"]'));
                var exact = links.find(function(link) {
                  var text = String(link.textContent || '').replace(/\s+/g, ' ').trim();
                  var kindOkay = kind === 'SERIES' ? /سریال/.test(text) : /فیلم/.test(text);
                  return kindOkay && clean(text) === wantedClean;
                });
                if (exact) {
                  clearInterval(timer);
                  AminSheyda.open(exact.getAttribute('href'));
                } else if (attempts >= 18) {
                  clearInterval(timer);
                  AminSheyda.failed();
                }
              }, 700);
            })();
        """.trimIndent()

        val EXTRACT_SCRIPT = """
            (function() {
              function clean(value) {
                return String(value || '').replace(/\s+/g, ' ').trim();
              }
              function people(label) {
                var group = Array.from(document.querySelectorAll('.product-casts')).find(
                  function(node) {
                    var heading = node.querySelector('.tag-title');
                    return heading && clean(heading.textContent).indexOf(label) >= 0;
                  }
                );
                if (!group) return [];
                return Array.from(group.querySelectorAll('a[href^="/a/"]')).map(
                  function(link) {
                    var nameNode = link.querySelector('span:not(.item-separator)');
                    var name = clean(nameNode ? nameNode.textContent : link.textContent)
                      .replace(/[،,]+$/g, '');
                    var href = link.getAttribute('href') || '';
                    return {
                      name: name.slice(0, 80),
                      providerId: href.replace(/^\/a\//, '').slice(0, 80),
                      profileUrl: new URL(href, location.origin).href
                    };
                  }
                ).filter(function(person) { return person.name; });
              }
              var title = clean(
                (document.querySelector('h1.product-title') || {}).textContent
              );
              if (!title) { AminSheyda.failed(); return; }
              var backdrop = document.querySelector('.preview-box picture.landscape img[src]');
              var years = Array.from(document.querySelectorAll(
                '.program-information .production-year'
              )).map(function(node) { return clean(node.textContent); });
              var year = years.find(function(value) { return /^1[34]\d{2}$/.test(value); }) || '';
              var genres = Array.from(document.querySelectorAll('.product-genres a')).map(
                function(link) { return clean(link.textContent); }
              ).filter(Boolean).slice(0, 4);
              AminSheyda.result(JSON.stringify({
                title: title,
                summary: clean((document.querySelector('.product-summary') || {}).textContent),
                year: year,
                genres: genres,
                backdropUrl: backdrop ? backdrop.src : '',
                directors: people('کارگردان'),
                cast: people('بازیگر')
              }));
            })();
        """.trimIndent()
    }
}
