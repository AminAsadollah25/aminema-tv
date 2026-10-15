package com.amin.tvos.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.amin.tvos.R
import com.amin.tvos.data.*
import com.amin.tvos.data.model.*
import com.amin.tvos.ui.components.FocusableCard
import com.amin.tvos.ui.components.SectionRow
import com.amin.tvos.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Native, cache-only destinations. No per-card browser or startup reference parsing. */
@Composable
fun JourneyEntryRow(onOpen: (Boolean) -> Unit, onFocused: () -> Unit) {
    SectionRow("یک سفر سینمایی شروع کن", listOf(false, true), key = { it }, showNavigation = false) { marvel ->
        FocusableCard(Modifier.width(408.dp), onClick = { onOpen(marvel) },
            onInteractionFocusChanged = { if (it) onFocused() }) {
            JourneyBanner(marvel, compact = true)
        }
    }
}

@Composable
private fun JourneyBanner(marvel: Boolean, compact: Boolean = false) {
    val context = LocalContext.current
    val height = if (compact) 176.dp else 214.dp
    Box(Modifier.fillMaxWidth().height(height).background(SurfaceElevated)) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(if (marvel) R.drawable.aminema_marvel_thor else R.drawable.aminema_mascot)
                .size(if (compact) 480 else 640).build(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.align(Alignment.CenterEnd).width(if (compact) 188.dp else 264.dp).fillMaxHeight()
        )
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Ink, Ink.copy(alpha = .92f), Ink.copy(alpha = .05f)))))
        Column(Modifier.align(Alignment.CenterStart).padding(24.dp).fillMaxWidth(if (compact) .60f else .68f),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(if (marvel) "MARVEL • AMINEMA" else "CINEMA JOURNEYS", color = TextSecondary,
                style = MaterialTheme.typography.labelMedium)
            Text(if (marvel) "سفر در دنیای مارول" else "ماراتون سینمایی",
                color = TextPrimary, style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium)
            Text(if (marvel) "جهان‌ها و قهرمانان · به ترتیب انتشار" else "یک مجموعه، یک مسیر · فیلم و فصل کنار هم",
                color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun JourneyScreen(
    marvel: Boolean, sections: List<CatalogSection>, catalogReady: Boolean,
    metadata: Map<String, TitleMetadata>, providerNames: Map<String, String>,
    onBack: () -> Unit, onOpen: (CanonicalMedia) -> Unit,
    lookupItems: List<CatalogItem> = emptyList(), onMatchAll: () -> Unit = {},
    onDiscoverFamily: (String) -> Unit = {}
) {
    val collections = if (marvel) JourneyCatalog.marvelCollections else
        JourneyCatalog.marathonCollections + JourneyCatalog.marvelCollections
    var selected by rememberSaveable(marvel) { mutableStateOf(if (marvel) collections.first().id else "") }
    var lastOpened by rememberSaveable { mutableStateOf("") }
    val collectionGridState = rememberLazyGridState()
    val detailListState = rememberLazyListState()
    var formatName by rememberSaveable(marvel) { mutableStateOf("") }
    var matchedOnly by rememberSaveable(marvel) { mutableStateOf(false) }
    var missingTitle by remember { mutableStateOf<String?>(null) }
    // Keep metadata alive while the detail list is shown. Recreating an empty grid
    // on Back would clamp its saved scroll position to the two header items.
    val marathonCards by produceState<List<MarathonCardInfo>>(emptyList(), marvel) {
        if (!marvel) value = withContext(Dispatchers.Default) {
            collections.map { collection ->
                val works = JourneyCatalog.select(collection.id)
                MarathonCardInfo(collection, works.size,
                    listOfNotNull(works.firstOrNull()?.release?.take(4), works.lastOrNull()?.release?.take(4))
                        .distinct().joinToString("–"))
            }
        }
    }
    val context = LocalContext.current
    var atlasError by remember { mutableStateOf("") }
    val atlasRepo = (context.applicationContext as com.amin.tvos.AminTvApp).marathonDiscoveryRepository
    val discoveryRecords by atlasRepo.records.collectAsState()
    val families by produceState<List<MarathonFamily>>(emptyList(), marvel) {
        if (!marvel) {
            runCatching { MarathonAtlas.load(context) }.onSuccess { value = it }
                .onFailure { atlasError = "خواندن اطلس ناموفق بود؛ فایل‌های قبلی حفظ شده‌اند." }
            runCatching { atlasRepo.load() }.onFailure { atlasError = "کش کاوش خوانده نشد؛ اطلس محلی در دسترس است." }
        }
    }
    var groupId by rememberSaveable { mutableStateOf("") }
    var atlasQuery by rememberSaveable { mutableStateOf("") }
    val gridScope = rememberCoroutineScope()
    if (!marvel && selected.isEmpty()) {
        MarathonAtlasGrid(families, collectionGridState, groupId, lastOpened, atlasQuery,
            onQuery={ atlasQuery=it;lastOpened="";gridScope.launch {collectionGridState.scrollToItem(0)} },
            marathonCards.associate { it.collection.id to it.count }, discoveryRecords, atlasError,
            onGroup = { groupId = it; lastOpened = ""; gridScope.launch { collectionGridState.scrollToItem(0) } },
            onBack = onBack,
            onSelectReference = { family -> lastOpened = family.id; formatName = ""; matchedOnly = false; selected = family.referenceId },
            onDiscover = { id -> lastOpened = id; onDiscoverFamily(id) })
        return
    }
    val returnToCollections: () -> Unit = { if (marvel) onBack() else selected = "" }
    val initialFocus = remember { FocusRequester() }
    LaunchedEffect(selected) { detailListState.scrollToItem(0); withFrameNanos { }; initialFocus.requestFocus() }
    val format = JourneyFormat.entries.firstOrNull { it.name == formatName }
    // Structural key avoids retaining the previous collection while an async resolve is pending.
    val result by produceState<List<JourneyMatch>?>(null, selected, formatName, sections, metadata, catalogReady, lookupItems) {
        value = null
        value = withContext(Dispatchers.Default) {
            JourneyCatalog.resolve(JourneyCatalog.select(selected, format),
                sections + CatalogSection("journey-lookup", all = lookupItems), metadata, providerNames)
        }
    }
    val matches = result.orEmpty()
    val visible = remember(matches, matchedOnly) { if (matchedOnly) matches.filter { it.media != null } else matches }
    BackHandler(onBack = returnToCollections)
    LazyColumn(Modifier.fillMaxSize().background(Ink), state = detailListState,
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item(key = "back") {
            FocusableCard(Modifier.focusRequester(initialFocus), onClick = returnToCollections) {
                Text(if (marvel) "بازگشت به خانه" else "بازگشت به ماراتون‌ها", Modifier.padding(horizontal = 24.dp, vertical = 14.dp), color = TextPrimary)
            }
        }
        item(key = "hero") {
            if (marvel) JourneyBanner(true) else Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(collections.first { it.id == selected }.title, color = TextPrimary,
                    style = MaterialTheme.typography.headlineMedium)
                Text(collections.first { it.id == selected }.description, color = TextSecondary)
            }
        }
        if (marvel) item(key = "match-all") {
            FocusableCard(onClick = onMatchAll) {
                Text("تطبیق همهٔ فهرست‌ها با سایت‌ها", Modifier.padding(horizontal = 24.dp, vertical = 16.dp), color = TextPrimary)
            }
        }
        if (marvel) item(key = "coverage") {
            Text("فهرست مرجع در حال تکمیل است؛ نبود تطبیق، به معنی نبود عنوان در سایت نیست. ترتیب فصل‌ها بر اساس شروع فصل است، نه تک‌تک قسمت‌ها.",
                color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        }
        if (marvel) item(key = "collections") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(6.dp)) {
                items(collections, key = { it.id }) { collection ->
                    FocusableCard(Modifier.width(256.dp), onClick = { selected = collection.id }) {
                        Column(Modifier.background(if (selected == collection.id) CinemaRed.copy(alpha = .24f) else SurfaceElevated)
                            .padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text((if (selected == collection.id) "✓ " else "") + collection.title,
                                color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                            Text(collection.description, color = TextSecondary, style = MaterialTheme.typography.bodyMedium,
                                maxLines = 3, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        if (marvel || selected in setOf("wick", "starwars")) item(key = "formats") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(6.dp)) {
                items(listOf("") + JourneyFormat.entries.map { it.name }, key = { it }) { name ->
                    FocusableCard(onClick = { formatName = name }) {
                        Text((if (formatName == name) "✓ " else "") +
                            (JourneyFormat.entries.firstOrNull { it.name == name }?.label ?: "همه"),
                            Modifier.padding(horizontal = 18.dp, vertical = 14.dp), color = TextPrimary)
                    }
                }
            }
        }
        item(key = "count") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FocusableCard(onClick = { matchedOnly = !matchedOnly }) {
                Text((if (matchedOnly) "✓ " else "") + "فقط عنوان‌های تطبیق‌یافته",
                    Modifier.padding(horizontal = 18.dp, vertical = 14.dp), color = TextPrimary)
            }
            if (!marvel) FocusableCard(onClick = onMatchAll) {
                Text("تطبیق همهٔ فهرست‌ها", Modifier.padding(horizontal = 18.dp, vertical = 14.dp), color = TextPrimary)
            }
            }
            Text(if (result == null) "در حال تطبیق با آرشیو…" else
                "${matches.size} اثر/فصل در مرجع · ${matches.count { it.media != null }} تطبیق در آرشیو ذخیره‌شده" +
                    if (!catalogReady) " · بارگذاری کش ادامه دارد" else "",
                color = TextSecondary)
            if (!marvel) Text("بدون تطبیق ≠ ناموجود در سایت · ترتیب انتشار، نه ترتیب داستان",
                color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
        itemsIndexed(visible, key = { _, match -> match.work.id }) { index, match ->
            val work = match.work
            JourneyTitleRow("${index + 1}. ${work.title}",
                work.format.label + (if (work.season > 0) " · فصل ${work.season}" else "") + " · " + work.dateLabel,
                work.id, if (marvel) "comic" else families.firstOrNull { it.referenceId == selected }?.groups?.firstOrNull() ?: "action",
                match.media, metadata) { match.media?.let(onOpen) ?: run { missingTitle = work.title } }
        }

        if (result != null && visible.isEmpty()) item(key = "empty") {
            Text(if (matchedOnly) "در کش فعلی تطبیق قطعی برای این فیلتر نداریم. فهرست کامل مرجع همچنان قابل نمایش است."
                else "در این فیلتر هنوز رکورد مرجع ثبت نشده است.", color = TextSecondary)
        }
    }
    missingTitle?.let { title ->
        AlertDialog(onDismissRequest = { missingTitle = null }, title = { Text(title) },
            text = { Text("هنوز تطبیق قطعی عنوان، سال و نوع در آرشیو ذخیره‌شده پیدا نشده است. ممکن است در سایت موجود باشد؛ از جستجوی فعلی اپ استفاده کن. این کارت لینک حدسی پخش ندارد.") },
            confirmButton = { TextButton(onClick = { missingTitle = null }) { Text("باشه") } })
    }
}

private data class MarathonCardInfo(val collection: JourneyCollection, val count: Int, val years: String)
