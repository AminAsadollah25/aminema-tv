package com.amin.tvos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.amin.tvos.data.model.CatalogItem
import com.amin.tvos.data.model.CatalogKind
import com.amin.tvos.ui.theme.SurfaceElevated

/**
 * A wide card built around the provider's own banner artwork.
 *
 * These are a different kind of image from a poster: landscape key art, roughly 16:9, chosen
 * by the provider's editors rather than ordered by release date. So the card is a different
 * shape too, and its information sits **on** the artwork instead of underneath it — a caption
 * strip below a 16:9 image would make each card twice as tall for very little gain.
 *
 * One provider bakes the title into its artwork and the other does not. The overlaid title is
 * kept small and bottom-aligned for exactly that reason: where the art already says the name
 * large and centred, a small caption reads as a label rather than a duplicate.
 */
@Composable
fun FeaturedBannerCard(
    item: CatalogItem,
    onClick: () -> Unit,
    onFocused: (Boolean) -> Unit = {}
) {
    val art = item.backdropUrl.ifBlank { item.posterUrl }
    val model = authenticatedPosterModel(art, item.contentUrl)
    FocusableCard(
        modifier = Modifier.width(352.dp),
        shape = RoundedCornerShape(18.dp),
        focusedScale = 1.05f,
        onClick = onClick,
        onInteractionFocusChanged = onFocused
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(18.dp))
                .background(SurfaceElevated)
        ) {
            AsyncImage(
                model = model,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Just enough darkness at the foot of the card to carry white text over any
            // artwork, without dimming the picture itself.
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.52f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.82f)
                        )
                    )
            )
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Text(
                    item.title,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val details = buildList {
                    add(if (item.kind == CatalogKind.SERIES) "سریال" else "فیلم")
                    item.episodeLabel.takeIf { it.isNotBlank() }?.let(::add)
                    item.year.takeIf { it.isNotBlank() }?.let(::add)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        details.joinToString("  •  "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.76f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
