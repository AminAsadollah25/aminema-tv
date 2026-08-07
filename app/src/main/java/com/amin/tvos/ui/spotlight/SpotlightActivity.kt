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
import com.amin.tvos.data.PublicTitleMetadataEnricher
import com.amin.tvos.data.model.CatalogKind
import com.amin.tvos.data.model.Episode
import com.amin.tvos.data.model.SeriesEdition
import com.amin.tvos.data.model.Season
import com.amin.tvos.data.model.SpotlightAction
import com.amin.tvos.data.model.SpotlightItem
import com.amin.tvos.data.model.TitleMetadata
import com.amin.tvos.data.model.isDecisionComplete
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
    private var sheydaMetadataLoader: SheydaMetadataLoader? = null
    private val publicMetadataEnricher = PublicTitleMetadataEnricher()
    private var showEpisodeNavigator by mutableStateOf(false)
    private var episodeNavLoading by mutableStateOf(false)
    private var episodeNavFailed by mutableStateOf(false)
    private var availableEditions by mutableStateOf<List<SeriesEdition>>(emptyList())
    private var episodeLoader: EpisodeLoader? = null

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

        if (decodedItem.kind == CatalogKind.SERIES) {
            showEpisodeNavigator = true
        }

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
                    showEpisodeNavigator = showEpisodeNavigator,
                    onBack = { finish() },
                    onToggleFavorite = { spotlightViewModel.toggleFavorite(item) },
                    onAction = { action ->
                        when (action) {
                            SpotlightAction.WATCH, SpotlightAction.CONTINUE -> {
                                if (item.kind == CatalogKind.SERIES) {
                                    // With the scrollable UI, 'Watch' will focus the episodes,
                                    // but if we want to play the latest/next episode, we can openBrowser.
                                    openBrowser(item)
                                } else {
                                    openBrowser(item)
                                }
                            }
                            SpotlightAction.SELECT_EPISODE -> {
                                // Handled implicitly by scrolling down in the UI now
                            }
                            SpotlightAction.LATEST_EPISODE -> openBrowser(item)
                        }
                    },
                    episodeNavigatorContent = {
                        EpisodeNavigatorInline(
                            editions = availableEditions,
                            isLoading = episodeNavLoading,
                            hasFailed = episodeNavFailed,
                            posterUrl = item.backdropUrl.ifEmpty { item.posterUrl },
                            contentUrl = item.contentUrl,
                            onEpisodeSelected = { episode, edition ->
                                openBrowserWithEpisode(item, episode, edition)
                            },
                            onDismiss = { }
                        )
                    }
                )
            }
        }

        if (decodedItem.kind == CatalogKind.SERIES) {
            startEpisodeLoad(decodedItem)
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
            // A fresh synopsis-only record is not complete. Retry the provider's ordinary
            // title metadata so newly supported credits can appear without clearing app data.
            val cacheIsFresh = cached?.isDecisionComplete() == true &&
                !cached.backdropUrl.isNullOrBlank() &&
                System.currentTimeMillis() - cached.fetchedAt < METADATA_MAX_AGE_MS
            if (isFinishing || isDestroyed) return@launch
            if (cacheIsFresh) {
                if (PublicTitleMetadataEnricher.shouldLookup(cached, initial)) {
                    completeFromPublicSource(initial, cached)
                }
                return@launch
            }

            metadataLoading = true
            metadataLoader = SpotlightMetadataLoader(
                activity = this@SpotlightActivity,
                app = app,
                onLoaded = { metadata ->
                    lifecycleScope.launch {
                        app.catalogRepository.saveTitleMetadata(metadata)
                        val merged = app.catalogRepository.metadataFor(initial.contentUrl)
                            ?: metadata
                        spotlightItem = (spotlightItem ?: initial).withMetadata(merged)
                        if (!PublicTitleMetadataEnricher.shouldLookup(merged, initial)) {
                            metadataLoading = false
                        } else {
                            completeFromPublicSource(initial, merged)
                        }
                    }
                },
                onFailed = {
                    lifecycleScope.launch {
                        completeFromPublicSource(initial, cached)
                    }
                }
            ).also { it.load(initial) }
        }
    }

    /** Provider-first; public metadata fills only fields that are still absent. */
    private suspend fun completeFromPublicSource(
        initial: SpotlightItem,
        known: TitleMetadata?
    ) {
        if (isFinishing || isDestroyed) return
        if (!PublicTitleMetadataEnricher.shouldLookup(known, initial)) {
            metadataLoading = false
            return
        }
        metadataLoading = true
        val external = publicMetadataEnricher.lookup(initial, known)
        val attempted = external ?: TitleMetadata(
            contentUrl = initial.contentUrl,
            externalLookupAt = System.currentTimeMillis(),
            externalLookupVersion = PublicTitleMetadataEnricher.LOOKUP_VERSION
        )
        val app = application as AminTvApp
        app.catalogRepository.saveTitleMetadata(attempted)
        val merged = app.catalogRepository.metadataFor(initial.contentUrl) ?: known
        if (merged != null && !isFinishing && !isDestroyed) {
            spotlightItem = (spotlightItem ?: initial).withMetadata(merged)
        }
        if (needsIranianCompletion(initial, merged)) {
            completeFromSheyda(initial)
        } else {
            metadataLoading = false
        }
    }

    /**
     * Iranian provider pages are often synopsis-only. Sheyda's public title UI is a second,
     * exact-match source for Persian directors/cast and never affects the watch destination.
     */
    private fun completeFromSheyda(initial: SpotlightItem) {
        sheydaMetadataLoader?.destroy()
        sheydaMetadataLoader = SheydaMetadataLoader(
            activity = this,
            onLoaded = { metadata ->
                lifecycleScope.launch {
                    val app = application as AminTvApp
                    app.catalogRepository.saveTitleMetadata(metadata)
                    app.catalogRepository.metadataFor(initial.contentUrl)?.let { merged ->
                        spotlightItem = (spotlightItem ?: initial).withMetadata(merged)
                    }
                    metadataLoading = false
                }
            },
            onFailed = { metadataLoading = false }
        ).also { it.load(initial) }
    }

    private fun needsIranianCompletion(
        initial: SpotlightItem,
        metadata: TitleMetadata?
    ): Boolean = (
        initial.serviceId.equals("parsiflix", true) ||
            initial.country.contains("ایران") || initial.country.contains("Iran", true)
        ) && (
        metadata?.summary.isNullOrBlank() ||
            metadata?.directors.isNullOrEmpty() || metadata?.cast.isNullOrEmpty()
        )

    private fun openBrowserWithEpisode(item: SpotlightItem, episode: Episode, edition: SeriesEdition) {
        val season = edition.seasons.firstOrNull { s -> s.episodes.any { it.id == episode.id } }
        startActivity(
            com.amin.tvos.browser.BrowserActivity.intent(
                context = this,
                serviceId = item.serviceId,
                url = item.browserStartUrl.ifBlank { item.contentUrl },
                resumePosition = 0L,
                contentUrl = item.contentUrl,
                contentPoster = item.posterUrl,
                autoResume = false,
                directPlay = true,
                smSeason = season?.id,
                smQuality = edition.resolution,
                smEpisode = episode.actionPayload
            )
        )
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
        sheydaMetadataLoader?.destroy()
        sheydaMetadataLoader = null
        episodeLoader?.destroy()
        episodeLoader = null
        super.onDestroy()
    }

    private fun startEpisodeLoad(item: SpotlightItem) {
        val app = application as AminTvApp
        episodeNavLoading = true
        episodeNavFailed = false
        episodeLoader?.destroy()
        episodeLoader = EpisodeLoader(
            activity = this,
            app = app,
            onLoaded = { editions ->
                availableEditions = editions
                episodeNavLoading = false
            },
            onFailed = {
                episodeNavLoading = false
                episodeNavFailed = true
            }
        ).also { it.load(item) }
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
