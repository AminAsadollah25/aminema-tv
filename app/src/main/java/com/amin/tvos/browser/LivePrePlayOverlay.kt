package com.amin.tvos.browser

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import coil.imageLoader
import coil.request.ImageRequest

/**
 * A small native connection screen for live channels.
 *
 * It deliberately uses the channel artwork already present in services.json. It does not
 * inspect, extract, or store the provider's media URL. The overlay stays visible until the
 * provider's own video reports that playback has actually started. Normal playback is
 * automatic; the action becomes clickable only when the provider needs a manual retry.
 */
class LivePrePlayOverlay(
    context: Context,
    private val channelName: String,
    private val posterUrl: String,
    private val onStartRequested: () -> Unit
) : FrameLayout(context) {

    private val action: TextView

    init {
        isClickable = true
        isFocusable = true
        isFocusableInTouchMode = true
        setBackgroundColor(Color.rgb(8, 8, 12))
        setOnClickListener {
            if (action.isEnabled) onStartRequested()
        }

        val artwork = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setColorFilter(Color.argb(92, 0, 0, 0))
            if (posterUrl.isNotBlank()) {
                val request = ImageRequest.Builder(context)
                    .data(posterUrl)
                    .crossfade(true)
                    .target(onSuccess = { drawable -> setImageDrawable(drawable) })
                    .build()
                context.imageLoader.enqueue(request)
            }
        }
        addView(
            artwork,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )

        val scrim = View(context).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.argb(30, 0, 0, 0), Color.argb(205, 0, 0, 0))
            )
        }
        addView(scrim, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        val content = FrameLayout(context).apply {
            isClickable = false
            isFocusable = false
        }
        addView(content, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        val title = TextView(context).apply {
            text = channelName
            setTextColor(Color.WHITE)
            textSize = 30f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        content.addView(
            title,
            centeredParams(width = LayoutParams.MATCH_PARENT, height = LayoutParams.WRAP_CONTENT).apply {
                leftMargin = dp(48)
                rightMargin = dp(48)
                bottomMargin = dp(118)
            }
        )

        action = TextView(context).apply {
            text = "در حال اتصال به پخش زنده…"
            setTextColor(Color.WHITE)
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            isFocusable = true
            isFocusableInTouchMode = true
            background = GradientDrawable().apply {
                setColor(Color.rgb(226, 28, 44))
                cornerRadius = dp(28).toFloat()
            }
            setPadding(dp(30), dp(16), dp(30), dp(16))
            setOnClickListener { onStartRequested() }
            // Some Android TV boxes deliver a USB/air-mouse click as a raw touch
            // sequence. Consume that sequence explicitly so the native hand-off
            // button cannot remain visually focused without invoking its action.
            setOnTouchListener { view, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> true
                    MotionEvent.ACTION_UP -> {
                        view.performClick()
                        true
                    }
                    else -> true
                }
            }
        }
        content.addView(
            action,
            centeredParams(width = dp(250), height = dp(64)).apply {
                bottomMargin = dp(34)
            }
        )
        bringToFront()
    }

    fun showConnecting() {
        visibility = View.VISIBLE
        action.text = "در حال اتصال به پخش زنده…"
        action.isEnabled = false
    }

    fun showStarting() {
        visibility = View.VISIBLE
        action.text = "در حال آماده‌سازی…"
        action.isEnabled = false
    }

    fun showManualStart() {
        visibility = View.VISIBLE
        action.text = "▶  تلاش دوباره"
        action.isEnabled = true
        action.requestFocus()
    }

    fun hideWhenPlaying() {
        animate()
            .alpha(0f)
            .setDuration(220L)
            .withEndAction {
                visibility = View.GONE
                alpha = 1f
            }
            .start()
    }

    /** Reveals the provider controls if its page did not expose a playing callback in time. */
    fun revealProviderPlayer() = hideWhenPlaying()

    private fun centeredParams(width: Int, height: Int) = LayoutParams(width, height).apply {
        gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
