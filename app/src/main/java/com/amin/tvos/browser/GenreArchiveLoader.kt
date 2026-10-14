package com.amin.tvos.browser

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.*
import com.amin.tvos.data.*
import com.amin.tvos.data.model.*
import org.json.JSONObject

/** One visible-results page, using the existing normal WebView cookie store. No media reading. */
class GenreArchiveLoader(
    context: android.content.Context,
    private val service: StreamingService,
    private val query: GenreArchiveQuery,
    private val pageNumber: Int,
    private val onPage: (GenreArchivePage) -> Unit,
    private val onError: (String) -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private var delivered = false
    private var destroyed = false
    private var requested = false
    private val timeout = Runnable { fail("دریافت صفحه طول کشید؛ اتصال یا ورود سایت را بررسی کن.") }

    @SuppressLint("SetJavaScriptEnabled")
    val view = WebView(context).apply {
        alpha = 0f
        isFocusable = false
        isFocusableInTouchMode = false
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            blockNetworkImage = true
            mediaPlaybackRequiresUserGesture = true
            allowFileAccess = false
            allowContentAccess = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            userAgentString = UserAgentMode.entries.firstOrNull { it.name == service.userAgent }?.value
                ?: WebSettings.getDefaultUserAgent(context)
        }
    }

    init {
        require(service.id == "mymoviz")
        CookieManager.getInstance().setAcceptThirdPartyCookies(view, true)
        view.addJavascriptInterface(Bridge(), "AminGenre")
        view.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                !query.acceptsPage(request.url.toString(), service.url, pageNumber)

            override fun onPageFinished(view: WebView, url: String) {
                if (destroyed || delivered || requested) return
                if (!query.acceptsPage(url, service.url, pageNumber)) {
                    fail("صفحهٔ نتیجه در دسترس نیست؛ ورود سایت را از مرورگر برنامه بررسی کن.")
                    return
                }
                requested = true
                view.evaluateJavascript(EXTRACT, null)
            }
            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) fail("دریافت صفحه ناموفق بود؛ نتیجه‌های ذخیره‌شده حفظ شده‌اند.")
            }
            override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                if (request.isForMainFrame) fail("سایت پاسخ موفق نداد؛ دوباره تلاش کن.")
            }
        }
    }

    fun start() {
        handler.postDelayed(timeout, 25_000)
        view.loadUrl(query.pageUrl(service.url, pageNumber))
    }
    private fun fail(message: String) {
        if (delivered || destroyed) return
        delivered = true
        handler.removeCallbacks(timeout)
        onError(message)
    }
    fun destroy() {
        if (destroyed) return
        destroyed = true
        delivered = true
        handler.removeCallbacksAndMessages(null)
        view.stopLoading()
        view.onPause()
        view.removeJavascriptInterface("AminGenre")
        view.destroy()
    }

    private inner class Bridge {
        @JavascriptInterface fun result(payload: String?) {
            handler.post {
                if (delivered || destroyed) return@post
                val parsed = runCatching { parse(JSONObject(payload.orEmpty())) }.getOrElse {
                    fail("ساختار نتیجهٔ سایت قابل‌خواندن نیست؛ پایان آرشیو فرض نمی‌شود.")
                    return@post
                }
                delivered = true
                handler.removeCallbacks(timeout)
                onPage(parsed)
            }
        }
    }

    private fun parse(root: JSONObject): GenreArchivePage {
        check(root.optBoolean("recognized"))
        val origin = java.net.URI(service.url)
        val array = root.getJSONArray("items")
        val adapter = ServiceAdapter(service)
        val items = buildList {
            for (index in 0 until minOf(array.length(), GenreArchivePolicy.MAX_PAGE_ITEMS)) {
                val raw = array.getJSONObject(index)
                val url = raw.optString("contentUrl").take(2_000)
                val uri = runCatching { java.net.URI(url) }.getOrNull() ?: continue
                if (uri.scheme != "https" || !uri.host.equals(origin.host, true) ||
                    uri.port != origin.port || uri.rawUserInfo != null || uri.rawQuery != null || uri.fragment != null ||
                    !adapter.isContentUrl(url)) continue
                val kind = when {
                    Regex("^/movies/[0-9]+/").containsMatchIn(uri.path) -> CatalogKind.MOVIE
                    Regex("^/tvshows/[0-9]+/").containsMatchIn(uri.path) -> CatalogKind.SERIES
                    else -> continue
                }
                if ((kind == CatalogKind.MOVIE) != (query.type == ArchiveType.MOVIES)) continue
                val title = raw.optString("title").trim().take(140)
                if (title.isBlank()) continue
                val poster = raw.optString("posterUrl").take(2_000).takeIf {
                    runCatching { java.net.URI(it).scheme in setOf("https", "http") && java.net.URI(it).rawUserInfo == null }.getOrDefault(false)
                }.orEmpty()
                add(CatalogItem(title = title, kind = kind, contentUrl = url, serviceId = service.id,
                    posterUrl = poster, year = raw.optString("year").take(4),
                    rating = raw.optString("rating").take(6), summary = raw.optString("summary").take(420),
                    imdbId = raw.optString("imdbId").takeIf { Regex("tt[0-9]{5,12}").matches(it) }.orEmpty(),
                    genres = raw.optJSONArray("genres")?.let { a -> (0 until minOf(a.length(), 4)).map { a.optString(it).take(40) } }.orEmpty(),
                    hasPersianDub = raw.optBoolean("dubbed"), hasPersianSubtitle = raw.optBoolean("subtitled"),
                    episodeLabel = raw.optString("episodeLabel").take(72)))
            }
        }.distinctBy { it.contentUrl }
        val total = root.optInt("totalCount", -1).takeIf { it in 0..1_000_000 }
        val rawNext = root.optString("nextUrl")
        val next = rawNext.takeIf { query.acceptsPage(it, service.url, pageNumber + 1) }.orEmpty()
        // Changed selectors, login pages and filtered-out responses must not become a false end.
        check(array.length() == 0 && total == 0 || items.isNotEmpty())
        check(rawNext.isBlank() || next.isNotBlank())
        return GenreArchivePage(query, pageNumber, items, next, total, System.currentTimeMillis())
    }

    companion object {
        val EXTRACT = """
            (function() {
              function text(node) { return node ? (node.textContent || '').replace(/\s+/g, ' ').trim() : ''; }
              function digits(s) { return s.replace(/[۰-۹]/g, function(c) { return String('۰۱۲۳۴۵۶۷۸۹'.indexOf(c)); }); }
              var countNode = document.querySelector('.mv-results__count');
              var count = digits(text(countNode)).match(/[0-9][0-9,]*/);
              var rows = Array.from(document.querySelectorAll('.mv-results-grid .mv-ritem'));
              var items = rows.map(function(row) {
                var card = row.querySelector('a.mv-card[href]'); if (!card) return null;
                var url = new URL(card.getAttribute('href'), location.href);
                if (url.origin !== location.origin || !/^\/(movies|tvshows)\/\d+\//.test(url.pathname)) return null;
                var img = card.querySelector('.mv-card__img');
                var poster = img && (img.getAttribute('data-src') || img.getAttribute('src'));
                var yr = text(row.querySelector('.mv-ritem__t-yr')).match(/(?:19|20)\d{2}/);
                var imdb = row.querySelector('a[href*="imdb.com/title/tt"]');
                var id = imdb && imdb.getAttribute('href').match(/tt\d{5,12}/i);
                var tags = text(row.querySelector('.mv-ritem__foot'));
                return {
                  title: text(row.querySelector('.mv-ritem__t-en')) || text(card.querySelector('.mv-card__title')),
                  contentUrl: url.origin + url.pathname,
                  posterUrl: poster && !/^data:/i.test(poster) ? new URL(poster, location.href).href : '',
                  year: yr ? yr[0] : '', imdbId: id ? id[0].toLowerCase() : '',
                  rating: text(row.querySelector('.mv-card__rate--imdb')).replace(/[^0-9.]/g, ''),
                  summary: text(row.querySelector('.mv-ritem__plot')),
                  genres: Array.from(row.querySelectorAll('.mv-ritem__genres a')).map(text),
                  dubbed: /دوبله/.test(tags), subtitled: /زیرنویس\s*فارسی/.test(tags),
                  episodeLabel: text(card.querySelector('.mv-card__ep'))
                };
              }).filter(Boolean).slice(0, 24);
              var next = document.querySelector('a.mv-pager__btn[aria-label="صفحه بعد"]');
              var nextUrl = next ? new URL(next.getAttribute('href'), location.href) : null;
              AminGenre.result(JSON.stringify({recognized: !!countNode && !!document.querySelector('.mv-results-grid'),
                items:items, totalCount: count ? Number(count[0].replace(/,/g,'')) : -1,
                nextUrl: nextUrl && !nextUrl.hash ? nextUrl.href : ''}));
            })();
        """.trimIndent()
    }
}
