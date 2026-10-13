package com.amin.tvos.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.amin.tvos.R
import com.amin.tvos.data.*
import com.amin.tvos.data.model.*
import com.amin.tvos.ui.components.FocusableCard
import com.amin.tvos.ui.components.SectionRow
import com.amin.tvos.ui.components.authenticatedPosterModel
import com.amin.tvos.ui.theme.*
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
    lookupItems: List<CatalogItem> = emptyList(), onMatchAll: () -> Unit = {}
) {
    val collections = if (marvel) JourneyCatalog.marvelCollections else JourneyCatalog.marathonCollections
    var selected by rememberSaveable(marvel) { mutableStateOf(collections.first().id) }
    var formatName by rememberSaveable(marvel) { mutableStateOf("") }
    var matchedOnly by rememberSaveable(marvel) { mutableStateOf(false) }
    var missingTitle by remember { mutableStateOf<String?>(null) }
    val initialFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { withFrameNanos { }; initialFocus.requestFocus() }
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
    BackHandler(onBack = onBack)
    LazyColumn(Modifier.fillMaxSize().background(Ink),
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item(key = "back") {
            FocusableCard(Modifier.focusRequester(initialFocus), onClick = onBack) {
                Text("بازگشت به خانه", Modifier.padding(horizontal = 24.dp, vertical = 14.dp), color = TextPrimary)
            }
        }
        item(key = "hero") { JourneyBanner(marvel) }
        item(key = "match-all") {
            FocusableCard(onClick = onMatchAll) {
                Text("تطبیق همهٔ فهرست‌ها با سایت‌ها", Modifier.padding(horizontal = 24.dp, vertical = 16.dp), color = TextPrimary)
            }
        }
        item(key = "coverage") {
            Text("فهرست مرجع در حال تکمیل است؛ نبود تطبیق، به معنی نبود عنوان در سایت نیست. ترتیب فصل‌ها بر اساس شروع فصل است، نه تک‌تک قسمت‌ها.",
                color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        }
        item(key = "collections") {
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
        item(key = "formats") {
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
            FocusableCard(onClick = { matchedOnly = !matchedOnly }) {
                Text((if (matchedOnly) "✓ " else "") + "فقط عنوان‌های تطبیق‌یافته",
                    Modifier.padding(horizontal = 18.dp, vertical = 14.dp), color = TextPrimary)
            }
            Text(if (result == null) "در حال تطبیق با آرشیو…" else
                "${matches.size} اثر/فصل در مرجع · ${matches.count { it.media != null }} تطبیق در آرشیو ذخیره‌شده" +
                    if (!catalogReady) " · بارگذاری کش ادامه دارد" else "",
                color = TextSecondary)
            }
        }
        itemsIndexed(visible, key = { _, match -> match.work.id }) { index, match ->
            val work = match.work
            FocusableCard(onClick = {
                match.media?.let(onOpen) ?: run { missingTitle = work.title }
            }) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    match.media?.representative?.let { item ->
                        if (item.posterUrl.isNotBlank()) AsyncImage(
                            model = authenticatedPosterModel(item.posterUrl, item.contentUrl, 160, 240),
                            contentDescription = null, contentScale = ContentScale.Fit,
                            modifier = Modifier.width(64.dp).height(96.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${index + 1}. ${work.title}", color = TextPrimary, style = MaterialTheme.typography.titleLarge)
                        Text("${work.format.label}" + (if (work.season > 0) " · فصل ${work.season}" else ""),
                            color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        Text(work.dateLabel, color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Ltr))
                    }
                    Text(if (match.media == null) "هنوز تطبیق ندارد" else if (work.season > 0) "اطلاعات و انتخاب فصل" else "اطلاعات و پخش",
                        color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                }
            }
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
