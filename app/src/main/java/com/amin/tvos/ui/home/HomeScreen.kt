package com.amin.tvos.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amin.tvos.R
import com.amin.tvos.browser.AccountSyncActivity
import com.amin.tvos.browser.BrowserActivity
import com.amin.tvos.data.ContentMetadataPolicy
import com.amin.tvos.data.model.CatalogFilter
import com.amin.tvos.data.model.CatalogItem
import com.amin.tvos.data.model.CatalogKind
import com.amin.tvos.data.model.CatalogSection
import com.amin.tvos.data.model.catalogKindFromUrl
import com.amin.tvos.data.model.LiveChannel
import com.amin.tvos.data.model.MovieItem
import com.amin.tvos.data.model.PlaybackSession
import com.amin.tvos.data.model.QuickLink
import com.amin.tvos.data.model.ResumeStrategy
import com.amin.tvos.data.model.SpotlightAction
import com.amin.tvos.data.model.SpotlightItem
import com.amin.tvos.data.model.StreamingService
import com.amin.tvos.ui.components.FeaturedBannerCard
import com.amin.tvos.ui.components.FocusableCard
import com.amin.tvos.ui.components.CatalogCard
import com.amin.tvos.ui.components.PosterCard
import com.amin.tvos.ui.components.SectionRow
import com.amin.tvos.ui.components.ServiceCard
import com.amin.tvos.ui.search.SearchActivity
import com.amin.tvos.ui.spotlight.SpotlightActivity
import com.amin.tvos.update.UpdateState
import com.amin.tvos.ui.theme.CinemaRed
import com.amin.tvos.ui.theme.TextSecondary
import java.util.Calendar
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onRefreshCatalog: (String) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val services by viewModel.services.collectAsState()
    val continueWatching by viewModel.continueWatching.collectAsState()
    val recents by viewModel.recentlyOpened.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val mySeries by viewModel.mySeries.collectAsState()
    val catalogSections by viewModel.catalogSections.collectAsState()
    val refreshingCatalogServices by viewModel.refreshingCatalogServices.collectAsState()
    val titleMetadata by viewModel.titleMetadata.collectAsState()
    val catalogItems = remember(catalogSections) {
        catalogSections
            .flatMap { it.all + it.movies + it.series + it.popularSeries + it.featured }
            .groupBy { ContentMetadataPolicy.canonicalContentUrl(it.contentUrl) }
            .values
            .map { variants -> variants.reduce(::mergeCatalogVariants) }
    }
    val catalogByUrl = remember(catalogItems) {
        catalogItems.associateBy {
            ContentMetadataPolicy.canonicalContentUrl(it.contentUrl)
        }
    }
    fun previewFor(item: MovieItem): CatalogItem {
        val key = ContentMetadataPolicy.canonicalContentUrl(item.url)
        return catalogByUrl[key] ?: CatalogItem(
            title = item.title,
            kind = catalogKindFromUrl(item.url) ?: CatalogKind.MOVIE,
            contentUrl = item.url,
            posterUrl = item.posterUrl,
            serviceId = item.serviceId
        )
    }

    fun serviceName(serviceId: String): String =
        services.firstOrNull { it.id == serviceId }?.name.orEmpty()

    fun openSpotlight(item: SpotlightItem) =
        context.startActivity(SpotlightActivity.intent(context, item))

    fun spotlightForCatalog(item: CatalogItem): SpotlightItem {
        val meta = titleMetadata[ContentMetadataPolicy.canonicalContentUrl(item.contentUrl)]
        return SpotlightItem(
            title = item.title,
            kind = item.kind,
            contentUrl = item.contentUrl,
            posterUrl = item.posterUrl.ifBlank { meta?.posterUrl.orEmpty() },
            backdropUrl = item.backdropUrl.ifBlank { meta?.backdropUrl.orEmpty() },
            serviceId = item.serviceId,
            serviceName = serviceName(item.serviceId),
            summary = meta?.summary?.takeIf { it.isNotBlank() } ?: item.summary,
            year = meta?.year?.takeIf { it.isNotBlank() } ?: item.year,
            genres = meta?.genres?.takeIf { it.isNotEmpty() } ?: item.genres,
            rating = meta?.rating?.takeIf { it.isNotBlank() } ?: item.rating,
            runtime = meta?.runtime?.takeIf { it.isNotBlank() } ?: item.runtime,
            episodeLabel = item.episodeLabel,
            country = meta?.country?.takeIf { it.isNotBlank() } ?: item.country,
            language = meta?.language?.takeIf { it.isNotBlank() } ?: item.language,
            hasPersianDub = meta?.hasPersianDub == true || item.hasPersianDub,
            hasPersianSubtitle = meta?.hasPersianSubtitle == true || item.hasPersianSubtitle,
            directors = meta?.directors?.takeIf { it.isNotEmpty() } ?: item.directors,
            cast = meta?.cast?.takeIf { it.isNotEmpty() } ?: item.cast,
            directPlay = item.kind == CatalogKind.MOVIE
        )
    }

    fun spotlightForMovie(item: MovieItem): SpotlightItem {
        val baseMeta = previewFor(item)
        val fetchedMeta = titleMetadata[ContentMetadataPolicy.canonicalContentUrl(item.url)]
        return SpotlightItem(
            title = item.title,
            kind = baseMeta.kind,
            contentUrl = item.url,
            posterUrl = item.posterUrl.ifBlank {
                baseMeta.posterUrl.ifBlank { fetchedMeta?.posterUrl.orEmpty() }
            },
            backdropUrl = baseMeta.backdropUrl.ifBlank {
                fetchedMeta?.backdropUrl.orEmpty()
            },
            serviceId = item.serviceId,
            serviceName = item.serviceName.ifBlank { serviceName(item.serviceId) },
            summary = fetchedMeta?.summary?.takeIf { it.isNotBlank() } ?: baseMeta.summary,
            year = fetchedMeta?.year?.takeIf { it.isNotBlank() } ?: baseMeta.year,
            genres = fetchedMeta?.genres?.takeIf { it.isNotEmpty() } ?: baseMeta.genres,
            rating = fetchedMeta?.rating?.takeIf { it.isNotBlank() } ?: baseMeta.rating,
            runtime = fetchedMeta?.runtime?.takeIf { it.isNotBlank() } ?: baseMeta.runtime,
            episodeLabel = baseMeta.episodeLabel,
            country = fetchedMeta?.country?.takeIf { it.isNotBlank() } ?: baseMeta.country,
            language = fetchedMeta?.language?.takeIf { it.isNotBlank() } ?: baseMeta.language,
            hasPersianDub = fetchedMeta?.hasPersianDub ?: baseMeta.hasPersianDub,
            hasPersianSubtitle = fetchedMeta?.hasPersianSubtitle ?: baseMeta.hasPersianSubtitle,
            directors = fetchedMeta?.directors?.takeIf { it.isNotEmpty() } ?: baseMeta.directors,
            cast = fetchedMeta?.cast?.takeIf { it.isNotEmpty() } ?: baseMeta.cast,
            primaryAction = if (item.resumePosition > 0L) {
                SpotlightAction.CONTINUE
            } else {
                SpotlightAction.WATCH
            },
            resumePosition = item.resumePosition,
            duration = item.duration,
            directPlay = baseMeta.kind == CatalogKind.MOVIE
        )
    }

    fun openService(service: StreamingService) =
        context.startActivity(BrowserActivity.intent(context, service.id, service.url))

    // Opens a named secondary page the site itself already builds.
    fun openQuickLink(service: StreamingService, quickLink: QuickLink) =
        context.startActivity(
            BrowserActivity.intent(
                context,
                service.id,
                service.url.trimEnd('/') + quickLink.path
            )
        )

    val secondaryQuickLinks = remember(services) {
        services.flatMap { service -> service.quickLinks.filterNot { it.prominent }.map { service to it } }
    }
    val liveSources = remember(services) {
        services.flatMap { service ->
            service.liveTv?.channels.orEmpty().map { channel -> service to channel }
        }
    }

    fun openLiveChannel(service: StreamingService, channel: LiveChannel) =
        context.startActivity(
            BrowserActivity.intent(
                context = context,
                serviceId = service.id,
                url = service.url.trimEnd('/') + channel.path,
                contentTitle = channel.name,
                liveTheaterMode = true
            )
        )

    fun spotlightForPlayback(
        session: PlaybackSession,
        releaseOverride: CatalogItem? = null
    ): SpotlightItem {
        // Some inline website players report the detail URL as their page URL.
        // It is not a reusable player destination unless it differs from content.
        val hasDedicatedPlaybackPage =
            session.playbackUrl.isNotBlank() &&
                session.playbackUrl.trimEnd('/') != session.contentUrl.trimEnd('/')
        val startUrl = when (session.resumeStrategy) {
            ResumeStrategy.OPEN_PLAYBACK_PAGE ->
                session.playbackUrl.takeIf { hasDedicatedPlaybackPage }
                    ?: session.contentUrl
            ResumeStrategy.CLICK_SITE_CONTINUE -> session.contentUrl
        }
        val useDirectResolver =
            !hasDedicatedPlaybackPage &&
                catalogKindFromUrl(session.contentUrl) == CatalogKind.MOVIE &&
                services.firstOrNull { it.id == session.serviceId }?.directPlay != null &&
                (
                    session.resumeStrategy == ResumeStrategy.OPEN_PLAYBACK_PAGE ||
                        // Account imports with Play-online labels need the same resolver.
                        session.actionButtonTextPatterns.isNotEmpty()
                    )
        val metadata = releaseOverride ?: catalogByUrl[
            ContentMetadataPolicy.canonicalContentUrl(session.contentUrl)
        ]
        val fetchedMetadata = titleMetadata[
            ContentMetadataPolicy.canonicalContentUrl(session.contentUrl)
        ]
        return SpotlightItem(
            title = session.title,
            kind = metadata?.kind ?: catalogKindFromUrl(session.contentUrl)
                ?: CatalogKind.MOVIE,
            contentUrl = session.contentUrl,
            posterUrl = session.posterUrl.ifBlank {
                metadata?.posterUrl.orEmpty().ifBlank {
                    fetchedMetadata?.posterUrl.orEmpty()
                }
            },
            backdropUrl = metadata?.backdropUrl.orEmpty().ifBlank {
                fetchedMetadata?.backdropUrl.orEmpty()
            },
            serviceId = session.serviceId,
            serviceName = session.serviceName.ifBlank { serviceName(session.serviceId) },
            summary = fetchedMetadata?.summary?.takeIf { it.isNotBlank() }
                ?: metadata?.summary.orEmpty(),
            year = fetchedMetadata?.year?.takeIf { it.isNotBlank() }
                ?: metadata?.year.orEmpty(),
            genres = fetchedMetadata?.genres?.takeIf { it.isNotEmpty() }
                ?: metadata?.genres.orEmpty(),
            rating = fetchedMetadata?.rating?.takeIf { it.isNotBlank() }
                ?: metadata?.rating.orEmpty(),
            runtime = fetchedMetadata?.runtime?.takeIf { it.isNotBlank() }
                ?: metadata?.runtime.orEmpty(),
            episodeLabel = metadata?.episodeLabel.orEmpty(),
            country = fetchedMetadata?.country?.takeIf { it.isNotBlank() }
                ?: metadata?.country.orEmpty(),
            language = fetchedMetadata?.language?.takeIf { it.isNotBlank() }
                ?: metadata?.language.orEmpty(),
            hasPersianDub = fetchedMetadata?.hasPersianDub == true ||
                metadata?.hasPersianDub == true,
            hasPersianSubtitle = fetchedMetadata?.hasPersianSubtitle == true ||
                metadata?.hasPersianSubtitle == true,
            directors = fetchedMetadata?.directors?.takeIf { it.isNotEmpty() }
                ?: metadata?.directors.orEmpty(),
            cast = fetchedMetadata?.cast?.takeIf { it.isNotEmpty() }
                ?: metadata?.cast.orEmpty(),
            primaryAction = SpotlightAction.CONTINUE,
            browserStartUrl = startUrl,
            resumePosition = session.resumePosition,
            duration = session.duration,
            autoResume = true,
            // Only account-synced movies with normal quality choices need the resolver.
            // ParsiFlix Continue remains the source of truth for episode/progress state.
            directPlay = useDirectResolver,
            resumeStrategy = session.resumeStrategy,
            actionButtonTextPatterns = session.actionButtonTextPatterns
        )
    }

    val iranianSection = catalogSections.firstOrNull {
        it.serviceId == HomeViewModel.IRANIAN_SERVICE_ID
    }
    val internationalSection = catalogSections.firstOrNull {
        it.serviceId == HomeViewModel.INTERNATIONAL_SERVICE_ID
    }

    /**
     * Slider entries are allowed to contain only a 16:9 banner. A Hero title is eligible
     * only when the same normal content URL also resolves to a genuine portrait poster.
     * The banner stays as ambient key art and is never stretched into the poster slot.
     */
    fun featuredWithPortrait(section: CatalogSection?): CatalogItem? {
        if (section == null) return null
        val portraitByUrl = (
            section.all + section.movies + section.series + section.popularSeries
            ).associateBy { ContentMetadataPolicy.canonicalContentUrl(it.contentUrl) }
        return section.featured.firstNotNullOfOrNull { featured ->
            (
                portraitByUrl[ContentMetadataPolicy.canonicalContentUrl(featured.contentUrl)]
                    ?: featured.takeIf {
                        it.posterUrl.isNotBlank() && it.posterUrl != it.backdropUrl
                    }
                )
                ?.takeIf { it.posterUrl.isNotBlank() }
                ?.let { portrait ->
                    portrait.copy(
                        backdropUrl = featured.backdropUrl.ifBlank { portrait.backdropUrl },
                        summary = portrait.summary.ifBlank { featured.summary }
                    )
                }
        }
    }

    // Home is content-first: Continue, a followed series and fresh titles become one
    // rotating cinematic moment. Provider doorways stay available near the bottom.
    val heroSlides = remember(
        continueWatching,
        mySeries,
        iranianSection,
        internationalSection,
        services,
        catalogItems,
        titleMetadata
    ) {
        buildList {
            continueWatching.firstOrNull()?.let { session ->
                add(
                    HomeHeroSlide(
                        id = "continue:${session.contentUrl}",
                        eyebrow = "ادامه تماشا",
                        actionLabel = "ادامه تماشا",
                        item = spotlightForPlayback(session)
                    )
                )
            }
            mySeries.firstOrNull()?.let { session ->
                val release = catalogItems.firstOrNull {
                    it.contentUrl.trimEnd('/') == session.contentUrl.trimEnd('/')
                }
                add(
                    HomeHeroSlide(
                        id = "series:${session.contentUrl}",
                        eyebrow = "سریال‌های من",
                        actionLabel = "مشاهده سریال",
                        item = spotlightForPlayback(session, release)
                    )
                )
            }
            (featuredWithPortrait(iranianSection)
                ?: iranianSection?.all?.firstOrNull())?.let { item ->
                add(
                    HomeHeroSlide(
                        id = "iranian:${item.contentUrl}",
                        eyebrow = if (item.backdropUrl.isNotBlank()) {
                            "برگزیده سینمای ایران"
                        } else {
                            "تازه از سینمای ایران"
                        },
                        actionLabel = if (item.kind == CatalogKind.SERIES) {
                            "مشاهده سریال"
                        } else {
                            "مشاهده فیلم"
                        },
                        item = spotlightForCatalog(item)
                    )
                )
            }
            (featuredWithPortrait(internationalSection)
                ?: internationalSection?.all?.firstOrNull())?.let { item ->
                add(
                    HomeHeroSlide(
                        id = "international:${item.contentUrl}",
                        eyebrow = if (item.backdropUrl.isNotBlank()) {
                            "برگزیده سینمای جهان"
                        } else {
                            "تازه از جهان"
                        },
                        actionLabel = if (item.kind == CatalogKind.SERIES) {
                            "مشاهده سریال"
                        } else {
                            "مشاهده فیلم"
                        },
                        item = spotlightForCatalog(item)
                    )
                )
            }
            internationalSection?.popularSeries?.firstOrNull()?.let { item ->
                add(
                    HomeHeroSlide(
                        id = "featured:${item.contentUrl}",
                        eyebrow = "سریال پیشنهادی امشب",
                        actionLabel = "جزئیات سریال",
                        item = spotlightForCatalog(item)
                    )
                )
            }
        }.distinctBy {
            ContentMetadataPolicy.canonicalContentUrl(it.item.contentUrl)
        }.take(5)
    }

    val fallbackBackdrop = remember(heroSlides, continueWatching, recents) {
        heroSlides.firstOrNull()?.item?.let {
            it.backdropUrl.ifBlank { it.posterUrl } to it.contentUrl
        }
            ?: continueWatching.firstOrNull()?.let { it.posterUrl to it.contentUrl }
            ?: recents.firstOrNull()?.let { it.posterUrl to it.url }
            ?: ("" to "")
    }
    var requestedBackdrop by remember { mutableStateOf(fallbackBackdrop) }
    var visibleBackdrop by remember { mutableStateOf(fallbackBackdrop) }
    LaunchedEffect(fallbackBackdrop) {
        if (requestedBackdrop.first.isBlank()) requestedBackdrop = fallbackBackdrop
    }
    // Dwell before changing the backdrop: fast DPAD sweeps do not start needless decodes.
    LaunchedEffect(requestedBackdrop) {
        delay(220L)
        visibleBackdrop = requestedBackdrop
    }
    fun previewBackdrop(posterUrl: String, pageUrl: String) {
        if (posterUrl.isNotBlank()) requestedBackdrop = posterUrl to pageUrl
    }

    // The hero rotates on its own every few seconds. Once the user starts browsing a rail
    // below it, that rotation must stop stealing the backdrop out from under their focus;
    // the hero takes it back only when one of its own controls is focused again.
    var heroOwnsBackdrop by remember { mutableStateOf(true) }
    fun previewFromRail(posterUrl: String, pageUrl: String) {
        heroOwnsBackdrop = false
        previewBackdrop(posterUrl, pageUrl)
    }

    // Recomputed every minute so the greeting keeps up with the clock on a TV that is
    // left running, and again whenever the underlying data changes.
    var greetingVariant by remember { mutableIntStateOf(0) }
    var minuteTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            minuteTick++
        }
    }
    val greeting = remember(
        greetingVariant, minuteTick, continueWatching.size, catalogSections
    ) {
        buildSmartGreeting(
            now = Calendar.getInstance(),
            variant = greetingVariant,
            hasContinue = continueWatching.isNotEmpty(),
            hasCatalog = catalogSections.any { it.all.isNotEmpty() }
        )
    }

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    // Where the Continue Watching row starts, so the greeting can jump to it.
    var continueRowOffset by remember { mutableIntStateOf(0) }
    var headerEntered by remember { mutableStateOf(false) }
    var heroEntered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        headerEntered = true
        delay(120L)
        heroEntered = true
    }

    Box(Modifier.fillMaxSize()) {
    CinematicBackground(posterUrl = visibleBackdrop.first, pageUrl = visibleBackdrop.second)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = 28.dp)
    ) {
        // ---------- Compact brand bar ----------
        AnimatedVisibility(
            visible = headerEntered,
            enter = fadeIn(tween(420)) +
                slideInVertically(tween(480, easing = FastOutSlowInEasing)) { -it / 3 }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.aminema_mascot),
                    contentDescription = "Aminema",
                    modifier = Modifier.size(54.dp)
                )
                Spacer(Modifier.width(13.dp))
                Column {
                    Text("AMINEMA", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "سینمای فارسی‌زبانان کهکشان",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CinemaRed
                    )
                }
                Spacer(Modifier.weight(1f))
                FocusableCard(
                    shape = RoundedCornerShape(50),
                    onClick = {
                        context.startActivity(
                            android.content.Intent(context, SearchActivity::class.java)
                        )
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("جستجو", style = MaterialTheme.typography.labelLarge)
                    }
                }
                Spacer(Modifier.width(10.dp))
                FocusableCard(
                    shape = RoundedCornerShape(50),
                    onClick = {
                        context.startActivity(
                            android.content.Intent(context, AccountSyncActivity::class.java)
                        )
                    }
                ) {
                    Icon(
                        Icons.Filled.Sync,
                        contentDescription = "همگام‌سازی حساب‌ها",
                        modifier = Modifier.padding(12.dp).size(22.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                FocusableCard(
                    shape = RoundedCornerShape(50),
                    onClick = onOpenSettings
                ) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = "تنظیمات",
                        modifier = Modifier.padding(12.dp).size(22.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // ---------- Self-update ----------
        val updateState by viewModel.updateState.collectAsState()
        UpdateBanner(
            state = updateState,
            onInstall = { release -> viewModel.downloadAndInstall(release, context) },
            onSkip = { release -> viewModel.skipUpdate(release) }
        )
        if (updateState is UpdateState.Available ||
            updateState is UpdateState.Downloading ||
            updateState is UpdateState.Failed
        ) {
            Spacer(Modifier.height(20.dp))
        }

        // ---------- Smart greeting ----------
        SmartGreetingHeader(
            greeting = greeting,
            onAction = { action ->
                when (action) {
                    // Scroll to the row rather than launching straight into the last
                    // title: the user still gets to choose which one to resume.
                    GreetingAction.CONTINUE_LAST -> coroutineScope.launch {
                        scrollState.animateScrollTo(continueRowOffset)
                    }
                    GreetingAction.PICK_MOVIE -> viewModel.setAllCatalogFilters(
                        CatalogFilter.MOVIE
                    )
                    GreetingAction.PICK_SERIES -> viewModel.setAllCatalogFilters(
                        CatalogFilter.SERIES
                    )
                    GreetingAction.SURPRISE ->
                        viewModel.randomCatalogItem()?.let {
                            openSpotlight(spotlightForCatalog(it))
                        }
                    GreetingAction.NONE -> Unit
                }
            },
            onShuffle = { greetingVariant++ }
        )
        Spacer(Modifier.height(14.dp))

        // ---------- Content-first cinematic hero ----------
        AnimatedVisibility(
            visible = heroEntered && heroSlides.isNotEmpty(),
            enter = fadeIn(tween(620)) +
                slideInVertically(tween(680, easing = FastOutSlowInEasing)) { it / 8 }
        ) {
            CinematicHero(
                slides = heroSlides,
                onOpen = { slide ->
                    if (slide.item.primaryAction == SpotlightAction.CONTINUE) {
                        context.startActivity(
                            BrowserActivity.intent(
                                context = context,
                                serviceId = slide.item.serviceId,
                                url = slide.item.browserStartUrl.ifBlank { slide.item.contentUrl },
                                resumePosition = slide.item.resumePosition,
                                contentUrl = slide.item.contentUrl,
                                contentTitle = slide.item.title,
                                contentPoster = slide.item.posterUrl,
                                autoResume = slide.item.autoResume,
                                directPlay = slide.item.directPlay,
                                resumeStrategyOverride = slide.item.resumeStrategy,
                                actionButtonTextPatterns = slide.item.actionButtonTextPatterns
                            )
                        )
                    } else {
                        openSpotlight(slide.item)
                    }
                },
                onVisibleSlideChanged = { slide ->
                    viewModel.enrichHeroMetadata(slide.item)
                    if (heroOwnsBackdrop) {
                        previewBackdrop(
                            slide.item.backdropUrl.ifBlank { slide.item.posterUrl },
                            slide.item.contentUrl
                        )
                    }
                },
                onHeroFocused = { focused -> if (focused) heroOwnsBackdrop = true },
                allowAutoAdvance = heroOwnsBackdrop
            )
        }
        if (heroSlides.isNotEmpty()) {
            Spacer(Modifier.height(22.dp))
        }

        // On a truly cold start there is no catalogue yet, so keep the two playful
        // cinema doorways close at hand until Home has enough content to build a Hero.
        if (heroSlides.isEmpty() && services.isNotEmpty()) {
            SectionRow(
                title = "از اینجا شروع کن",
                items = services,
                key = { it.id },
                showNavigation = false
            ) { service ->
                ServiceCard(service = service, onClick = { openService(service) })
            }
            Spacer(Modifier.height(16.dp))
        }

        // ---------- Continue Watching ----------
        if (continueWatching.isNotEmpty()) {
            Box(
                Modifier.onGloballyPositioned { coordinates ->
                    continueRowOffset =
                        (coordinates.positionInParent().y.toInt() - 24).coerceAtLeast(0)
                }
            ) {
            SectionRow(
                title = "ادامه تماشا",
                items = continueWatching,
                key = { it.id }
            ) { session ->
                    val item = MovieItem(
                        id = session.id,
                        title = session.title,
                        posterUrl = session.posterUrl,
                        serviceId = session.serviceId,
                        serviceName = session.serviceName,
                        url = session.contentUrl,
                        lastOpened = session.lastPlayed,
                        resumePosition = session.resumePosition,
                        duration = session.duration,
                        isPlayable = true,
                        isFavorite = favorites.any { it.id == session.id }
                    )
                    PosterCard(
                        item = item,
                        showContinueBadge = true,
                        onClick = {
                            val spotlightItem = spotlightForPlayback(session)
                            context.startActivity(
                                BrowserActivity.intent(
                                    context = context,
                                    serviceId = spotlightItem.serviceId,
                                    url = spotlightItem.browserStartUrl.ifBlank { spotlightItem.contentUrl },
                                    resumePosition = spotlightItem.resumePosition,
                                    contentUrl = spotlightItem.contentUrl,
                                    contentTitle = spotlightItem.title,
                                    contentPoster = spotlightItem.posterUrl,
                                    autoResume = spotlightItem.autoResume,
                                    directPlay = spotlightItem.directPlay,
                                    resumeStrategyOverride = spotlightItem.resumeStrategy,
                                    actionButtonTextPatterns = spotlightItem.actionButtonTextPatterns
                                )
                            )
                        },
                        onLongClick = { viewModel.toggleFavorite(item.id) },
                        onFocused = { focused ->
                            if (focused) previewFromRail(item.posterUrl, item.url)
                        }
                    )
            }
            }
            Spacer(Modifier.height(16.dp))
        }

        // ---------- My Series ----------
        if (mySeries.isNotEmpty()) {
            SectionRow(
                title = "سریال‌های من",
                items = mySeries,
                key = { it.id }
            ) { session ->
                    val release = catalogItems.firstOrNull {
                        it.contentUrl.trimEnd('/') == session.contentUrl.trimEnd('/')
                    }
                    CatalogCard(
                        item = CatalogItem(
                            title = session.title,
                            kind = CatalogKind.SERIES,
                            contentUrl = session.contentUrl,
                            posterUrl = session.posterUrl.ifBlank {
                                release?.posterUrl.orEmpty()
                            },
                            serviceId = session.serviceId,
                            // Publication status only — never claim "unwatched" without
                            // exact episode evidence from this Aminema device.
                            episodeLabel = release?.episodeLabel.orEmpty(),
                            summary = release?.summary.orEmpty(),
                            year = release?.year.orEmpty(),
                            genres = release?.genres.orEmpty(),
                            rating = release?.rating.orEmpty(),
                            runtime = release?.runtime.orEmpty()
                        ),
                        onClick = {
                            openSpotlight(
                                spotlightForPlayback(
                                    session = session,
                                    releaseOverride = release
                                )
                            )
                        },
                        onFocused = { focused ->
                            if (focused) {
                                previewFromRail(
                                    session.posterUrl.ifBlank {
                                        release?.posterUrl.orEmpty()
                                    },
                                    session.contentUrl
                                )
                            }
                        }
                    )
            }
            Spacer(Modifier.height(16.dp))
        }

        // ---------- Four explicit cinema categories ----------
        // TV users should not have to open a tiny filter and remember its state. Each
        // destination is now a first-class rail and is rendered only when it has content.
        listOf(
            Triple("سریال ایرانی", iranianSection, CatalogKind.SERIES),
            Triple("سریال خارجی", internationalSection, CatalogKind.SERIES),
            Triple("فیلم ایرانی", iranianSection, CatalogKind.MOVIE),
            Triple("فیلم خارجی", internationalSection, CatalogKind.MOVIE)
        ).forEach { (title, section, kind) ->
            val items = when (kind) {
                CatalogKind.MOVIE -> section?.movies.orEmpty()
                CatalogKind.SERIES -> section?.series.orEmpty()
            }
            if (items.isNotEmpty()) {
                CatalogSectionRow(
                    title = title,
                    section = section,
                    filter = if (kind == CatalogKind.MOVIE) {
                        CatalogFilter.MOVIE
                    } else {
                        CatalogFilter.SERIES
                    },
                    onFilterChange = {},
                    onRefresh = {
                        section?.serviceId?.let(onRefreshCatalog)
                    },
                    onOpen = { openSpotlight(spotlightForCatalog(it)) },
                    onPreview = { previewFromRail(it.posterUrl, it.contentUrl) },
                    itemsOverride = items,
                    showFilters = false,
                    isRefreshing = section?.serviceId?.let {
                        it in refreshingCatalogServices
                    } == true
                )
                Spacer(Modifier.height(16.dp))
            }
        }

        // Kept separate from episode-release ordering: these are provider-curated titles,
        // not necessarily recently updated shows.
        CatalogSectionRow(
            title = "سریال‌های برگزیده",
            section = internationalSection,
            filter = CatalogFilter.SERIES,
            onFilterChange = {},
            onRefresh = { onRefreshCatalog(HomeViewModel.INTERNATIONAL_SERVICE_ID) },
            onOpen = { openSpotlight(spotlightForCatalog(it)) },
            onPreview = { previewFromRail(it.posterUrl, it.contentUrl) },
            itemsOverride = internationalSection?.popularSeries.orEmpty(),
            showFilters = false,
            isRefreshing = HomeViewModel.INTERNATIONAL_SERVICE_ID in refreshingCatalogServices
        )
        Spacer(Modifier.height(16.dp))

        // ---------- Each provider's own editorial banner picks ----------
        // Deliberately below the "latest" rails: this is someone else's editorial choice, so
        // it supports Home rather than leading it. Wide key art, one row per provider, and the
        // two are kept apart because Iranian and international picks are unrelated selections.
        listOf(
            "برگزیده‌های سینمای ایران" to iranianSection,
            "برگزیده‌های سینمای جهان" to internationalSection
        ).forEach { (title, section) ->
            val banners = section?.featured.orEmpty()
            if (banners.isNotEmpty()) {
                SectionRow(
                    title = title,
                    items = banners,
                    key = { it.contentUrl }
                ) { banner ->
                    FeaturedBannerCard(
                        item = banner,
                        onClick = { openSpotlight(spotlightForCatalog(banner)) },
                        onFocused = { focused ->
                            if (focused) {
                                previewFromRail(
                                    banner.backdropUrl.ifBlank { banner.posterUrl },
                                    banner.contentUrl
                                )
                            }
                        }
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        // ---------- Native Live TV — one click opens the selected channel fullscreen ----------
        LiveTvSectionRow(
            sources = liveSources,
            onOpen = { service, channel -> openLiveChannel(service, channel) }
        )
        if (liveSources.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
        }

        // ---------- Secondary quick links (e.g. YouTube tab) — lower priority, lower down ----------
        if (secondaryQuickLinks.isNotEmpty()) {
            SectionRow(
                title = "بیشتر",
                items = secondaryQuickLinks,
                key = { (service, quickLink) -> "${service.id}:${quickLink.id}" }
            ) { (service, quickLink) ->
                    FocusableCard(
                        shape = RoundedCornerShape(16.dp),
                        onClick = { openQuickLink(service, quickLink) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                        ) {
                            Icon(
                                Icons.Filled.SmartDisplay,
                                contentDescription = null,
                                tint = CinemaRed,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(quickLink.label, style = MaterialTheme.typography.titleMedium)
                        }
                    }
            }
            Spacer(Modifier.height(16.dp))
        }

        // ---------- Recently Opened ----------
        if (recents.isNotEmpty()) {
            SectionRow(
                title = "اخیراً بازشده",
                items = recents,
                key = { it.id }
            ) { item ->
                    PosterCard(
                        item = item,
                        onClick = { openSpotlight(spotlightForMovie(item)) },
                        onLongClick = { viewModel.toggleFavorite(item.id) },
                        onFocused = { focused ->
                            if (focused) previewFromRail(item.posterUrl, item.url)
                        }
                    )
            }
            Spacer(Modifier.height(16.dp))
        }

        // ---------- Favorites ----------
        if (favorites.isNotEmpty()) {
            SectionRow(
                title = "موردعلاقه‌ها",
                items = favorites,
                key = { it.id }
            ) { item ->
                    PosterCard(
                        item = item,
                        onClick = { openSpotlight(spotlightForMovie(item)) },
                        onLongClick = { viewModel.toggleFavorite(item.id) },
                        onFocused = { focused ->
                            if (focused) previewFromRail(item.posterUrl, item.url)
                        }
                    )
            }
            Spacer(Modifier.height(16.dp))
        }

        // ---------- Provider doorways — deliberately last, useful but no longer dominant ----------
        if (services.isNotEmpty() && heroSlides.isNotEmpty()) {
            SectionRow(
                title = "ورود مستقیم به سینماها",
                items = services,
                key = { it.id },
                showNavigation = false
            ) { service ->
                ServiceCard(service = service, onClick = { openService(service) })
            }
        } else if (services.isEmpty()) {
            Text(
                "هنوز سینمایی تنظیم نشده؛ از تنظیمات یک سرویس اضافه کن.",
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 48.dp, vertical = 20.dp)
            )
        }

        if (continueWatching.isEmpty() && recents.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                Text(
                    "یک عنوان را جستجو کن یا از ورودی سینماها شروع کن.\n" +
                        "آمینما کم‌کم این صفحه را برای خودت می‌چیند.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        Spacer(Modifier.height(32.dp))
    }
    }
}

/**
 * The same title can be present in `all`, release-ordered series and curated series. One
 * list may carry the episode label while another carries synopsis/credits; keeping only
 * the first one silently erased that richer metadata (the real The Hawk bug).
 */
private fun mergeCatalogVariants(base: CatalogItem, richer: CatalogItem): CatalogItem =
    base.copy(
        posterUrl = base.posterUrl.ifBlank { richer.posterUrl },
        backdropUrl = base.backdropUrl.ifBlank { richer.backdropUrl },
        episodeLabel = base.episodeLabel.ifBlank { richer.episodeLabel },
        summary = listOf(base.summary, richer.summary).maxByOrNull { it.length }.orEmpty(),
        year = base.year.ifBlank { richer.year },
        genres = base.genres.ifEmpty { richer.genres },
        rating = base.rating.ifBlank { richer.rating },
        runtime = base.runtime.ifBlank { richer.runtime },
        country = base.country.ifBlank { richer.country },
        language = base.language.ifBlank { richer.language },
        hasPersianDub = base.hasPersianDub || richer.hasPersianDub,
        hasPersianSubtitle = base.hasPersianSubtitle || richer.hasPersianSubtitle,
        directors = base.directors.ifEmpty { richer.directors },
        cast = base.cast.ifEmpty { richer.cast }
    )
