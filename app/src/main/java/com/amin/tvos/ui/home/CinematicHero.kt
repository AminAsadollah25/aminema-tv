package com.amin.tvos.ui.home

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.amin.tvos.data.model.CatalogKind
import com.amin.tvos.data.model.SpotlightItem
import com.amin.tvos.ui.components.FocusableCard
import com.amin.tvos.ui.components.authenticatedPosterModel
import com.amin.tvos.ui.components.rememberArtworkAccent
import com.amin.tvos.ui.metadata.displayReleaseYear
import com.amin.tvos.ui.theme.CinemaRed
import kotlinx.coroutines.delay

/** One content-first cinematic moment on Home. */
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
    onHeroFocused: (Boolean) -> Unit = {},
    /** False after the user starts browsing rails, so off-screen Hero work stops. */
    allowAutoAdvance: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (slides.isEmpty()) return

    var activeIndex by remember(slides.map { it.id }) { mutableIntStateOf(0) }
    var controlsFocused by remember { mutableStateOf(false) }
    val active = slides[activeIndex.coerceIn(0, slides.lastIndex)]

    LaunchedEffect(slides.size, activeIndex, controlsFocused, allowAutoAdvance) {
        if (allowAutoAdvance && !controlsFocused && slides.size > 1) {
            delay(11_000L)
            activeIndex = (activeIndex + 1) % slides.size
        }
    }
    LaunchedEffect(active.id) { onVisibleSlideChanged(active) }

    AnimatedContent(
        targetState = active,
        transitionSpec = {
            (fadeIn(tween(460, easing = FastOutSlowInEasing)) +
                scaleIn(initialScale = 0.992f, animationSpec = tween(560))) togetherWith
                (fadeOut(tween(260)) + scaleOut(targetScale = 1.006f, animationSpec = tween(320)))
        },
        label = "homeHero",
        modifier = modifier.fillMaxWidth().padding(horizontal = 48.dp)
    ) { slide ->
        HeroSlideContent(
            slide = slide,
            slideCount = slides.size,
            activeIndex = activeIndex,
            onOpen = { onOpen(slide) },
            onPrevious = {
                activeIndex = (activeIndex - 1 + slides.size) % slides.size
            },
            onNext = { activeIndex = (activeIndex + 1) % slides.size },
            onInteractionChanged = { focused ->
                controlsFocused = focused
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
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onInteractionChanged: (Boolean) -> Unit
) {
    val item = slide.item
    val hasWideArtwork = item.backdropUrl.isNotBlank()
    val heroArtUrl = item.backdropUrl.ifBlank { item.posterUrl }
    val artModel = authenticatedPosterModel(
        posterUrl = heroArtUrl,
        pageUrl = item.contentUrl,
        widthPx = 1280,
        heightPx = 720
    )
    val portraitModel = authenticatedPosterModel(
        posterUrl = item.posterUrl,
        pageUrl = item.contentUrl,
        widthPx = 420,
        heightPx = 630
    )
    val accent = rememberArtworkAccent(
        posterUrl = heroArtUrl,
        pageUrl = item.contentUrl,
        fallback = Color(0xFF24212C)
    )
    val deep = lerp(Color(0xFF09090E), accent, 0.42f)
    val shape = RoundedCornerShape(28.dp)

    // One slow drift per slide instead of an infinite transition. It keeps the cinematic
    // feeling but stops GPU work as soon as the slide leaves composition.
    val artScale = remember(slide.id) { Animatable(if (hasWideArtwork) 1.01f else 1.04f) }
    LaunchedEffect(slide.id, heroArtUrl) {
        if (heroArtUrl.isNotBlank()) {
            artScale.animateTo(
                targetValue = if (hasWideArtwork) 1.035f else 1.055f,
                animationSpec = tween(11_000, easing = LinearEasing)
            )
        }
    }

    var primaryFocused by remember(slide.id) { mutableStateOf(false) }
    var nextFocused by remember(slide.id) { mutableStateOf(false) }
    var previousFocused by remember(slide.id) { mutableStateOf(false) }
    LaunchedEffect(primaryFocused, nextFocused, previousFocused) {
        onInteractionChanged(primaryFocused || nextFocused || previousFocused)
    }

    Box(
        Modifier
            .fillMaxWidth()
            .height(340.dp)
            .clip(shape)
            .background(deep)
    ) {
        if (heroArtUrl.isNotBlank()) {
            AsyncImage(
                model = artModel,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = artScale.value
                        scaleY = artScale.value
                        alpha = if (hasWideArtwork) 1f else 0.72f
                    }
                    .then(
                        if (!hasWideArtwork && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Modifier.blur(34.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                        } else Modifier
                    )
            )
        }

        // Physical geometry stays stable in Persian and English: copy on the left, artwork
        // on the right. Text inside the copy block is still genuinely RTL.
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    0f to deep.copy(alpha = 1f),
                    0.47f to deep.copy(alpha = 0.96f),
                    0.72f to Color.Black.copy(alpha = 0.42f),
                    1f to Color.Transparent
                )
            )
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.10f),
                    0.58f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.78f)
                )
            )
        )

        // Wide key art is atmosphere, not the title's main identity. Keep the familiar
        // portrait poster visible even when a banner exists. Requests are strictly sized
        // and only the active slide is composed, so the extra bitmap remains bounded.
        if (item.posterUrl.isNotBlank()) {
            AsyncImage(
                model = portraitModel,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(AbsoluteAlignment.CenterRight)
                    .padding(end = 34.dp)
                    .height(292.dp)
                    .width(195.dp)
                    .clip(RoundedCornerShape(20.dp))
            )
        }

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                Modifier
                    .align(AbsoluteAlignment.CenterLeft)
                    .fillMaxWidth(0.58f)
                    .fillMaxHeight()
                    .padding(start = 48.dp, end = 26.dp, top = 22.dp, bottom = 18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = CinemaRed,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        slide.eyebrow,
                        style = MaterialTheme.typography.labelLarge,
                        color = CinemaRed
                    )
                }
                Spacer(Modifier.height(5.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        heroTitle(item),
                        color = Color.White,
                        fontSize = 35.sp,
                        lineHeight = 40.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            shadow = Shadow(Color.Black.copy(alpha = 0.72f), blurRadius = 18f)
                        )
                    )
                    displayReleaseYear(item).takeIf { it.isNotBlank() }?.let { year ->
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(Color.Black.copy(alpha = 0.46f))
                                .border(
                                    BorderStroke(0.7.dp, Color.White.copy(alpha = 0.30f)),
                                    RoundedCornerShape(9.dp)
                                )
                                .padding(horizontal = 11.dp, vertical = 6.dp)
                        ) {
                            Text(
                                year,
                                color = Color.White.copy(alpha = 0.94f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
                Spacer(Modifier.height(7.dp))
                HeroMetadata(item)

                if (item.summary.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        item.summary,
                        style = MaterialTheme.typography.bodyLarge,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = Color.White.copy(alpha = 0.80f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                HeroCredits(item)

                Spacer(Modifier.weight(1f))

                if (item.duration > 0L && item.resumePosition > 0L) {
                    val progress = (item.resumePosition.toFloat() / item.duration.toFloat())
                        .coerceIn(0.02f, 1f)
                    Box(
                        Modifier.width(270.dp).height(4.dp).clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.20f))
                    ) {
                        Box(
                            Modifier.fillMaxWidth(progress).height(4.dp)
                                .background(CinemaRed)
                        )
                    }
                    Spacer(Modifier.height(11.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FocusableCard(
                        shape = RoundedCornerShape(50),
                        focusedScale = 1.035f,
                        onClick = onOpen,
                        onInteractionFocusChanged = { primaryFocused = it }
                    ) { focused ->
                        val background = if (focused) Color.White else CinemaRed
                        val foreground = if (focused) Color.Black else Color.White
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .width(164.dp)
                                .background(background)
                                .padding(horizontal = 20.dp, vertical = 9.dp)
                        ) {
                            Text(
                                slide.actionLabel.ifBlank { "تماشا" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = foreground,
                                maxLines = 1
                            )
                            Spacer(Modifier.width(7.dp))
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = foreground,
                                modifier = Modifier.size(23.dp)
                            )
                        }
                    }
                }
            }
        }

        // Keep navigation in one quiet corner. Edge-centred arrows can sit on top of a long
        // synopsis; this compact cluster stays clear of both the copy and the main action.
        if (slideCount > 1) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    modifier = Modifier
                        .align(AbsoluteAlignment.BottomLeft)
                        .padding(start = 20.dp, bottom = 18.dp)
                ) {
                    HeroCarouselArrow(
                        pointsLeft = false,
                        contentDescription = "اسلاید بعدی",
                        onClick = onNext,
                        onFocusChanged = { nextFocused = it }
                    )
                    HeroCarouselArrow(
                        pointsLeft = true,
                        contentDescription = "اسلاید قبلی",
                        onClick = onPrevious,
                        onFocusChanged = { previousFocused = it }
                    )
                }
            }
        }

        if (slideCount > 1) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    modifier = Modifier.align(AbsoluteAlignment.BottomRight).padding(22.dp)
                ) {
                    repeat(slideCount) { index ->
                        Box(
                            Modifier
                                .width(if (index == activeIndex) 24.dp else 7.dp)
                                .height(7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index == activeIndex) CinemaRed
                                    else Color.White.copy(alpha = 0.34f)
                                )
                        )
                    }
                }
            }
        }

        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(1.dp).background(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, CinemaRed.copy(alpha = 0.34f), Color.Transparent)
                )
            )
        )
    }
}

@Composable
private fun HeroCarouselArrow(
    pointsLeft: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    FocusableCard(
        shape = CircleShape,
        focusedScale = 1.12f,
        onClick = onClick,
        onInteractionFocusChanged = onFocusChanged,
        modifier = modifier
    ) { focused ->
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (focused) Color.White
                    else Color.Black.copy(alpha = 0.58f)
                )
                .border(
                    BorderStroke(
                        1.dp,
                        if (focused) Color.White
                        else Color.White.copy(alpha = 0.32f)
                    ),
                    CircleShape
                )
        ) {
            Icon(
                imageVector = if (pointsLeft) {
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft
                } else {
                    Icons.AutoMirrored.Filled.KeyboardArrowRight
                },
                contentDescription = contentDescription,
                tint = if (focused) Color.Black else Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

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
        add(if (item.kind == CatalogKind.SERIES) "سریال" else "فیلم")
        item.episodeLabel.takeIf { it.isNotBlank() }?.let(::add)
        when {
            item.hasPersianDub -> add("دوبله فارسی")
            item.hasPersianSubtitle -> add("زیرنویس فارسی")
        }
        item.rating.takeIf { it.isNotBlank() }?.let { add("★ $it") }
        item.runtime.takeIf { it.isNotBlank() }?.let(::add)
        item.genres.firstOrNull { it.isNotBlank() }?.let(::add)
    }.take(5)

    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        labels.forEach { label ->
            val isDub = label == "دوبله فارسی"
            Box(
                Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .background(
                        if (isDub) Color(0xFF087A4B).copy(alpha = 0.94f)
                        else Color.Black.copy(alpha = 0.34f)
                    )
                    .border(
                        BorderStroke(
                            0.5.dp,
                            if (isDub) Color.Transparent else Color.White.copy(alpha = 0.26f)
                        ),
                        RoundedCornerShape(7.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.94f),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun HeroCredits(item: SpotlightItem) {
    if (item.directors.isEmpty() && item.cast.isEmpty()) return
    val text = buildList {
        item.directors.take(1).takeIf { it.isNotEmpty() }?.let { directors ->
            add("کارگردان: ${directors.joinToString { it.name }}")
        }
        item.cast.take(3).takeIf { it.isNotEmpty() }?.let { cast ->
            add("بازیگران: ${cast.joinToString("، ") { it.name }}")
        }
    }.joinToString("   •   ")
    if (text.isBlank()) return
    Spacer(Modifier.height(5.dp))
    Text(
        text,
        color = Color.White.copy(alpha = 0.72f),
        fontSize = 12.sp,
        lineHeight = 16.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}
