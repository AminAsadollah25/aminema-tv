package com.amin.tvos.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.amin.tvos.data.model.MovieItem
import com.amin.tvos.data.model.StreamingService
import com.amin.tvos.ui.theme.CinemaRed
import com.amin.tvos.ui.theme.SurfaceElevated
import com.amin.tvos.ui.theme.TextPrimary
import com.amin.tvos.ui.theme.TextSecondary

/**
 * Focus-aware wrapper: scales up + red glow border when focused (DPAD)
 * or hovered (air mouse / USB mouse).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FocusableCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(14.dp),
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    content: @Composable (focused: Boolean) -> Unit
) {
    var dpadFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val focused = dpadFocused || hovered
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.08f else 1f,
        animationSpec = tween(160),
        label = "focusScale"
    )
    val borderColor by animateColorAsState(
        targetValue = if (focused) CinemaRed else Color.Transparent,
        animationSpec = tween(160),
        label = "focusBorder"
    )
    Surface(
        color = SurfaceElevated,
        contentColor = TextPrimary,
        shape = shape,
        border = BorderStroke(2.5.dp, borderColor),
        modifier = modifier
            .scale(scale)
            .onFocusChanged { dpadFocused = it.isFocused || it.hasFocus }
            .hoverable(interactionSource)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        content(focused)
    }
}

/** Large "My Services" card. */
@Composable
fun ServiceCard(
    service: StreamingService,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val accent = remember(service.color) {
        runCatching { Color(android.graphics.Color.parseColor(service.color)) }
            .getOrDefault(CinemaRed)
    }
    FocusableCard(
        modifier = Modifier.width(260.dp).height(140.dp),
        onClick = onClick,
        onLongClick = onLongClick
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(accent.copy(alpha = 0.35f), SurfaceElevated)
                    )
                )
        ) {
            Column(
                Modifier.align(Alignment.CenterStart).padding(20.dp)
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White)
                }
                Spacer(Modifier.height(12.dp))
                Text(service.name, style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

/** Netflix-style poster card for Continue Watching / Recents / Favorites. */
@Composable
fun PosterCard(
    item: MovieItem,
    showContinueBadge: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    FocusableCard(
        modifier = Modifier.width(190.dp),
        onClick = onClick,
        onLongClick = onLongClick
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(260.dp)) {
                if (item.posterUrl.isNotBlank()) {
                    AsyncImage(
                        model = item.posterUrl,
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
                if (item.isFavorite) {
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = "Favorite",
                        tint = CinemaRed,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .size(22.dp)
                    )
                }
                if (showContinueBadge) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CinemaRed)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Continue", style = MaterialTheme.typography.labelLarge, color = Color.White)
                    }
                }
            }
            Column(Modifier.padding(12.dp)) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    item.serviceName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

/** Row with section title + horizontally scrolling content (smooth on air mouse & DPAD). */
@Composable
fun SectionRow(
    title: String,
    content: @Composable () -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 48.dp, vertical = 12.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 48.dp, vertical = 8.dp)
        ) { content() }
    }
}
