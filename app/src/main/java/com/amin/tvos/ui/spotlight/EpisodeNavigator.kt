package com.amin.tvos.ui.spotlight

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.amin.tvos.data.model.Episode
import com.amin.tvos.data.model.Season
import com.amin.tvos.data.model.SeriesEdition
import com.amin.tvos.ui.components.FocusableCard
import com.amin.tvos.ui.theme.CinemaRed
import com.amin.tvos.ui.theme.Ink
import com.amin.tvos.ui.theme.TextPrimary
import com.amin.tvos.ui.theme.TextSecondary
import com.amin.tvos.ui.components.authenticatedPosterModel

sealed interface EpisodeNavState {
    data object Loading : EpisodeNavState
    data object Failed : EpisodeNavState
    data class Loaded(val editions: List<SeriesEdition>) : EpisodeNavState
}

@Composable
fun EpisodeNavigatorInline(
    editions: List<SeriesEdition>,
    isLoading: Boolean,
    hasFailed: Boolean,
    posterUrl: String,
    contentUrl: String,
    onEpisodeSelected: (Episode, Season, SeriesEdition) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedEdition by remember(editions) {
        mutableStateOf(editions.firstOrNull { it.isDefault } ?: editions.firstOrNull())
    }
    var selectedSeason by remember(selectedEdition) {
        mutableStateOf(selectedEdition?.seasons?.firstOrNull())
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 300.dp, max = 450.dp)
    ) {
        when {
            isLoading -> {
                Column(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = CinemaRed,
                        modifier = Modifier.size(56.dp),
                        strokeWidth = 4.dp
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = "در حال بارگذاری قسمت‌ها...",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            hasFailed || editions.isEmpty() -> {
                Column(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "⚠️", style = MaterialTheme.typography.displayMedium)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "اطلاعات قسمت‌ها یافت نشد",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            else -> {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Seasons (Horizontal Tabs)
                    selectedEdition?.seasons?.let { seasons ->
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(seasons) { season ->
                                val isSelected = season.id == selectedSeason?.id
                                FocusableCard(
                                    modifier = Modifier.height(48.dp),
                                    shape = RoundedCornerShape(24.dp),
                                    focusedScale = 1.05f,
                                    onClick = { selectedSeason = season }
                                ) { focused ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                when {
                                                    focused -> Color.White
                                                    isSelected -> Color.White.copy(alpha = 0.15f)
                                                    else -> Color.White.copy(alpha = 0.05f)
                                                }
                                            )
                                            .then(
                                                if (!focused && !isSelected) Modifier.border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp)) else Modifier
                                            )
                                            .padding(horizontal = 24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = season.name,
                                            color = when {
                                                focused -> Ink
                                                isSelected -> Color.White
                                                else -> TextSecondary
                                            },
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = if (focused || isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Episodes (Horizontal Cards with Arrows)
                    selectedSeason?.episodes?.let { episodes ->
                        val listState = rememberLazyListState()
                        val scope = rememberCoroutineScope()
                        val seasonName = selectedSeason?.name ?: ""
                        
                        val density = androidx.compose.ui.platform.LocalDensity.current
                        val itemWidthPx = remember(density) { with(density) { 240.dp.toPx() } }
                        
                        LaunchedEffect(selectedSeason) {
                            listState.scrollToItem(0)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Right arrow (RTL: scroll right = previous)
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        listState.animateScrollBy(-itemWidthPx)
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(50))
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ChevronRight,
                                    contentDescription = "قبلی",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            LazyRow(
                                state = listState,
                                flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(20.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(episodes) { ep ->
                                    FocusableCard(
                                        modifier = Modifier
                                            .width(220.dp)
                                            .height(130.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        focusedScale = 1.06f,
                                        onClick = {
                                            selectedEdition?.let { edition ->
                                                selectedSeason?.let { season ->
                                                    onEpisodeSelected(ep, season, edition)
                                                }
                                            }
                                        }
                                    ) { focused ->
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            // Background Image (Poster)
                                            AsyncImage(
                                                model = authenticatedPosterModel(posterUrl, contentUrl),
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )

                                            // Dark Gradient Overlay
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        Brush.verticalGradient(
                                                            colors = listOf(
                                                                Color.Black.copy(alpha = 0.1f),
                                                                Color.Black.copy(alpha = 0.85f)
                                                            )
                                                        )
                                                    )
                                            )
                                            
                                            // Focus border overlay
                                            if (focused) {
                                                Box(modifier = Modifier.fillMaxSize().border(3.dp, Color.White, RoundedCornerShape(12.dp)))
                                            }

                                            // Episode Info Overlay
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(12.dp),
                                                verticalArrangement = Arrangement.Bottom
                                            ) {
                                                // Season name label
                                                if (seasonName.isNotBlank()) {
                                                    Text(
                                                        text = seasonName,
                                                        color = Color.White.copy(alpha = 0.7f),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Medium,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Spacer(Modifier.height(2.dp))
                                                }
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    val persianTitle = ep.title.map { char -> 
                                                        if (char in '0'..'9') (char - '0' + '۰'.code).toChar() else char 
                                                    }.joinToString("")
                                                    
                                                    Box(
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .background(if (focused) CinemaRed else Color.White.copy(alpha = 0.2f), RoundedCornerShape(50)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Filled.PlayArrow,
                                                            contentDescription = "پخش",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                    
                                                    Spacer(Modifier.width(10.dp))
                                                    
                                                    Text(
                                                        text = "\u200F$persianTitle",
                                                        color = Color.White,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }

                                            if (ep.isWatched) {
                                                Icon(
                                                    imageVector = Icons.Filled.Check,
                                                    contentDescription = "دیده\u200Cشده",
                                                    tint = CinemaRed,
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(8.dp)
                                                        .size(24.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Left arrow (RTL: scroll left = next)
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        listState.animateScrollBy(itemWidthPx)
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(50))
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ChevronLeft,
                                    contentDescription = "بعدی",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
