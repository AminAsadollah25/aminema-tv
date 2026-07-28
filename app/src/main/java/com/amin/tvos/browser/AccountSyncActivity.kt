package com.amin.tvos.browser

import android.annotation.SuppressLint
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
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
import com.amin.tvos.data.model.PlaybackSession
import com.amin.tvos.data.model.ResumeStrategy
import com.amin.tvos.data.model.StreamingService
import com.amin.tvos.data.model.UserAgentMode
import kotlinx.coroutines.launch
import org.json.JSONArray

/**
 * Visible, user-triggered account-history sync.
 *
 * It reads only the signed-in websites' own Continue/Recent account sections.
 * Authentication values stay inside WebView. No media URL is read or stored.
 */
class AccountSyncActivity : ComponentActivity() {

    private enum class Stage { PARSI, FILMROOZ, DONE }

    private lateinit var root: FrameLayout
    private lateinit var webView: WebView
    private lateinit var status: TextView
    private lateinit var progress: ProgressBar

    private var stage = Stage.PARSI
    private var scriptStarted = false
    private var parsiCount = 0
    private var filmRoozCount = 0
    private var filmPosterJobs = 0
    private var filmCompleteRequested = false
    private var finishScheduled = false
    private val receivedServices = mutableSetOf<String>()
    private val errors = mutableListOf<String>()
    private val handler = Handler(Looper.getMainLooper())
    private val app get() = application as AminTvApp

    private val stageTimeout = Runnable {
        when (stage) {
            Stage.PARSI -> {
                errors += "فیلم ایرانی: timeout"
                startFilmRooz()
            }
            Stage.FILMROOZ -> {
                errors += "فیلم خارجی: timeout"
                finishSync()
            }
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
        webView.addJavascriptInterface(SyncBridge(), "AminAccountSync")
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                CookieManager.getInstance().flush()
                if (scriptStarted) return
                when (stage) {
                    Stage.PARSI -> {
                        if (Uri.parse(url).host.equals("app.parsiflix.com", true)) {
                            scriptStarted = true
                            view.postDelayed({ syncParsiFlix() }, 1_500L)
                        }
                    }
                    Stage.FILMROOZ -> {
                        if (Uri.parse(url).path.orEmpty().contains("/user/panel")) {
                            scriptStarted = true
                            view.postDelayed({ syncFilmRooz() }, 1_200L)
                        }
                    }
                    Stage.DONE -> Unit
                }
            }
        }

        root.addView(
            webView,
            FrameLayout.LayoutParams(2, 2, Gravity.BOTTOM or Gravity.END)
        )
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
            text = "Preparing account sync…"
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
            app.libraryRepository.load()
            startParsiFlix()
        }
    }

    private fun startParsiFlix() {
        stage = Stage.PARSI
        scriptStarted = false
        setStatus("در حال همگام‌سازی فیلم‌های ایرانی…")
        val service = app.servicesRepository.findById("parsiflix")
        if (service == null) {
            errors += "بخش فیلم ایرانی تنظیم نشده است"
            startFilmRooz()
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
        filmCompleteRequested = false
        setStatus("در حال همگام‌سازی فیلم‌های خارجی…")
        val service = app.servicesRepository.findById("filmrooz")
        if (service == null) {
            errors += "بخش فیلم خارجی تنظیم نشده است"
            finishSync()
            return
        }
        applyUserAgent(service)
        scheduleTimeout()
        webView.loadUrl(service.url.trimEnd('/') + "/user/panel/")
    }

    private fun scheduleTimeout() {
        handler.removeCallbacks(stageTimeout)
        handler.postDelayed(stageTimeout, 25_000L)
    }

    private fun applyUserAgent(service: StreamingService) {
        val mode = service.userAgent
            ?.let { value -> UserAgentMode.entries.firstOrNull { it.name == value } }
        webView.settings.userAgentString =
            mode?.value ?: WebSettings.getDefaultUserAgent(this)
    }

    private fun syncParsiFlix() {
        val script = """
            (async function() {
              try {
                var token = localStorage.getItem('accessToken');
                if (!token) {
                  AminAccountSync.failed('parsiflix', 'Login required');
                  return;
                }
                try { token = JSON.parse(token); } catch (_) {}
                var response = await fetch('https://api.parsiflix.com/app/home', {
                  headers: {
                    Authorization: 'Bearer ' + token,
                    appVersion: '1.0.0',
                    Accept: 'application/json, text/plain, */*'
                  }
                });
                if (!response.ok) {
                  AminAccountSync.failed('parsiflix', 'Login expired');
                  return;
                }
                var data = await response.json();
                var section = (data.sections || []).find(function(item) {
                  return /ادامه\s*تماشا|continue\s*watching/i.test(item.title || '');
                });
                var items = (section ? section.items : []).slice(0, 30).map(
                  function(item) {
                    var kind = String(item.type || '').toUpperCase() === 'MOVIE'
                      ? 'movies' : 'series';
                    return {
                      title: String(item.title || '').slice(0, 140),
                      contentUrl: location.origin + '/medias/' + kind + '/' + item.id,
                      posterUrl: item.coverLink || item.thumbnailLink || '',
                      resumePosition: 0
                    };
                  }
                );
                AminAccountSync.items('parsiflix', JSON.stringify(items));
              } catch (error) {
                AminAccountSync.failed('parsiflix', 'Service unavailable');
              }
            })();
        """.trimIndent()
        webView.evaluateJavascript(script, null)
    }

    private fun syncFilmRooz() {
        val script = """
            (async function() {
              try {
                function findBox() {
                  var heading = Array.from(
                    document.querySelectorAll('h1,h2,h3,h4,h5')
                  ).find(function(el) {
                    return /مشاهدات\s*اخیر|recent/i.test(el.innerText || '');
                  });
                  return heading && heading.parentElement
                    ? heading.parentElement.nextElementSibling : null;
                }
                // The "Recent" box can arrive via a slower AJAX call than a fixed
                // delay assumes; poll a few times before treating it as missing.
                var box = findBox();
                for (var attempt = 0; !box && attempt < 4; attempt++) {
                  await new Promise(function(resolve) { setTimeout(resolve, 900); });
                  box = findBox();
                }
                if (!box) {
                  AminAccountSync.failed('filmrooz', 'Login required');
                  return;
                }
                var seen = {};
                var items = Array.from(box.querySelectorAll('a[href*="/post/"]'))
                  .map(function(anchor) {
                    var url = new URL(anchor.href, location.href);
                    if (seen[url.href]) return null;
                    seen[url.href] = true;
                    var parts = url.pathname.split('/').filter(Boolean);
                    var contentId = parts.length > 2 ? parts[2] : '';
                    var slug = parts.length > 3 ? decodeURIComponent(parts[3]) : '';
                    var title = slug.replace(/[-_]+/g, ' ').replace(
                      /\b[a-z]/g, function(letter) { return letter.toUpperCase(); }
                    ).trim();
                    var image = anchor.querySelector('img');
                    // FilmRooz lazy-loads off-screen posters. In that state
                    // currentSrc/src is only a grey SVG placeholder while the
                    // real authenticated image lives in data-src.
                    var posterCandidate = image ? (
                      image.dataset.src ||
                      image.dataset.lazySrc ||
                      image.dataset.original ||
                      image.currentSrc ||
                      image.src ||
                      ''
                    ) : '';
                    var poster = posterCandidate &&
                      !/^data:/i.test(posterCandidate)
                        ? new URL(posterCandidate, location.href).href : '';
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
                  }).filter(Boolean).slice(0, 30);
                window.__aminFilmSyncItems = items;
                AminAccountSync.items('filmrooz', JSON.stringify(items));
              } catch (error) {
                AminAccountSync.failed('filmrooz', 'Service unavailable');
              }
            })();
        """.trimIndent()
        webView.evaluateJavascript(script, null)
    }

    private fun cacheFilmRoozPosters() {
        val script = """
            (async function() {
              var items = window.__aminFilmSyncItems || [];
              await Promise.allSettled(items.map(async function(item) {
                if (!/^https?:/i.test(item.posterUrl || '')) return;
                try {
                  var response = await fetch(item.posterUrl, {credentials: 'include'});
                  var blob = await response.blob();
                  if (!/^image\/(webp|jpeg|png)$/i.test(blob.type) ||
                      blob.size <= 0 || blob.size > 1500000) return;
                  var encoded = await new Promise(function(resolve, reject) {
                    var reader = new FileReader();
                    reader.onload = function() {
                      resolve(String(reader.result || '').split(',')[1] || '');
                    };
                    reader.onerror = reject;
                    reader.readAsDataURL(blob);
                  });
                  if (encoded) {
                    AminAccountSync.poster(
                      item.contentUrl, item.posterUrl, blob.type, encoded
                    );
                  }
                } catch (_) {}
              }));
              AminAccountSync.complete('filmrooz');
            })();
        """.trimIndent()
        webView.evaluateJavascript(script, null)
    }

    private inner class SyncBridge {
        @JavascriptInterface
        fun items(serviceId: String?, payload: String?) {
            val id = serviceId.orEmpty()
            if (id !in setOf("parsiflix", "filmrooz")) return
            val sessions = parseSessions(id, payload.orEmpty())
            runOnUiThread {
                if (!receivedServices.add(id)) return@runOnUiThread
                lifecycleScope.launch {
                    // A successful provider read is authoritative even when empty:
                    // stale account-only rows must disappear on every device.
                    app.libraryRepository.syncAccountSessions(id, sessions)
                    if (id == "parsiflix") {
                        parsiCount = sessions.size
                        startFilmRooz()
                    } else {
                        filmRoozCount = sessions.size
                        handler.removeCallbacks(stageTimeout)
                        cacheFilmRoozPosters()
                    }
                }
            }
        }

        @JavascriptInterface
        fun poster(
            contentUrl: String?,
            imageUrl: String?,
            mimeType: String?,
            encoded: String?
        ) {
            val content = contentUrl.orEmpty().take(2_000)
            val image = imageUrl.orEmpty().take(2_000)
            val mime = mimeType.orEmpty().lowercase().substringBefore(';')
            val payload = encoded.orEmpty()
            if (payload.length > 2_100_000) return
            val service = app.servicesRepository.findById("filmrooz") ?: return
            val valid = runCatching {
                val contentUri = Uri.parse(content)
                val imageUri = Uri.parse(image)
                val serviceUri = Uri.parse(service.url)
                contentUri.host.equals(serviceUri.host, true) &&
                    imageUri.host.equals(serviceUri.host, true) &&
                    ServiceAdapter(service).isContentUrl(content)
            }.getOrDefault(false)
            if (!valid) return
            val bytes = runCatching {
                Base64.decode(payload, Base64.DEFAULT)
            }.getOrNull() ?: return
            runOnUiThread {
                filmPosterJobs++
                lifecycleScope.launch {
                    try {
                        app.libraryRepository.saveLocalPoster(content, mime, bytes)
                    } finally {
                        filmPosterJobs--
                        maybeFinishFilmRooz()
                    }
                }
            }
        }

        @JavascriptInterface
        fun complete(serviceId: String?) {
            if (serviceId != "filmrooz") return
            runOnUiThread {
                filmCompleteRequested = true
                maybeFinishFilmRooz()
            }
        }

        @JavascriptInterface
        fun failed(serviceId: String?, reason: String?) {
            val id = serviceId.orEmpty()
            runOnUiThread {
                errors += "${if (id == "parsiflix") "فیلم ایرانی" else "فیلم خارجی"}: " +
                    reason.orEmpty().take(80)
                if (id == "parsiflix") startFilmRooz() else finishSync()
            }
        }
    }

    private fun parseSessions(
        serviceId: String,
        payload: String
    ): List<PlaybackSession> {
        val service = app.servicesRepository.findById(serviceId) ?: return emptyList()
        val adapter = ServiceAdapter(service)
        val array = runCatching { JSONArray(payload) }.getOrNull() ?: return emptyList()
        val now = System.currentTimeMillis()
        val actionPatterns = if (serviceId == "filmrooz") {
            listOf("پخش آنلاین", "پخش انلاین", "تماشای آنلاین", "Watch Online")
        } else {
            emptyList()
        }
        return buildList {
            for (index in 0 until minOf(array.length(), 20)) {
                val item = array.optJSONObject(index) ?: continue
                val contentUrl = item.optString("contentUrl").take(2_000)
                if (!adapter.isContentUrl(contentUrl)) continue
                val serviceHost = Uri.parse(service.url).host
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

    private fun maybeFinishFilmRooz() {
        if (filmCompleteRequested && filmPosterJobs == 0) finishSync()
    }

    private fun finishSync() {
        if (finishScheduled) return
        finishScheduled = true
        handler.removeCallbacks(stageTimeout)
        stage = Stage.DONE
        progress.visibility = View.GONE
        val total = parsiCount + filmRoozCount
        val detail = if (errors.isEmpty()) {
            "فیلم ایرانی: $parsiCount  •  فیلم خارجی: $filmRoozCount"
        } else {
            "Synced $total items\n" + errors.joinToString("\n")
        }
        setStatus("Account sync complete\n$detail")
        handler.postDelayed({ finish() }, 1_800L)
    }

    private fun setStatus(message: String) {
        status.text = message
    }

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
}
