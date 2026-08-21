package com.amin.tvos.browser

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.amin.tvos.R
import coil.imageLoader
import coil.request.ImageRequest

/**
 * Covers the unavoidable website detail-page bootstrap while Aminema follows
 * the site's own normal Continue/Play action. It never reads media requests.
 */
class PlaybackLoadingView(context: Context) : FrameLayout(context) {

    private val artwork = ImageView(context).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
        alpha = 0.42f
    }

    private val title = TextView(context).apply {
        text = "در حال آماده‌سازی پخش…"
        setTextColor(Color.WHITE)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 30f)
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
    }

    private val subtitle = TextView(context).apply {
        text = "چند لحظه تا شروع فیلم؛ خوش بگذره!"
        setTextColor(Color.parseColor("#A7A7B2"))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        gravity = Gravity.CENTER
        setPadding(0, dp(10), 0, 0)
    }

    init {
        visibility = View.GONE
        isClickable = true
        isFocusable = false
        background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(
                Color.parseColor("#17080D"),
                Color.parseColor("#08080C"),
                Color.BLACK
            )
        )

        // Keep the first frame cinematic instead of exposing an empty provider/player page.
        // This is artwork already present in the native catalog; it is never a media URL.
        addView(artwork, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(
            View(context).apply {
                background = GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    intArrayOf(Color.argb(55, 0, 0, 0), Color.argb(210, 0, 0, 0))
                )
            },
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )

        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        panel.addView(
            ImageView(context).apply {
                setImageResource(R.drawable.aminema_loading_popcorn)
                scaleType = ImageView.ScaleType.CENTER_CROP
            },
            LinearLayout.LayoutParams(dp(190), dp(190)).apply {
                bottomMargin = dp(20)
                gravity = Gravity.CENTER_HORIZONTAL
            }
        )
        panel.addView(title)
        panel.addView(subtitle)
        panel.addView(
            ProgressBar(context).apply { isIndeterminate = true },
            LinearLayout.LayoutParams(dp(48), dp(48)).apply {
                topMargin = dp(26)
                gravity = Gravity.CENTER_HORIZONTAL
            }
        )
        addView(
            panel,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT,
                Gravity.CENTER
            )
        )
    }

    fun showPreparing(
        isContinue: Boolean,
        episodeLabel: String? = null,
        titleText: String = "",
        backdropUrl: String = "",
        posterUrl: String = ""
    ) {
        val artUrl = backdropUrl.ifBlank { posterUrl }
        if (artUrl.isNotBlank()) {
            artwork.visibility = View.VISIBLE
            context.imageLoader.enqueue(
                ImageRequest.Builder(context)
                    .data(artUrl)
                    .size(1280, 720)
                    .crossfade(true)
                    .target(onSuccess = { drawable -> artwork.setImageDrawable(drawable) })
                    .build()
            )
        } else {
            artwork.setImageDrawable(null)
            artwork.visibility = View.GONE
        }
        title.text = if (!episodeLabel.isNullOrBlank()) {
            "در حال آماده‌سازی $episodeLabel…"
        } else if (titleText.isNotBlank()) {
            "در حال آماده‌سازی «$titleText»…"
        } else if (isContinue) {
            "پاپ‌کورن یادت نره… 🍿"
        } else {
            "چراغ‌ها خاموش، پاپ‌کورن آماده… 🍿"
        }
        subtitle.text = if (!episodeLabel.isNullOrBlank()) {
            "انتخابت ثبت شد؛ داریم دقیقاً همون قسمت رو باز می‌کنیم 🍿"
        } else if (isContinue) {
            "داریم فیلمت رو از همون‌جایی که جا گذاشتی میاریم؛ خوش بگذره!"
        } else {
            "چند لحظه تا شروع فیلم؛ خوش بگذره!"
        }
        visibility = View.VISIBLE
        bringToFront()
    }

    fun hide() {
        visibility = View.GONE
    }

    val isShowing: Boolean
        get() = visibility == View.VISIBLE

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
