package com.amin.tvos.ui.components

import android.net.Uri
import android.webkit.CookieManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.amin.tvos.data.model.CatalogItem
import com.amin.tvos.data.model.CatalogKind
import com.amin.tvos.ui.theme.SurfaceElevated
import com.amin.tvos.ui.theme.TextSecondary

/**
 * Builds the poster request for a card.
 *
 * Some services only serve posters to a signed-in browser. The session cookie is attached
 * strictly when the image lives on the same host as the content page, so nothing leaks to
 * a third-party CDN.
 */
@Composable
fun authenticatedPosterModel(posterUrl: String, pageUrl: String): Any? {
    val context = LocalContext.current
    return remember(posterUrl, pageUrl) {
        if (!posterUrl.startsWith("http")) return@remember posterUrl
        posterRequest(context, posterUrl, pageUrl).build()
    }
}

/**
 * The same authenticated request, deliberately asked for at a tiny size so that scaling it
 * up is what produces the blur. That keeps the soft cinema backdrop identical on Android 9 —
 * where `Modifier.blur` does nothing — and costs a fraction of the memory of a full poster.
 */
@Composable
fun blurredBackdropModel(posterUrl: String, pageUrl: String): Any? {
    val context = LocalContext.current
    return remember(posterUrl, pageUrl) {
        if (!posterUrl.startsWith("http")) return@remember posterUrl
        posterRequest(context, posterUrl, pageUrl)
            .size(96, 144)
            .crossfade(true)
            .build()
    }
}

/** Shared builder so the same-host cookie rule lives in exactly one place. */
private fun posterRequest(
    context: android.content.Context,
    posterUrl: String,
    pageUrl: String
): ImageRequest.Builder = ImageRequest.Builder(context)
    .data(posterUrl)
    .apply {
        val posterHost = runCatching { Uri.parse(posterUrl).host }.getOrNull()
        val pageHost = runCatching { Uri.parse(pageUrl).host }.getOrNull()
        if (!posterHost.isNullOrBlank() && posterHost.equals(pageHost, true)) {
            CookieManager.getInstance().getCookie(posterUrl)
                ?.takeIf { it.isNotBlank() }
                ?.let { addHeader("Cookie", it) }
            addHeader("Referer", pageUrl)
        }
    }

/** Poster card for a "latest" catalog row. Opens the service's normal detail page. */
@Composable
fun CatalogCard(
    item: CatalogItem,
    onClick: () -> Unit,
    onFocused: (Boolean) -> Unit = {}
) {
    val posterModel = authenticatedPosterModel(item.posterUrl, item.contentUrl)
    FocusableCard(
        modifier = Modifier.width(190.dp),
        focusedScale = 1.06f,
        onClick = onClick,
        onInteractionFocusChanged = onFocused
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(260.dp)) {
                if (item.posterUrl.isNotBlank()) {
                    AsyncImage(
                        model = posterModel,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(SurfaceElevated, Color(0xFF2A2A36))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.72f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        if (item.kind == CatalogKind.SERIES) "سریال" else "فیلم",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                }
            }
            Text(
                item.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 4.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (item.kind == CatalogKind.SERIES && item.episodeLabel.isNotBlank()) {
                    Text(
                        item.episodeLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
