package com.amin.tvos.ui.search

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.amin.tvos.AminTvApp
import com.amin.tvos.MainActivity
import com.amin.tvos.browser.SiteSearchEngine
import com.amin.tvos.data.CanonicalLibrary
import com.amin.tvos.data.ContentMetadataPolicy
import com.amin.tvos.data.model.CanonicalMedia
import com.amin.tvos.data.model.CatalogKind
import com.amin.tvos.data.model.SearchGroup
import com.amin.tvos.data.model.SearchResult
import com.amin.tvos.data.model.SpotlightItem
import com.amin.tvos.data.model.defaultSpotlightAction
import com.amin.tvos.ui.components.FocusableCard
import com.amin.tvos.ui.components.RailNavigationControls
import com.amin.tvos.ui.components.CatalogCard
import com.amin.tvos.ui.theme.AminTvTheme
import com.amin.tvos.ui.theme.CinemaRed
import com.amin.tvos.ui.theme.Ink
import com.amin.tvos.ui.theme.SurfaceDark
import com.amin.tvos.ui.theme.TextPrimary
import com.amin.tvos.ui.theme.TextSecondary
import com.amin.tvos.ui.spotlight.SpotlightActivity
import kotlinx.coroutines.launch

/**
 * Unified search across the user's own services.
 *
 * One query is sent to each website's own search, then verified duplicates are collapsed into
 * one canonical card. Every provider page remains available from Spotlight as a source choice.
 */
class SearchActivity : ComponentActivity() {

    private var query by mutableStateOf("")
    private var persianLayout by mutableStateOf(true)
    private var keyboardVisible by mutableStateOf(true)
    private var groups by mutableStateOf(
        SEARCH_SERVICE_IDS.associateWith(::SearchGroup)
    )

    private lateinit var engines: Map<String, SiteSearchEngine>
    private val app get() = application as AminTvApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUi()

        val root = FrameLayout(this)
        // Each service gets its own engine so a slow site never delays the others.
        engines = SEARCH_SERVICE_IDS.associateWith { serviceId ->
            SiteSearchEngine(
                context = this,
                onResults = { id, results -> runOnUiThread { applyResults(id, results) } },
                onFailure = { id, reason -> runOnUiThread { applyFailure(id, reason) } }
            ).also { engine ->
                root.addView(engine.view, FrameLayout.LayoutParams(2, 2))
            }
        }

        val compose = ComposeView(this).apply {
            setContent {
                AminTvTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Ink,
                        contentColor = TextPrimary
                    ) { SearchScreen() }
                }
            }
        }
        root.addView(
            compose,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        setContentView(root)

        // Search may be restored by Android as the root of its task after process reclaim.
        // In that case a plain finish() returns to the TV launcher. Both remote Back and the
        // visible button therefore use the same guaranteed Home fallback.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = returnToHome()
        })

        lifecycleScope.launch {
            app.servicesRepository.load()
            app.catalogRepository.load()
        }
    }

    private fun returnToHome() {
        startActivity(
            Intent(this, MainActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        )
        finish()
    }

    private fun applyResults(serviceId: String, results: List<SearchResult>) {
        groups = groups + (
            serviceId to SearchGroup(serviceId, results, loading = false, searched = true)
            )
    }

    private fun applyFailure(serviceId: String, reason: String) {
        groups = groups + (
            serviceId to SearchGroup(serviceId, emptyList(), false, reason, searched = true)
            )
    }

    private fun runSearch() {
        val trimmed = query.trim()
        if (trimmed.length < 2) return
        keyboardVisible = false
        SEARCH_SERVICE_IDS.forEach { serviceId ->
            val service = app.servicesRepository.findById(serviceId)
            if (service == null) {
                applyFailure(serviceId, "سرویس تنظیم نشده است")
                return@forEach
            }
            groups = groups + (
                serviceId to SearchGroup(serviceId, loading = true, searched = true)
                )
            engines[serviceId]?.search(service, trimmed)
        }
    }

    private fun open(media: CanonicalMedia) {
        val representative = media.representative
        val selectedVariant = media.variants.firstOrNull {
            ContentMetadataPolicy.isSameTopLevelPage(
                it.item.contentUrl,
                representative.contentUrl
            )
        } ?: media.variants.first()
        startActivity(
            SpotlightActivity.intent(
                context = this,
                item = SpotlightItem(
                    title = representative.title,
                    kind = representative.kind,
                    contentUrl = selectedVariant.item.contentUrl,
                    posterUrl = representative.posterUrl,
                    backdropUrl = representative.backdropUrl,
                    serviceId = selectedVariant.providerId,
                    serviceName = selectedVariant.providerName,
                    summary = representative.summary,
                    year = representative.year,
                    genres = representative.genres,
                    rating = representative.rating,
                    runtime = representative.runtime,
                    country = representative.country,
                    language = representative.language,
                    hasPersianDub = representative.hasPersianDub,
                    hasPersianSubtitle = representative.hasPersianSubtitle,
                    directors = representative.directors,
                    cast = representative.cast,
                    primaryAction = representative.kind.defaultSpotlightAction(),
                    directPlay = representative.kind == CatalogKind.MOVIE &&
                        app.servicesRepository.findById(selectedVariant.providerId)?.directPlay != null,
                    canonicalId = media.canonicalId,
                    sourceVariants = media.variants
                )
            )
        )
    }

    @Composable
    private fun SearchScreen() {
        val titleMetadata by app.catalogRepository.titleMetadata.collectAsState()
        val providerNames = remember(groups) {
            SEARCH_SERVICE_IDS.associateWith { id ->
                app.servicesRepository.findById(id)?.let { service ->
                    service.sourceLabel.ifBlank { service.name }
                }.orEmpty()
            }
        }
        val canonicalResults = remember(groups, titleMetadata, providerNames) {
            CanonicalLibrary.fromSearchResults(
                results = SEARCH_SERVICE_IDS.flatMap { groups[it]?.results.orEmpty() },
                metadataByUrl = titleMetadata,
                providerNames = providerNames
            )
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 48.dp, vertical = 20.dp)
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FocusableCard(
                        shape = RoundedCornerShape(50),
                        onClick = { returnToHome() }
                    ) { focused ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(
                                    if (focused) Color.White.copy(alpha = 0.16f)
                                    else SurfaceDark,
                                    RoundedCornerShape(50)
                                )
                                .padding(horizontal = 17.dp, vertical = 11.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("بازگشت", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    Spacer(Modifier.width(18.dp))
                    Column {
                        Text("جستجو", style = MaterialTheme.typography.displayMedium)
                        Text(
                            "یک جستجو، همه آرشیوها",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // The keyboard owns the screen until there is something to show, then folds
            // away so the results get the full 1080p height.
            if (keyboardVisible) {
                SearchKeyboard(
                    query = query,
                    persian = persianLayout,
                    onKey = { key -> if (query.length < 60) query += key },
                    onBackspace = { query = query.dropLast(1) },
                    onClear = { query = "" },
                    onToggleLanguage = { persianLayout = !persianLayout },
                    onSubmit = { runSearch() }
                )
            } else {
                CollapsedSearchBar(query = query) { keyboardVisible = true }
            }

            if (groups.values.any { it.searched }) {
                Spacer(Modifier.height(24.dp))
                UnifiedResultGroup(canonicalResults)
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    @Composable
    private fun CollapsedSearchBar(query: String, onEdit: () -> Unit) {
        FocusableCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            onClick = onEdit
        ) { focused ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (focused) CinemaRed.copy(alpha = 0.10f) else SurfaceDark,
                        RoundedCornerShape(18.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    tint = CinemaRed
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    query,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1
                )
                Text(
                    "ویرایش جستجو",
                    color = if (focused) TextPrimary else TextSecondary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }

    @Composable
    private fun UnifiedResultGroup(results: List<CanonicalMedia>) {
        val scrollState = rememberScrollState()
        val loading = groups.values.any { it.loading }
        val errors = SEARCH_SERVICE_IDS.mapNotNull(groups::get)
            .filter { it.error.isNotBlank() }
            .joinToString("  •  ") { group ->
                val name = app.servicesRepository.findById(group.serviceId)?.name
                    .orEmpty().ifBlank { group.serviceId }
                "$name: ${group.error}"
            }
        Column(Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("نتایج همه آرشیوها", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.width(16.dp))
                if (loading) {
                    CircularProgressIndicator(
                        color = CinemaRed,
                        modifier = Modifier.height(22.dp).width(22.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                RailNavigationControls(scrollState)
            }
            Spacer(Modifier.height(10.dp))
            if (errors.isNotBlank()) {
                Text(
                    "یک آرشیو پاسخ نداد؛ نتیجه‌های آرشیو دیگر حفظ شد.  $errors",
                    color = CinemaRed,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
            }
            when {
                loading && results.isEmpty() -> Text(
                    "در حال جستجو…",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyLarge
                )

                results.isEmpty() -> Text(
                    "چیزی در آرشیوهای پاسخ‌داده پیدا نشد.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyLarge
                )

                else -> Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.horizontalScroll(scrollState)
                ) {
                    results.forEach { media ->
                        CatalogCard(
                            item = media.representative,
                            footerLabel = if (media.variants.size > 1) {
                                "${media.variants.size} منبع در دسترس"
                            } else {
                                media.variants.firstOrNull()?.providerName.orEmpty()
                            },
                            onClick = { open(media) }
                        )
                    }
                }
            }
        }
    }

    private fun hideSystemUi() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUi()
    }

    override fun onDestroy() {
        engines.values.forEach { it.destroy() }
        super.onDestroy()
    }

    private companion object {
        const val IRANIAN_ID = "parsiflix"
        const val INTERNATIONAL_ID = "filmrooz"
        const val MYMOVIZ_ID = "mymoviz"
        val SEARCH_SERVICE_IDS = listOf(IRANIAN_ID, INTERNATIONAL_ID, MYMOVIZ_ID)
    }
}
