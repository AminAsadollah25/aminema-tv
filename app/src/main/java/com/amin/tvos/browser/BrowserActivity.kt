package com.amin.tvos.browser

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.http.SslError
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.amin.tvos.AminTvApp
import com.amin.tvos.data.model.ResumeStrategy
import com.amin.tvos.data.model.StreamingService
import com.amin.tvos.data.model.UserAgentMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.json.JSONTokener

/**
 * Mouse-first browser shell for Android TV.
 *
 * Important rules:
 *  - The content/player is never covered by floating controls.
 *  - Login/QR pages may use the configured reduced scale.
 *  - Content and player pages always render at 100%.
 *  - Video fullscreen is handled by WebChromeClient's native custom-view path.
 *  - No stream extraction, authentication bypass, or protected-content access.
 */
class BrowserActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_SERVICE_ID = "service_id"
        private const val EXTRA_URL = "url"
        private const val EXTRA_RESUME_POSITION = "resume_position"
        private const val EXTRA_CONTENT_URL = "content_url"
        private const val EXTRA_CONTENT_TITLE = "content_title"
        private const val EXTRA_CONTENT_POSTER = "content_poster"
        private const val EXTRA_AUTO_RESUME = "auto_resume"
        private const val EXTRA_RESUME_STRATEGY = "resume_strategy"
        private const val EXTRA_ACTION_BUTTON_PATTERNS = "action_button_patterns"

        fun intent(
            context: Context,
            serviceId: String,
            url: String,
            resumePosition: Long = 0L,
            contentUrl: String = "",
            contentTitle: String = "",
            contentPoster: String = "",
            autoResume: Boolean = false,
            resumeStrategyOverride: ResumeStrategy? = null,
            actionButtonTextPatterns: List<String> = emptyList()
        ): Intent =
            Intent(context, BrowserActivity::class.java)
                .putExtra(EXTRA_SERVICE_ID, serviceId)
                .putExtra(EXTRA_URL, url)
                .putExtra(EXTRA_RESUME_POSITION, resumePosition)
                .putExtra(EXTRA_CONTENT_URL, contentUrl)
                .putExtra(EXTRA_CONTENT_TITLE, contentTitle)
                .putExtra(EXTRA_CONTENT_POSTER, contentPoster)
                .putExtra(EXTRA_AUTO_RESUME, autoResume)
                .putExtra(EXTRA_RESUME_STRATEGY, resumeStrategyOverride?.name)
                .putStringArrayListExtra(
                    EXTRA_ACTION_BUTTON_PATTERNS,
                    ArrayList(actionButtonTextPatterns)
                )
    }

    private lateinit var root: FrameLayout
    private lateinit var webView: WebView
    private lateinit var errorView: TvErrorView
    private lateinit var mouseKeyboard: MouseKeyboardOverlay
    private lateinit var quickMenu: QuickMenuOverlay

    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    private var serviceId = ""
    private var serviceName = ""
    private var serviceProfile: StreamingService? = null
    private var serviceAdapter: ServiceAdapter? = null
    private var loginZoomPercent = 85
    private var lastFailedUrl: String? = null
    private var requestedResumePosition = 0L
    private var resumeApplied = false
    private var autoResumeRequested = false
    private var autoResumeActionTriggered = false
    private var requestedResumeStrategy: ResumeStrategy? = null
    private var requestedActionButtonPatterns: List<String> = emptyList()

    private var lastContentUrl = ""
    private var lastContentTitle = ""
    private var lastContentPoster = ""

    private var currentTitle = ""
    private var currentPoster = ""
    private var currentResumePosition = 0L
    private var currentDuration = 0L
    private var currentIsPlayable = false

    private val metadataHandler = Handler(Looper.getMainLooper())
    private val metadataRunnable = object : Runnable {
        override fun run() {
            if (::webView.isInitialized && !isFinishing && !isDestroyed) {
                webView.url?.let { captureResumeMetadata(it) }
                metadataHandler.postDelayed(this, 15_000L)
            }
        }
    }

    private val app get() = application as AminTvApp

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemUi()

        serviceId = intent.getStringExtra(EXTRA_SERVICE_ID).orEmpty()
        val startUrl = intent.getStringExtra(EXTRA_URL).orEmpty()
        requestedResumePosition = intent.getLongExtra(EXTRA_RESUME_POSITION, 0L)
        lastContentUrl = intent.getStringExtra(EXTRA_CONTENT_URL).orEmpty()
        lastContentTitle = intent.getStringExtra(EXTRA_CONTENT_TITLE).orEmpty()
        lastContentPoster = intent.getStringExtra(EXTRA_CONTENT_POSTER).orEmpty()
        autoResumeRequested = intent.getBooleanExtra(EXTRA_AUTO_RESUME, false)
        requestedResumeStrategy = intent.getStringExtra(EXTRA_RESUME_STRATEGY)
            ?.let { name ->
                ResumeStrategy.entries.firstOrNull { it.name == name }
            }
        requestedActionButtonPatterns =
            intent.getStringArrayListExtra(EXTRA_ACTION_BUTTON_PATTERNS).orEmpty()

        root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        webView = WebView(this).apply {
            setBackgroundColor(Color.BLACK)
            isFocusable = true
            isFocusableInTouchMode = true
        }
        errorView = TvErrorView(this) { retry() }.apply { visibility = View.GONE }

        root.addView(
            webView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        root.addView(
            errorView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        mouseKeyboard = MouseKeyboardOverlay(
            this,
            onValueChanged = {
                updateFocusedWebInput(it, submit = false, finalize = false)
            },
            onAction = { value, submit ->
                updateFocusedWebInput(value, submit, finalize = true)
            },
            onDismissed = { releaseFocusedWebInput() }
        )
        root.addView(
            mouseKeyboard,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        quickMenu = QuickMenuOverlay(this) { handleQuickAction(it) }
        root.addView(
            quickMenu,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        setContentView(root)

        configureWebView()
        setupBackNavigation()

        lifecycleScope.launch {
            serviceProfile = app.servicesRepository.services.value
                .firstOrNull { it.id == serviceId }
            // BrowserActivity can also be restored or launched before Home's
            // ViewModel has populated the repository. Never continue without
            // the adapter rules: they distinguish details, roots and players.
            if (serviceProfile == null) {
                app.servicesRepository.load()
                serviceProfile = app.servicesRepository.services.value
                    .firstOrNull { it.id == serviceId }
            }
            serviceName = serviceProfile?.name ?: serviceId
            serviceAdapter = serviceProfile?.let(::ServiceAdapter)
            loginZoomPercent = serviceProfile?.loginZoomPercent
                ?: app.settingsRepository.browserZoom.first()
            applyUserAgent()

            if (savedInstanceState == null) {
                webView.loadUrl(startUrl)
            } else {
                webView.restoreState(savedInstanceState)
            }
            webView.requestFocus()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            allowFileAccess = false
            allowContentAccess = false
            javaScriptCanOpenWindowsAutomatically = true
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            cacheMode = WebSettings.LOAD_DEFAULT
            setSupportMultipleWindows(false)
        }

        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }
        webView.addJavascriptInterface(KeyboardBridge(), "AminKeyboard")
        webView.addJavascriptInterface(PlaybackBridge(), "AminPlayback")
        webView.addJavascriptInterface(PosterBridge(), "AminPoster")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                errorView.visibility = View.GONE
                currentTitle = ""
                currentPoster = ""
                currentResumePosition = 0L
                currentDuration = 0L
                currentIsPlayable = false
                // Remove a scale left by a previous SPA route immediately.
                view.evaluateJavascript(
                    "document.documentElement.style.zoom='100%';" +
                        "if(document.body)document.body.style.zoom='100%';",
                    null
                )
            }

            override fun onPageFinished(view: WebView, url: String) {
                CookieManager.getInstance().flush()
                installAdaptivePageScale(view)
                installMouseKeyboardBridge(view)
                installPlaybackBridge(view)
                captureResumeMetadata(url)
                tryRestoreResumePosition(view)
                scheduleSiteContinue(view)
            }

            override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
                super.doUpdateVisitedHistory(view, url, isReload)
                // ParsiFlix is an SPA; route changes often do not call onPageFinished.
                view.postDelayed({ installAdaptivePageScale(view) }, 350)
                view.postDelayed({ installMouseKeyboardBridge(view) }, 400)
                view.postDelayed({ installPlaybackBridge(view) }, 450)
                url?.let { changedUrl ->
                    view.postDelayed({ captureResumeMetadata(changedUrl) }, 900)
                    view.postDelayed({ tryRestoreResumePosition(view) }, 1_100)
                    view.postDelayed({ scheduleSiteContinue(view) }, 1_200)
                }
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                if (request.isForMainFrame) {
                    lastFailedUrl = request.url.toString()
                    errorView.show(
                        title = "Can't reach $serviceName",
                        message = "Check your internet connection, then try again."
                    )
                }
            }

            override fun onReceivedSslError(
                view: WebView,
                handler: SslErrorHandler,
                error: SslError
            ) {
                handler.cancel()
                lastFailedUrl = webView.url
                errorView.show(
                    title = "Secure connection failed",
                    message = "This site's security certificate could not be verified."
                )
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                if (customView != null) {
                    callback.onCustomViewHidden()
                    return
                }

                customView = view
                customViewCallback = callback
                webView.visibility = View.GONE
                errorView.visibility = View.GONE
                mouseKeyboard.dismiss()
                quickMenu.dismiss()
                (view.parent as? ViewGroup)?.removeView(view)
                root.addView(
                    view,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
                view.requestFocus()
                hideSystemUi()
            }

            override fun onHideCustomView() {
                val fullscreenView = customView ?: return
                root.removeView(fullscreenView)
                customView = null
                customViewCallback?.onCustomViewHidden()
                customViewCallback = null
                webView.visibility = View.VISIBLE
                webView.requestFocus()
                hideSystemUi()
            }
        }
    }

    private suspend fun applyUserAgent() {
        val configuredMode = serviceProfile?.userAgent
            ?.let { value -> UserAgentMode.entries.firstOrNull { it.name == value } }
            ?: app.settingsRepository.userAgentMode.first()
        configuredMode.value?.let { webView.settings.userAgentString = it }
    }

    /**
     * Applies reduced scale only to login/QR routes. Player/content routes remain
     * at 100%, preventing the clipped player seen in the prototype.
     *
     * A MutationObserver handles QR dialogs and video players mounted later by
     * ParsiFlix's single-page application.
     */
    private fun installAdaptivePageScale(view: WebView = webView) {
        val zoom = loginZoomPercent.coerceIn(60, 100)
        val playerSelector = JSONObject.quote(
            serviceAdapter?.playerSelectors.orEmpty().joinToString(",").ifBlank {
                "video,iframe[allowfullscreen],.video-js,.plyr," +
                    "[class*='video-player' i],[class*='player-wrapper' i]"
            }
        )
        val script = """
            (function() {
              window.__aminLoginZoom = $zoom;
              window.__aminPlayerSelector = $playerSelector;

              function visible(el) {
                if (!el) return false;
                var r = el.getBoundingClientRect();
                var s = window.getComputedStyle(el);
                return r.width > 24 && r.height > 24 &&
                       s.display !== 'none' && s.visibility !== 'hidden';
              }

              function isPlayerPage() {
                var video = Array.from(document.querySelectorAll('video')).some(visible);
                var player = Array.from(
                  document.querySelectorAll(window.__aminPlayerSelector)
                ).some(visible);
                return video || player || !!document.fullscreenElement ||
                       !!document.webkitFullscreenElement;
              }

              function isLoginOrQrPage() {
                var path = (location.pathname + location.hash).toLowerCase();
                var route = /login|signin|sign-in|auth|device|pair|qr/.test(path);
                var qr = Array.from(document.querySelectorAll(
                  '[class*="qr" i], [id*="qr" i], img[src*="qr" i], canvas'
                )).some(function(el) {
                  if (!visible(el)) return false;
                  var r = el.getBoundingClientRect();
                  return r.width > 100 && r.height > 100;
                });
                return route || qr;
              }

              window.__aminApplyScale = function() {
                var scale = isPlayerPage() ? 100 :
                            (isLoginOrQrPage() ? window.__aminLoginZoom : 100);
                document.documentElement.style.setProperty('zoom', scale + '%', 'important');
                if (document.body) {
                  document.body.style.setProperty('zoom', '100%', 'important');
                }
                return scale;
              };

              window.__aminApplyScale();

              if (window.__aminScaleObserver) window.__aminScaleObserver.disconnect();
              var timer = 0;
              window.__aminScaleObserver = new MutationObserver(function() {
                clearTimeout(timer);
                timer = setTimeout(window.__aminApplyScale, 120);
              });
              window.__aminScaleObserver.observe(
                document.documentElement,
                { childList: true, subtree: true, attributes: true,
                  attributeFilter: ['class', 'style', 'src'] }
              );
            })();
        """.trimIndent()
        view.evaluateJavascript(script, null)
    }

    /** Hooks normal website inputs to Amin TV OS's mouse-clickable keyboard. */
    private fun installMouseKeyboardBridge(view: WebView = webView) {
        val script = """
            (function() {
              if (window.__aminKeyboardInstalled) return;
              window.__aminKeyboardInstalled = true;
              document.addEventListener('focusin', function(event) {
                var el = event.target;
                if (!el) return;
                var tag = (el.tagName || '').toLowerCase();
                var editable = tag === 'input' || tag === 'textarea' ||
                               el.isContentEditable;
                if (!editable) return;
                var type = (el.type || 'text').toLowerCase();
                if (/button|submit|checkbox|radio|file|range|color/.test(type)) return;
                window.__aminActiveInput = el;
                var value = type === 'password' ? '' : (el.value || el.textContent || '');
                if (window.AminKeyboard) {
                  window.AminKeyboard.open(type, String(value).slice(0, 160));
                }
              }, true);
            })();
        """.trimIndent()
        view.evaluateJavascript(script, null)
    }

    /**
     * Reports only real HTML5 playback events. The bridge intentionally sends
     * the top-level page URL and never reads or stores video.currentSrc.
     */
    private fun installPlaybackBridge(view: WebView = webView) {
        val script = """
            (function() {
              if (window.__aminPlaybackInstalled) return;
              window.__aminPlaybackInstalled = true;
              window.__aminLastPlaybackReport = 0;

              function report(video, reason) {
                if (!video || !window.AminPlayback) return;
                var now = Date.now();
                if (reason === 'timeupdate' &&
                    now - window.__aminLastPlaybackReport < 10000) return;
                window.__aminLastPlaybackReport = now;
                var duration = isFinite(video.duration)
                  ? Math.round(video.duration * 1000) : 0;
                var position = isFinite(video.currentTime)
                  ? Math.round(video.currentTime * 1000) : 0;
                var heading = Array.from(document.querySelectorAll('h1,h2'))
                  .find(function(el) {
                    var text = (el.innerText || '').trim();
                    return text.length > 1 && text.length < 140;
                  });
                window.AminPlayback.update(JSON.stringify({
                  pageUrl: location.href,
                  referrer: document.referrer || '',
                  title: heading ? heading.innerText.trim() : (document.title || ''),
                  poster: video.poster || '',
                  position: position,
                  duration: duration,
                  reason: reason
                }));
              }

              window.__aminReportActivePlayback = function(reason) {
                var video = document.querySelector('video');
                if (video) report(video, reason || 'checkpoint');
              };

              document.addEventListener('play', function(event) {
                if (event.target && event.target.tagName === 'VIDEO') {
                  report(event.target, 'play');
                }
              }, true);
              document.addEventListener('timeupdate', function(event) {
                if (event.target && event.target.tagName === 'VIDEO') {
                  report(event.target, 'timeupdate');
                }
              }, true);
              document.addEventListener('pause', function(event) {
                if (event.target && event.target.tagName === 'VIDEO') {
                  report(event.target, 'pause');
                }
              }, true);
              document.addEventListener('ended', function(event) {
                if (event.target && event.target.tagName === 'VIDEO') {
                  report(event.target, 'ended');
                }
              }, true);
            })();
        """.trimIndent()
        view.evaluateJavascript(script, null)
    }

    private inner class KeyboardBridge {
        @JavascriptInterface
        fun open(inputType: String?, initialValue: String?) {
            runOnUiThread {
                hideSystemKeyboard()
                mouseKeyboard.open(
                    initialValue = initialValue.orEmpty().take(160),
                    inputType = inputType.orEmpty().take(24)
                )
                root.postDelayed({ hideSystemKeyboard() }, 120)
                root.postDelayed({ hideSystemKeyboard() }, 400)
            }
        }
    }

    private inner class PlaybackBridge {
        @JavascriptInterface
        fun update(payload: String?) {
            val data = runCatching { JSONObject(payload.orEmpty()) }.getOrNull()
                ?: return
            runOnUiThread {
                val playbackUrl = data.optString("pageUrl").take(2_000)
                val referrer = data.optString("referrer").take(2_000)
                val adapter = serviceAdapter ?: return@runOnUiThread
                val contentUrl = lastContentUrl.ifBlank {
                    referrer.takeIf { adapter.isContentUrl(it) }.orEmpty()
                }.ifBlank {
                    playbackUrl.takeIf { adapter.isContentUrl(it) }.orEmpty()
                }
                if (contentUrl.isBlank()) return@runOnUiThread

                val position = data.optLong("position").coerceAtLeast(0L)
                val duration = data.optLong("duration").coerceAtLeast(0L)
                val title = lastContentTitle.ifBlank {
                    data.optString("title").trim()
                }
                val poster = lastContentPoster.ifBlank {
                    data.optString("poster").trim()
                }
                currentResumePosition = position
                currentDuration = duration
                currentIsPlayable = true

                lifecycleScope.launch {
                    app.libraryRepository.recordPlayback(
                        serviceId = serviceId,
                        serviceName = serviceName,
                        contentUrl = contentUrl,
                        playbackUrl = playbackUrl,
                        title = title,
                        posterUrl = poster,
                        resumePosition = position,
                        duration = duration,
                        resumeStrategy = adapter.resumeStrategy
                    )
                }
            }
        }
    }

    private inner class PosterBridge {
        @JavascriptInterface
        fun save(
            pageUrl: String?,
            imageUrl: String?,
            mimeType: String?,
            encoded: String?
        ) {
            val page = pageUrl.orEmpty().take(2_000)
            val image = imageUrl.orEmpty().take(2_000)
            val mime = mimeType.orEmpty().lowercase().substringBefore(';')
            val payload = encoded.orEmpty()
            if (payload.length > 2_100_000) return
            val sameHost = runCatching {
                android.net.Uri.parse(page).host.equals(
                    android.net.Uri.parse(image).host,
                    ignoreCase = true
                )
            }.getOrDefault(false)
            if (!sameHost || serviceAdapter?.isContentUrl(page) != true) return
            val bytes = runCatching {
                Base64.decode(payload, Base64.DEFAULT)
            }.getOrNull() ?: return
            lifecycleScope.launch {
                val localUrl = app.libraryRepository.saveLocalPoster(
                    contentUrl = page,
                    mimeType = mime,
                    bytes = bytes
                ) ?: return@launch
                if (lastContentUrl == page) {
                    lastContentPoster = localUrl
                    currentPoster = localUrl
                }
            }
        }
    }

    private fun updateFocusedWebInput(
        value: String,
        submit: Boolean,
        finalize: Boolean
    ) {
        val quoted = org.json.JSONObject.quote(value)
        val script = """
            (function() {
              var el = window.__aminActiveInput || document.activeElement;
              if (!el) return;
              if ('value' in el) {
                var setter = Object.getOwnPropertyDescriptor(
                  HTMLInputElement.prototype, 'value'
                );
                if (setter && el instanceof HTMLInputElement) {
                  setter.set.call(el, $quoted);
                } else {
                  el.value = $quoted;
                }
              } else {
                el.textContent = $quoted;
              }
              el.dispatchEvent(new Event('input', {bubbles:true}));
              el.dispatchEvent(new Event('change', {bubbles:true}));
              if (!$finalize) {
                return JSON.stringify({action:'updated'});
              }
              if ($submit) {
                ['keydown','keypress','keyup'].forEach(function(name) {
                  el.dispatchEvent(new KeyboardEvent(name, {
                    key:'Enter', code:'Enter', keyCode:13, which:13, bubbles:true
                  }));
                });
                if (el.form && (el.type === 'search' || el.type === 'password')) {
                  if (el.form.requestSubmit) el.form.requestSubmit();
                }
                return JSON.stringify({action:'submitted'});
              }
              var fields = Array.from(document.querySelectorAll(
                "input:not([type='hidden']):not([disabled])," +
                "textarea:not([disabled]),[contenteditable='true']"
              )).filter(function(field) {
                var type = (field.type || 'text').toLowerCase();
                var r = field.getBoundingClientRect();
                return !/button|submit|checkbox|radio|file|range|color/.test(type) &&
                       r.width > 20 && r.height > 15;
              });
              var index = fields.indexOf(el);
              var next = index >= 0 ? fields[index + 1] : null;
              if (next) {
                next.scrollIntoView({block:'center', behavior:'smooth'});
                next.focus();
                next.click();
                window.__aminActiveInput = next;
                return JSON.stringify({
                  action:'next',
                  type:(next.type || 'text').toLowerCase()
                });
              }
              el.blur();
              window.__aminActiveInput = null;
              return JSON.stringify({action:'done'});
            })();
        """.trimIndent()
        webView.evaluateJavascript(script) { raw ->
            val result = decodeJavascriptJson(raw)
            if (!finalize) {
                hideSystemKeyboard()
            } else if (result?.optString("action") == "next") {
                mouseKeyboard.open(
                    initialValue = "",
                    inputType = result.optString("type").ifBlank { "text" }
                )
            } else {
                mouseKeyboard.dismiss()
                webView.requestFocus()
            }
            hideSystemKeyboard()
        }
    }

    private fun releaseFocusedWebInput() {
        if (!::webView.isInitialized) return
        webView.evaluateJavascript(
            """
                (function() {
                  var el = window.__aminActiveInput || document.activeElement;
                  if (el && el.blur) el.blur();
                  window.__aminActiveInput = null;
                })();
            """.trimIndent(),
            null
        )
        hideSystemKeyboard()
        webView.requestFocus()
    }

    private fun hideSystemKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(webView.windowToken, 0)
    }

    /**
     * Remote shortcut for sites whose fullscreen control is hard to reach:
     * MENU/red/F11 or long-press OK asks the actual player to enter native
     * fullscreen. Normal mouse clicks on the website's own control remain the
     * preferred path.
     */
    private fun requestPlayerFullscreen() {
        val selectors = serviceProfile?.fullscreenSelectors.orEmpty().joinToString(",")
            .ifBlank {
                "button[aria-label*='fullscreen' i],button[title*='fullscreen' i]," +
                    ".vjs-fullscreen-control,.plyr__control[data-plyr='fullscreen']," +
                    "[class*='fullscreen' i]"
            }
        val safeSelectors = selectors.replace("\\", "\\\\").replace("'", "\\'")
        val script = """
            (function() {
              var buttons = Array.from(document.querySelectorAll('$safeSelectors'));
              var button = buttons.find(function(el) {
                var r = el.getBoundingClientRect();
                return r.width > 0 && r.height > 0;
              });
              if (button) {
                button.click();
                return 'button';
              }

              var video = document.querySelector('video');
              var target = video || document.querySelector('iframe[allowfullscreen]');
              if (!target) return 'none';

              try {
                if (video && video.webkitEnterFullscreen) {
                  video.webkitEnterFullscreen();
                  return 'webkit-video';
                }
                var fn = target.requestFullscreen || target.webkitRequestFullscreen;
                if (fn) {
                  fn.call(target);
                  return 'request';
                }
              } catch (e) {}
              return 'blocked';
            })();
        """.trimIndent()
        webView.evaluateJavascript(script, null)
    }

    private fun openQuickMenu() {
        if (customView != null) return
        if (quickMenu.isShowing) {
            quickMenu.dismiss()
            webView.requestFocus()
            return
        }
        mouseKeyboard.dismiss()
        val url = webView.url.orEmpty()
        val favoriteUrl = if (serviceAdapter?.isPlaybackUrl(url) == true) {
            lastContentUrl.ifBlank { url }
        } else {
            url
        }
        quickMenu.open(
            title = displayPageTitle(),
            serviceName = serviceName,
            isFavorite = app.libraryRepository.isFavorite(favoriteUrl),
            canGoBack = webView.canGoBack()
        )
    }

    private fun handleQuickAction(action: QuickAction) {
        when (action) {
            QuickAction.FULLSCREEN -> requestPlayerFullscreen()
            QuickAction.FAVORITE -> toggleCurrentFavorite()
            QuickAction.SEARCH -> requestSiteSearch()
            QuickAction.RELOAD -> webView.reload()
            QuickAction.BACK -> {
                if (webView.canGoBack()) webView.goBack() else finish()
            }
            QuickAction.HOME -> finish()
        }
    }

    private fun toggleCurrentFavorite() {
        val pageUrl = webView.url.orEmpty()
        val onPlaybackPage = serviceAdapter?.isPlaybackUrl(pageUrl) == true
        val url = if (onPlaybackPage) lastContentUrl.ifBlank { pageUrl } else pageUrl
        if (url.isBlank()) return
        lifecycleScope.launch {
            val favorite = app.libraryRepository.toggleFavoriteForPage(
                serviceId = serviceId,
                serviceName = serviceName,
                url = url,
                title = if (onPlaybackPage) {
                    lastContentTitle.ifBlank { displayPageTitle() }
                } else {
                    displayPageTitle()
                },
                posterUrl = if (onPlaybackPage) {
                    lastContentPoster.ifBlank { currentPoster }
                } else {
                    currentPoster
                },
                resumePosition = currentResumePosition,
                duration = currentDuration,
                isPlayable = currentIsPlayable
            )
            Toast.makeText(
                this@BrowserActivity,
                if (favorite) "Added to Favorites" else "Removed from Favorites",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun requestSiteSearch() {
        val selector = JSONObject.quote(
            serviceAdapter?.searchSelectors.orEmpty().joinToString(",").ifBlank {
                "input[type='search'],input[placeholder*='search' i]," +
                    "input[placeholder*='جست' i],a[href*='search' i]," +
                    "[class*='search' i] input"
            }
        )
        val script = """
            (function() {
              try {
                function findInput() {
                  return document.querySelector(
                    "input[type='search'],input[placeholder*='search' i]," +
                    "input[placeholder*='جست' i],[class*='search' i] input"
                  );
                }
                function focusInput(input) {
                  if (!input) return false;
                  input.scrollIntoView({block:'center', behavior:'smooth'});
                  input.click();
                  input.focus();
                  return true;
                }
                var candidates = Array.from(document.querySelectorAll($selector));
                var target = candidates.find(function(el) {
                  var r = el.getBoundingClientRect();
                  return r.width > 0 && r.height > 0;
                }) || candidates[0];
                if (!target) {
                  target = Array.from(document.querySelectorAll(
                    "a,button,[role='button']"
                  )).find(function(el) {
                    return /search|جستجو|جست‌وجو/i.test(
                      (el.innerText || el.textContent || '').trim()
                    );
                  });
                }
                if (!target) return 'none';
                if (target.matches("input,textarea,[contenteditable='true']")) {
                  focusInput(target);
                  return 'focused';
                }
                target.click();
                setTimeout(function() { focusInput(findInput()); }, 300);
                setTimeout(function() { focusInput(findInput()); }, 900);
                return 'opened';
              } catch (e) {
                return 'none';
              }
            })();
        """.trimIndent()
        webView.evaluateJavascript(script) { result ->
            if (result?.contains("none") == true) {
                Toast.makeText(
                    this,
                    "Search control was not found on this page",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun displayPageTitle(): String {
        if (
            serviceAdapter?.isPlaybackUrl(webView.url.orEmpty()) == true &&
            lastContentTitle.isNotBlank()
        ) return lastContentTitle.take(100)
        val raw = currentTitle.ifBlank { webView.title.orEmpty() }.trim()
        val looksLikeAsset = Regex(
            """(?:^[a-f0-9]{20,}\.(?:jpe?g|png|webp)$)|(?:\.(?:jpe?g|png|webp)$)""",
            RegexOption.IGNORE_CASE
        ).containsMatchIn(raw)
        return if (raw.isBlank() || looksLikeAsset) serviceName else raw.take(100)
    }

    private fun scheduleSiteContinue(view: WebView = webView) {
        val adapter = serviceAdapter ?: return
        if (
            !autoResumeRequested ||
            autoResumeActionTriggered ||
            (requestedResumeStrategy ?: adapter.resumeStrategy) !=
                ResumeStrategy.CLICK_SITE_CONTINUE
        ) return
        listOf(250L, 800L, 1_600L, 3_000L).forEach { delay ->
            view.postDelayed({ tryClickSiteContinue(view) }, delay)
        }
    }

    /**
     * Activates a normal visible website action on a stable detail page.
     * ParsiFlix uses Continue Watching; account-synced FilmRooz items use
     * Play Online. No media URL is read or touched.
     */
    private fun tryClickSiteContinue(view: WebView = webView) {
        if (autoResumeActionTriggered) return
        val adapter = serviceAdapter ?: return
        if (!adapter.isContentUrl(view.url.orEmpty())) return
        val textPatterns = requestedActionButtonPatterns.ifEmpty {
            adapter.resumeButtonTextPatterns
        }
        if (textPatterns.isEmpty()) return
        val pattern = JSONObject.quote(
            textPatterns.joinToString("|") { "(?:$it)" }
        )
        val script = """
            (function() {
              try {
                var matcher = new RegExp($pattern, 'i');
                var candidates = Array.from(document.querySelectorAll(
                  'button,[role="button"],a'
                )).filter(function(el) {
                  var text = (el.innerText || el.textContent || '').trim();
                  var r = el.getBoundingClientRect();
                  return text && matcher.test(text) &&
                         r.width > 30 && r.height > 20;
                });
                if (!candidates.length) return 'none';
                candidates.sort(function(a, b) {
                  function score(el) {
                    var text = (el.innerText || '').trim();
                    var r = el.getBoundingClientRect();
                    return r.width + text.length * 4 +
                      (/فصل|قسمت|episode|season/i.test(text) ? 1000 : 0);
                  }
                  return score(b) - score(a);
                });
                candidates[0].click();
                return (candidates[0].innerText || 'clicked').trim().slice(0, 120);
              } catch (e) {
                return 'none';
              }
            })();
        """.trimIndent()
        view.evaluateJavascript(script) { result ->
            if (!result.isNullOrBlank() && result != "\"none\"" && result != "null") {
                autoResumeActionTriggered = true
                view.postDelayed({ tryRestoreResumePosition(view) }, 1_000L)
            }
        }
    }

    private fun captureResumeMetadata(url: String) {
        val playerSelector = JSONObject.quote(
            serviceAdapter?.playerSelectors.orEmpty().joinToString(",").ifBlank {
                "video,iframe[allowfullscreen],.video-js,.plyr," +
                    "[class*='video-player' i],[class*='player-wrapper' i]"
            }
        )
        val script = """
            (function() {
                function content(selector) {
                  var el = document.querySelector(selector);
                  return el ? (el.content || el.src || '') : '';
                }
                var video = document.querySelector('video');
                var player = null;
                try { player = document.querySelector($playerSelector); } catch (e) {}
                var position = video && isFinite(video.currentTime)
                  ? Math.round(video.currentTime * 1000) : 0;
                var duration = video && isFinite(video.duration)
                  ? Math.round(video.duration * 1000) : 0;
                var largeImage = Array.from(document.images || []).find(function(img) {
                  var r = img.getBoundingClientRect();
                  var text = ((img.alt || '') + ' ' + (img.className || '')).toLowerCase();
                  return r.width >= 160 && r.height >= 160 &&
                         !/logo|avatar|icon|profile|qr/.test(text);
                });
                var poster = content('meta[property="og:image"]') ||
                             content('meta[name="twitter:image"]') ||
                             (video ? (video.poster || '') : '') ||
                             (largeImage ? (largeImage.currentSrc || largeImage.src || '') : '');
                var heading = Array.from(document.querySelectorAll('h1,h2'))
                  .find(function(el) {
                    var r = el.getBoundingClientRect();
                    var text = (el.innerText || '').trim();
                    return r.width > 0 && r.height > 0 &&
                           text.length > 1 && text.length < 140;
                  });
                var imageTitle = largeImage ? (largeImage.alt || '') : '';
                imageTitle = imageTitle.replace(/\s*کاور\s*$/i, '').trim();
                return JSON.stringify({
                    // Several TV sites keep a generic app name in og:title.
                    // Prefer the visible content heading for a useful card.
                    title: (heading ? heading.innerText.trim() : '') ||
                           imageTitle ||
                           content('meta[property="og:title"]') ||
                           document.title || '',
                    poster: poster,
                    position: position,
                    duration: duration,
                    playable: !!(video || player)
                });
            })();
        """.trimIndent()
        webView.evaluateJavascript(script) { raw ->
            val payload = decodeJavascriptJson(raw) ?: return@evaluateJavascript
            val title = payload.optString("title").trim()
            val poster = payload.optString("poster").trim()
            val position = payload.optLong("position").coerceAtLeast(0L)
            val duration = payload.optLong("duration").coerceAtLeast(0L)
            val detectedPlayer = payload.optBoolean("playable")
            val adapter = serviceAdapter
            val isContent = adapter?.isContentUrl(url) == true
            val playable = detectedPlayer || adapter?.isPlaybackUrl(url) == true
            val savedLocalPoster = if (isContent) {
                app.libraryRepository.localPoster(url)
            } else {
                null
            }
            val resolvedPoster = savedLocalPoster ?: poster

            currentTitle = title
            currentPoster = resolvedPoster
            currentResumePosition = position
            currentDuration = duration
            currentIsPlayable = playable
            if (isContent) {
                lastContentUrl = url
                lastContentTitle = title
                lastContentPoster = resolvedPoster
            }

            if (adapter?.shouldRecord(
                    url = url,
                    title = title,
                    hasPoster = resolvedPoster.isNotBlank(),
                    playerDetected = detectedPlayer
                ) != true
            ) {
                return@evaluateJavascript
            }

            lifecycleScope.launch {
                app.libraryRepository.recordVisit(
                    serviceId = serviceId,
                    serviceName = serviceName,
                    url = url,
                    title = title,
                    posterUrl = resolvedPoster,
                    resumePosition = 0L,
                    duration = 0L,
                    isPlayable = false
                )
            }
            if (
                isContent &&
                savedLocalPoster == null &&
                poster.startsWith("http")
            ) {
                webView.postDelayed(
                    { cacheAuthenticatedPoster(url, poster) },
                    500L
                )
            }
        }
    }

    private fun decodeJavascriptJson(raw: String?): JSONObject? = runCatching {
        val decoded = JSONTokener(raw.orEmpty()).nextValue() as? String
            ?: return@runCatching null
        JSONObject(decoded)
    }.getOrNull()

    private fun cacheAuthenticatedPoster(pageUrl: String, posterUrl: String) {
        val sameHost = runCatching {
            android.net.Uri.parse(pageUrl).host.equals(
                android.net.Uri.parse(posterUrl).host,
                ignoreCase = true
            )
        }.getOrDefault(false)
        if (!sameHost || app.libraryRepository.hasLocalPoster(pageUrl)) return
        val safePage = JSONObject.quote(pageUrl)
        val safePoster = JSONObject.quote(posterUrl)
        val script = """
            (function() {
              if (!window.AminPoster) return;
              window.__aminPosterCache = window.__aminPosterCache || {};
              if (window.__aminPosterCache[$safePoster]) return;
              window.__aminPosterCache[$safePoster] = true;
              fetch($safePoster, {credentials:'include'})
                .then(function(response) {
                  var type = (response.headers.get('content-type') || '')
                    .toLowerCase().split(';')[0];
                  if (!response.ok || type.indexOf('image/') !== 0) {
                    throw new Error('not-image');
                  }
                  return response.blob().then(function(blob) {
                    return {blob:blob, type:type};
                  });
                })
                .then(function(result) {
                  if (result.blob.size > 1500000) throw new Error('too-large');
                  var reader = new FileReader();
                  reader.onload = function() {
                    var value = String(reader.result || '');
                    var comma = value.indexOf(',');
                    if (comma < 0) return;
                    window.AminPoster.save(
                      $safePage,
                      $safePoster,
                      result.type,
                      value.slice(comma + 1)
                    );
                  };
                  reader.readAsDataURL(result.blob);
                })
                .catch(function() {
                  window.__aminPosterCache[$safePoster] = false;
                });
            })();
        """.trimIndent()
        webView.evaluateJavascript(script, null)
    }

    /**
     * Best-effort resume for ordinary HTML5 video. Some DRM or iframe players
     * intentionally prevent seeking from the host page; in that case the exact
     * content URL still resumes and the website remains in control.
     */
    private fun tryRestoreResumePosition(view: WebView = webView) {
        if (requestedResumePosition < 5_000L || resumeApplied) return
        val seconds = requestedResumePosition / 1000.0
        val script = """
            (function() {
              window.__aminResumeTarget = $seconds;
              function applyResume() {
                var video = document.querySelector('video');
                if (!video || !isFinite(video.duration) || video.duration <= 0) return false;
                var target = Math.min(
                  window.__aminResumeTarget,
                  Math.max(0, video.duration - 10)
                );
                try {
                  if (Math.abs((video.currentTime || 0) - target) > 3) {
                    video.currentTime = target;
                  }
                  return true;
                } catch (e) {
                  return false;
                }
              }
              if (applyResume()) return true;
              if (!window.__aminResumeTimer) {
                var tries = 0;
                window.__aminResumeTimer = setInterval(function() {
                  tries++;
                  if (applyResume() || tries >= 30) {
                    clearInterval(window.__aminResumeTimer);
                    window.__aminResumeTimer = 0;
                  }
                }, 500);
              }
              return false;
            })();
        """.trimIndent()
        view.evaluateJavascript(script) { result ->
            if (result == "true") resumeApplied = true
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    quickMenu.isShowing -> {
                        quickMenu.dismiss()
                        webView.requestFocus()
                    }
                    mouseKeyboard.isShowing -> mouseKeyboard.dismiss()
                    customView != null -> webView.webChromeClient?.onHideCustomView()
                    errorView.visibility == View.VISIBLE && webView.canGoBack() -> {
                        errorView.visibility = View.GONE
                        webView.goBack()
                    }
                    webView.canGoBack() -> webView.goBack()
                    else -> finish()
                }
            }
        })
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && mouseKeyboard.isShowing) {
            mouseKeyboard.dismiss()
            return true
        }
        when (keyCode) {
            KeyEvent.KEYCODE_MENU,
            KeyEvent.KEYCODE_INFO -> {
                openQuickMenu()
                return true
            }
            KeyEvent.KEYCODE_PROG_RED,
            KeyEvent.KEYCODE_F11 -> {
                requestPlayerFullscreen()
                return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> {
                if (event.repeatCount >= 1) {
                    requestPlayerFullscreen()
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (
            event.keyCode == KeyEvent.KEYCODE_BACK &&
            mouseKeyboard.isShowing
        ) {
            if (event.action == KeyEvent.ACTION_DOWN) mouseKeyboard.dismiss()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        // USB mouse back/forward buttons behave like browser navigation.
        if (event.actionMasked == MotionEvent.ACTION_BUTTON_PRESS) {
            when (event.actionButton) {
                MotionEvent.BUTTON_SECONDARY -> {
                    if (mouseKeyboard.isShowing) {
                        mouseKeyboard.dismiss()
                        return true
                    }
                    openQuickMenu()
                    return true
                }
                MotionEvent.BUTTON_BACK -> {
                    onBackPressedDispatcher.onBackPressed()
                    return true
                }
                MotionEvent.BUTTON_FORWARD -> {
                    if (webView.canGoForward()) webView.goForward()
                    return true
                }
            }
        }
        // Left click and wheel scrolling are deliberately left to WebView.
        return super.dispatchGenericMotionEvent(event)
    }

    private fun retry() {
        errorView.visibility = View.GONE
        val target = lastFailedUrl ?: webView.url
        if (target != null) webView.loadUrl(target) else webView.reload()
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

    override fun onPause() {
        metadataHandler.removeCallbacks(metadataRunnable)
        if (::webView.isInitialized) {
            webView.evaluateJavascript(
                "if(window.__aminReportActivePlayback)" +
                    "window.__aminReportActivePlayback('checkpoint');",
                null
            )
            webView.url?.let { captureResumeMetadata(it) }
        }
        CookieManager.getInstance().flush()
        webView.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        metadataHandler.removeCallbacks(metadataRunnable)
        metadataHandler.postDelayed(metadataRunnable, 8_000L)
        hideSystemUi()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onDestroy() {
        metadataHandler.removeCallbacks(metadataRunnable)
        customView?.let { root.removeView(it) }
        customView = null
        webView.apply {
            stopLoading()
            (parent as? ViewGroup)?.removeView(this)
            destroy()
        }
        super.onDestroy()
    }
}
