package com.amin.tvos.ui.home

import android.content.Intent
import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.amin.tvos.AminTvApp
import com.amin.tvos.browser.GenreArchiveLoader
import com.amin.tvos.data.*
import com.amin.tvos.data.model.*
import com.amin.tvos.ui.components.CatalogCard
import com.amin.tvos.ui.components.FocusableCard
import com.amin.tvos.ui.spotlight.SpotlightActivity
import com.amin.tvos.ui.theme.*
import kotlinx.coroutines.*
import kotlin.coroutines.resume

/** Explicit archive entry; no repository or WebView is initialized during Home startup. */
class GenreArchiveActivity : ComponentActivity() {
    private val app get() = application as AminTvApp
    private lateinit var root: FrameLayout
    private lateinit var repository: GenreArchiveRepository
    private var service: StreamingService? = null
    private var bootstrap: Job? = null
    private var loadJob: Job? = null
    private var loader: GenreArchiveLoader? = null
    private var generation = 0
    private var ready by mutableStateOf(false)
    private var query by mutableStateOf(GenreArchiveQuery())
    private var number by mutableIntStateOf(1)
    private var page by mutableStateOf<GenreArchivePage?>(null)
    private var loading by mutableStateOf(false)
    private var error by mutableStateOf("")
    private var cachedMode by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val genre = ArchiveGenre.entries.firstOrNull { it.name == intent.getStringExtra("archiveGenre") }
            ?: ArchiveGenre.ROMANCE
        query = query.copy(genre = genre)
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        }
        repository = GenreArchiveRepository(this)
        root = FrameLayout(this)
        root.addView(ComposeView(this).apply { setContent {
            AminTvTheme {
                val holder = rememberSaveableStateHolder()
                Surface(color = Ink, modifier = Modifier.fillMaxSize()) {
                    if (cachedMode) holder.SaveableStateProvider("cached") {
                        val sections by app.catalogRepository.sections.collectAsState()
                        val metadata by app.catalogRepository.titleMetadata.collectAsState()
                        CatalogLibraryScreen(
                            title = "${query.genre.category.label} · ذخیره‌شدهٔ همهٔ منابع",
                            itemsProvider = { GenreDiscovery.select(query.genre.category, sections, metadata) },
                            catalogRevision = sections, refreshingServices = emptySet(), providerIds = emptySet(),
                            onRefresh = {}, onLoadMore = { _, done -> done() },
                            onBack = { cachedMode = false }, onOpen = ::openTitle)
                    } else holder.SaveableStateProvider("archive") { ArchiveContent() }
                }
            }
        } }, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (cachedMode) cachedMode = false else finish()
            }
        })
    }

    override fun onStart() {
        super.onStart()
        if (!ready && bootstrap?.isActive != true) bootstrap = lifecycleScope.launch {
            try {
                repository.load()
                app.servicesRepository.load()
                service = app.servicesRepository.services.value.firstOrNull { it.id == "mymoviz" }
                ready = true
                request(query, number)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { error = "آماده‌سازی آرشیو ناموفق بود؛ داده‌های قبلی حفظ شده‌اند." }
        }
    }

    private fun stopRequest() {
        generation++
        loadJob?.cancel()
        loader?.let { root.removeView(it.view); it.destroy() }
        loader = null
        if (loading) error = "دریافت متوقف شد؛ برای ادامه دوباره تلاش کن."
        loading = false
    }

    private fun request(target: GenreArchiveQuery, targetNumber: Int, force: Boolean = false) {
        stopRequest()
        query = target
        number = targetNumber
        error = ""
        val source = service
        page = repository.find(target, targetNumber)?.takeIf {
            source != null && GenreArchivePolicy.belongsToOrigin(it, source.url)
        }
        if (source == null) { error = "مای‌موویز در سرویس‌ها فعال نیست؛ از آرشیو ذخیره‌شده استفاده کن."; return }
        if (!force && page?.let { GenreArchivePolicy.fresh(it, System.currentTimeMillis()) } == true) return
        loading = true
        val token = generation
        loadJob = lifecycleScope.launch {
            var current: GenreArchiveLoader? = null
            try {
                val result = suspendCancellableCoroutine<GenreArchivePage> { continuation ->
                    val engine = GenreArchiveLoader(this@GenreArchiveActivity, source, target, targetNumber,
                        onPage = { if (continuation.isActive) continuation.resume(it) },
                        onError = { if (continuation.isActive) continuation.resumeWith(Result.failure(IllegalStateException(it))) })
                    current = engine
                    loader = engine
                    root.addView(engine.view, FrameLayout.LayoutParams(2, 2))
                    engine.start()
                }
                if (generation != token) return@launch
                page = result
                runCatching { repository.save(result) }.onFailure {
                    if (it is CancellationException) throw it
                    error = "صفحه نمایش داده شد، ولی ذخیرهٔ کش موفق نبود."
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { if (generation == token) error = failure.message ?: "دریافت صفحه ناموفق بود." }
            finally {
                current?.let { root.removeView(it.view); it.destroy() }
                if (generation == token) { loader = null; loading = false }
            }
        }
    }

    private fun openTitle(item: CatalogItem) {
        lifecycleScope.launch {
            val services = app.servicesRepository.services.value
            val media = withContext(Dispatchers.Default) {
                CanonicalLibrary.canonicalize(listOf(item), app.catalogRepository.titleMetadata.value,
                    services.associate { it.id to it.name }).first()
            }
            val title = media.representative
            startActivity(SpotlightActivity.intent(this@GenreArchiveActivity, SpotlightItem(
                title = title.title, kind = title.kind, contentUrl = title.contentUrl,
                posterUrl = title.posterUrl, backdropUrl = title.backdropUrl, serviceId = title.serviceId,
                year = title.year, rating = title.rating, summary = title.summary, genres = title.genres,
                hasPersianDub = title.hasPersianDub, hasPersianSubtitle = title.hasPersianSubtitle,
                primaryAction = title.kind.defaultSpotlightAction(), canonicalId = media.canonicalId,
                sourceVariants = media.variants, directPlay = title.kind == CatalogKind.MOVIE &&
                    services.firstOrNull { it.id == title.serviceId }?.directPlay != null)))
        }
    }

    @Composable private fun ArchiveContent() {
        // LazyGrid's content lambda may be evaluated after the page state is cleared.
        // Capture the immutable window now; never dereference mutable page state inside it.
        val displayedPage = page
        val displayedItems = displayedPage?.items.orEmpty()
        val grid = rememberLazyGridState()
        LaunchedEffect(query, number) { grid.scrollToItem(0) }
        Column(Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Chip("بازگشت", onClick = { finish() })
                Text("${query.genre.category.label} · آرشیو مای‌موویز", color = TextPrimary, style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f))
                Chip("ذخیره‌شدهٔ همه منابع", onClick = { stopRequest(); cachedMode = true })
                Chip("بروزرسانی", enabled = ready && !loading, onClick = { request(query, number, true) })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ArchiveType.entries.forEach { type -> Chip(type.label, query.type == type, ready,
                    onClick = { if (query.type != type) request(query.copy(type = type), 1) }) }
                Spacer(Modifier.width(18.dp))
                ArchiveSort.entries.forEach { sort -> Chip(sort.label, query.sort == sort, ready,
                    onClick = { if (query.sort != sort) request(query.copy(sort = sort), 1) }) }
            }
            Text("صفحه $number · ${displayedItems.size} عنوان دریافت‌شده" +
                (displayedPage?.totalCount?.let { " · $it نتیجه طبق سایت" } ?: "") +
                " · هر صفحه جدا دریافت می‌شود", color = TextSecondary)
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = CinemaRed)
            if (error.isNotBlank()) Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(error, color = TextSecondary, modifier = Modifier.weight(1f))
                Chip("تلاش دوباره", enabled = ready && !loading, onClick = { request(query, number, true) })
            }
            if (displayedItems.isEmpty()) Box(Modifier.weight(1f).fillMaxWidth()) {
                Text(if (loading || !ready) "دریافت صفحهٔ نتیجه…" else if (error.isBlank())
                    "سایت برای این فیلتر نتیجه‌ای اعلام نکرد." else "آرشیو ذخیره‌شده همچنان قابل دسترسی است.",
                    color = TextSecondary, modifier = Modifier.padding(24.dp))
            } else LazyVerticalGrid(columns = GridCells.Adaptive(128.dp), state = grid,
                horizontalArrangement = Arrangement.spacedBy(20.dp), verticalArrangement = Arrangement.spacedBy(22.dp),
                contentPadding = PaddingValues(10.dp), modifier = Modifier.weight(1f)) {
                items(displayedItems, key = { it.contentUrl }) { item ->
                    CatalogCard(item, onClick = { openTitle(item) }, posterHeight = 166.dp,
                        footerLabel = listOf(item.year, item.rating.takeIf { it.isNotBlank() }?.let { "IMDb $it" }.orEmpty())
                            .filter(String::isNotBlank).joinToString(" · "))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Chip("صفحه قبل", enabled = ready && !loading && number > 1,
                    onClick = { request(query, number - 1) })
                Chip("صفحه بعد", enabled = ready && !loading && page?.nextUrl?.isNotBlank() == true,
                    onClick = { request(query, number + 1) })
                Text("امتیاز و ترتیب از خود سایت است؛ تعداد رأی و صحت امتیاز سایت تضمین نمی‌شود.",
                    color = TextSecondary, modifier = Modifier.padding(10.dp))
            }
        }
    }

    @Composable private fun Chip(label: String, chosen: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
        FocusableCard(enabled = enabled, onClick = onClick, modifier = Modifier.semantics { selected = chosen }) {
            Text(label, color = if (chosen) CinemaRed else TextPrimary, modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp))
        }
    }

    override fun onStop() {
        stopRequest()
        if (!ready) bootstrap?.cancel()
        super.onStop()
    }
    override fun onDestroy() { stopRequest(); super.onDestroy() }

    companion object {
        fun intent(context: android.content.Context, category: DiscoveryCategory): Intent {
            val genre = requireNotNull(ArchiveGenre.forCategory(category))
            return Intent(context, GenreArchiveActivity::class.java).putExtra("archiveGenre", genre.name)
        }
    }
}
