package com.amin.tvos.ui.home

import android.content.Intent
import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.amin.tvos.AminTvApp
import com.amin.tvos.browser.SiteSearchEngine
import com.amin.tvos.data.*
import com.amin.tvos.data.model.*
import com.amin.tvos.ui.components.FocusableCard
import com.amin.tvos.ui.spotlight.SpotlightActivity
import com.amin.tvos.ui.theme.*
import kotlinx.coroutines.*
import kotlin.coroutines.resume

/** Real site search on explicit collection entry. No catalog-wide/startup scanning. */
class MarathonDiscoveryActivity:ComponentActivity() {
    private lateinit var root:FrameLayout
    private val app get()=application as AminTvApp
    private var family by mutableStateOf<MarathonFamily?>(null)
    private var results by mutableStateOf<List<CanonicalMedia>>(emptyList())
    private var busy by mutableStateOf(false)
    private var status by mutableStateOf("آماده‌سازی مجموعه…")
    private var failures=0
    private var job:Job?=null
    private val engines=mutableListOf<SiteSearchEngine>()
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        root=FrameLayout(this)
        setContentView(root)
        root.addView(androidx.compose.ui.platform.ComposeView(this).apply { setContent {
            AminTvTheme { DiscoveryContent() }
        } },FrameLayout.LayoutParams(-1,-1))
        window.decorView.let { view ->
            androidx.core.view.WindowInsetsControllerCompat(window,view).apply {
                hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior=androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
        job=lifecycleScope.launch {
            try {
                family=MarathonAtlas.load(this@MarathonDiscoveryActivity).firstOrNull { it.id==intent.getStringExtra(FAMILY_ID) }
                if(family==null) { status="شناسهٔ مجموعه معتبر نیست."; return@launch }
                app.servicesRepository.load()
                app.marathonDiscoveryRepository.load()
                refreshRows()
                discover(false)
            } catch(error:CancellationException) { throw error }
            catch(error:Exception) { status="دریافت فهرست ناموفق بود؛ کش قبلی پاک نشده است." }
        }
    }
    private suspend fun refreshRows() {
        val current=family?:return
        val records=app.marathonDiscoveryRepository.records.value.filter { it.key.startsWith(MarathonDiscoveryRepository.prefix(current.id)) }
        val names=app.servicesRepository.services.value.associate { it.id to it.sourceLabel.ifBlank { it.name } }
        val metadata=app.catalogRepository.titleMetadata.value
        results=withContext(Dispatchers.Default) {
            val related=JourneyLookupPlanner.bestItems(records).filter { MarathonAtlas.related(current,it) }
            CanonicalLibrary.canonicalize(related,metadata,names).sortedWith(compareBy<CanonicalMedia> {
                publicMatchYear(it.representative.serviceId,it.representative.year,"")?:9999
            }.thenBy { it.representative.title })
        }
    }
    private suspend fun discover(force:Boolean) {
        val current=family?:return
        busy=true; failures=0
        try {
            val allowed=if("iran" in current.groups) setOf("farsiland") else JourneyLookupPlanner.providers
            val services=app.servicesRepository.services.value.filter { it.id in allowed }
            if(services.isEmpty()) { status="سرویس مناسب پیکربندی نشده است.";return }
            val queries=current.queries.map { it.replace(Regex("\\s(?:19|20)\\d{2}$"),"") }.distinct().take(3)
            coroutineScope {
                services.map { service -> async {
                    var consecutive=0
                    for(query in queries) {
                        ensureActive()
                        val key=MarathonDiscoveryRepository.key(current.id,service.id,query)
                        val cached=app.marathonDiscoveryRepository.records.value.firstOrNull { it.key==key }
                        if(!force && cached!=null && JourneyLookupPlanner.isFresh(cached,System.currentTimeMillis()))continue
                        status="${service.sourceLabel.ifBlank { service.name }} · $query"
                        val outcome=search(service,query)
                        val items=withContext(Dispatchers.Default) {
                            outcome.first.map(JourneyLookupPlanner::catalogItem).filter { MarathonAtlas.related(current,it) }
                        }
                        app.marathonDiscoveryRepository.save(JourneyLookupRecord(key,service.id,query,
                            System.currentTimeMillis(),outcome.second,items))
                        if(outcome.second.isNotBlank()) { failures+=1;consecutive+=1 } else consecutive=0
                        refreshRows()
                        if(consecutive>=2)break
                    }
                } }.awaitAll()
            }
            refreshRows()
            status=when {
                failures>0 -> "بعضی جستجوها ناموفق بودند؛ نتایج سالم و کش قبلی حفظ شده‌اند."
                results.isEmpty() -> "در این نوبتِ جستجو نتیجهٔ مرتبطی پیدا نشد؛ این به معنی نبود عنوان در سایت نیست."
                else -> "${results.size} عنوان مرتبط در نتایج و کش فعلی · مرتب‌شده بر اساس سالِ موجود"
            }
        } finally { busy=false }
    }
    private suspend fun search(service:StreamingService,query:String):Pair<List<SearchResult>,String> {
        var engine:SiteSearchEngine?=null
        return try {
            withTimeoutOrNull(20_000L) { suspendCancellableCoroutine { continuation ->
                val current=SiteSearchEngine(this@MarathonDiscoveryActivity,
                    onResults={_,items -> runOnUiThread { if(continuation.isActive)continuation.resume(items to "") } },
                    onFailure={_,_ -> runOnUiThread { if(continuation.isActive)continuation.resume(emptyList<SearchResult>() to "جستجو ناموفق") } },
                    metadataOnly=true)
                engine=current;engines+=current
                current.view.isFocusable=false;current.view.isFocusableInTouchMode=false
                root.addView(current.view,FrameLayout.LayoutParams(2,2));current.search(service,query)
            } } ?: (emptyList<SearchResult>() to "مهلت دریافت تمام شد")
        } finally { engine?.let { if(engines.remove(it)){root.removeView(it.view);it.destroy()} } }
    }
    private fun open(media:CanonicalMedia) {
        val item=media.representative
        startActivity(SpotlightActivity.intent(this,SpotlightItem(title=item.title,kind=item.kind,
            contentUrl=item.contentUrl,posterUrl=item.posterUrl,backdropUrl=item.backdropUrl,
            serviceId=item.serviceId,year=item.year,summary=item.summary,rating=item.rating,genres=item.genres,
            cast=item.cast,directors=item.directors,hasPersianDub=item.hasPersianDub,
            hasPersianSubtitle=item.hasPersianSubtitle,primaryAction=item.kind.defaultSpotlightAction(),
            directPlay=item.kind==CatalogKind.MOVIE && app.servicesRepository.findById(item.serviceId)?.directPlay!=null,
            canonicalId=media.canonicalId,sourceVariants=media.variants)))
    }
    @Composable private fun DiscoveryContent() {
        val metadata by app.catalogRepository.titleMetadata.collectAsState()
        val firstFocus=remember { FocusRequester() }
        LaunchedEffect(Unit) { withFrameNanos {};firstFocus.requestFocus() }
        LazyColumn(Modifier.fillMaxSize().background(Ink),contentPadding=PaddingValues(48.dp,24.dp),
            verticalArrangement=Arrangement.spacedBy(20.dp)) {
            item(key="back") { Row(horizontalArrangement=Arrangement.spacedBy(20.dp)) {
                FocusableCard(Modifier.focusRequester(firstFocus),onClick={finish()}) { Text("بازگشت به ماراتون‌ها",Modifier.padding(18.dp),color=TextPrimary) }
                FocusableCard(enabled=!busy,onClick={ job=lifecycleScope.launch {
                    try { discover(true) } catch(error:CancellationException){throw error}
                    catch(error:Exception){status="بروزرسانی ناموفق بود؛ داده‌های قبلی محفوظ‌اند."}
                } }) { Text("بررسی دوباره",Modifier.padding(18.dp),color=TextPrimary) }
            } }
            item(key="heading") { Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text(family?.title?:"کاوش مجموعه",color=TextPrimary,style=MaterialTheme.typography.headlineMedium)
                Text("کاوش خانواده در سایت‌ها؛ نه فهرست مرجعِ کامل و ممیزی‌شده. نتیجهٔ جستجو ممکن است همهٔ شاخه‌ها، نام‌های متفاوت یا فصل‌ها را پوشش ندهد.",
                    color=TextSecondary,style=MaterialTheme.typography.bodyMedium)
                Text(status,color=TextPrimary)
                if(busy)LinearProgressIndicator(Modifier.fillMaxWidth(),color=CinemaRed)
            } }
            if(results.isEmpty())item(key="empty-art") {
                JourneyArtwork(family?.title?:"ماراتون",family?.id?:"loading",family?.groups?.firstOrNull()?:"scifi",
                    Modifier.fillMaxWidth().height(170.dp))
            }
            items(results,key={it.canonicalId}) { media -> JourneyTitleRow(media.representative.title,
                listOf(media.representative.kind.name.let { if(it=="SERIES")"سریال" else "فیلم" },media.representative.year)
                    .filter { it.isNotBlank() }.joinToString(" · "),media.canonicalId,
                family?.groups?.firstOrNull()?:"scifi",media,metadata) { open(media) } }
        }
    }
    override fun onStop() { super.onStop();if(busy){job?.cancel();status="دریافت متوقف شد؛ برای ادامه بررسی دوباره را بزن."} }
    override fun onDestroy() { job?.cancel();engines.toList().forEach {root.removeView(it.view);it.destroy()};engines.clear();super.onDestroy() }
    companion object { const val FAMILY_ID="family_id" }
}
