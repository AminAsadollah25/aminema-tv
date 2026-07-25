package com.amin.tvos.ui.search

import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.amin.tvos.AminTvApp
import com.amin.tvos.browser.BrowserActivity
import com.amin.tvos.browser.SiteSearchEngine
import com.amin.tvos.data.model.CatalogItem
import com.amin.tvos.data.model.CatalogKind
import com.amin.tvos.data.model.SearchGroup
import com.amin.tvos.data.model.SearchResult
import com.amin.tvos.ui.components.FocusableCard
import com.amin.tvos.ui.components.CatalogCard
import com.amin.tvos.ui.theme.AminTvTheme
import com.amin.tvos.ui.theme.CinemaRed
import com.amin.tvos.ui.theme.Ink
import com.amin.tvos.ui.theme.TextPrimary
import com.amin.tvos.ui.theme.TextSecondary
import kotlinx.coroutines.launch

/**
 * Unified search across the user's own services.
 *
 * One query is sent to each website's own search and the hits are shown in two groups.
 * Clicking a hit opens only that title's normal detail page.
 */
class SearchActivity : ComponentActivity() {

    private var query by mutableStateOf("")
    private var persianLayout by mutableStateOf(true)
    private var keyboardVisible by mutableStateOf(true)
    private var iranian by mutableStateOf(SearchGroup(IRANIAN_ID))
    private var international by mutableStateOf(SearchGroup(INTERNATIONAL_ID))

    private lateinit var engines: Map<String, SiteSearchEngine>
    private val app get() = application as AminTvApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUi()

        val root = FrameLayout(this)
        // Each service gets its own engine so a slow site never delays the other.
        engines = listOf(IRANIAN_ID, INTERNATIONAL_ID).associateWith { serviceId ->
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

        lifecycleScope.launch { app.servicesRepository.load() }
    }

    private fun applyResults(serviceId: String, results: List<SearchResult>) {
        val group = SearchGroup(serviceId, results, loading = false, searched = true)
        if (serviceId == IRANIAN_ID) iranian = group else international = group
    }

    private fun applyFailure(serviceId: String, reason: String) {
        val group = SearchGroup(serviceId, emptyList(), false, reason, searched = true)
        if (serviceId == IRANIAN_ID) iranian = group else international = group
    }

    private fun runSearch() {
        val trimmed = query.trim()
        if (trimmed.length < 2) return
        keyboardVisible = false
        listOf(IRANIAN_ID, INTERNATIONAL_ID).forEach { serviceId ->
            val service = app.servicesRepository.findById(serviceId)
            if (service == null) {
                applyFailure(serviceId, "سرویس تنظیم نشده است")
                return@forEach
            }
            val group = SearchGroup(serviceId, loading = true, searched = true)
            if (serviceId == IRANIAN_ID) iranian = group else international = group
            engines[serviceId]?.search(service, trimmed)
        }
    }

    private fun open(result: SearchResult) {
        startActivity(
            BrowserActivity.intent(
                context = this,
                serviceId = result.serviceId,
                url = result.contentUrl,
                contentUrl = result.contentUrl,
                contentTitle = result.title,
                contentPoster = result.posterUrl,
                directPlay = result.kind == CatalogKind.MOVIE
            )
        )
    }

    @Composable
    private fun SearchScreen() {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 48.dp, vertical = 28.dp)
        ) {
            Text("جستجو", style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                query.ifBlank { "عبارت را با کیبورد پایین بنویسید" },
                style = MaterialTheme.typography.headlineMedium,
                color = if (query.isBlank()) TextSecondary else CinemaRed
            )
            Spacer(Modifier.height(16.dp))

            // The keyboard owns the screen until there is something to show, then folds
            // away so the results get the full 1080p height.
            if (keyboardVisible) {
                SearchKeyboard(
                    persian = persianLayout,
                    onKey = { key -> if (query.length < 60) query += key },
                    onBackspace = { query = query.dropLast(1) },
                    onClear = { query = "" },
                    onToggleLanguage = { persianLayout = !persianLayout },
                    onSubmit = { runSearch() }
                )
            } else {
                FocusableCard(
                    shape = RoundedCornerShape(50),
                    onClick = { keyboardVisible = true }
                ) {
                    Text(
                        "ویرایش عبارت",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            ResultGroup("نتایج ایرانی", iranian)
            Spacer(Modifier.height(20.dp))
            ResultGroup("نتایج خارجی", international)
            Spacer(Modifier.height(32.dp))
        }
    }

    @Composable
    private fun ResultGroup(title: String, group: SearchGroup) {
        Column(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.width(16.dp))
                if (group.loading) {
                    CircularProgressIndicator(
                        color = CinemaRed,
                        modifier = Modifier.height(22.dp).width(22.dp)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            when {
                group.error.isNotBlank() -> Text(
                    "این بخش جستجو نشد: ${group.error}",
                    color = CinemaRed,
                    style = MaterialTheme.typography.bodyMedium
                )

                group.loading -> Text(
                    "در حال جستجو…",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyLarge
                )

                !group.searched -> Text(
                    "هنوز جستجویی انجام نشده است.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyLarge
                )

                group.results.isEmpty() -> Text(
                    emptyMessage(group.serviceId),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyLarge
                )

                else -> Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    group.results.forEach { result ->
                        CatalogCard(
                            item = CatalogItem(
                                title = result.title,
                                kind = result.kind,
                                contentUrl = result.contentUrl,
                                posterUrl = result.posterUrl,
                                serviceId = result.serviceId
                            ),
                            onClick = { open(result) }
                        )
                    }
                }
            }
        }
    }

    /**
     * The Iranian service only indexes Persian titles, so a Latin query legitimately finds
     * nothing there. Say so instead of leaving an unexplained empty row.
     */
    private fun emptyMessage(serviceId: String): String =
        if (serviceId == IRANIAN_ID && query.trim().any { it in 'a'..'z' || it in 'A'..'Z' }) {
            "چیزی پیدا نشد. این بخش فقط با نام فارسی جستجو می‌کند."
        } else {
            "چیزی پیدا نشد."
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
    }
}
