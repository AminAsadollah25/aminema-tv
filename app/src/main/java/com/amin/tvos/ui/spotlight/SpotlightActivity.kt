package com.amin.tvos.ui.spotlight

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amin.tvos.AminTvApp
import com.amin.tvos.browser.BrowserActivity
import com.amin.tvos.data.ContentMetadataPolicy
import com.amin.tvos.data.model.SpotlightItem
import com.amin.tvos.data.model.withMetadata
import com.amin.tvos.ui.theme.AminTvTheme
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Native, TV-first title page shared by Iranian and international movies/series.
 *
 * It is intentionally a separate Activity: Back restores the exact Home/Search rail and
 * scroll position, while Live TV remains on its existing one-click BrowserActivity path.
 */
class SpotlightActivity : ComponentActivity() {
    private var spotlightItem by mutableStateOf<SpotlightItem?>(null)
    private var metadataLoading by mutableStateOf(false)
    private var metadataLoader: SpotlightMetadataLoader? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUi()
        val decodedItem = intent.getStringExtra(EXTRA_ITEM)
            ?.let { encoded -> runCatching { json.decodeFromString<SpotlightItem>(encoded) }.getOrNull() }
        if (decodedItem == null) {
            finish()
            return
        }
        spotlightItem = decodedItem

        setContent {
            AminTvTheme {
                val spotlightViewModel: SpotlightViewModel = viewModel()
                val favoriteUrls by spotlightViewModel.favoriteUrls.collectAsState()
                val item = spotlightItem ?: return@AminTvTheme
                val isFavorite = ContentMetadataPolicy.canonicalContentUrl(item.contentUrl) in
                    favoriteUrls

                SpotlightScreen(
                    item = item,
                    isFavorite = isFavorite,
                    metadataLoading = metadataLoading,
                    onBack = ::finish,
                    onToggleFavorite = { spotlightViewModel.toggleFavorite(item) },
                    onWatch = { openBrowser(item) }
                )
            }
        }

        enrichMetadata(decodedItem)
    }

    private fun enrichMetadata(initial: SpotlightItem) {
        val app = application as AminTvApp
        lifecycleScope.launch {
            app.servicesRepository.load()
            app.catalogRepository.load()
            val cached = app.catalogRepository.metadataFor(initial.contentUrl)
            if (cached != null) {
                spotlightItem = (spotlightItem ?: initial).withMetadata(cached)
            }
            val cacheIsFresh = cached != null &&
                System.currentTimeMillis() - cached.fetchedAt < METADATA_MAX_AGE_MS
            if (cacheIsFresh || isFinishing || isDestroyed) return@launch

            metadataLoading = true
            metadataLoader = SpotlightMetadataLoader(
                activity = this@SpotlightActivity,
                app = app,
                onLoaded = { metadata ->
                    spotlightItem = (spotlightItem ?: initial).withMetadata(metadata)
                    metadataLoading = false
                    lifecycleScope.launch {
                        app.catalogRepository.saveTitleMetadata(metadata)
                    }
                },
                onFailed = {
                    metadataLoading = false
                }
            ).also { it.load(initial) }
        }
    }

    private fun openBrowser(item: SpotlightItem) {
        startActivity(
            BrowserActivity.intent(
                context = this,
                serviceId = item.serviceId,
                url = item.browserStartUrl.ifBlank { item.contentUrl },
                resumePosition = item.resumePosition,
                contentUrl = item.contentUrl,
                contentTitle = item.title,
                contentPoster = item.posterUrl,
                autoResume = item.autoResume,
                directPlay = item.directPlay,
                resumeStrategyOverride = item.resumeStrategy,
                actionButtonTextPatterns = item.actionButtonTextPatterns
            )
        )
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
        metadataLoader?.destroy()
        metadataLoader = null
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_ITEM = "spotlight_item"
        private const val METADATA_MAX_AGE_MS = 14L * 24L * 60L * 60L * 1_000L
        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        fun intent(context: Context, item: SpotlightItem): Intent =
            Intent(context, SpotlightActivity::class.java)
                .putExtra(EXTRA_ITEM, json.encodeToString(item))
    }
}
