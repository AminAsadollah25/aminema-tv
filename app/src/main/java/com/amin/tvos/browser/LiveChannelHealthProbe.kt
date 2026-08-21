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
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
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
        // This WebView is a silent health probe, not a second player. Resume it only for
        // the bounded probe window; leaving it paused prevents a provider's media element
        // from leaking audio while the user is browsing the Live TV menu.
        webView.onResume()
        setProbeAudioMuted(true)
        val token = ++attemptToken
        try {
            withTimeoutOrNull(PROBE_TIMEOUT_MS) {
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
        } finally {
            // A timeout cancels the continuation but does not guarantee that the field is
            // cleared before a late page callback arrives. Clear it here and reset the
            // WebView on every exit path, while the token guard protects a newer probe.
            if (token == attemptToken) {
                continuation = null
                silenceAndReset()
            }
        }
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
        silenceAndReset()
        if (pending.isActive) pending.resume(status)
    }

    private fun cancelCurrent() {
        attemptToken += 1
        continuation?.let { pending ->
            continuation = null
            if (pending.isActive) pending.resume(LiveHealthStatus.INACTIVE)
        }
        silenceAndReset()
    }

    private fun silenceAndReset() {
        // Cleanup runs before navigation because stopLoading() alone does not stop an
        // already-started HTML5/JWPlayer media element. onPause() is an additional native
        // guard for pages whose player lives inside an inaccessible cross-origin iframe.
        webView.evaluateJavascript(CLEANUP_SCRIPT, null)
        setProbeAudioMuted(true)
        webView.stopLoading()
        webView.onPause()
        webView.loadUrl("about:blank")
    }

    /** Stop an in-flight health check and silence the hidden player immediately. */
    fun cancel() {
        cancelCurrent()
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
        // JavaScript-level muting cannot reach a cross-origin iframe. Mute the WebView
        // itself when the installed WebView provider exposes AndroidX's MUTE_AUDIO API.
        // This is the important guard that prevents the hidden health scanner from
        // becoming a second audible player while the user is in Live TV or another screen.
        setProbeAudioMuted(true)
    }

    private fun setProbeAudioMuted(muted: Boolean) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.MUTE_AUDIO)) return
        runCatching { WebViewCompat.setAudioMuted(webView, muted) }
    }

    override fun close() {
        cancelCurrent()
        silenceAndReset()
        webView.destroy()
    }

    private companion object {
        const val PROBE_TIMEOUT_MS = 7_500L
        const val POLL_INTERVAL_MS = 750L
        const val MAX_POLLS = 8

        val START_SCRIPT = """
            (function() {
              // Providers often create the actual video after the first click. Keep a
              // short-lived, page-local silencer installed so that a newly-created player
              // cannot become an audible second channel during the health scan.
              try {
                if (window.__aminemaHealthSilencer) clearInterval(window.__aminemaHealthSilencer);
                window.__aminemaHealthSilencer = setInterval(function() {
                  try {
                    document.querySelectorAll('video,audio').forEach(function(media) {
                      media.muted = true;
                      media.defaultMuted = true;
                      media.volume = 0;
                      media.setAttribute('muted', '');
                    });
                    if (typeof window.jwplayer === 'function') {
                      var active = window.jwplayer();
                      if (active) { try { active.setMute(true); } catch (_) {} }
                    }
                  } catch (_) {}
                }, 50);
              } catch (_) {}
              document.addEventListener('play', function(event) {
                try {
                  var media = event.target;
                  if (media && (media.tagName === 'VIDEO' || media.tagName === 'AUDIO')) {
                    media.muted = true;
                    media.defaultMuted = true;
                    media.volume = 0;
                    media.setAttribute('muted', '');
                  }
                } catch (_) {}
              }, true);
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

        val CLEANUP_SCRIPT = """
            (function() {
              try {
                if (window.__aminemaHealthSilencer) {
                  clearInterval(window.__aminemaHealthSilencer);
                  window.__aminemaHealthSilencer = null;
                }
                document.querySelectorAll('video,audio').forEach(function(media) {
                  try { media.pause(); } catch (_) {}
                  try { media.muted = true; media.volume = 0; } catch (_) {}
                });
                if (typeof window.jwplayer === 'function') {
                  var active = window.jwplayer();
                  if (active) {
                    try { active.setMute(true); } catch (_) {}
                    try { active.stop(); } catch (_) {}
                  }
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
