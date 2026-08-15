package com.amin.tvos.browser

import android.annotation.SuppressLint
import android.net.Uri
import android.net.http.SslError
import android.view.View
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.amin.tvos.data.LiveChannelSource
import com.amin.tvos.data.model.LiveHealthStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import java.net.URI

/**
 * Performs one no-retry playback probe in a dedicated, hidden WebView.
 * It observes only page/player state and never reads, logs or stores media URLs.
 */
class LiveChannelHealthProbe(private val webView: WebView) : AutoCloseable {

    private var attemptToken = 0L
    private var startedToken = -1L
    private var continuation: kotlinx.coroutines.CancellableContinuation<LiveHealthStatus>? = null

    init {
        configureWebView()
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                val token = attemptToken
                view.postDelayed({ startPlayerProbe(token) }, 550L)
            }

            override fun onPageCommitVisible(view: WebView, url: String) {
                val token = attemptToken
                view.postDelayed({ startPlayerProbe(token) }, 300L)
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                if (request.isForMainFrame) finish(attemptToken, LiveHealthStatus.INACTIVE)
            }

            override fun onReceivedSslError(
                view: WebView,
                handler: SslErrorHandler,
                error: SslError
            ) {
                val host = runCatching { Uri.parse(error.url).host }.getOrNull()
                if (host == "ssl.p.jwpcdn.com") handler.proceed() else handler.cancel()
            }
        }
    }

    suspend fun check(source: LiveChannelSource): LiveHealthStatus = withContext(Dispatchers.Main.immediate) {
        cancelCurrent()
        val token = ++attemptToken
        val result = withTimeoutOrNull(PROBE_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                continuation = cont
                val originalUrl = absoluteUrl(source)
                val target = optimizedParsaUrl(source, originalUrl)
                if (target != originalUrl) {
                    webView.loadUrl(target, mapOf("Referer" to originalUrl))
                } else {
                    webView.loadUrl(target)
                }
                cont.invokeOnCancellation {
                    webView.post { if (token == attemptToken) webView.stopLoading() }
                }
            }
        } ?: LiveHealthStatus.INACTIVE
        if (token == attemptToken) {
            webView.stopLoading()
            webView.loadUrl("about:blank")
        }
        result
    }

    private fun startPlayerProbe(token: Long) {
        if (token != attemptToken || continuation == null || startedToken == token) return
        startedToken = token
        webView.evaluateJavascript(START_SCRIPT, null)
        pollPlayer(token, 0)
    }

    private fun pollPlayer(token: Long, pass: Int) {
        if (token != attemptToken || continuation == null) return
        webView.evaluateJavascript(PROBE_SCRIPT) { raw ->
            if (token != attemptToken || continuation == null) return@evaluateJavascript
            if (raw?.trim() == "true") {
                finish(token, LiveHealthStatus.ACTIVE)
            } else if (pass < MAX_POLLS) {
                webView.postDelayed({ pollPlayer(token, pass + 1) }, POLL_INTERVAL_MS)
            } else {
                finish(token, LiveHealthStatus.INACTIVE)
            }
        }
    }

    private fun finish(token: Long, status: LiveHealthStatus) {
        if (token != attemptToken) return
        val pending = continuation ?: return
        continuation = null
        webView.stopLoading()
        if (pending.isActive) pending.resume(status)
    }

    private fun cancelCurrent() {
        attemptToken += 1
        continuation?.let { pending ->
            continuation = null
            if (pending.isActive) pending.resume(LiveHealthStatus.INACTIVE)
        }
        webView.stopLoading()
    }

    private fun absoluteUrl(source: LiveChannelSource): String {
        if (source.channel.path.startsWith("http", ignoreCase = true)) return source.channel.path
        return runCatching {
            URI(source.service.url).resolve(source.channel.path).toString()
        }.getOrElse { source.service.url.trimEnd('/') + "/" + source.channel.path.trimStart('/') }
    }

    private fun optimizedParsaUrl(source: LiveChannelSource, original: String): String {
        if (source.service.id != "parsatv") return original
        val channelName = original.substringAfter("/name=", "")
            .substringBefore('?')
            .substringBefore('#')
            .trim('/')
        if (channelName.isBlank()) return original
        val parsed = Uri.parse(original)
        return Uri.Builder()
            .scheme(parsed.scheme ?: "https")
            .authority(parsed.authority ?: "www.parsatv.com")
            .appendPath("embed.php")
            .appendQueryParameter("name", channelName)
            .appendQueryParameter("auto", "true")
            .build()
            .toString()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            javaScriptCanOpenWindowsAutomatically = false
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            cacheMode = WebSettings.LOAD_DEFAULT
            setSupportMultipleWindows(false)
        }
        android.webkit.CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }
    }

    override fun close() {
        cancelCurrent()
        webView.stopLoading()
        webView.destroy()
    }

    private companion object {
        const val PROBE_TIMEOUT_MS = 7_500L
        const val POLL_INTERVAL_MS = 750L
        const val MAX_POLLS = 8

        val START_SCRIPT = """
            (function() {
              function roots() {
                var all = [document];
                document.querySelectorAll('iframe').forEach(function(frame) {
                  try { if (frame.contentDocument) all.push(frame.contentDocument); } catch (_) {}
                });
                return all;
              }
              roots().forEach(function(doc) {
                doc.querySelectorAll('video').forEach(function(video) {
                  try { video.muted = true; video.volume = 0; video.play().catch(function() {}); } catch (_) {}
                });
                doc.querySelectorAll(
                  '.jw-icon-display,.jw-display-icon-container,.vjs-big-play-button,' +
                  '.plyr__control--overlaid,button[aria-label*="play" i],button[title*="play" i]'
                ).forEach(function(button) {
                  try { button.click(); } catch (_) {}
                });
              });
              try {
                if (typeof window.jwplayer === 'function') {
                  var player = window.jwplayer();
                  if (player) { try { player.setMute(true); } catch (_) {} try { player.play(true); } catch (_) {} }
                }
              } catch (_) {}
            })();
        """.trimIndent()

        val PROBE_SCRIPT = """
            (function() {
              function roots() {
                var all = [window];
                document.querySelectorAll('iframe').forEach(function(frame) {
                  try { if (frame.contentWindow) all.push(frame.contentWindow); } catch (_) {}
                });
                return all;
              }
              var playing = false;
              roots().forEach(function(win) {
                try {
                  win.document.querySelectorAll('video').forEach(function(video) {
                    var decoded = Number(video.webkitDecodedFrameCount || 0);
                    if (!video.paused && video.readyState >= 2 && (decoded > 0 || Number(video.currentTime || 0) > 0)) playing = true;
                  });
                  if (typeof win.jwplayer === 'function') {
                    var player = win.jwplayer();
                    if (player && String(player.getState && player.getState() || '').toLowerCase() === 'playing') playing = true;
                  }
                } catch (_) {}
              });
              return playing;
            })();
        """.trimIndent()
    }
}
