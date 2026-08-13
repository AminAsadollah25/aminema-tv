package com.amin.tvos.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as columnItems
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.amin.tvos.data.ContentMetadataPolicy
import com.amin.tvos.data.model.CatalogItem
import com.amin.tvos.data.model.CatalogKind
import com.amin.tvos.data.model.CatalogSection
import com.amin.tvos.ui.components.CatalogCard
import com.amin.tvos.ui.components.FocusableCard
import com.amin.tvos.ui.components.LanguageBadge
import com.amin.tvos.ui.components.authenticatedPosterModel
import com.amin.tvos.ui.theme.CinemaRed
import com.amin.tvos.ui.theme.Ink
import com.amin.tvos.ui.theme.SurfaceElevated
import com.amin.tvos.ui.theme.TextPrimary
import com.amin.tvos.ui.theme.TextSecondary
import com.amin.tvos.ui.metadata.toPersianMetadataLabel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.collectLatest

private enum class LibraryTypeFilter(val label: String) {
    ALL("همه"),
    MOVIES("فیلم"),
    SERIES("سریال")
}

private enum class LibraryLanguageFilter(val label: String) {
    ALL("همه زبان‌ها"),
    DUBBED("دوبله فارسی"),
    SUBTITLED("زیرنویس فارسی")
}

private enum class LibrarySort(val label: String) {
    ORIGINAL("جدیدترین"),
    TITLE("عنوان"),
    YEAR("سال"),
    RATING("امتیاز")
}

private enum class LibraryViewMode {
    GRID,
    LIST
}

/**
 * Full-screen, TV-friendly view of one Home catalog rail.
 *
 * The source list is already canonicalized by Home. Both presentations reveal the result in
 * small pages as the user approaches the end, so a large archive never creates every card at
 * once. The provider adapters currently hand this screen the verified cached window; extending
 * that window is deliberately separate from the UI and never touches cookies or login state.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CatalogLibraryScreen(
    title: String,
    itemsProvider: () -> List<CatalogItem>,
    catalogRevision: List<CatalogSection>,
    refreshingServices: Set<String>,
    providerIds: Set<String>,
    onRefresh: () -> Unit,
    onLoadMore: (Int, () -> Unit) -> Unit,
    onBack: () -> Unit,
    onOpen: (CatalogItem) -> Unit
) {
    // Read the current provider window on every catalog revision. This is important for the
    // View All screen: a background page fetch replaces the cached section without recreating
    // the navigation route.
    val items = itemsProvider()
    var typeFilter by remember { mutableStateOf(LibraryTypeFilter.ALL) }
    var languageFilter by remember { mutableStateOf(LibraryLanguageFilter.ALL) }
    var genreFilter by remember { mutableStateOf(ALL_GENRES) }
    var sort by remember { mutableStateOf(LibrarySort.ORIGINAL) }
    var viewMode by remember { mutableStateOf(LibraryViewMode.GRID) }
    var visibleCount by remember { mutableStateOf(PAGE_SIZE) }
    val loadedPageLimit = catalogRevision
        .filter { it.serviceId in providerIds }
        .maxOfOrNull(CatalogSection::loadedPageLimit)
        ?.coerceAtLeast(DEFAULT_PAGE_LIMIT)
        ?: DEFAULT_PAGE_LIMIT
    var requestedPageLimit by remember(providerIds) {
        mutableStateOf(loadedPageLimit + PAGE_STEP)
    }
    var isLoadingMore by remember { mutableStateOf(false) }
    var noMoreItems by remember { mutableStateOf(false) }
    var itemCountBeforeLoad by remember { mutableStateOf(-1) }
    var loadCompletionToken by remember { mutableStateOf(0) }
    val isRefreshing = providerIds.any { it in refreshingServices }
    // A provider can finish a deeper background request while this route is open. Keep the
    // next request ahead of the cache instead of re-fetching an already-loaded page window.
    LaunchedEffect(loadedPageLimit) {
        if (!isLoadingMore && loadedPageLimit > requestedPageLimit) {
            requestedPageLimit = loadedPageLimit + PAGE_STEP
        }
    }
    val providerHasMore = catalogRevision.any { section ->
        section.serviceId in providerIds && (
            section.hasMoreAll ||
                section.hasMoreMovies ||
                section.hasMoreSeries ||
                section.hasMorePopularSeries
            )
    }
    val genres = remember(items) {
        listOf(ALL_GENRES) + items.flatMap { it.genres }
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
            .sorted()
            .take(MAX_GENRE_FILTERS)
    }
    LaunchedEffect(genres, genreFilter) {
        if (genreFilter !in genres) genreFilter = ALL_GENRES
    }
    val visibleItems = remember(items, typeFilter, languageFilter, genreFilter, sort) {
        items
            .asSequence()
            .distinctBy { ContentMetadataPolicy.canonicalContentUrl(it.contentUrl) }
            .filter { item ->
                typeFilter == LibraryTypeFilter.ALL ||
                    (typeFilter == LibraryTypeFilter.MOVIES && item.kind == CatalogKind.MOVIE) ||
                    (typeFilter == LibraryTypeFilter.SERIES && item.kind == CatalogKind.SERIES)
            }
            .filter { item ->
                languageFilter == LibraryLanguageFilter.ALL ||
                    (languageFilter == LibraryLanguageFilter.DUBBED && item.hasPersianDub) ||
                    (languageFilter == LibraryLanguageFilter.SUBTITLED && item.hasPersianSubtitle)
            }
            .filter { item -> genreFilter == ALL_GENRES || genreFilter in item.genres }
            .let { sequence ->
                when (sort) {
                    LibrarySort.ORIGINAL -> sequence
                    LibrarySort.TITLE -> sequence.sortedBy { it.title.lowercase() }
                    LibrarySort.YEAR -> sequence.sortedByDescending { it.year.toIntOrNull() ?: 0 }
                    LibrarySort.RATING -> sequence.sortedByDescending {
                        it.rating.replace('٫', '.').toDoubleOrNull() ?: 0.0
                    }
                }
            }
            .toList()
    }
    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()
    val filterKey = listOf(typeFilter, languageFilter, genreFilter, sort, viewMode)
    LaunchedEffect(filterKey) {
        visibleCount = PAGE_SIZE
    }
    LaunchedEffect(loadCompletionToken) {
        if (loadCompletionToken == 0) return@LaunchedEffect
        // Give the repository-backed selector one frame before deciding whether the archive
        // actually grew.
        kotlinx.coroutines.delay(250L)
        val latestItems = itemsProvider()
        val latestProviderHasMore = catalogRevision.any { section ->
            section.serviceId in providerIds && (
                section.hasMoreAll || section.hasMoreMovies ||
                    section.hasMoreSeries || section.hasMorePopularSeries
                )
        }
        noMoreItems = latestItems.size <= itemCountBeforeLoad && !latestProviderHasMore
        isLoadingMore = false
    }
    val displayedItems = remember(visibleItems, visibleCount) {
        visibleItems.take(visibleCount)
    }
    val hasMoreItems = displayedItems.size < visibleItems.size

    fun requestMore() {
        when {
            displayedItems.size < visibleItems.size -> {
                visibleCount = (visibleCount + PAGE_SIZE).coerceAtMost(visibleItems.size)
            }
            // The first library window may come from an older cache without the new hasMore
            // flags. Allow one probe refresh; the provider result then decides whether to stop.
            !noMoreItems && !isRefreshing && !isLoadingMore && loadedPageLimit < MAX_PAGE_LIMIT -> {
                requestedPageLimit = (loadedPageLimit + PAGE_STEP).coerceAtMost(MAX_PAGE_LIMIT)
                itemCountBeforeLoad = items.size
                isLoadingMore = true
                onLoadMore(requestedPageLimit) {
                    loadCompletionToken += 1
                }
            }
        }
    }

    LaunchedEffect(gridState, displayedItems.size, visibleItems.size, viewMode, isRefreshing) {
        if (viewMode != LibraryViewMode.GRID) return@LaunchedEffect
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .distinctUntilChanged()
            .filter { lastIndex ->
                lastIndex >= displayedItems.lastIndex - LOAD_AHEAD
            }
            .collectLatest {
                requestMore()
            }
    }
    LaunchedEffect(listState, displayedItems.size, visibleItems.size, viewMode, isRefreshing) {
        if (viewMode != LibraryViewMode.LIST) return@LaunchedEffect
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .distinctUntilChanged()
            .filter { lastIndex ->
                lastIndex >= displayedItems.lastIndex - LOAD_AHEAD
            }
            .collectLatest {
                requestMore()
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 48.dp, end = 48.dp, top = 28.dp, bottom = 16.dp)
        ) {
            FocusableCard(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(50),
                onClick = onBack
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "بازگشت",
                        tint = TextPrimary
                    )
                }
            }
            Spacer(Modifier.width(18.dp))
            FocusableCard(
                shape = RoundedCornerShape(50),
                enabled = !isRefreshing,
                onClick = {
                    onRefresh()
                }
            ) {
                Text(
                    if (isRefreshing) "در حال بروزرسانی" else "بروزرسانی",
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(title, style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            Spacer(Modifier.width(14.dp))
            Text(
                "${visibleItems.size} عنوان",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(Modifier.weight(1f))
            Icon(Icons.Filled.FilterList, contentDescription = null, tint = TextSecondary)
            Spacer(Modifier.width(8.dp))
            Text("فیلتر و مرتب‌سازی", color = TextSecondary)
            Spacer(Modifier.width(18.dp))
            FocusableCard(
                shape = RoundedCornerShape(50),
                onClick = { viewMode = LibraryViewMode.GRID }
            ) {
                Icon(
                    Icons.Filled.GridView,
                    contentDescription = "نمای کاشی",
                    tint = if (viewMode == LibraryViewMode.GRID) CinemaRed else TextSecondary,
                    modifier = Modifier.padding(12.dp)
                )
            }
            FocusableCard(
                shape = RoundedCornerShape(50),
                onClick = { viewMode = LibraryViewMode.LIST }
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ViewList,
                    contentDescription = "نمای فهرست",
                    tint = if (viewMode == LibraryViewMode.LIST) CinemaRed else TextSecondary,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp, vertical = 4.dp)
        ) {
            FilterGroup(
                values = LibraryTypeFilter.entries,
                selected = typeFilter,
                onSelected = { typeFilter = it },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(10.dp))
            FilterGroup(
                values = LibraryLanguageFilter.entries,
                selected = languageFilter,
                onSelected = { languageFilter = it },
                modifier = Modifier.weight(1.35f)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp, vertical = 4.dp)
        ) {
            Text("ژانر", color = TextSecondary, style = MaterialTheme.typography.labelLarge)
            FilterGroup(
                values = genres,
                selected = genreFilter,
                onSelected = { genreFilter = it },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(10.dp))
            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, tint = TextSecondary)
            FilterGroup(
                values = LibrarySort.entries,
                selected = sort,
                onSelected = { sort = it },
                modifier = Modifier.weight(1.35f)
            )
        }

        if (visibleItems.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("با این فیلتر عنوانی پیدا نشد.", color = TextSecondary)
            }
        } else {
            if (viewMode == LibraryViewMode.GRID) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 180.dp),
                    state = gridState,
                    contentPadding = PaddingValues(48.dp, 22.dp, 48.dp, 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(26.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = displayedItems,
                        key = { ContentMetadataPolicy.canonicalContentUrl(it.contentUrl) }
                    ) { item ->
                        CatalogCard(item = item, onClick = { onOpen(item) })
                    }
                    if (hasMoreItems || isLoadingMore || (!noMoreItems && loadedPageLimit < MAX_PAGE_LIMIT)) {
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                            LoadingMoreLabel(
                                loading = isLoadingMore,
                                onClick = ::requestMore
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(48.dp, 22.dp, 48.dp, 48.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    columnItems(
                        items = displayedItems,
                        key = { ContentMetadataPolicy.canonicalContentUrl(it.contentUrl) }
                    ) { item ->
                        CatalogListItem(item = item, onClick = { onOpen(item) })
                    }
                    if (hasMoreItems || isLoadingMore || (!noMoreItems && loadedPageLimit < MAX_PAGE_LIMIT)) {
                        item {
                            LoadingMoreLabel(
                                loading = isLoadingMore,
                                onClick = ::requestMore
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingMoreLabel(loading: Boolean, onClick: () -> Unit) {
    FocusableCard(
        shape = RoundedCornerShape(50),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp)
        ) {
            Text(
                if (loading) "در حال دریافت ادامه فهرست…" else "نمایش ادامه فهرست",
                color = TextSecondary
            )
            Spacer(Modifier.width(10.dp))
            if (loading) {
                CircularProgressIndicator(
                    color = CinemaRed,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun CatalogListItem(item: CatalogItem, onClick: () -> Unit) {
    val posterModel = authenticatedPosterModel(item.posterUrl, item.contentUrl)
    FocusableCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        focusedScale = 1.015f,
        onClick = onClick
    ) { focused ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(14.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        item.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = if (focused) Color.White else TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(14.dp))
                    Text(
                        if (item.kind == CatalogKind.SERIES) "سریال" else "فیلم",
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item.year.takeIf(String::isNotBlank)?.let { MetaPill(it) }
                    item.rating.takeIf(String::isNotBlank)?.let { MetaPill("IMDb ${it}") }
                    item.episodeLabel.takeIf(String::isNotBlank)?.let { MetaPill(it) }
                    MetaPill(sourceLabel(item.serviceId))
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (item.hasPersianDub) LanguageBadge("دوبله فارسی", Color(0xFF138A57))
                    if (item.hasPersianSubtitle) {
                        LanguageBadge("زیرنویس فارسی", Color(0xFF2E6F9E))
                    }
                }
                Text(
                    item.summary.ifBlank { "برای مشاهده جزئیات و تماشا انتخاب کنید." },
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.width(22.dp))
            // Explicitly keep the poster last so it stays on the right in the Persian TV layout.
            Box(
                modifier = Modifier
                    .size(width = 112.dp, height = 168.dp)
                    .clip(RoundedCornerShape(12.dp))
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
                    Text(
                        "بدون پوستر",
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetaPill(text: String) {
    Text(
        text,
        color = TextSecondary,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.24f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

private fun sourceLabel(serviceId: String): String = when (serviceId) {
    "filmrooz" -> "فیلم‌روز"
    "mymoviz" -> "مای‌موویز"
    "parsiflix" -> "پارسی‌فلیکس"
    else -> serviceId
}

@Composable
private fun <T> FilterGroup(
    values: List<T>,
    selected: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) where T : Enum<T> {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.horizontalScroll(rememberScrollState())
    ) {
        values.forEach { value ->
            val label = when (value) {
                is LibraryTypeFilter -> value.label
                is LibraryLanguageFilter -> value.label
                is LibrarySort -> value.label
                else -> value.name
            }
            FocusableCard(
                shape = RoundedCornerShape(50),
                onClick = { onSelected(value) }
            ) {
                Text(
                    label,
                    color = if (value == selected) CinemaRed else TextSecondary,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                )
            }
        }
    }
}

@Composable
private fun FilterGroup(
    values: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.horizontalScroll(rememberScrollState())
    ) {
        values.forEach { value ->
            FocusableCard(
                shape = RoundedCornerShape(50),
                onClick = { onSelected(value) }
            ) {
                Text(
                    value.toPersianMetadataLabel(),
                    color = if (value == selected) CinemaRed else TextSecondary,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                )
            }
        }
    }
}

private const val ALL_GENRES = "همه ژانرها"
private const val MAX_GENRE_FILTERS = 12
private const val PAGE_SIZE = 24
private const val LOAD_AHEAD = 5
private const val DEFAULT_PAGE_LIMIT = 4
private const val PAGE_STEP = 4
private const val MAX_PAGE_LIMIT = 200
