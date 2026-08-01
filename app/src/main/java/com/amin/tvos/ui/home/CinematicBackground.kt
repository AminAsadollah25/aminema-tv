package com.amin.tvos.ui.home

import android.net.Uri
import android.os.Build
import android.webkit.CookieManager
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.amin.tvos.ui.theme.Ink

/**
 * Soft, cinema-like backdrop built from the artwork of whatever the user watched last.
 *
 * The image is deliberately requested at a tiny size and scaled up: that alone produces the
 * blur, so Android 9 boxes — where `Modifier.blur` does nothing — get the same look as newer
 * ones, at a fraction of the memory. Two scrims keep every foreground text and poster
 * readable, and the whole thing crossfades when the source title changes.
 */
@Composable
fun CinematicBackground(
    posterUrl: String,
    pageUrl: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Box(modifier.fillMaxSize().background(Ink)) {
        Crossfade(
            targetState = posterUrl,
            animationSpec = tween(700),
            label = "cinematicBackdrop"
        ) { url ->
            if (url.isNotBlank()) {
                val request = remember(url, pageUrl) {
                    ImageRequest.Builder(context)
                        .data(url)
                        .crossfade(true)
                        .apply {
                            val posterHost = runCatching { Uri.parse(url).host }.getOrNull()
                            val pageHost = runCatching { Uri.parse(pageUrl).host }.getOrNull()
                            // Same-host only, exactly like the poster cards.
                            if (!posterHost.isNullOrBlank() &&
                                posterHost.equals(pageHost, true)
                            ) {
                                CookieManager.getInstance().getCookie(url)
                                    ?.takeIf { it.isNotBlank() }
                                    ?.let { addHeader("Cookie", it) }
                                addHeader("Referer", pageUrl)
                            }
                        }
                        .build()
                }
                AsyncImage(
                    model = request,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                Modifier.blur(48.dp)
                            } else {
                                Modifier
                            }
                        )
                )
            }
        }
        // Darken enough that white TV text stays readable, but not so much that the
        // artwork disappears: the top stays visibly cinematic and the rows below sit on
        // near-solid Ink.
        Box(
            Modifier
                .fillMaxSize()
                .background(Ink.copy(alpha = 0.45f))
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.35f to Ink.copy(alpha = 0.55f),
                        0.7f to Ink.copy(alpha = 0.92f),
                        1f to Ink
                    )
                )
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to Ink.copy(alpha = 0.65f),
                        0.6f to Color.Transparent
                    )
                )
        )
    }
}
