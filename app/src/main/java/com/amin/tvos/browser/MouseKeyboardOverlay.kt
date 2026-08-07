package com.amin.tvos.browser

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

enum class KeyboardInputKind {
    TEXT,
    PASSWORD,
    SEARCH;

    companion object {
        fun fromHtmlType(type: String): KeyboardInputKind = when {
            type.equals("password", ignoreCase = true) -> PASSWORD
            type.equals("search", ignoreCase = true) -> SEARCH
            else -> TEXT
        }
    }
}

enum class KeyboardAction {
    NEXT,
    SUBMIT
}

private enum class KeyboardLanguage { ENGLISH, PERSIAN }

private data class KeyboardDeckState(
    val sessionId: Long = 0L,
    val kind: KeyboardInputKind = KeyboardInputKind.TEXT,
    val buffer: String = "",
    val language: KeyboardLanguage = KeyboardLanguage.ENGLISH,
    val capsLock: Boolean = false,
    val revealPassword: Boolean = false
)

/**
 * Aminema's mouse/DPAD Input Deck for ordinary website fields.
 *
 * All key rows are created exactly once. Caps, language and password reveal only
 * render a new immutable [KeyboardDeckState]; they never remove the focused row.
 * Every callback carries a session id, so a delayed click cannot write into a
 * newer DOM field after Username → Password.
 */
class MouseKeyboardOverlay(
    context: Context,
    private val onValueChanged: (sessionId: Long, value: String) -> Unit,
    private val onAction: (sessionId: Long, value: String, action: KeyboardAction) -> Unit,
    private val onDismissed: (sessionId: Long) -> Unit,
    private val onInteraction: (sessionId: Long) -> Unit
) : FrameLayout(context) {

    private val panel = LinearLayout(context)
    private val fieldBadge = TextView(context)
    private val preview = TextView(context)
    private val revealButton: Button
    private val closeButton: Button
    private val englishKeys = LinearLayout(context)
    private val persianKeys = LinearLayout(context)
    private val actionButton: Button
    private val capsButton: Button
    private val languageButton: Button
    private val persianSpecialButtons = mutableListOf<Button>()
    private val alphabetButtons = mutableListOf<Button>()
    private var englishFocusAnchor: Button? = null
    private var persianFocusAnchor: Button? = null

    private var state = KeyboardDeckState()

    val isShowing: Boolean get() = visibility == View.VISIBLE
    val isPasswordMode: Boolean get() = state.kind == KeyboardInputKind.PASSWORD
    val currentSessionId: Long get() = state.sessionId

    init {
        visibility = View.GONE
        isClickable = true
        isFocusable = true
        isFocusableInTouchMode = true
        setBackgroundColor(Color.parseColor("#B0000000"))
        setOnClickListener { dismiss() }

        panel.apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(14), dp(24), dp(16))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#FA15151D"))
                cornerRadii = floatArrayOf(
                    dp(24).toFloat(), dp(24).toFloat(),
                    dp(24).toFloat(), dp(24).toFloat(),
                    0f, 0f, 0f, 0f
                )
                setStroke(dp(1), Color.parseColor("#30303B"))
            }
            elevation = dp(18).toFloat()
            setOnClickListener { /* Keep panel clicks away from the dimmed scrim. */ }
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
        fieldBadge.apply {
            setTextColor(Color.parseColor("#F5F5F7"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            background = rounded("#3A1218", 50, "#74222A")
        }
        header.addView(
            fieldBadge,
            LinearLayout.LayoutParams(dp(112), dp(50)).apply {
                marginEnd = dp(9)
            }
        )

        preview.apply {
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
            textDirection = View.TEXT_DIRECTION_LTR
            setPadding(dp(16), 0, dp(16), 0)
            background = rounded("#20202A", 14, "#363642")
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.START
        }
        header.addView(
            preview,
            LinearLayout.LayoutParams(0, dp(50), 1f).apply {
                marginEnd = dp(9)
            }
        )

        revealButton = makeButton("نمایش", tone = ButtonTone.SECONDARY) {
            reduce(state.copy(revealPassword = !state.revealPassword))
            onInteraction(state.sessionId)
        }.apply { visibility = View.INVISIBLE }
        header.addView(
            revealButton,
            LinearLayout.LayoutParams(dp(88), dp(50)).apply {
                marginEnd = dp(9)
            }
        )

        closeButton = makeButton("بستن", tone = ButtonTone.SECONDARY) {
            dismiss()
        }
        header.addView(
            closeButton,
            LinearLayout.LayoutParams(dp(82), dp(50))
        )
        panel.addView(
            header,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val keyArea = FrameLayout(context)
        englishKeys.apply { orientation = LinearLayout.VERTICAL }
        persianKeys.apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        keyArea.addView(
            englishKeys,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        keyArea.addView(
            persianKeys,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        panel.addView(
            keyArea,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
        )

        addNumberRow(englishKeys)
        addAlphabetRow(
            englishKeys,
            listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
        )
        englishFocusAnchor =
            (englishKeys.getChildAt(1) as? ViewGroup)?.getChildAt(0) as? Button
        addAlphabetRow(
            englishKeys,
            listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
            edgeInset = 0.5f
        )
        addAlphabetRow(
            englishKeys,
            listOf("z", "x", "c", "v", "b", "n", "m"),
            edgeInset = 1.5f
        )

        addNumberRow(persianKeys)
        addLiteralRow(
            persianKeys,
            listOf("ض", "ص", "ث", "ق", "ف", "غ", "ع", "ه", "خ", "ح", "ج", "چ")
        )
        persianFocusAnchor =
            (persianKeys.getChildAt(1) as? ViewGroup)?.getChildAt(0) as? Button
        addLiteralRow(
            persianKeys,
            listOf("ش", "س", "ی", "ب", "ل", "ا", "ت", "ن", "م", "ک", "گ"),
            edgeInset = 0.5f
        )
        addLiteralRow(
            persianKeys,
            listOf("ظ", "ط", "ز", "ر", "ذ", "د", "پ", "و", ".", "؟"),
            edgeInset = 1f
        )

        val controls = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        capsButton = makeWeightedButton("Caps") {
            reduce(state.copy(capsLock = !state.capsLock))
            capsButton.requestFocus()
            onInteraction(state.sessionId)
        }
        controls.addView(capsButton)

        languageButton = makeWeightedButton("فا") {
            val next = if (state.language == KeyboardLanguage.ENGLISH) {
                KeyboardLanguage.PERSIAN
            } else {
                KeyboardLanguage.ENGLISH
            }
            reduce(
                state.copy(
                    language = next,
                    capsLock = if (next == KeyboardLanguage.PERSIAN) false else state.capsLock
                )
            )
            languageButton.requestFocus()
            onInteraction(state.sessionId)
        }
        controls.addView(languageButton)

        listOf("ژ", "آ").forEach { letter ->
            val button = makeWeightedButton(letter, weight = 0.72f) { append(letter) }
                .apply { visibility = View.GONE }
            persianSpecialButtons += button
            controls.addView(button)
        }
        controls.addView(makeWeightedButton("@") { append("@") })
        controls.addView(makeWeightedButton(".") { append(".") })
        controls.addView(makeWeightedButton("_") { append("_") })
        controls.addView(makeWeightedButton("-") { append("-") })
        controls.addView(
            makeWeightedButton("فاصله", weight = 2.1f) { append(" ") }
        )
        controls.addView(makeWeightedButton("⌫") {
            if (state.buffer.isNotEmpty()) {
                changeBuffer(state.buffer.dropLast(1))
            }
        })

        actionButton = makeButton("بعدی", tone = ButtonTone.PRIMARY) {
            val action = if (state.kind == KeyboardInputKind.TEXT) {
                KeyboardAction.NEXT
            } else {
                KeyboardAction.SUBMIT
            }
            onAction(state.sessionId, state.buffer, action)
        }
        controls.addView(
            actionButton,
            LinearLayout.LayoutParams(0, dp(44), 1.55f).apply {
                setMargins(dp(3), dp(3), dp(3), dp(3))
            }
        )
        panel.addView(
            controls,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(3) }
        )

        reduce(state)
    }

    fun open(
        sessionId: Long,
        initialValue: String,
        inputType: String
    ) {
        val kind = KeyboardInputKind.fromHtmlType(inputType)
        state = state.copy(
            sessionId = sessionId,
            kind = kind,
            // Existing passwords never cross the WebView bridge.
            buffer = if (kind == KeyboardInputKind.PASSWORD) "" else initialValue.take(160),
            capsLock = false,
            revealPassword = false
        )
        reduce(state)
        visibility = View.VISIBLE
        bringToFront()
        post {
            val anchor = if (state.language == KeyboardLanguage.PERSIAN) {
                persianFocusAnchor
            } else {
                englishFocusAnchor
            }
            if (isShowing && anchor?.requestFocus() != true) requestFocus()
        }
    }

    fun dismiss() {
        if (!isShowing) return
        val dismissedSession = state.sessionId
        visibility = View.GONE
        onDismissed(dismissedSession)
    }

    private fun reduce(next: KeyboardDeckState) {
        state = next
        val persian = state.language == KeyboardLanguage.PERSIAN
        englishKeys.visibility = if (persian) View.GONE else View.VISIBLE
        persianKeys.visibility = if (persian) View.VISIBLE else View.GONE
        persianSpecialButtons.forEach {
            it.visibility = if (persian) View.VISIBLE else View.GONE
        }
        languageButton.text = if (persian) "EN" else "فا"
        capsButton.text = if (state.capsLock) "CAPS ●" else "Caps"
        capsButton.isEnabled = !persian
        capsButton.alpha = if (persian) 0.36f else 1f
        alphabetButtons.forEach { button ->
            val base = button.tag as? String ?: return@forEach
            button.text = if (state.capsLock) base.uppercase() else base
        }

        fieldBadge.text = when (state.kind) {
            KeyboardInputKind.TEXT -> "نام کاربری"
            KeyboardInputKind.PASSWORD -> "رمز عبور"
            KeyboardInputKind.SEARCH -> "جستجو"
        }
        actionButton.text = when (state.kind) {
            KeyboardInputKind.TEXT -> "بعدی"
            KeyboardInputKind.PASSWORD -> "ورود"
            KeyboardInputKind.SEARCH -> "جستجو"
        }
        revealButton.visibility = View.VISIBLE
        revealButton.isEnabled = state.kind == KeyboardInputKind.PASSWORD
        revealButton.alpha = if (state.kind == KeyboardInputKind.PASSWORD) 1f else 0.72f
        revealButton.text = when (state.kind) {
            KeyboardInputKind.TEXT -> "بعدی: رمز"
            KeyboardInputKind.SEARCH -> "جستجو"
            KeyboardInputKind.PASSWORD ->
                if (state.revealPassword) "پنهان" else "نمایش"
        }
        updatePreview()
    }

    private fun append(raw: String) {
        if (state.buffer.length >= 160) return
        val value = when {
            state.language == KeyboardLanguage.PERSIAN -> raw
            state.capsLock -> raw.uppercase()
            else -> raw.lowercase()
        }
        changeBuffer((state.buffer + value).take(160))
    }

    private fun changeBuffer(value: String) {
        reduce(state.copy(buffer = value))
        onValueChanged(state.sessionId, state.buffer)
    }

    private fun updatePreview() {
        preview.text = when {
            state.buffer.isEmpty() -> when (state.kind) {
                KeyboardInputKind.TEXT -> "نام کاربری را وارد کنید"
                KeyboardInputKind.PASSWORD -> "رمز عبور را وارد کنید"
                KeyboardInputKind.SEARCH -> "عبارت جستجو را وارد کنید"
            }
            state.kind == KeyboardInputKind.PASSWORD && !state.revealPassword ->
                "•".repeat(state.buffer.length)
            else -> state.buffer
        }
        preview.setTextColor(
            if (state.buffer.isEmpty()) Color.parseColor("#9E9EA8") else Color.WHITE
        )
    }

    private fun addNumberRow(container: LinearLayout) {
        addLiteralRow(container, listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"))
    }

    private fun addAlphabetRow(
        container: LinearLayout,
        keys: List<String>,
        edgeInset: Float = 0f
    ) {
        addRow(container, keys, edgeInset) { base ->
            makeWeightedButton(base) {
                append(if (state.capsLock) base.uppercase() else base)
            }.apply {
                tag = base
                alphabetButtons += this
            }
        }
    }

    private fun addLiteralRow(
        container: LinearLayout,
        keys: List<String>,
        edgeInset: Float = 0f
    ) {
        addRow(container, keys, edgeInset) { key ->
            makeWeightedButton(key) { append(key) }
        }
    }

    private fun addRow(
        container: LinearLayout,
        keys: List<String>,
        edgeInset: Float,
        button: (String) -> Button
    ) {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        if (edgeInset > 0f) {
            row.addView(View(context), LinearLayout.LayoutParams(0, dp(44), edgeInset))
        }
        keys.forEach { row.addView(button(it)) }
        if (edgeInset > 0f) {
            row.addView(View(context), LinearLayout.LayoutParams(0, dp(44), edgeInset))
        }
        container.addView(
            row,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
    }

    private fun makeWeightedButton(
        label: String,
        weight: Float = 1f,
        action: () -> Unit
    ): Button = makeButton(label, action = action).apply {
        layoutParams = LinearLayout.LayoutParams(0, dp(44), weight).apply {
            setMargins(dp(3), dp(3), dp(3), dp(3))
        }
    }

    private enum class ButtonTone { NORMAL, SECONDARY, PRIMARY }

    private fun makeButton(
        label: String,
        tone: ButtonTone = ButtonTone.NORMAL,
        action: () -> Unit
    ): Button = Button(context).apply {
        text = label
        isAllCaps = false
        minWidth = 0
        minimumWidth = 0
        minHeight = 0
        minimumHeight = 0
        isFocusable = true
        setPadding(dp(4), 0, dp(4), 0)
        setTextColor(Color.WHITE)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f)
        typeface = if (tone == ButtonTone.PRIMARY) Typeface.DEFAULT_BOLD else Typeface.DEFAULT

        var focused = false
        var hovered = false
        fun render() {
            val active = focused || hovered
            val color = when {
                tone == ButtonTone.PRIMARY && active -> "#FF2731"
                tone == ButtonTone.PRIMARY -> "#E50914"
                active -> "#3B3B48"
                tone == ButtonTone.SECONDARY -> "#292933"
                else -> "#24242E"
            }
            val stroke = if (active) "#F5F5F7" else "#353541"
            background = rounded(color, 11, stroke, if (active) 2 else 1)
            scaleX = if (active) 1.035f else 1f
            scaleY = if (active) 1.035f else 1f
        }
        setOnFocusChangeListener { _, hasFocus ->
            focused = hasFocus
            render()
        }
        setOnHoverListener { _, event ->
            hovered = event.actionMasked != MotionEvent.ACTION_HOVER_EXIT
            render()
            false
        }
        setOnClickListener { action() }
        render()
    }

    private fun rounded(
        color: String,
        radiusDp: Int,
        strokeColor: String? = null,
        strokeWidthDp: Int = 1
    ) = GradientDrawable().apply {
        setColor(Color.parseColor(color))
        cornerRadius = dp(radiusDp).toFloat()
        if (strokeColor != null) {
            setStroke(dp(strokeWidthDp), Color.parseColor(strokeColor))
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
