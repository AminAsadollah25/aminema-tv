package com.amin.tvos.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.amin.tvos.ui.theme.Ink
import com.amin.tvos.ui.components.authenticatedPosterModel

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
    Box(modifier.fillMaxSize().background(Ink)) {
        Crossfade(
            targetState = posterUrl,
            // Background is atmosphere, not the interaction target. A shorter crossfade
            // keeps DPAD navigation responsive while preserving the cinematic handoff.
            animationSpec = tween(420),
            label = "cinematicBackdrop"
        ) { url ->
            if (url.isNotBlank()) {
                // CookieManager may start Chromium. Never initialise it in composition.
                val request = authenticatedPosterModel(url, pageUrl, 960, 540)
                AsyncImage(
                    model = request,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    // The dark scrims below provide the cinematic separation. A full-screen
                    // RenderEffect blur here forced the TV GPU/CPU to redraw the whole 1080p
                    // surface on every crossfade frame; keep the backdrop static and cheap.
                    modifier = Modifier.fillMaxSize()
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
