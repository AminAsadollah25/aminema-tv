package com.amin.tvos.browser

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

enum class QuickAction {
    FULLSCREEN,
    FAVORITE,
    SEARCH,
    RELOAD,
    BACK,
    HOME
}

/**
 * A remote- and mouse-friendly browser menu.
 *
 * It stays completely hidden during normal browsing and playback. MENU/INFO or
 * a mouse right-click opens it, so no floating control covers website players.
 */
class QuickMenuOverlay(
    context: Context,
    private val onAction: (QuickAction) -> Unit
) : FrameLayout(context) {

    private val panel = LinearLayout(context)
    private val titleView = TextView(context)
    private val serviceView = TextView(context)
    private val actions = LinearLayout(context)

    val isShowing: Boolean get() = visibility == View.VISIBLE

    init {
        visibility = View.GONE
        isClickable = true
        isFocusable = true
        setBackgroundColor(Color.parseColor("#A6000000"))
        setOnClickListener { dismiss() }

        panel.apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(22), dp(24), dp(16))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#FA15151D"))
                cornerRadii = floatArrayOf(
                    dp(24).toFloat(), dp(24).toFloat(),
                    0f, 0f,
                    0f, 0f,
                    dp(24).toFloat(), dp(24).toFloat()
                )
            }
            setOnClickListener { /* Do not close for clicks inside the panel. */ }
        }
        addView(
            panel,
            LayoutParams(dp(440), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.END)
        )

        TextView(context).apply {
            text = "AMIN TV  •  QUICK MENU"
            setTextColor(Color.parseColor("#E50914"))
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15f)
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.08f
            panel.addView(
                this,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }

        serviceView.apply {
            setTextColor(Color.parseColor("#AEB0BE"))
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(0, dp(12), 0, dp(2))
            panel.addView(this)
        }
        titleView.apply {
            setTextColor(Color.WHITE)
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 20f)
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 2
            panel.addView(
                this,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }

        View(context).also { spacer ->
            panel.addView(
                spacer,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
            )
        }

        actions.orientation = LinearLayout.VERTICAL
        panel.addView(
            actions,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        TextView(context).apply {
            text = "MENU / INFO / Right-click to close"
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#797B88"))
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(0, dp(8), 0, 0)
            panel.addView(
                this,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
    }

    fun open(
        title: String,
        serviceName: String,
        isFavorite: Boolean,
        canGoBack: Boolean
    ) {
        titleView.text = title.ifBlank { "Current page" }
        serviceView.text = serviceName
        rebuildActions(isFavorite, canGoBack)
        visibility = View.VISIBLE
        bringToFront()
        actions.getChildAt(0)?.post { actions.getChildAt(0)?.requestFocus() }
    }

    fun dismiss() {
        visibility = View.GONE
    }

    private fun rebuildActions(isFavorite: Boolean, canGoBack: Boolean) {
        actions.removeAllViews()
        addAction("⛶   Fullscreen", QuickAction.FULLSCREEN)
        addAction(
            if (isFavorite) "♥   Remove from Favorites" else "♡   Add to Favorites",
            QuickAction.FAVORITE
        )
        addAction("⌕   Search this service", QuickAction.SEARCH)
        addAction("↻   Reload page", QuickAction.RELOAD)
        if (canGoBack) addAction("←   Browser back", QuickAction.BACK)
        addAction("⌂   Amin TV Home", QuickAction.HOME, accent = true)
    }

    private fun addAction(label: String, action: QuickAction, accent: Boolean = false) {
        actions.addView(
            Button(context).apply {
                text = label
                isAllCaps = false
                isFocusable = true
                isFocusableInTouchMode = true
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                setPadding(dp(22), 0, dp(16), 0)
                setTextColor(Color.WHITE)
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16f)
                typeface = if (accent) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                minHeight = 0
                minimumHeight = 0
                background = rounded(if (accent) "#B80B14" else "#2B2B36", 12)
                setOnFocusChangeListener { view, focused ->
                    view.background = rounded(
                        if (focused) "#E50914"
                        else if (accent) "#B80B14"
                        else "#2B2B36",
                        12
                    )
                }
                setOnClickListener {
                    dismiss()
                    onAction(action)
                }
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(46)
            ).apply { setMargins(0, dp(2), 0, dp(2)) }
        )
    }

    private fun rounded(color: String, radiusDp: Int) =
        GradientDrawable().apply {
            setColor(Color.parseColor(color))
            cornerRadius = dp(radiusDp).toFloat()
        }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
