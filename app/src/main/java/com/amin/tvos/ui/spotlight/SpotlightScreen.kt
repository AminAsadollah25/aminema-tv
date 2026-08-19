package com.amin.tvos.ui.spotlight

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.amin.tvos.data.ContentMetadataPolicy
import com.amin.tvos.data.model.AwardEvent
import com.amin.tvos.data.model.CatalogKind
import com.amin.tvos.data.model.PersonRef
import com.amin.tvos.data.model.SourceVariant
import com.amin.tvos.data.model.SpotlightAction
import com.amin.tvos.data.model.SpotlightItem
import com.amin.tvos.ui.components.FocusableCard
import com.amin.tvos.ui.components.authenticatedPosterModel
import com.amin.tvos.ui.metadata.displayReleaseYear
import com.amin.tvos.ui.metadata.isIranianTitle
import com.amin.tvos.ui.metadata.spotlightCategory
import com.amin.tvos.ui.metadata.toPersianMetadataLabel
import com.amin.tvos.ui.theme.CinemaRed
import com.amin.tvos.ui.theme.Ink
import com.amin.tvos.ui.theme.SurfaceElevated
import com.amin.tvos.ui.theme.TextPrimary
import com.amin.tvos.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpotlightScreen(
    item: SpotlightItem,
    isFavorite: Boolean,
    metadataLoading: Boolean,
    showEpisodeNavigator: Boolean = false,
    episodeNavigatorContent: @Composable () -> Unit = {},
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSourceSelected: (SourceVariant) -> Unit = {},
    onAction: (SpotlightAction) -> Unit
) {
    val primaryFocus = remember { FocusRequester() }
    var primaryActionReady by remember(item.contentUrl) { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val posterModel = authenticatedPosterModel(item.posterUrl, item.contentUrl)
    val contentVisibleState = remember { androidx.compose.animation.core.MutableTransitionState(false) }

    val listState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 50 }
    }

    LaunchedEffect(item.contentUrl, primaryActionReady, metadataLoading) {
        delay(150L)
        contentVisibleState.targetState = true
        if (!primaryActionReady || metadataLoading) return@LaunchedEffect
        // AnimatedVisibility does not attach its focus node on the first composition.
        // Metadata can also make the focused button move and auto-scroll the whole Hero.
        // Wait for the stable layout, restore its top, then focus without opening mid-page.
        delay(50L)
        listState.scrollToItem(0, 0)
        runCatching { primaryFocus.requestFocus() }
        delay(50L)
        listState.scrollToItem(0, 0)
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val heroArtUrl = item.backdropUrl.ifBlank { item.posterUrl }
        val backdropModel = authenticatedPosterModel(heroArtUrl, item.contentUrl)

        Box(Modifier.fillMaxSize().background(Ink)) {
            // Background Layer
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
                
                // When scrolled, we blur and darken the background. Otherwise it is clear.
                // If we are using a portrait poster as fallback, we blur it slightly by default.
                val blurRadius by animateFloatAsState(if (isScrolled) 24f else if (isFallback) 48f else 0f)
                val alphaOverlay by animateFloatAsState(if (isScrolled) 0.85f else 0.0f)

                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = backdropModel,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(if (blurRadius > 0f) Modifier.blur(blurRadius.dp, edgeTreatment = androidx.compose.ui.draw.BlurredEdgeTreatment.Unbounded) else Modifier)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                    )
                    // Darken overlay when scrolled
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = alphaOverlay)))
                }
            }
            // Gradients
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
                    // Fit, not Crop. Poster aspect ratios are not standard across providers:
                    // measured on live data, ParsiFlix ships 1080x1920 (0.56:1) while the
                    // international service ships roughly 2:3. Forcing either into a fixed
                    // 2:3 frame with Crop cut the top and bottom off the taller ones — the
                    // artwork arrived intact and the frame is what damaged it.
                    AsyncImage(
                        model = posterModel,
                        contentDescription = item.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Scrollable Content
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                // ITEM 0: HERO SECTION (Takes full height)
                item {
                    Box(modifier = Modifier.fillParentMaxSize()) {
                        // Back button
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

                        // Left side Text and Buttons
                        androidx.compose.animation.AnimatedVisibility(
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
                                        .fillMaxWidth(0.55f)
                                        .fillMaxHeight()
                                        .align(Alignment.CenterEnd) // Left side
                                        .padding(top = 12.dp),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f, fill = false),
                                        horizontalAlignment = Alignment.Start
                                    ) {
                                        Text(
                                            spotlightCategory(item),
                                            color = CinemaRed,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(Modifier.height(12.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                spotlightTitle(item),
                                                color = TextPrimary,
                                                fontSize = 32.sp,
                                                fontWeight = FontWeight.Bold,
                                                lineHeight = 38.sp,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Start,
                                                modifier = Modifier.weight(1f)
                                            )
                                            displayReleaseYear(item).takeIf { it.isNotBlank() }?.let { year ->
                                                Text(
                                                    year,
                                                    color = TextPrimary,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(Color.White.copy(alpha = 0.12f))
                                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(16.dp))

                                        MetadataChips(item)

                                        Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 16.dp)) {
                                            Column(modifier = Modifier.fillMaxSize()) {
                                                if (item.kind == CatalogKind.SERIES && item.episodeLabel.isNotBlank()) {
                                                    Spacer(Modifier.height(14.dp))
                                                    Text(
                                                        "آخرین انتشار  •  ${item.episodeLabel}",
                                                        color = Color(0xFFFFC857),
                                                        style = MaterialTheme.typography.titleMedium
                                                    )
                                                }

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

                                                if (item.summary.isNotBlank()) {
                                                    Spacer(Modifier.height(17.dp))
                                                    Text(
                                                        item.summary,
                                                        color = TextPrimary.copy(alpha = 0.90f),
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        lineHeight = 26.sp,
                                                        maxLines = 4,
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
                                                // CreditsBlock removed from hero, moved below
                                                
                                                Spacer(Modifier.height(32.dp))
                                                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                                    SpotlightButton(
                                                        text = primaryLabel(item),
                                                        icon = {
                                                            Icon(
                                                                if (item.primaryAction == SpotlightAction.SELECT_EPISODE) Icons.AutoMirrored.Filled.List else Icons.Filled.PlayArrow,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(28.dp)
                                                            )
                                                        },
                                                        primary = true,
                                                        modifier = Modifier
                                                            .focusRequester(primaryFocus)
                                                            .onGloballyPositioned {
                                                                primaryActionReady = true
                                                            },
                                                        onClick = {
                                                            if (item.primaryAction == SpotlightAction.SELECT_EPISODE) {
                                                                coroutineScope.launch {
                                                                    // Multi-source titles insert the source chooser between
                                                                    // the Hero and episodes; keep the primary action pointed
                                                                    // at the actual episode navigator in both layouts.
                                                                    val episodeItemIndex =
                                                                        if (item.sourceVariants.size > 1) 2 else 1
                                                                    listState.animateScrollToItem(episodeItemIndex)
                                                                }
                                                            } else {
                                                                onAction(item.primaryAction)
                                                            }
                                                        }
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
                    }
                }

                // Source choice stays outside the fixed Hero skeleton so adding providers can
                // never push the title/summary/actions below the visible 1080p decision area.
                if (item.sourceVariants.size > 1) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 72.dp, vertical = 18.dp)
                        ) {
                            SourceSelector(item, onSourceSelected)
                        }
                    }
                }

                // Episodes (If Series)
                if (item.kind == CatalogKind.SERIES && showEpisodeNavigator) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 72.dp, vertical = 24.dp)) {
                            Text(
                                "قسمت‌ها",
                                color = TextPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            episodeNavigatorContent()
                        }
                    }
                }

                // ITEM 2: Cast & Crew Avatars
                if (item.cast.isNotEmpty() || item.directors.isNotEmpty()) {
                    item {
                        CastAndCrewSection(item)
                    }
                }
                
                // ITEM 3: Awards Section
                if (item.awards.isNotEmpty()) {
                    item {
                        AwardsSection(item)
                    }
                }
                
                // ITEM 4: Extra padding at bottom
                item {
                    Spacer(Modifier.height(120.dp))
                }
            }
        }
    }
}

@Composable
private fun CastAndCrewSection(item: SpotlightItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 72.dp, vertical = 32.dp)
    ) {
        Text(
            "بازیگران و عوامل",
            color = TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 20.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val allPeople = mutableListOf<Pair<PersonRef, String>>()
            item.directors.forEach { allPeople.add(it to "کارگردان") }
            item.cast.forEach { allPeople.add(it to "بازیگر") }
            
            items(allPeople.distinctBy { it.first.name }) { (person, role) ->
                PersonAvatar(person = person, role = role, contentUrl = item.contentUrl)
            }
        }
    }
}

@Composable
private fun AwardsSection(item: SpotlightItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 72.dp, end = 72.dp, top = 16.dp, bottom = 32.dp)
    ) {
        Text(
            "جوایز و افتخارات",
            color = TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 20.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(item.awards) { award ->
                AwardCard(award)
            }
        }
    }
}

@Composable
private fun AwardCard(awardEvent: AwardEvent) {
    var isFocused by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .width(260.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .clip(RoundedCornerShape(16.dp))
            .background(if (isFocused) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Star,
                contentDescription = null,
                tint = Color(0xFFFFD700),
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = awardEvent.event,
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(12.dp))
        awardEvent.awards.take(3).forEach { detail ->
            Text(
                text = "• $detail",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
            )
        }
        if (awardEvent.awards.size > 3) {
            Text(
                text = "و ${awardEvent.awards.size - 3} مورد دیگر...",
                color = TextSecondary.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
    }
}

@Composable
private fun PersonAvatar(person: PersonRef, role: String, contentUrl: String) {
    var isFocused by remember { mutableStateOf(false) }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(120.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .clip(RoundedCornerShape(8.dp)) // Just for focus visual if needed, but we don't strictly need it focusable yet unless we want clickable actors
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            if (person.profileUrl.isNotBlank()) {
                AsyncImage(
                    model = authenticatedPosterModel(person.profileUrl, contentUrl),
                    contentDescription = person.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Fallback Initials
                val initial = person.name.trim().take(1)
                Text(
                    text = initial,
                    color = TextPrimary,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = person.name,
            color = TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Text(
            text = role,
            color = TextSecondary,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MetadataChips(item: SpotlightItem) {
    data class Chip(val text: String, val background: Color, val foreground: Color = TextPrimary)

    val neutral = Color.White.copy(alpha = 0.11f)
    val chips = buildList {
        item.rating.takeIf { it.isNotBlank() }?.let { rating ->
            val numeric = rating
                .toLatinNumberDigits()
                .replace('٫', '.')
                .let { Regex("""\d+(?:\.\d+)?""").find(it)?.value }
                ?.toFloatOrNull()
            if (!item.isIranianTitle() && numeric != null) {
                val (label, color, foreground) = when {
                    numeric < 5f -> Triple("ضعیف", Color(0xFF8E2430), Color(0xFFFFE8EA))
                    numeric < 7f -> Triple("متوسط", Color(0xFFA45F00), Color(0xFFFFF0D6))
                    numeric < 9f -> Triple("خوب", Color(0xFF087A4B), Color(0xFFE8FFF2))
                    else -> Triple("عالی", Color(0xFFD5A514), Color(0xFF1B1500))
                }
                add(Chip("IMDb  ★ $rating  •  $label", color, foreground))
            } else {
                add(Chip("★ $rating", neutral))
            }
        }
        item.runtime.takeIf { it.isNotBlank() }?.let { add(Chip(it, neutral)) }
        item.country.takeIf { it.isNotBlank() }?.let { add(Chip("محصول $it", neutral)) }
        item.language.takeIf { it.isNotBlank() }?.let { add(Chip("زبان $it", neutral)) }
        if (item.hasPersianDub) {
            add(Chip(text = "دوبله فارسی", background = Color(0xFF147A4A), foreground = Color(0xFFE8FFF2)))
        }
        if (item.hasPersianSubtitle) {
            add(Chip(text = "زیرنویس فارسی", background = Color(0xFF255A83), foreground = Color(0xFFE8F5FF)))
        }
        addAll(
            item.genres.filter { it.isNotBlank() }.take(4)
                .map { Chip(it.toPersianMetadataLabel(), neutral) }
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

private fun spotlightTitle(item: SpotlightItem): String {
    if (item.year.isBlank()) return item.title
    val rawYear = item.year.toLatinNumberDigits().filter(Char::isDigit).take(4)
    if (rawYear.isBlank()) return item.title
    return item.title
        .replace(Regex("""\s*[（(]\s*$rawYear\s*[)）]\s*$"""), "")
        .trim()
        .ifBlank { item.title }
}

private fun String.toLatinNumberDigits(): String = map { character ->
    when (character) {
        in '۰'..'۹' -> ('0'.code + character.code - '۰'.code).toChar()
        in '٠'..'٩' -> ('0'.code + character.code - '٠'.code).toChar()
        else -> character
    }
}.joinToString("")

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
                        primary && focused -> Color(0xFFFF2736)
                        primary -> CinemaRed
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

/** Compact TV-first source choice; it appears only for a verified canonical duplicate. */
@Composable
private fun SourceSelector(
    item: SpotlightItem,
    onSourceSelected: (SourceVariant) -> Unit
) {
    Row(
        modifier = Modifier
            .background(Ink.copy(alpha = 0.72f), RoundedCornerShape(24.dp))
            .then(
                Modifier.border(
                    BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                    RoundedCornerShape(24.dp)
                )
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "منبع تماشا",
            color = TextPrimary,
            style = MaterialTheme.typography.labelLarge
        )
        item.sourceVariants.forEach { source ->
            val selected = ContentMetadataPolicy.isSameTopLevelPage(
                item.contentUrl,
                source.item.contentUrl
            )
            FocusableCard(
                shape = RoundedCornerShape(50),
                focusedScale = 1.04f,
                onClick = { onSourceSelected(source) }
            ) { focused ->
                Text(
                    source.providerName.ifBlank { source.providerId },
                    color = if (selected || focused) Color.White else TextSecondary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier
                        .background(
                            when {
                                focused -> CinemaRed
                                selected -> CinemaRed.copy(alpha = 0.62f)
                                else -> Color.White.copy(alpha = 0.08f)
                            },
                            RoundedCornerShape(50)
                        )
                        .padding(horizontal = 16.dp, vertical = 9.dp)
                )
            }
        }
    }
}

private fun primaryLabel(item: SpotlightItem): String = when (item.primaryAction) {
    SpotlightAction.CONTINUE -> "ادامه تماشا"
    SpotlightAction.SELECT_EPISODE -> "انتخاب قسمت"
    SpotlightAction.LATEST_EPISODE -> "پخش آخرین قسمت"
    SpotlightAction.WATCH -> "تماشا"
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
