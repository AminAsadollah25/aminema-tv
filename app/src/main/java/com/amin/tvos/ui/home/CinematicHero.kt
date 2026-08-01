package com.amin.tvos.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.amin.tvos.data.model.CatalogKind
import com.amin.tvos.data.model.SpotlightItem
import com.amin.tvos.ui.components.FocusableCard
import com.amin.tvos.ui.components.authenticatedPosterModel
import com.amin.tvos.ui.components.rememberArtworkAccent
import com.amin.tvos.ui.theme.CinemaRed
import com.amin.tvos.ui.theme.SurfaceDark
import com.amin.tvos.ui.theme.TextSecondary
import kotlinx.coroutines.delay

/**
 * One content-first Home hero. The provider is deliberately absent from the visual hierarchy:
 * the user chooses a title here and Aminema keeps the exact provider action inside Spotlight.
 */
data class HomeHeroSlide(
    val id: String,
    val eyebrow: String,
    val actionLabel: String,
    val item: SpotlightItem
)

@Composable
fun CinematicHero(
    slides: List<HomeHeroSlide>,
    onOpen: (HomeHeroSlide) -> Unit,
    onVisibleSlideChanged: (HomeHeroSlide) -> Unit,
    /** True while one of the hero's own controls holds focus or hover. */
    onHeroFocused: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (slides.isEmpty()) return

    var activeIndex by remember(slides.map { it.id }) { mutableIntStateOf(0) }
    var interactionPaused by remember { mutableStateOf(false) }
    val active = slides[activeIndex.coerceIn(0, slides.lastIndex)]

    LaunchedEffect(slides.size, activeIndex, interactionPaused) {
        if (!interactionPaused && slides.size > 1) {
            delay(11_000L)
            activeIndex = (activeIndex + 1) % slides.size
        }
    }
    LaunchedEffect(active.id) {
        onVisibleSlideChanged(active)
    }

    AnimatedContent(
        targetState = active,
        transitionSpec = {
            (
                fadeIn(tween(520, easing = FastOutSlowInEasing)) +
                    scaleIn(
                        initialScale = 0.985f,
                        animationSpec = tween(620, easing = FastOutSlowInEasing)
                    )
                ) togetherWith (
                fadeOut(tween(300)) +
                    scaleOut(targetScale = 1.01f, animationSpec = tween(360))
                )
        },
        label = "homeHero",
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp)
    ) { slide ->
        HeroSlideContent(
            slide = slide,
            slideCount = slides.size,
            activeIndex = activeIndex,
            onOpen = { onOpen(slide) },
            onNext = {
                activeIndex = (activeIndex + 1) % slides.size
            },
            onInteractionChanged = { focused ->
                interactionPaused = focused
                onHeroFocused(focused)
            }
        )
    }
}

@Composable
private fun HeroSlideContent(
    slide: HomeHeroSlide,
    slideCount: Int,
    activeIndex: Int,
    onOpen: () -> Unit,
    onNext: () -> Unit,
    onInteractionChanged: (Boolean) -> Unit
) {
    val item = slide.item
    // The wide key art the provider publishes for its own banner carousel is the right
    // shape for a hero; the portrait poster is the fallback when a title has none.
    val heroArtUrl = item.backdropUrl.ifBlank { item.posterUrl }
    val backdropModel = authenticatedPosterModel(heroArtUrl, item.contentUrl)
    val posterModel = authenticatedPosterModel(item.posterUrl, item.contentUrl)
    val shape = RoundedCornerShape(30.dp)

    val accent = rememberArtworkAccent(
        posterUrl = heroArtUrl,
        pageUrl = item.contentUrl,
        fallback = Color(0xFF23202B)
    )

    val themeDeep = lerp(Color(0xFF0A0A0F), accent, 0.65f)
    val themeNearArtwork = lerp(Color(0xFF0A0A0F), accent, 0.98f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(336.dp)
            .clip(shape)
            .background(themeDeep)
    ) {
        // 1. Full-bleed background from the wide backdrop
        if (heroArtUrl.isNotBlank()) {
            val isFallback = item.backdropUrl.isBlank() && item.posterUrl.isNotBlank()
            val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "kenBurns")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1.0f,
                targetValue = 1.05f,
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    animation = tween(25000, easing = androidx.compose.animation.core.LinearEasing),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
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

        val isRtl = androidx.compose.ui.platform.LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl

        // 2. Gradients for text readability and cinematic depth
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { scaleX = if (isRtl) -1f else 1f }
                .background(
                    Brush.horizontalGradient(
                        0f to themeDeep.copy(alpha = 1.0f),
                        0.45f to themeDeep.copy(alpha = 0.98f),
                        0.75f to themeDeep.copy(alpha = 0.6f),
                        1f to Color.Transparent
                    )
                )
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to themeDeep.copy(alpha = 0.4f),
                        0.3f to Color.Transparent,
                        0.65f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.9f)
                    )
                )
        )

        // 3. Right side: Full height portrait poster
        if (item.posterUrl.isNotBlank()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .aspectRatio(2f / 3f)
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        val isRtlDraw = layoutDirection == androidx.compose.ui.unit.LayoutDirection.Rtl
                        val start = if (isRtlDraw) size.width else 0f
                        val end = if (isRtlDraw) 0f else size.width
                        drawRect(
                            brush = Brush.horizontalGradient(
                                0f to Color.Transparent,
                                0.25f to Color.Black,
                                1f to Color.Black,
                                startX = start,
                                endX = end
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

        // 4. Left/Start side: The fixed-skeleton text block
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth(0.55f)
                .fillMaxHeight()
                .padding(start = 56.dp, top = 44.dp, bottom = 40.dp)
        ) {
            // Eyebrow
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = CinemaRed.copy(alpha = 0.9f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    slide.eyebrow,
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = 14.sp,
                    letterSpacing = 1.2.sp,
                    color = CinemaRed.copy(alpha = 0.9f)
                )
            }
            Spacer(Modifier.height(8.dp))

            // Title
            Text(
                heroTitle(item),
                color = Color.White,
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 56.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.headlineLarge.copy(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.8f),
                        offset = Offset(0f, 8f),
                        blurRadius = 24f
                    )
                )
            )
            Spacer(Modifier.height(14.dp))

            // Metadata Chips
            HeroMetadata(item)
            Spacer(Modifier.height(18.dp))

            // Summary
            Text(
                item.summary.ifBlank { "بدون توضیحات." },
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 16.sp,
                lineHeight = 28.sp,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(0.9f)
            )

            // Dynamic space guarantees the buttons stay anchored to the bottom
            Spacer(Modifier.weight(1f))

            // Progress bar (if any) is squeezed above the buttons
            if (item.duration > 0L && item.resumePosition > 0L) {
                val progress = (item.resumePosition.toFloat() / item.duration.toFloat())
                    .coerceIn(0.02f, 1f)
                Box(
                    Modifier
                        .width(280.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f))
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(progress)
                            .height(4.dp)
                            .background(CinemaRed)
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            // Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Primary Button
                FocusableCard(
                    shape = RoundedCornerShape(percent = 50), // Pill shaped
                    focusedScale = 1.05f,
                    onClick = onOpen,
                    onInteractionFocusChanged = onInteractionChanged
                ) { focused ->
                    val bgColor = when {
                        focused -> Color.White
                        else -> Color.White.copy(alpha = 0.15f)
                    }
                    val contentColor = if (focused) Color.Black else Color.White
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(bgColor)
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            slide.actionLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )
                    }
                }

                // Secondary Button
                if (slideCount > 1) {
                    FocusableCard(
                        shape = RoundedCornerShape(percent = 50), // Pill shaped
                        focusedScale = 1.05f,
                        onClick = onNext,
                        onInteractionFocusChanged = onInteractionChanged
                    ) { focused ->
                        val bgColor = when {
                            focused -> Color.White
                            else -> Color.White.copy(alpha = 0.08f)
                        }
                        val contentColor = if (focused) Color.Black else Color.White
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(bgColor)
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Text(
                                "اسلاید بعدی",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = contentColor
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Pagination dots
        if (slideCount > 1) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier
                    .align(AbsoluteAlignment.BottomRight)
                    .padding(24.dp)
            ) {
                repeat(slideCount) { index ->
                    Box(
                        Modifier
                            .width(if (index == activeIndex) 24.dp else 7.dp)
                            .height(7.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == activeIndex) {
                                    CinemaRed
                                } else {
                                    Color.White.copy(alpha = 0.3f)
                                }
                            )
                    )
                }
            }
        }

        // A subtle lower edge makes the hero feel projected rather than card-like.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, CinemaRed.copy(alpha = 0.35f), Color.Transparent)
                    )
                )
        )
    }
}

/**
 * Providers often bake the year into the title — "House of the Dragon (2022)" — and the
 * metadata row shows a year chip of its own, so the same number appeared twice. Spotlight
 * already guarded against this; the hero now does too.
 */
private fun heroTitle(item: SpotlightItem): String {
    if (item.year.isBlank()) return item.title
    return item.title
        .replace(Regex("""\s*[（(]\s*${Regex.escape(item.year)}\s*[)）]\s*$"""), "")
        .trim()
        .ifBlank { item.title }
}

@Composable
private fun HeroMetadata(item: SpotlightItem) {
    val labels = buildList {
        item.year.takeIf { it.isNotBlank() }?.let(::add)
        add(if (item.kind == CatalogKind.SERIES) "سریال" else "فیلم")
        item.episodeLabel.takeIf { it.isNotBlank() }?.let(::add)
        when {
            item.hasPersianDub -> add("دوبله فارسی")
            item.hasPersianSubtitle -> add("زیرنویس فارسی")
        }
        item.rating.takeIf { it.isNotBlank() }?.let { add("★ $it") }
    }.take(4)

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEach { label ->
            val isDub = label == "دوبله فارسی"
            Box(
                Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isDub) Color(0xFF087A4B).copy(alpha = 0.92f)
                        else Color.White.copy(alpha = 0.12f)
                    )
                    .border(
                        BorderStroke(0.5.dp, if (isDub) Color.Transparent else Color.White.copy(alpha = 0.3f)),
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1
                )
            }
        }
    }
}
