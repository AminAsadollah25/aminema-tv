package com.amin.tvos.ui.components

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The signature colour of a title's own artwork, for tinting the Home hero.
 *
 * Deliberately no new dependency: the hero already asks Coil for a 96×144 copy of the poster
 * to build its blur, so the same tiny bitmap is read here. That is under fourteen thousand
 * pixels, which is cheap enough to scan on a weak Android box, and it is already in Coil's
 * cache by the time this runs.
 *
 * Picking the *dominant* colour of a poster usually returns near-black, because posters are
 * mostly dark. What reads as "the colour of this film" is instead its most colourful region,
 * so pixels are scored by saturation and only mid-brightness ones are allowed to win.
 *
 * The result animates, so a rotating hero drifts from one film's palette to the next instead
 * of snapping.
 */
@Composable
fun rememberArtworkAccent(
    posterUrl: String,
    pageUrl: String,
    fallback: Color
): Color {
    val context = LocalContext.current
    var resolved by remember(posterUrl) { mutableStateOf(fallback) }

    LaunchedEffect(posterUrl, pageUrl) {
        if (posterUrl.isBlank()) {
            resolved = fallback
            return@LaunchedEffect
        }
        val request = ImageRequest.Builder(context)
            .data(posterUrl)
            .size(96, 144)
            // Pixels can only be read back from a software bitmap.
            .allowHardware(false)
            .build()
        val bitmap = runCatching {
            (context.imageLoader.execute(request) as? SuccessResult)
                ?.drawable
                ?.let { drawable ->
                    (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                }
        }.getOrNull()
        resolved = bitmap
            ?.let { withContext(Dispatchers.Default) { accentOf(it) } }
            ?: fallback
    }

    val animated by animateColorAsState(
        targetValue = resolved,
        animationSpec = tween(700),
        label = "artworkAccent"
    )
    return animated
}

/**
 * Scores every pixel by how colourful it is and averages the winners.
 *
 * Very dark, very bright and washed-out pixels are skipped: they are the background of most
 * posters and would drag any average towards grey. If a poster really is greyscale, the
 * caller's fallback is used instead of inventing a hue.
 */
private fun accentOf(bitmap: Bitmap): Color {
    // Two readings of the same artwork.
    //
    // "toneSum" is the plain average of every pixel: what the poster looks like from across
    // the room. "vividSum" is the average of only its colourful pixels. Using the vivid one
    // alone was wrong — a cream poster with a red title came out deep red, when the poster
    // plainly reads as cream. Using the plain average alone is flat. The answer is mostly
    // the honest tone with a little of the vivid pulled in.
    var toneCount = 0.0
    var toneR = 0.0; var toneG = 0.0; var toneB = 0.0
    var vividWeight = 0.0
    var vividR = 0.0; var vividG = 0.0; var vividB = 0.0

    val stepX = (bitmap.width / 48).coerceAtLeast(1)
    val stepY = (bitmap.height / 72).coerceAtLeast(1)
    val hsv = FloatArray(3)

    var y = 0
    while (y < bitmap.height) {
        var x = 0
        while (x < bitmap.width) {
            val pixel = bitmap.getPixel(x, y)
            val r = AndroidColor.red(pixel)
            val g = AndroidColor.green(pixel)
            val b = AndroidColor.blue(pixel)

            toneR += r; toneG += g; toneB += b
            toneCount += 1.0

            AndroidColor.colorToHSV(pixel, hsv)
            if (hsv[1] > 0.32f && hsv[2] in 0.22f..0.92f) {
                // Squared so a genuinely vivid pixel outvotes several dull ones.
                val weight = (hsv[1] * hsv[1] * hsv[2]).toDouble()
                vividR += r * weight; vividG += g * weight; vividB += b * weight
                vividWeight += weight
            }
            x += stepX
        }
        y += stepY
    }

    if (toneCount <= 0.0) return Color.Unspecified

    val tone = Color(
        red = (toneR / toneCount / 255.0).toFloat().coerceIn(0f, 1f),
        green = (toneG / toneCount / 255.0).toFloat().coerceIn(0f, 1f),
        blue = (toneB / toneCount / 255.0).toFloat().coerceIn(0f, 1f)
    )
    val base = if (vividWeight > 0.0) {
        val vivid = Color(
            red = (vividR / vividWeight / 255.0).toFloat().coerceIn(0f, 1f),
            green = (vividG / vividWeight / 255.0).toFloat().coerceIn(0f, 1f),
            blue = (vividB / vividWeight / 255.0).toFloat().coerceIn(0f, 1f)
        )
        androidx.compose.ui.graphics.lerp(tone, vivid, 0.34f)
    } else {
        tone
    }
    return base.deepenForScrim()
}

/**
 * Pushes a colour towards something that can sit behind white TV text: keeps the hue, lifts
 * saturation a little, and caps brightness so a bright poster cannot wash the hero out.
 */
private fun Color.deepenForScrim(): Color {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(
        AndroidColor.rgb(
            (red * 255).toInt().coerceIn(0, 255),
            (green * 255).toInt().coerceIn(0, 255),
            (blue * 255).toInt().coerceIn(0, 255)
        ),
        hsv
    )
    hsv[1] = (hsv[1] * 1.32f).coerceAtMost(0.92f)
    // Rich enough that each poster visibly re-themes the hero, but still dark enough to sit
    // behind white TV text: the ceiling is what stops a bright poster becoming coloured haze.
    hsv[2] = hsv[2].coerceIn(0.30f, 0.58f)
    return Color(AndroidColor.HSVToColor(hsv))
}
