package com.amin.tvos.browser

import android.annotation.SuppressLint
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
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
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.amin.tvos.AminTvApp
import com.amin.tvos.data.model.CatalogItem
import com.amin.tvos.data.model.CatalogKind
import com.amin.tvos.data.model.CatalogSection
import com.amin.tvos.data.model.StreamingService
import com.amin.tvos.data.model.UserAgentMode
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Visible, user-triggered refresh of the two "latest" rows.
 *
 * Each service is read through its own adapter, inside the signed-in WebView, using only
 * the website's own catalog views:
 *
 *  - فیلم ایرانی: the account's own `جدیدترین‌ها` home section plus the movie/series
 *    catalog endpoint that the website itself calls for `/medias/movies` and
 *    `/medias/series`.
 *  - فیلم خارجی: the signed-in menu's own `فیلم های جدید` and `سریال های جدید` pages.
 *
 * Only title, poster and the normal detail-page link are read. No media URL, stream URL,
 * DRM value or token leaves the WebView, and one failing adapter never blocks the other.
 */
class CatalogSyncActivity : ComponentActivity() {

    private enum class Stage { PARSI, FILMROOZ, DONE }

    private lateinit var root: FrameLayout
    private lateinit var webView: WebView
    private lateinit var status: TextView
    private lateinit var progress: ProgressBar

    private var stage = Stage.PARSI
    private var scriptStarted = false
    private var finishScheduled = false
    private val counts = linkedMapOf<String, Int>()
    private val errors = mutableListOf<String>()
    private val handled = mutableSetOf<String>()
    private val handler = Handler(Looper.getMainLooper())
    private val app get() = application as AminTvApp

    private val stageTimeout = Runnable {
        when (stage) {
            Stage.PARSI -> failStage(PARSI_ID, "timeout")
            Stage.FILMROOZ -> failStage(FILMROOZ_ID, "timeout")
            Stage.DONE -> Unit
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemUi()

        root = FrameLayout(this).apply { setBackgroundColor(Color.rgb(8, 8, 12)) }
        webView = WebView(this).apply {
            alpha = 0.01f
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
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }
        webView.addJavascriptInterface(CatalogBridge(), "AminCatalog")
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                CookieManager.getInstance().flush()
                if (scriptStarted) return
                when (stage) {
                    Stage.PARSI -> if (
                        Uri.parse(url).host.equals(PARSI_HOST, true)
                    ) {
                        scriptStarted = true
                        view.postDelayed({ webView.evaluateJavascript(PARSI_SCRIPT, null) }, 1_500L)
                    }
                    Stage.FILMROOZ -> if (
                        Uri.parse(url).host.equals(filmRoozHost(), true)
                    ) {
                        scriptStarted = true
                        view.postDelayed({ webView.evaluateJavascript(FILMROOZ_SCRIPT, null) }, 1_200L)
                    }
                    Stage.DONE -> Unit
                }
            }
        }

        root.addView(webView, FrameLayout.LayoutParams(2, 2, Gravity.BOTTOM or Gravity.END))
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(80, 48, 80, 48)
        }
        progress = ProgressBar(this).apply { isIndeterminate = true }
        status = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 28f
            gravity = Gravity.CENTER
            setPadding(0, 34, 0, 0)
            text = "در حال بروزرسانی تازه‌ها…"
        }
        panel.addView(progress, LinearLayout.LayoutParams(72, 72))
        panel.addView(
            status,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        root.addView(
            panel,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        setContentView(root)

        lifecycleScope.launch {
            app.servicesRepository.load()
            app.catalogRepository.load()
            startParsi()
        }
    }

    private fun startParsi() {
        stage = Stage.PARSI
        scriptStarted = false
        setStatus("در حال بروزرسانی تازه‌های ایرانی…")
        val service = app.servicesRepository.findById(PARSI_ID)
        if (service == null) {
            failStage(PARSI_ID, "سرویس تنظیم نشده است")
            return
        }
        applyUserAgent(service)
        scheduleTimeout()
        webView.loadUrl(service.url)
    }

    private fun startFilmRooz() {
        handler.removeCallbacks(stageTimeout)
        stage = Stage.FILMROOZ
        scriptStarted = false
        setStatus("در حال بروزرسانی تازه‌های خارجی…")
        val service = app.servicesRepository.findById(FILMROOZ_ID)
        if (service == null) {
            failStage(FILMROOZ_ID, "سرویس تنظیم نشده است")
            return
        }
        applyUserAgent(service)
        scheduleTimeout()
        webView.loadUrl(service.url)
    }

    /** Adapters are independent: a failure records itself and moves on. */
    private fun failStage(serviceId: String, reason: String) {
        if (!handled.add(serviceId)) return
        errors += "${label(serviceId)}: $reason"
        lifecycleScope.launch {
            app.catalogRepository.recordError(serviceId, reason.take(120))
            if (serviceId == PARSI_ID) startFilmRooz() else finishSync()
        }
    }

    private fun scheduleTimeout() {
        handler.removeCallbacks(stageTimeout)
        handler.postDelayed(stageTimeout, 30_000L)
    }

    private fun applyUserAgent(service: StreamingService) {
        val mode = service.userAgent
            ?.let { value -> UserAgentMode.entries.firstOrNull { it.name == value } }
        webView.settings.userAgentString = mode?.value ?: WebSettings.getDefaultUserAgent(this)
    }

    private fun filmRoozHost(): String =
        app.servicesRepository.findById(FILMROOZ_ID)
            ?.let { Uri.parse(it.url).host }
            .orEmpty()

    private inner class CatalogBridge {
        @JavascriptInterface
        fun section(serviceId: String?, payload: String?) {
            val id = serviceId.orEmpty()
            if (id !in setOf(PARSI_ID, FILMROOZ_ID)) return
            val section = parseSection(id, payload.orEmpty())
            runOnUiThread {
                if (!handled.add(id)) return@runOnUiThread
                handler.removeCallbacks(stageTimeout)
                lifecycleScope.launch {
                    app.catalogRepository.save(section)
                    counts[id] = section.all.size
                    if (id == PARSI_ID) startFilmRooz() else finishSync()
                }
            }
        }

        @JavascriptInterface
        fun failed(serviceId: String?, reason: String?) {
            val id = serviceId.orEmpty()
            if (id !in setOf(PARSI_ID, FILMROOZ_ID)) return
            runOnUiThread { failStage(id, reason.orEmpty().take(80)) }
        }
    }

    /**
     * Everything crossing the bridge is untrusted page output: only same-host links that
     * match the service's own configured content pattern are accepted.
     */
    private fun parseSection(serviceId: String, payload: String): CatalogSection {
        val service = app.servicesRepository.findById(serviceId)
            ?: return CatalogSection(serviceId = serviceId, error = "سرویس تنظیم نشده است")
        val adapter = ServiceAdapter(service)
        val serviceHost = Uri.parse(service.url).host
        val root = runCatching { JSONObject(payload) }.getOrNull()
            ?: return CatalogSection(serviceId = serviceId, error = "پاسخ نامعتبر")

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
                            serviceId = service.id
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
            syncedAt = System.currentTimeMillis(),
            error = ""
        )
    }

    private fun finishSync() {
        if (finishScheduled) return
        finishScheduled = true
        handler.removeCallbacks(stageTimeout)
        stage = Stage.DONE
        progress.visibility = View.GONE
        val detail = buildString {
            append("تازه‌های ایرانی: ${counts[PARSI_ID] ?: 0}")
            append("  •  ")
            append("تازه‌های خارجی: ${counts[FILMROOZ_ID] ?: 0}")
            if (errors.isNotEmpty()) append("\n" + errors.joinToString("\n"))
        }
        setStatus("بروزرسانی تازه‌ها انجام شد\n$detail")
        handler.postDelayed({ finish() }, 1_800L)
    }

    private fun setStatus(message: String) {
        status.text = message
    }

    private fun label(serviceId: String): String =
        if (serviceId == PARSI_ID) "تازه‌های ایرانی" else "تازه‌های خارجی"

    private fun hideSystemUi() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUi()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        webView.apply {
            stopLoading()
            (parent as? ViewGroup)?.removeView(this)
            destroy()
        }
        super.onDestroy()
    }

    private companion object {
        const val PARSI_ID = "parsiflix"
        const val FILMROOZ_ID = "filmrooz"
        const val PARSI_HOST = "app.parsiflix.com"
        const val MAX_ITEMS = 20

        /**
         * `جدیدترین‌ها` is the account home's own combined latest section; the per-type
         * lists use `/medias?type=…`, the exact call the website makes for its own
         * `/medias/movies` and `/medias/series` pages.
         */
        val PARSI_SCRIPT = """
            (async function() {
              function map(item) {
                var kind = String(item.type || '').toUpperCase() === 'SERIES'
                  ? 'SERIES' : 'MOVIE';
                var path = kind === 'SERIES' ? 'series' : 'movies';
                return {
                  title: String(item.title || '').slice(0, 140),
                  kind: kind,
                  contentUrl: location.origin + '/medias/' + path + '/' + item.id,
                  posterUrl: item.coverLink || item.thumbnailLink || ''
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
                var all = ((latest && latest.items) || []).slice(0, 20).map(map);

                async function typed(type) {
                  var response = await fetch(
                    'https://api.parsiflix.com/medias?type=' + type + '&page=1&size=20',
                    {headers: headers}
                  );
                  if (!response.ok) return [];
                  var data = await response.json();
                  return (data.elements || []).slice(0, 20).map(map);
                }
                var movies = await typed('MOVIE');
                var series = await typed('SERIES');
                if (!all.length) {
                  // Keep the combined tab useful even if the home section disappears.
                  all = movies.slice(0, 10).concat(series.slice(0, 10));
                }
                AminCatalog.section('parsiflix', JSON.stringify({
                  all: all, movies: movies, series: series
                }));
              } catch (error) {
                AminCatalog.failed('parsiflix', 'Service unavailable');
              }
            })();
        """.trimIndent()

        /**
         * The two paths come from the signed-in site's own menu
         * (`فیلم های جدید` and `سریال های جدید`), not from a guess.
         */
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
                    // Cards mix relative and absolute links, so match on the path.
                    var url = new URL(href, location.origin);
                    if (url.origin !== location.origin) return;
                    var match = url.pathname.match(/^\/post\/(film|series)\/(\d+)\//);
                    if (!match) return;
                    if (seen[url.pathname]) return;
                    var kind = match[1] === 'series' ? 'SERIES' : 'MOVIE';
                    if (expectedKind && kind !== expectedKind) return;
                    seen[url.pathname] = true;
                    var card = anchor.closest('.postMeta') || anchor.parentElement;
                    // The card's own text link carries the real title, e.g. "Colony (2026)".
                    // It must point at *this* item, and an episode-status link
                    // ("قسمت ۱۶ فصل اول…") is never a title.
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
                    // Posters are lazy-loaded: the real image lives in data-src.
                    var image = anchor.querySelector('img');
                    var candidate = image
                      ? (image.getAttribute('data-src') || image.getAttribute('src') || '')
                      : '';
                    var poster = candidate && !/^data:/i.test(candidate)
                      ? new URL(candidate, location.origin).href : '';
                    items.push({
                      title: title.slice(0, 140),
                      kind: kind,
                      contentUrl: url.origin + url.pathname,
                      posterUrl: poster
                    });
                  }
                );
                return items.slice(0, 20);
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
              try {
                if (!document.querySelector('a[href*="/user/panel"]')) {
                  AminCatalog.failed('filmrooz', 'Login required');
                  return;
                }
                var movies = await page('/archive/category/new-films/', 'MOVIE');
                var series = await page('/archive/category/new-tv-show/', 'SERIES');
                if (!movies.length && !series.length) {
                  AminCatalog.failed('filmrooz', 'Empty catalog');
                  return;
                }
                // The site has no combined "new" page, so the merged tab interleaves
                // both lists while keeping each one's own order.
                var all = [];
                for (var i = 0; i < Math.max(movies.length, series.length); i++) {
                  if (movies[i]) all.push(movies[i]);
                  if (series[i]) all.push(series[i]);
                }
                AminCatalog.section('filmrooz', JSON.stringify({
                  all: all.slice(0, 20), movies: movies, series: series
                }));
              } catch (error) {
                AminCatalog.failed('filmrooz', 'Service unavailable');
              }
            })();
        """.trimIndent()
    }
}
