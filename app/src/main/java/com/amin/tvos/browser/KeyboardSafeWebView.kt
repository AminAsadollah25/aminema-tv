package com.amin.tvos.browser

import android.content.Context
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.webkit.WebView

/**
 * WebView used with Amin TV OS's own mouse/remote keyboard.
 *
 * Some Android boxes keep their system TV IME attached after an HTML input
 * loses focus. That IME consumes Back/Next before BrowserActivity can handle
 * them and leaves the page in an apparently locked state. Returning no native
 * input connection prevents that second keyboard from opening; text is still
 * written through the explicit, same-page JavaScript bridge.
 */
class KeyboardSafeWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : WebView(context, attrs) {

    override fun onCheckIsTextEditor(): Boolean = false

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? = null
}
