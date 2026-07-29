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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    LaunchedEffect(item.contentUrl) {
        delay(220L)
        primaryFocus.requestFocus()
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(Modifier.fillMaxSize().background(Ink)) {
            CinematicBackground(
                posterUrl = item.posterUrl,
                pageUrl = item.contentUrl
            )
            // Spotlight uses a stronger side fade than Home: the information block remains
            // crisp even when the source only provides a bright portrait poster.
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            0f to Ink.copy(alpha = 0.98f),
                            0.62f to Ink.copy(alpha = 0.68f),
                            1f to Ink.copy(alpha = 0.20f)
                        )
                    )
            )

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

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 64.dp, end = 72.dp, top = 42.dp, bottom = 42.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // In RTL the first child sits on the right: the poster anchors the page
                // while all decision-making controls stay in the large left content area.
                Box(
                    modifier = Modifier
                        .width(300.dp)
                        .fillMaxHeight(0.74f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(SurfaceElevated)
                ) {
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
                                        listOf(Color(0xFF292934), Ink)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(78.dp)
                            )
                        }
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.74f))
                                )
                            )
                    )
                }

                Spacer(Modifier.width(54.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 12.dp),
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
                        primary && focused -> Color(0xFFFF2631)
                        primary -> CinemaRed
                        focused -> Color.White
                        else -> Color.White.copy(alpha = 0.90f)
                    },
                    RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides
                    if (primary) Color.White else Ink
            ) {
                icon()
                Spacer(Modifier.width(9.dp))
                Text(
                    text,
                    color = if (primary) Color.White else Ink,
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
