package com.amin.tvos.ui.spotlight

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.amin.tvos.data.model.CatalogKind
import com.amin.tvos.data.model.SpotlightAction
import com.amin.tvos.data.model.SpotlightItem
import com.amin.tvos.ui.components.FocusableCard
import com.amin.tvos.ui.components.authenticatedPosterModel
import com.amin.tvos.ui.home.CinematicBackground
import com.amin.tvos.ui.theme.CinemaRed
import com.amin.tvos.ui.theme.Ink
import com.amin.tvos.ui.theme.SurfaceElevated
import com.amin.tvos.ui.theme.TextPrimary
import com.amin.tvos.ui.theme.TextSecondary
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.Surface
import kotlinx.coroutines.delay
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpotlightScreen(
    item: SpotlightItem,
    isFavorite: Boolean,
    metadataLoading: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onWatch: () -> Unit
) {
    // Metadata arrives asynchronously and recomposes this screen. Remembering the requester
    // prevents a delayed focus request from targeting the detached pre-enrichment tree.
    val primaryFocus = remember { FocusRequester() }
    val posterModel = authenticatedPosterModel(item.posterUrl, item.contentUrl)

    val contentVisibleState = remember { MutableTransitionState(false) }

    LaunchedEffect(item.contentUrl) {
        delay(150L)
        contentVisibleState.targetState = true
        delay(100L)
        primaryFocus.requestFocus()
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val heroArtUrl = item.backdropUrl.ifBlank { item.posterUrl }
        val backdropModel = authenticatedPosterModel(heroArtUrl, item.contentUrl)

        Box(Modifier.fillMaxSize().background(Ink)) {
            // 1. Full-bleed background from the wide backdrop with Ken Burns effect
            if (heroArtUrl.isNotBlank()) {
                val isFallback = item.backdropUrl.isBlank() && item.posterUrl.isNotBlank()
                val infiniteTransition = rememberInfiniteTransition(label = "kenBurnsSpotlight")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 1.0f,
                    targetValue = 1.05f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(25000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "backdropScale"
                )
                AsyncImage(
                    model = backdropModel,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (isFallback) Modifier.blur(48.dp, edgeTreatment = androidx.compose.ui.draw.BlurredEdgeTreatment.Unbounded) else Modifier)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                )
            }

            // 2. Gradients for text readability and cinematic depth (RTL: Text is on the left)
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            0f to Ink.copy(alpha = 0.95f),
                            0.55f to Ink.copy(alpha = 0.85f),
                            0.85f to Ink.copy(alpha = 0.4f),
                            1f to Color.Transparent
                        )
                    )
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.7f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.85f)
                        )
                    )
            )

            // 3. Right side: Full height portrait poster
            if (item.posterUrl.isNotBlank()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.CenterStart) // CenterStart in RTL means Right side
                        .fillMaxHeight()
                        .aspectRatio(2f / 3f)
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    0f to Color.Transparent,
                                    0.25f to Color.Black,
                                    1f to Color.Black
                                ),
                                blendMode = BlendMode.DstIn
                            )
                        },
                    color = Color.Transparent
                ) {
                    AsyncImage(
                        model = posterModel,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // 4. Back button
            FocusableCard(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(34.dp)
                    .size(54.dp),
                shape = RoundedCornerShape(50),
                focusedScale = 1.08f,
                onClick = onBack
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "بازگشت",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // 5. Left side: Text, Metadata, Synopsis, Buttons
            AnimatedVisibility(
                visibleState = contentVisibleState,
                enter = fadeIn(tween(800)) + slideInHorizontally(
                    tween(800, easing = FastOutSlowInEasing),
                    initialOffsetX = { it / 10 }
                ),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 72.dp, end = 64.dp, top = 42.dp, bottom = 42.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.55f) // Take up 55% of the screen width
                            .align(Alignment.CenterEnd) // CenterEnd in RTL means Left side
                            .padding(top = 12.dp),
                        horizontalAlignment = Alignment.Start // Start in RTL means Right-aligned text
                    ) {
                    // Everything descriptive lives in this flexible block. A long title,
                    // wrapped metadata chips, a resume bar, a synopsis and a credits list
                    // can together outgrow a 1080p screen; when they do, this block gives
                    // room back instead of squeezing the action buttons below it off-screen.
                    Column(
                        modifier = Modifier.weight(1f, fill = false),
                        horizontalAlignment = Alignment.Start
                    ) {
                    Text(
                        "AMINEMA  •  ${if (item.kind == CatalogKind.SERIES) "سریال" else "فیلم"}",
                        color = CinemaRed,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        buildAnnotatedString {
                            append(item.title)
                            if (
                                item.year.isNotBlank() &&
                                !item.title.contains(item.year)
                            ) {
                                append("  ")
                                withStyle(
                                    SpanStyle(
                                        color = TextSecondary,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                ) {
                                    append("(${item.year})")
                                }
                            }
                        },
                        color = TextPrimary,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 38.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))

                    MetadataChips(item)

                    if (item.kind == CatalogKind.SERIES && item.episodeLabel.isNotBlank()) {
                        Spacer(Modifier.height(14.dp))
                        Text(
                            "آخرین انتشار  •  ${item.episodeLabel}",
                            color = Color(0xFFFFC857),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    // Tiny website beacons (for example three seconds) are not useful
                    // viewing progress and should not take visual space on a TV screen.
                    if (item.resumePosition >= 60_000L) {
                        Spacer(Modifier.height(14.dp))
                        val hasTrustworthyProgress =
                            item.duration >= 5 * 60_000L &&
                                item.resumePosition < item.duration * 0.95f
                        val progress = (item.resumePosition.toFloat() / item.duration.toFloat())
                            .coerceIn(0f, 1f)
                        if (hasTrustworthyProgress) {
                            LinearProgressIndicator(
                                progress = { progress },
                                color = CinemaRed,
                                trackColor = Color.White.copy(alpha = 0.18f),
                                modifier = Modifier
                                    .fillMaxWidth(0.62f)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(50))
                            )
                            Spacer(Modifier.height(7.dp))
                        }
                        Text(
                            "ادامه از ${formatDuration(item.resumePosition)}",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    // Optional information never reserves empty space. Old recents can still
                    // present a complete screen when the provider does not publish a synopsis.
                    if (item.summary.isNotBlank()) {
                        Spacer(Modifier.height(17.dp))
                        Text(
                            item.summary,
                            color = TextPrimary.copy(alpha = 0.90f),
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 26.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (metadataLoading) {
                        Spacer(Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(
                                color = CinemaRed,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "در حال تکمیل اطلاعات…",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    CreditsBlock(item)
                    }

                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        SpotlightButton(
                            text = primaryLabel(item),
                            icon = {
                                Icon(
                                    Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp)
                                )
                            },
                            primary = true,
                            modifier = Modifier.focusRequester(primaryFocus),
                            onClick = onWatch
                        )
                        SpotlightButton(
                            text = if (isFavorite) "در لیست من" else "افزودن به لیست",
                            icon = {
                                Icon(
                                    if (isFavorite) Icons.Filled.Check else Icons.Filled.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(25.dp)
                                )
                            },
                            onClick = onToggleFavorite
                        )
                }
            }
            }
        }
    }
}
}

@Composable
private fun CreditsBlock(item: SpotlightItem) {
    if (item.directors.isEmpty() && item.cast.isEmpty()) return
    Spacer(Modifier.height(15.dp))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (item.directors.isNotEmpty()) {
            CreditLine(
                label = "کارگردان",
                names = item.directors.take(2).map { it.name }
            )
        }
        if (item.cast.isNotEmpty()) {
            CreditLine(
                label = "بازیگران",
                names = item.cast.take(4).map { it.name }
            )
        }
    }
}

@Composable
private fun CreditLine(label: String, names: List<String>) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            "$label:",
            color = CinemaRed,
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(Modifier.width(10.dp))
        Text(
            names.joinToString("  •  "),
            color = TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MetadataChips(item: SpotlightItem) {
    data class Chip(val text: String, val background: Color, val foreground: Color = TextPrimary)

    val neutral = Color.White.copy(alpha = 0.11f)
    val chips = buildList {
        item.rating.takeIf { it.isNotBlank() }?.let { add(Chip("★ $it", neutral)) }
        item.runtime.takeIf { it.isNotBlank() }?.let { add(Chip(it, neutral)) }
        item.country.takeIf { it.isNotBlank() }?.let {
            add(Chip("محصول $it", neutral))
        }
        if (item.hasPersianDub) {
            add(
                Chip(
                    text = "دوبله فارسی",
                    background = Color(0xFF147A4A),
                    foreground = Color(0xFFE8FFF2)
                )
            )
        }
        if (item.hasPersianSubtitle) {
            add(
                Chip(
                    text = "زیرنویس فارسی",
                    background = Color(0xFF255A83),
                    foreground = Color(0xFFE8F5FF)
                )
            )
        }
        addAll(
            item.genres
                .filter { it.isNotBlank() }
                .take(if (item.country.isNotBlank()) 1 else 2)
                .map { Chip(it, neutral) }
        )
    }.distinct()

    if (chips.isEmpty()) return
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        chips.forEach { chip ->
            Text(
                chip.text,
                color = chip.foreground,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(chip.background)
                    .padding(horizontal = 13.dp, vertical = 7.dp)
            )
        }
    }
}

@Composable
private fun SpotlightButton(
    text: String,
    icon: @Composable () -> Unit,
    primary: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    FocusableCard(
        modifier = modifier.height(58.dp),
        shape = RoundedCornerShape(16.dp),
        focusedScale = 1.055f,
        onClick = onClick
    ) { focused ->
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .background(
                    when {
                        primary && focused -> CinemaRed
                        primary -> Color.White.copy(alpha = 0.15f)
                        focused -> Color.White.copy(alpha = 0.85f)
                        else -> Color.White.copy(alpha = 0.08f)
                    },
                    RoundedCornerShape(50)
                )
                .padding(horizontal = 26.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides
                    if (primary && focused) Color.White else if (primary) Color.White else if (focused) Ink else Color.White
            ) {
                icon()
                Spacer(Modifier.width(12.dp))
                Text(
                    text,
                    color = if (primary && focused) Color.White else if (primary) Color.White else if (focused) Ink else Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun primaryLabel(item: SpotlightItem): String = when {
    item.primaryAction == SpotlightAction.CONTINUE -> "ادامه تماشا"
    item.kind == CatalogKind.SERIES -> "مشاهده سریال"
    else -> "تماشا"
}

private fun formatDuration(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    return if (hours > 0L) {
        "${hours}:${minutes.toString().padStart(2, '0')}"
    } else {
        "${minutes}:${(totalSeconds % 60L).toString().padStart(2, '0')}"
    }
}
