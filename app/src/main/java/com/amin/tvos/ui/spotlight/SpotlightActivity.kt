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
import com.amin.tvos.data.model.SourceVariant
import com.amin.tvos.data.model.TitleMetadata
import com.amin.tvos.data.model.defaultSpotlightAction
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
                    onSourceSelected = ::selectSource,
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
                            onEpisodeSelected = { episode, season, edition ->
                                openBrowserWithEpisode(item, episode, season, edition)
                            },
                            onRetry = { startEpisodeLoad(item) },
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
            if (cached != null && isCurrentSource(initial)) {
                spotlightItem = (spotlightItem ?: initial).withMetadata(cached)
            }
            if (!isCurrentSource(initial)) return@launch
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
                        if (!isCurrentSource(initial)) return@launch
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
                        if (!isCurrentSource(initial)) return@launch
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
        if (isFinishing || isDestroyed || !isCurrentSource(initial)) return
        if (!PublicTitleMetadataEnricher.shouldLookup(known, initial)) {
            metadataLoading = false
            return
        }
        metadataLoading = true
        val external = publicMetadataEnricher.lookup(initial, known)
        if (!isCurrentSource(initial)) return
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
                    if (!isCurrentSource(initial)) return@launch
                    app.catalogRepository.metadataFor(initial.contentUrl)?.let { merged ->
                        spotlightItem = (spotlightItem ?: initial).withMetadata(merged)
                    }
                    metadataLoading = false
                }
            },
            onFailed = {
                if (isCurrentSource(initial)) metadataLoading = false
            }
        ).also { it.load(initial) }
    }

    /**
     * Switches only the ordinary provider title page behind the canonical card.
     * Existing cookies, provider sessions and persistent library files are untouched.
     */
    private fun selectSource(source: SourceVariant) {
        val current = spotlightItem ?: return
        if (ContentMetadataPolicy.isSameTopLevelPage(current.contentUrl, source.item.contentUrl)) {
            return
        }

        metadataLoader?.destroy()
        metadataLoader = null
        sheydaMetadataLoader?.destroy()
        sheydaMetadataLoader = null
        episodeLoader?.destroy()
        episodeLoader = null
        metadataLoading = false
        availableEditions = emptyList()
        episodeNavLoading = false
        episodeNavFailed = false

        val variant = source.item
        val switched = current.copy(
            title = variant.title,
            kind = variant.kind,
            contentUrl = variant.contentUrl,
            posterUrl = variant.posterUrl.ifBlank { current.posterUrl },
            backdropUrl = variant.backdropUrl.ifBlank { current.backdropUrl },
            serviceId = source.providerId,
            serviceName = source.providerName,
            summary = variant.summary.ifBlank { current.summary },
            year = variant.year.ifBlank { current.year },
            genres = variant.genres.ifEmpty { current.genres },
            rating = variant.rating.ifBlank { current.rating },
            runtime = variant.runtime.ifBlank { current.runtime },
            episodeLabel = variant.episodeLabel,
            country = variant.country.ifBlank { current.country },
            language = variant.language.ifBlank { current.language },
            hasPersianDub = variant.hasPersianDub,
            hasPersianSubtitle = variant.hasPersianSubtitle,
            directors = variant.directors.ifEmpty { current.directors },
            cast = variant.cast.ifEmpty { current.cast },
            // Switching provider deliberately drops provider-specific resume state. Series must
            // still return to the native navigator instead of opening the provider page directly.
            primaryAction = variant.kind.defaultSpotlightAction(),
            browserStartUrl = variant.contentUrl,
            resumePosition = 0L,
            duration = 0L,
            editionTimelineId = "",
            autoResume = false,
            directPlay = variant.kind == CatalogKind.MOVIE,
            resumeStrategy = null,
            actionButtonTextPatterns = emptyList()
        )
        spotlightItem = switched
        showEpisodeNavigator = switched.kind == CatalogKind.SERIES
        if (showEpisodeNavigator) startEpisodeLoad(switched)
        enrichMetadata(switched)
    }

    private fun isCurrentSource(candidate: SpotlightItem): Boolean =
        spotlightItem?.let { current ->
            ContentMetadataPolicy.isSameTopLevelPage(current.contentUrl, candidate.contentUrl)
        } == true

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

    private fun openBrowserWithEpisode(
        item: SpotlightItem,
        episode: Episode,
        season: Season,
        edition: SeriesEdition
    ) {
        startActivity(
            com.amin.tvos.browser.BrowserActivity.intent(
                context = this,
                serviceId = item.serviceId,
                // Episode selection always starts from the stable title page, never
                // from a previously saved player page belonging to another episode.
                url = item.contentUrl,
                resumePosition = 0L,
                contentUrl = item.contentUrl,
                contentTitle = item.title,
                contentPoster = item.posterUrl,
                autoResume = false,
                directPlay = true,
                smSeason = season.id,
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
