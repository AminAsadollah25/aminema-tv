package com.amin.tvos.browser

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.http.SslError
import android.os.Bundle
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
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.amin.tvos.AminTvApp
import com.amin.tvos.data.model.StreamingService
import com.amin.tvos.data.model.UserAgentMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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

        fun intent(context: Context, serviceId: String, url: String): Intent =
            Intent(context, BrowserActivity::class.java)
                .putExtra(EXTRA_SERVICE_ID, serviceId)
                .putExtra(EXTRA_URL, url)
    }

    private lateinit var root: FrameLayout
    private lateinit var webView: WebView
    private lateinit var errorView: TvErrorView
    private lateinit var mouseKeyboard: MouseKeyboardOverlay

    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    private var serviceId = ""
    private var serviceName = ""
    private var serviceProfile: StreamingService? = null
    private var loginZoomPercent = 85
    private var lastFailedUrl: String? = null

    private val app get() = application as AminTvApp

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemUi()

        serviceId = intent.getStringExtra(EXTRA_SERVICE_ID).orEmpty()
        val startUrl = intent.getStringExtra(EXTRA_URL).orEmpty()

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
            onValueChanged = { updateFocusedWebInput(it, submit = false) },
            onAction = { value, submit -> updateFocusedWebInput(value, submit) }
        )
        root.addView(
            mouseKeyboard,
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
            serviceName = serviceProfile?.name ?: serviceId
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

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                errorView.visibility = View.GONE
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
                captureResumeMetadata(url)
            }

            override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
                super.doUpdateVisitedHistory(view, url, isReload)
                // ParsiFlix is an SPA; route changes often do not call onPageFinished.
                view.postDelayed({ installAdaptivePageScale(view) }, 350)
                view.postDelayed({ installMouseKeyboardBridge(view) }, 400)
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
        val script = """
            (function() {
              window.__aminLoginZoom = $zoom;

              function visible(el) {
                if (!el) return false;
                var r = el.getBoundingClientRect();
                var s = window.getComputedStyle(el);
                return r.width > 24 && r.height > 24 &&
                       s.display !== 'none' && s.visibility !== 'hidden';
              }

              function isPlayerPage() {
                var video = Array.from(document.querySelectorAll('video')).some(visible);
                var player = Array.from(document.querySelectorAll(
                  'iframe[allowfullscreen], iframe[src*="player"], iframe[src*="embed"],' +
                  '.video-js, .plyr, [class*="video-player"], [class*="player-wrapper"]'
                )).some(visible);
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

    private fun updateFocusedWebInput(value: String, submit: Boolean) {
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
              if ($submit) {
                ['keydown','keypress','keyup'].forEach(function(name) {
                  el.dispatchEvent(new KeyboardEvent(name, {
                    key:'Enter', code:'Enter', keyCode:13, which:13, bubbles:true
                  }));
                });
                if (el.form && (el.type === 'search' || el.type === 'password')) {
                  if (el.form.requestSubmit) el.form.requestSubmit();
                }
              }
            })();
        """.trimIndent()
        webView.evaluateJavascript(script, null)
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

    private fun captureResumeMetadata(url: String) {
        val script = """
            (function() {
                var img = document.querySelector('meta[property="og:image"]');
                return JSON.stringify({
                    title: document.title || '',
                    poster: img ? (img.content || '') : ''
                });
            })();
        """.trimIndent()
        webView.evaluateJavascript(script) { raw ->
            val cleaned = raw?.trim('"')
                ?.replace("\\\"", "\"")
                ?.replace("\\\\", "\\")
                ?: return@evaluateJavascript
            val title = Regex("\"title\":\"(.*?)\"")
                .find(cleaned)?.groupValues?.get(1).orEmpty()
            val poster = Regex("\"poster\":\"(.*?)\"")
                .find(cleaned)?.groupValues?.get(1).orEmpty()
            lifecycleScope.launch {
                app.libraryRepository.recordVisit(
                    serviceId = serviceId,
                    serviceName = serviceName,
                    url = url,
                    title = title,
                    posterUrl = poster
                )
            }
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
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
        when (keyCode) {
            KeyEvent.KEYCODE_MENU,
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

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        // USB mouse back/forward buttons behave like browser navigation.
        if (event.actionMasked == MotionEvent.ACTION_BUTTON_PRESS) {
            when (event.actionButton) {
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
        CookieManager.getInstance().flush()
        webView.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        hideSystemUi()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onDestroy() {
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
