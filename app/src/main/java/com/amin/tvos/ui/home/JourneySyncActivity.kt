package com.amin.tvos.ui.home

import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.amin.tvos.AminTvApp
import com.amin.tvos.browser.SiteSearchEngine
import com.amin.tvos.data.*
import com.amin.tvos.data.model.*
import com.amin.tvos.ui.components.FocusableCard
import com.amin.tvos.ui.theme.*
import com.amin.tvos.ui.spotlight.SpotlightMetadataLoader
import kotlinx.coroutines.*
import kotlin.coroutines.resume

/** Explicit, bounded metadata search. Cancelling retains every completed checkpoint. */
class JourneySyncActivity : ComponentActivity() {
    private var completed by mutableIntStateOf(0)
    private var total by mutableIntStateOf(0)
    private var failures by mutableIntStateOf(0)
    private var finished by mutableStateOf(false)
    private var status by mutableStateOf("آماده‌سازی فهرست مرجع…")
    private var fatal by mutableStateOf(false)
    private var job: Job? = null
    private lateinit var root: FrameLayout
    private val engines = mutableListOf<SiteSearchEngine>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        root = FrameLayout(this)
        root.addView(ComposeView(this).apply { setContent {
            AminTvTheme {
                Surface(color = Ink, modifier = Modifier.fillMaxSize()) {
                    Column(Modifier.padding(48.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        Text("تطبیق ماراتون و مارول", color = TextPrimary, style = MaterialTheme.typography.headlineMedium)
                        Text("جستجوی متادیتا در سایت‌های خودت؛ بدون پخش و بدون تغییر جدیدترین‌ها. نتیجهٔ هر جستجو ذخیره می‌شود؛ می‌توانی توقف کنی و بعد ادامه بدهی.", color = TextSecondary)
                        LinearProgressIndicator(progress = { if (total == 0) 0f else completed.toFloat() / total }, modifier = Modifier.fillMaxWidth())
                        Text("$completed از $total جستجو بررسی شده · $failures خطا", color = TextPrimary)
                        Text(status, color = TextPrimary)
                        if (finished || fatal) Text("نتیجه‌های پیدا‌نشده یا خطادار لینک حدسی ندارند. تطبیق فقط با عنوان، سال و نوع قطعی است.", color = TextSecondary)
                        FocusableCard(onClick = { job?.cancel(); finish() }) {
                            Text(if (finished || fatal) "بازگشت به فهرست‌ها" else "توقف و بازگشت", Modifier.padding(20.dp), color = TextPrimary)
                        }
                    }
                }
            }
        } }, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { job?.cancel(); finish() }
        })
        job = lifecycleScope.launch {
            try {
                val app = application as AminTvApp
                app.servicesRepository.load()
                val repository = app.journeyLookupRepository
                repository.load()
                val services = app.servicesRepository.services.value.filter { it.id in JourneyLookupPlanner.providers }
                val available = services.map { it.id }.toSet()
                val references = withContext(Dispatchers.Default) { JourneyCatalog.works.filter { it.release.take(4).toInt() <= java.time.LocalDate.now().year } }
                val tasks = JourneyLookupPlanner.tasks(references, available)
                total = tasks.size
                if (total == 0) { fatal = true; status = "سرویس خارجی فعالی برای جستجو نداریم."; return@launch }
                val fresh = repository.records.value.filter { JourneyLookupPlanner.isFresh(it, System.currentTimeMillis()) }.map { it.key }.toSet()
                completed = tasks.count { it.key in fresh }
                var stoppedProviders = 0
                coroutineScope {
                    services.map { service -> async {
                        var consecutiveFailures = 0
                        for (task in tasks.filter { it.serviceId == service.id && it.key !in fresh }) {
                            ensureActive()
                            status = "${service.name} · ${task.query}"
                            val outcome = search(service, task.query)
                            repository.save(JourneyLookupRecord(task.key, service.id, task.query,
                                System.currentTimeMillis(), outcome.second,
                                withContext(Dispatchers.Default) { JourneyLookupPlanner.relevantItems(outcome.first, references) }))
                            completed += 1
                            if (outcome.second.isNotBlank()) { failures += 1; consecutiveFailures += 1 }
                            else consecutiveFailures = 0
                            // Avoid hundreds of retries on a logged-out/unreachable provider.
                            if (consecutiveFailures >= 2) { stoppedProviders += 1; break }
                            yield()
                        }
                    } }.awaitAll()
                }
                status = "ساختن گزارش تطبیق…"
                app.catalogRepository.load()
                val lookupItems = withContext(Dispatchers.Default) { JourneyLookupPlanner.bestItems(repository.records.value) }
                suspend fun resolveAudit() = withContext(Dispatchers.Default) {
                    JourneyCatalog.resolve(references, app.catalogRepository.sections.value +
                        CatalogSection("journey-lookup", all = lookupItems),
                        app.catalogRepository.titleMetadata.value, services.associate { it.id to it.name })
                }
                val provisional = resolveAudit()
                val disputed = withContext(Dispatchers.Default) {
                    provisional.filter { match -> match.media == null || match.media.variants
                        .groupBy { it.providerId }.values.any { variants -> variants.size > 1 && variants.any { it.imdbId.isBlank() } }
                    }.flatMap { missing ->
                        val titles = (setOf(missing.work.title) + missing.work.aliases).map(CanonicalText::normalizeTitle).toSet()
                        lookupItems.filter { it.kind == missing.work.kind && CanonicalText.normalizeTitle(it.title) in titles }
                    }.distinctBy { it.contentUrl }.take(12)
                }
                for (candidate in disputed) {
                    status = "بررسی صفحهٔ واقعی · ${candidate.title}"
                    detailMetadata(candidate, app)?.let { app.catalogRepository.saveTitleMetadata(it) }
                }
                val audit = resolveAudit()
                repository.saveAudit(audit)
                finished = true
                val summary = "${audit.count { it.media != null }} از ${audit.size} رکورد مرجع به صفحهٔ عنوان متصل شد؛ موجودبودن تک‌تک فصل‌ها هنگام انتخاب قسمت بررسی می‌شود."
                status = if (stoppedProviders > 0 || failures > 0) "$summary جستجو موارد خطادار/بررسی‌نشده دارد؛ اجرای بعدی آن‌ها را دوباره بررسی می‌کند."
                    else "$summary جستجوی همهٔ عنوان‌های ثبت‌شده انجام شد."
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) { fatal = true; status = "ذخیره یا آماده‌سازی تطبیق ناموفق بود؛ داده‌های قبلی حفظ شده‌اند." }
        }
    }

    private suspend fun search(service: StreamingService, query: String): Pair<List<SearchResult>, String> {
        var engine: SiteSearchEngine? = null
        return try {
            withTimeoutOrNull(20_000L) {
                suspendCancellableCoroutine { continuation ->
                    val searchEngine = SiteSearchEngine(this@JourneySyncActivity,
                        onResults = { _, items -> runOnUiThread {
                            if (continuation.isActive) continuation.resume(items to "")
                        } }, onFailure = { _, _ -> runOnUiThread {
                            if (continuation.isActive) continuation.resume(emptyList<SearchResult>() to "جستجو ناموفق؛ اتصال یا ورود سایت را بررسی کن")
                        } }, metadataOnly = true, onStage = { stage ->
                            if (com.amin.tvos.BuildConfig.DEBUG) android.util.Log.d("JourneyLookup", "${service.id}:$stage")
                        })
                    engine = searchEngine
                    engines += searchEngine
                    searchEngine.view.isFocusable = false
                    searchEngine.view.isFocusableInTouchMode = false
                    root.addView(searchEngine.view, FrameLayout.LayoutParams(2, 2))
                    searchEngine.search(service, query)
                }
            } ?: (emptyList<SearchResult>() to "مهلت جستجو تمام شد")
        } finally {
            engine?.let { if (engines.remove(it)) { root.removeView(it.view); it.destroy() } }
        }
    }

    private suspend fun detailMetadata(item: CatalogItem, app: AminTvApp): TitleMetadata? {
        var loader: SpotlightMetadataLoader? = null
        return try {
            withTimeoutOrNull(20_000) { suspendCancellableCoroutine { continuation ->
                val current = SpotlightMetadataLoader(this@JourneySyncActivity, app,
                    onLoaded = { if (continuation.isActive) continuation.resume(it) },
                    onFailed = { if (continuation.isActive) continuation.resume(null) })
                loader = current
                current.load(SpotlightItem(item.title, item.kind, item.contentUrl, serviceId = item.serviceId))
            } }
        } finally { loader?.destroy() }
    }

    override fun onStop() {
        super.onStop()
        // No hidden page activity while this explicit scan isn't visible.
        if (!finished) job?.cancel()
    }
    override fun onResume() {
        super.onResume()
        if (job?.isCancelled == true) {
            fatal = true
            status = "تطبیق متوقف شد؛ نتیجه‌های قبلی ذخیره‌اند. برگرد و برای ادامه دوباره تطبیق را اجرا کن."
        }
    }
    override fun onDestroy() {
        job?.cancel()
        engines.toList().forEach { root.removeView(it.view); it.destroy() }
        engines.clear()
        super.onDestroy()
    }
}
