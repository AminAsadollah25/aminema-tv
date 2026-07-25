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
        ImageRequest.Builder(context)
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
            .build()
    }
}

/** Poster card for a "latest" catalog row. Opens the service's normal detail page. */
@Composable
fun CatalogCard(
    item: CatalogItem,
    onClick: () -> Unit
) {
    val posterModel = authenticatedPosterModel(item.posterUrl, item.contentUrl)
    FocusableCard(
        modifier = Modifier.width(190.dp),
        onClick = onClick
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
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}
