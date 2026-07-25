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

/**
 * A mouse-clickable keyboard owned by Aminema.
 *
 * Android TV IMEs are commonly DPAD-only. This overlay keeps login/search usable
 * on boxes where a USB mouse is the primary input device.
 */
class MouseKeyboardOverlay(
    context: Context,
    private val onValueChanged: (String) -> Unit,
    private val onAction: (String, Boolean) -> Unit,
    private val onDismissed: () -> Unit,
    private val onModeChanged: () -> Unit
) : FrameLayout(context) {

    private val panel = LinearLayout(context)
    private val preview = TextView(context)
    private val keysContainer = LinearLayout(context)
    private val actionButton = Button(context)
    private val revealButton = Button(context)
    private val alphabetButtons = mutableListOf<Button>()
    private lateinit var capsButton: Button
    private lateinit var languageButton: Button

    private var buffer = ""
    private var passwordMode = false
    private var searchMode = false
    private var persian = false
    private var capsLock = false
    private var showPassword = false

    val isShowing: Boolean get() = visibility == View.VISIBLE
    val isPasswordMode: Boolean get() = passwordMode

    init {
        visibility = View.GONE
        isClickable = true
        isFocusable = true
        isFocusableInTouchMode = true
        setBackgroundColor(Color.parseColor("#99000000"))
        setOnClickListener { dismiss() }

        panel.apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(28), dp(18), dp(28), dp(20))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F21A1A22"))
                cornerRadii = floatArrayOf(
                    dp(22).toFloat(), dp(22).toFloat(),
                    dp(22).toFloat(), dp(22).toFloat(),
                    0f, 0f, 0f, 0f
                )
            }
            setOnClickListener { /* Keep clicks inside the panel. */ }
        }
        addView(
            panel,
            LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
            )
        )

        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        preview.apply {
            setTextColor(Color.WHITE)
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 18f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), 0, dp(18), 0)
            background = rounded("#262632", 14)
            maxLines = 1
        }
        header.addView(
            preview,
            LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                marginEnd = dp(10)
                }
        )
        revealButton.apply {
            visibility = View.GONE
            text = "Show"
            isAllCaps = false
            setTextColor(Color.WHITE)
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15f)
            background = rounded("#30303C", 12)
            setOnClickListener {
                showPassword = !showPassword
                text = if (showPassword) "Hide" else "Show"
                updatePreview()
                onModeChanged()
            }
        }
        header.addView(
            revealButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(48)
            ).apply { marginEnd = dp(10) }
        )
        header.addView(makeControlButton("Cancel") { dismiss() })
        panel.addView(
            header,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        keysContainer.orientation = LinearLayout.VERTICAL
        panel.addView(
            keysContainer,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(12) }
        )

        actionButton.apply {
            isAllCaps = false
            setTextColor(Color.WHITE)
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 17f)
            typeface = Typeface.DEFAULT_BOLD
            background = rounded("#E50914", 12)
            setOnClickListener {
                // BrowserActivity dismisses after the website confirms whether
                // this action submitted or moved focus to the password field.
                onAction(buffer, searchMode || passwordMode)
            }
        }

        rebuildKeys()
    }

    fun open(initialValue: String, inputType: String) {
        passwordMode = inputType.equals("password", ignoreCase = true)
        searchMode = inputType.equals("search", ignoreCase = true)
        showPassword = false
        revealButton.text = "Show"
        revealButton.visibility = if (passwordMode) View.VISIBLE else View.GONE
        // Never pull an existing password out of the website into Android UI.
        buffer = if (passwordMode) "" else initialValue.take(160)
        actionButton.text = when {
            searchMode -> "Search"
            passwordMode -> "Done"
            else -> "Next"
        }
        updatePreview()
        visibility = View.VISIBLE
        bringToFront()
        requestFocus()
    }

    fun dismiss() {
        if (!isShowing) return
        visibility = View.GONE
        onDismissed()
    }

    private fun rebuildKeys() {
        // Move Android focus to a stable view before removing key rows. On
        // several physical TV boxes, removing the currently focused Caps/فا
        // button hands focus back to WebView and reactivates Username.
        val focusedLabel = (findFocus() as? Button)?.text?.toString()
        requestFocus()
        keysContainer.removeAllViews()
        alphabetButtons.clear()

        addKeyRow(listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"))
        if (persian) {
            addKeyRow(listOf("ض", "ص", "ث", "ق", "ف", "غ", "ع", "ه", "خ", "ح", "ج", "چ"))
            addKeyRow(listOf("ش", "س", "ی", "ب", "ل", "ا", "ت", "ن", "م", "ک", "گ"))
            addKeyRow(listOf("ظ", "ط", "ز", "ر", "ذ", "د", "پ", "و"))
        } else {
            val rows = listOf(
                listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
                listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
                listOf("z", "x", "c", "v", "b", "n", "m")
            )
            rows.forEach { row ->
                addAlphabetRow(row)
            }
        }

        val controls = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        capsButton = makeWeightedButton(if (capsLock) "CAPS ●" else "Caps") {
            capsLock = !capsLock
            updateCapsLabels()
        }
        controls.addView(capsButton)
        languageButton = makeWeightedButton(if (persian) "EN" else "فا") {
            persian = !persian
            rebuildKeys()
            post { onModeChanged() }
        }
        controls.addView(languageButton)
        controls.addView(makeWeightedButton("@") { append("@") })
        controls.addView(makeWeightedButton(".") { append(".") })
        controls.addView(makeWeightedButton("_") { append("_") })
        controls.addView(makeWeightedButton("-") { append("-") })
        controls.addView(
            makeWeightedButton("Space", weight = 2f) { append(" ") }
        )
        controls.addView(makeWeightedButton("⌫") {
            if (buffer.isNotEmpty()) {
                buffer = buffer.dropLast(1)
                valueChanged()
            }
        })
        // The action button is reused between keyboard rebuilds (Caps/language).
        // Detach it from the old controls row before attaching it to the new row.
        (actionButton.parent as? ViewGroup)?.removeView(actionButton)
        controls.addView(
            actionButton,
            LinearLayout.LayoutParams(0, dp(46), 1.5f).apply {
                setMargins(dp(4), dp(4), dp(4), dp(4))
            }
        )
        keysContainer.addView(
            controls,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        when {
            focusedLabel == "Caps" || focusedLabel == "CAPS ●" ->
                capsButton.requestFocus()
            focusedLabel == "فا" || focusedLabel == "EN" ->
                languageButton.requestFocus()
            else -> requestFocus()
        }
    }

    /**
     * Caps only changes labels in place. Rebuilding the whole native keyboard
     * here used to remove the focused Caps button and could return DOM focus to
     * FilmRooz's username input on older Android TV boxes.
     */
    private fun updateCapsLabels() {
        capsButton.text = if (capsLock) "CAPS ●" else "Caps"
        alphabetButtons.forEach { button ->
            val base = button.tag as? String ?: return@forEach
            button.text = if (capsLock) base.uppercase() else base
        }
        capsButton.requestFocus()
        post { onModeChanged() }
    }

    private fun addAlphabetRow(keys: List<String>) {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        keys.forEach { base ->
            val button = makeWeightedButton(
                if (capsLock) base.uppercase() else base
            ) {
                append(if (capsLock) base.uppercase() else base)
            }.apply { tag = base }
            alphabetButtons += button
            row.addView(button)
        }
        keysContainer.addView(
            row,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
    }

    private fun addKeyRow(keys: List<String>) {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        keys.forEach { key ->
            row.addView(makeWeightedButton(key) { append(key) })
        }
        keysContainer.addView(
            row,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
    }

    private fun append(value: String) {
        if (buffer.length >= 160) return
        buffer += when {
            persian -> value
            capsLock -> value.uppercase()
            else -> value.lowercase()
        }
        valueChanged()
    }

    private fun valueChanged() {
        updatePreview()
        onValueChanged(buffer)
    }

    private fun updatePreview() {
        preview.text = when {
            buffer.isEmpty() -> if (passwordMode) "Password" else "Type with mouse…"
            passwordMode && !showPassword -> "•".repeat(buffer.length)
            else -> buffer
        }
    }

    private fun makeWeightedButton(
        label: String,
        weight: Float = 1f,
        action: () -> Unit
    ): Button = makeButton(label, action).apply {
        layoutParams = LinearLayout.LayoutParams(0, dp(46), weight).apply {
            setMargins(dp(4), dp(4), dp(4), dp(4))
        }
    }

    private fun makeControlButton(label: String, action: () -> Unit): Button =
        makeButton(label, action).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(48)
            )
        }

    private fun makeButton(label: String, action: () -> Unit): Button =
        Button(context).apply {
            text = label
            isAllCaps = false
            minWidth = 0
            minimumWidth = 0
            minHeight = 0
            minimumHeight = 0
            setPadding(dp(4), 0, dp(4), 0)
            setTextColor(Color.WHITE)
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15f)
            background = rounded("#30303C", 10)
            setOnFocusChangeListener { view, focused ->
                view.background = rounded(if (focused) "#E50914" else "#30303C", 10)
            }
            setOnClickListener { action() }
        }

    private fun rounded(color: String, radiusDp: Int) =
        GradientDrawable().apply {
            setColor(Color.parseColor(color))
            cornerRadius = dp(radiusDp).toFloat()
        }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
