package com.amin.tvos.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.amin.tvos.data.model.CatalogItem
import com.amin.tvos.data.model.CatalogKind
import com.amin.tvos.ui.components.authenticatedPosterModel
import com.amin.tvos.ui.theme.CinemaRed
import com.amin.tvos.ui.theme.TextSecondary

/**
 * Apple-TV-like metadata glance shown after a short mouse-hover / DPAD-focus dwell.
 *
 * It is a title-level, spoiler-safe preview: episode plots and autoplay video are
 * deliberately excluded. The overlay never changes the rail layout or steals focus.
 */
@Composable
fun CinematicHoverPreview(
    item: CatalogItem?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = item != null,
        enter = fadeIn(tween(170)) + scaleIn(tween(190), initialScale = 0.97f),
        exit = fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.98f),
        modifier = modifier.zIndex(20f)
    ) {
        val current = item ?: return@AnimatedVisibility
        val poster = authenticatedPosterModel(current.posterUrl, current.contentUrl)
        Surface(
            color = Color(0xFF16171D),
            tonalElevation = 16.dp,
            shadowElevation = 22.dp,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.width(570.dp).height(250.dp)
        ) {
            Box(Modifier.fillMaxSize()) {
                if (current.posterUrl.isNotBlank()) {
                    AsyncImage(
                        model = poster,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(210.dp)
                            .fillMaxHeight()
                    )
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF16171D),
                                    Color(0xFF16171D),
                                    Color(0xE616171D),
                                    Color.Transparent
                                )
                            )
                        )
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0xD916171D))
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .width(420.dp)
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                ) {
                    Text(
                        if (current.kind == CatalogKind.SERIES) {
                            "سریال • نگاه سریع"
                        } else {
                            "فیلم • نگاه سریع"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = CinemaRed,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        current.title,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(9.dp))
                    val badges = buildList {
                        current.year.takeIf { it.isNotBlank() }?.let(::add)
                        current.rating.takeIf { it.isNotBlank() }?.let { add("★ $it") }
                        current.runtime.takeIf { it.isNotBlank() }?.let(::add)
                        current.episodeLabel.takeIf { it.isNotBlank() }?.let(::add)
                    }
                    if (badges.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            badges.take(3).forEach { badge ->
                                Text(
                                    badge,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(Color.White.copy(alpha = 0.12f))
                                        .padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(9.dp))
                    }
                    Text(
                        current.summary.ifBlank {
                            current.genres.takeIf { it.isNotEmpty() }
                                ?.joinToString(" • ")
                                ?: "برای دیدن جزئیات، کارت را باز کن."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (current.summary.isNotBlank() && current.genres.isNotEmpty()) {
                        Spacer(Modifier.height(7.dp))
                        Text(
                            current.genres.take(3).joinToString(" • "),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.76f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    "معرفی بدون اسپویل",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.52f),
                    modifier = Modifier.align(Alignment.BottomEnd).padding(14.dp)
                )
            }
        }
    }
}
