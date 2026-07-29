package com.amin.tvos.ui.components

import android.net.Uri
import android.webkit.CookieManager
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
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.amin.tvos.data.model.MovieItem
import com.amin.tvos.data.model.CatalogItem
import com.amin.tvos.data.model.StreamingService
import com.amin.tvos.ui.theme.CinemaRed
import com.amin.tvos.ui.theme.SurfaceElevated
import com.amin.tvos.ui.theme.TextPrimary
import com.amin.tvos.ui.theme.TextSecondary
import kotlinx.coroutines.launch

/**
 * Focus-aware wrapper: scales up + red glow border when focused (DPAD)
 * or hovered (air mouse / USB mouse).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FocusableCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(14.dp),
    enabled: Boolean = true,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onInteractionFocusChanged: (Boolean) -> Unit = {},
    content: @Composable (focused: Boolean) -> Unit
) {
    var dpadFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val focused = enabled && (dpadFocused || hovered)
    androidx.compose.runtime.LaunchedEffect(focused) {
        onInteractionFocusChanged(focused)
    }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.05f else 1f,
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
            .alpha(if (enabled) 1f else 0.34f)
            .scale(scale)
            .onFocusChanged { dpadFocused = it.isFocused || it.hasFocus }
            .hoverable(interactionSource)
            .combinedClickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        content(focused)
    }
}

/**
 * Shared TV rail controls. They remain in the same top-right position on every
 * horizontal category, so mouse and DPAD users never need to hunt for scrolling.
 */
@Composable
fun RailNavigationControls(
    scrollState: ScrollState,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val page = {
        (scrollState.viewportSize * 0.82f).toInt().coerceAtLeast(360)
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        FocusableCard(
            modifier = Modifier.size(width = 48.dp, height = 42.dp),
            shape = RoundedCornerShape(50),
            enabled = scrollState.value > 0,
            onClick = {
                scope.launch {
                    scrollState.animateScrollTo(
                        (scrollState.value - page()).coerceAtLeast(0)
                    )
                }
            }
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "نمایش موارد قبلی",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        FocusableCard(
            modifier = Modifier.size(width = 48.dp, height = 42.dp),
            shape = RoundedCornerShape(50),
            enabled = scrollState.value < scrollState.maxValue,
            onClick = {
                scope.launch {
                    scrollState.animateScrollTo(
                        (scrollState.value + page()).coerceAtMost(scrollState.maxValue)
                    )
                }
            }
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "نمایش موارد بعدی",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

/** Lazy-rail variant: keeps only the visible TV cards composed and decoded. */
@Composable
fun RailNavigationControls(
    listState: LazyListState,
    itemCount: Int,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        FocusableCard(
            modifier = Modifier.size(width = 48.dp, height = 42.dp),
            shape = RoundedCornerShape(50),
            enabled = listState.canScrollBackward,
            onClick = {
                scope.launch {
                    val pageSize = (
                        listState.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1) - 1
                        ).coerceAtLeast(1)
                    listState.animateScrollToItem(
                        (listState.firstVisibleItemIndex - pageSize).coerceAtLeast(0)
                    )
                }
            }
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "نمایش موارد قبلی",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        FocusableCard(
            modifier = Modifier.size(width = 48.dp, height = 42.dp),
            shape = RoundedCornerShape(50),
            enabled = listState.canScrollForward,
            onClick = {
                scope.launch {
                    val pageSize = (
                        listState.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1) - 1
                        ).coerceAtLeast(1)
                    listState.animateScrollToItem(
                        (listState.firstVisibleItemIndex + pageSize)
                            .coerceAtMost((itemCount - 1).coerceAtLeast(0))
                    )
                }
            }
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "نمایش موارد بعدی",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

/** Large "My Services" card. */
@Composable
fun ServiceCard(
    service: StreamingService,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val accent = remember(service.color) {
        runCatching { Color(android.graphics.Color.parseColor(service.color)) }
            .getOrDefault(CinemaRed)
    }
    val artworkModel = remember(service.artwork) {
        when {
            service.artwork.startsWith("http") -> service.artwork
            service.artwork.isNotBlank() -> context.resources.getIdentifier(
                service.artwork,
                "drawable",
                context.packageName
            ).takeIf { it != 0 }
            else -> null
        }
    }
    FocusableCard(
        modifier = Modifier.width(400.dp).height(220.dp),
        onClick = onClick,
        onLongClick = onLongClick
    ) {
        Box(Modifier.fillMaxSize()) {
            if (artworkModel != null) {
                AsyncImage(
                    model = artworkModel,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(accent.copy(alpha = 0.4f), SurfaceElevated)
                            )
                        )
                )
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.92f),
                                Color.Black.copy(alpha = 0.58f),
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
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.58f))
                        )
                    )
            )
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(28.dp)
            ) {
                if (service.subtitle.isNotBlank()) {
                    Text(
                        service.subtitle.uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = accent
                    )
                    Spacer(Modifier.height(6.dp))
                }
                Text(service.name, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(14.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.14f))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("ورود", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

/** Netflix-style poster card for Continue Watching / Recents / Favorites. */
@Composable
fun PosterCard(
    item: MovieItem,
    showContinueBadge: Boolean = false,
    previewItem: CatalogItem? = null,
    onPreviewStateChange: (CatalogItem, Boolean) -> Unit = { _, _ -> },
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    // FilmRooz returns HTML for poster requests without the authenticated WebView
    // cookie, so the shared helper attaches it for same-host images only.
    val posterModel = authenticatedPosterModel(item.posterUrl, item.url)
    var focused by remember(item.url) { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(focused, item.url, previewItem) {
        val preview = previewItem ?: return@LaunchedEffect
        if (focused) {
            kotlinx.coroutines.delay(520L)
            onPreviewStateChange(preview, true)
        } else {
            onPreviewStateChange(preview, false)
        }
    }
    FocusableCard(
        modifier = Modifier.width(190.dp),
        onClick = onClick,
        onLongClick = onLongClick,
        onInteractionFocusChanged = { focused = it }
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
                        Text(
                            "ادامه",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                    }
                }
                if (item.duration > 0L && item.resumePosition > 0L) {
                    val progress = (item.resumePosition.toFloat() / item.duration.toFloat())
                        .coerceIn(0.02f, 1f)
                    Box(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(5.dp)
                            .background(Color.White.copy(alpha = 0.25f))
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(progress)
                                .height(5.dp)
                                .background(CinemaRed)
                        )
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
fun <T> SectionRow(
    title: String,
    items: List<T>,
    key: (T) -> Any = { it.hashCode() },
    content: @Composable (T) -> Unit
) {
    val listState = rememberLazyListState()
    Column(Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp, vertical = 12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.weight(1f))
            RailNavigationControls(listState, items.size)
        }
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(items, key = key) { item -> content(item) }
        }
    }
}
