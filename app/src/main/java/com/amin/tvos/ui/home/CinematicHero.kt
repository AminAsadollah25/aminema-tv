package com.amin.tvos.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
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
    val posterModel = authenticatedPosterModel(item.posterUrl, item.contentUrl)
    val shape = RoundedCornerShape(30.dp)

    // The hero takes its colour from the film itself, so the whole card shifts palette as
    // the carousel moves instead of every title sitting on the same grey slab.
    val accent = rememberArtworkAccent(
        posterUrl = heroArtUrl,
        pageUrl = item.contentUrl,
        fallback = Color(0xFF23202B)
    )

    // Two strengths of the film's own colour.
    //
    // The text sits on the darker one, because white type has to stay readable — that is the
    // one real constraint on how colourful this can get. Approaching the poster the colour
    // lightens towards the artwork's own tone, so the two meet at nearly the same value and
    // the join disappears without touching the poster itself.
    val themeDeep = lerp(Color(0xFF0A0A0F), accent, 0.55f)
    val themeNearArtwork = lerp(Color(0xFF0A0A0F), accent, 0.95f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(336.dp)
            .clip(shape)
            .background(themeDeep)
    ) {
        // Order matters here. The poster is drawn first, underneath, and a single wash is
        // then drawn across the *whole* card on top of it. That is what removes the hard
        // vertical seam: when the poster carried its own fade, the fade ended where the
        // poster ended, so the card's colour and the poster's colour met at a visible line.
        // One continuous gradient spanning both means the poster has no edge of its own —
        // it simply dissolves into the film's colour somewhere around the middle.
        if (item.posterUrl.isNotBlank()) {
            Box(
                Modifier
                    .align(AbsoluteAlignment.CenterRight)
                    .fillMaxHeight()
                    .aspectRatio(2f / 3f)
                    // Offscreen compositing is required for the mask below to apply to this
                    // layer rather than to everything already painted on the card.
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        // The poster's own pixels are erased towards its left edge. Tinting
                        // over the poster could never hide the seam — a translucent wash just
                        // colours both sides of a hard boundary. Making the image itself
                        // transparent is what actually dissolves it into the card.
                        // Only the outermost sliver of the poster is softened — just enough
                        // that it has no razor edge. Fading deep into the poster did hide the
                        // seam, but it also washed the artwork out, and the poster has to stay
                        // fully readable. Matching the card's colour to the poster's own tone
                        // is what hides the join now, not erasing the poster.
                        drawRect(
                            brush = Brush.horizontalGradient(
                                0f to Color.Transparent,
                                0.16f to Color.Black
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
            ) {
                AsyncImage(
                    model = posterModel,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Box(
            Modifier
                .fillMaxSize()
                .background(
                    // Spreads the film's colour from the text side outwards, lightening into
                    // the artwork's own tone as it approaches it, and going fully clear just
                    // before the poster starts so the poster is never tinted or dimmed.
                    // A long ramp starting around the middle. Lightening over a short distance
                    // produced a visible bright band next to the poster; spread across half
                    // the card it reads as the artwork's colour diffusing outwards instead.
                    Brush.horizontalGradient(
                        0f to themeDeep,
                        0.28f to themeDeep,
                        0.50f to lerp(themeDeep, themeNearArtwork, 0.38f),
                        0.66f to lerp(themeDeep, themeNearArtwork, 0.76f),
                        0.75f to themeNearArtwork,
                        0.83f to Color.Transparent
                    )
                )
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.05f),
                        0.5f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.42f)
                    )
                )
        )

        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .align(AbsoluteAlignment.CenterLeft)
                .fillMaxWidth(0.60f)
                .padding(start = 36.dp, end = 24.dp, top = 26.dp, bottom = 26.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = CinemaRed,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    slide.eyebrow,
                    style = MaterialTheme.typography.labelLarge,
                    lineHeight = 20.sp,
                    color = CinemaRed
                )
            }

            // Every block below reserves its space whether or not it has content. The
            // providers describe their titles very unevenly — some carry a year, rating and
            // episode label, some only a kind — and letting each block collapse made the
            // buttons land at a different height on every slide, so the whole card twitched
            // as the carousel turned.
            Spacer(Modifier.height(10.dp))
            Box(Modifier.height(92.dp), contentAlignment = Alignment.CenterStart) {
                Text(
                    heroTitle(item),
                    color = Color.White,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 44.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(Modifier.height(36.dp), contentAlignment = Alignment.CenterStart) {
                HeroMetadata(item)
            }

            Box(Modifier.height(56.dp), contentAlignment = Alignment.TopStart) {
                Text(
                    item.summary,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 24.sp,
                    color = Color.White.copy(alpha = 0.78f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (item.duration > 0L && item.resumePosition > 0L) {
                val progress = (item.resumePosition.toFloat() / item.duration.toFloat())
                    .coerceIn(0.02f, 1f)
                Spacer(Modifier.height(14.dp))
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
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FocusableCard(
                    shape = RoundedCornerShape(50),
                    focusedScale = 1.035f,
                    onClick = onOpen,
                    onInteractionFocusChanged = onInteractionChanged
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(CinemaRed)
                            .padding(horizontal = 22.dp, vertical = 12.dp)
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            slide.actionLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                    }
                }

                if (slideCount > 1) {
                    FocusableCard(
                        shape = RoundedCornerShape(50),
                        focusedScale = 1.035f,
                        onClick = onNext,
                        onInteractionFocusChanged = onInteractionChanged
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
                        ) {
                            Text("بعدی", style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

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
                .fillMaxWidth(0.68f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, CinemaRed.copy(alpha = 0.45f), Color.Transparent)
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
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (isDub) {
                            Color(0xFF087A4B).copy(alpha = 0.92f)
                        } else {
                            SurfaceDark.copy(alpha = 0.86f)
                        }
                    )
                    .padding(horizontal = 11.dp, vertical = 6.dp)
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDub) Color.White else TextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}
