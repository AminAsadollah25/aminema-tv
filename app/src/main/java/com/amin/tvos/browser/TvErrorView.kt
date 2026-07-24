package com.amin.tvos.browser

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Lightweight TV-friendly error overlay for the browser layer.
 * Big text, big DPAD-focusable retry button, dark cinematic styling.
 */
class TvErrorView(
    context: Context,
    private val onRetry: () -> Unit
) : LinearLayout(context) {

    private val titleView = TextView(context).apply {
        setTextColor(Color.WHITE)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 34f)
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
    }

    private val messageView = TextView(context).apply {
        setTextColor(Color.parseColor("#9E9EA8"))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
        gravity = Gravity.CENTER
        setPadding(0, dp(12), 0, dp(28))
    }

    private val retryButton = Button(context).apply {
        text = "▶  Retry"
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
        setTextColor(Color.WHITE)
        setBackgroundColor(Color.parseColor("#E50914"))
        setPadding(dp(36), dp(14), dp(36), dp(14))
        isFocusable = true
        isFocusableInTouchMode = false
        setOnClickListener { onRetry() }
    }

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        setBackgroundColor(Color.parseColor("#0B0B0F"))
        addView(titleView)
        addView(messageView)
        addView(retryButton)
    }

    fun show(title: String, message: String) {
        titleView.text = title
        messageView.text = message
        visibility = VISIBLE
        retryButton.requestFocus()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
