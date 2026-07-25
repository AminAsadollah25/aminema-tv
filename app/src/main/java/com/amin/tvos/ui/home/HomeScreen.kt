package com.amin.tvos.ui.home

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
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amin.tvos.R
import com.amin.tvos.browser.AccountSyncActivity
import com.amin.tvos.browser.BrowserActivity
import com.amin.tvos.browser.CatalogSyncActivity
import com.amin.tvos.data.model.CatalogFilter
import com.amin.tvos.data.model.CatalogItem
import com.amin.tvos.data.model.CatalogKind
import com.amin.tvos.data.model.MovieItem
import com.amin.tvos.data.model.PlaybackSession
import com.amin.tvos.data.model.ResumeStrategy
import com.amin.tvos.data.model.StreamingService
import com.amin.tvos.ui.components.FocusableCard
import com.amin.tvos.ui.components.PosterCard
import com.amin.tvos.ui.components.SectionRow
import com.amin.tvos.ui.components.ServiceCard
import com.amin.tvos.ui.search.SearchActivity
import com.amin.tvos.ui.theme.CinemaRed
import com.amin.tvos.ui.theme.TextSecondary
import java.util.Calendar
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val services by viewModel.services.collectAsState()
    val continueWatching by viewModel.continueWatching.collectAsState()
    val recents by viewModel.recentlyOpened.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val catalogSections by viewModel.catalogSections.collectAsState()
    val iranianFilter by viewModel.iranianFilter.collectAsState()
    val internationalFilter by viewModel.internationalFilter.collectAsState()

    fun refreshCatalog() = context.startActivity(
        android.content.Intent(context, CatalogSyncActivity::class.java)
    )

    fun openCatalogItem(item: CatalogItem) =
        context.startActivity(
            BrowserActivity.intent(
                context,
                item.serviceId,
                item.contentUrl,
                contentUrl = item.contentUrl,
                contentTitle = item.title,
                contentPoster = item.posterUrl,
                // Films go one step further, straight to the site's own player page.
                // Series still open on their detail page until episode selection lands.
                directPlay = item.kind == CatalogKind.MOVIE
            )
        )

    fun openService(service: StreamingService) =
        context.startActivity(BrowserActivity.intent(context, service.id, service.url))

    fun openItem(item: MovieItem) =
        context.startActivity(
            BrowserActivity.intent(
                context,
                item.serviceId,
                item.url,
                item.resumePosition,
                contentUrl = item.url,
                contentTitle = item.title,
                contentPoster = item.posterUrl
            )
        )

    fun openPlayback(session: PlaybackSession) {
        val startUrl = when (session.resumeStrategy) {
            ResumeStrategy.OPEN_PLAYBACK_PAGE ->
                session.playbackUrl.ifBlank { session.contentUrl }
            ResumeStrategy.CLICK_SITE_CONTINUE -> session.contentUrl
        }
        context.startActivity(
            BrowserActivity.intent(
                context = context,
                serviceId = session.serviceId,
                url = startUrl,
                resumePosition = session.resumePosition,
                contentUrl = session.contentUrl,
                contentTitle = session.title,
                contentPoster = session.posterUrl,
                autoResume = true,
                resumeStrategyOverride = session.resumeStrategy,
                actionButtonTextPatterns = session.actionButtonTextPatterns
            )
        )
    }

    // The backdrop follows the most recent title, falling back to the last opened one.
    val backdropSource = remember(continueWatching, recents) {
        continueWatching.firstOrNull()
            ?.let { it.posterUrl to it.contentUrl }
            ?: recents.firstOrNull()?.let { it.posterUrl to it.url }
            ?: ("" to "")
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

    Box(Modifier.fillMaxSize()) {
    CinematicBackground(posterUrl = backdropSource.first, pageUrl = backdropSource.second)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 28.dp)
    ) {
        // ---------- Header ----------
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.aminema_mascot),
                contentDescription = "Aminema",
                modifier = Modifier.size(72.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text("AMINEMA", style = MaterialTheme.typography.displayMedium)
                Text(
                    "YOUR PERSONAL CINEMA",
                    style = MaterialTheme.typography.labelLarge,
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
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("جستجو", style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(Modifier.width(12.dp))
            FocusableCard(
                shape = RoundedCornerShape(50),
                onClick = {
                    context.startActivity(
                        android.content.Intent(context, AccountSyncActivity::class.java)
                    )
                }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Icon(
                        Icons.Filled.Sync,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Sync accounts", style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(Modifier.width(12.dp))
            FocusableCard(
                shape = RoundedCornerShape(50),
                onClick = onOpenSettings
            ) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = "Settings",
                    modifier = Modifier.padding(14.dp)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // ---------- Smart greeting ----------
        SmartGreetingHeader(
            greeting = greeting,
            onAction = { action ->
                when (action) {
                    GreetingAction.CONTINUE_LAST ->
                        continueWatching.firstOrNull()?.let { openPlayback(it) }
                    GreetingAction.PICK_MOVIE -> viewModel.setAllCatalogFilters(
                        CatalogFilter.MOVIE
                    )
                    GreetingAction.PICK_SERIES -> viewModel.setAllCatalogFilters(
                        CatalogFilter.SERIES
                    )
                    GreetingAction.SURPRISE ->
                        viewModel.randomCatalogItem()?.let { openCatalogItem(it) }
                    GreetingAction.NONE -> Unit
                }
            },
            onShuffle = { greetingVariant++ }
        )
        Spacer(Modifier.height(20.dp))

        // ---------- Cinemas ----------
        SectionRow("سینماهای من") {
            services.forEach { service ->
                ServiceCard(service = service, onClick = { openService(service) })
            }
            if (services.isEmpty()) {
                Text(
                    "No services configured. Add them in Settings or edit services.json.",
                    color = TextSecondary
                )
            }
        }
        Spacer(Modifier.height(24.dp))

        // ---------- Continue Watching ----------
        if (continueWatching.isNotEmpty()) {
            SectionRow("Continue Watching") {
                continueWatching.forEach { session ->
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
                        onClick = { openPlayback(session) },
                        onLongClick = { viewModel.toggleFavorite(item.id) }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // ---------- Latest Iranian / International ----------
        CatalogSectionRow(
            title = "تازه‌های ایرانی",
            section = catalogSections.firstOrNull {
                it.serviceId == HomeViewModel.IRANIAN_SERVICE_ID
            },
            filter = iranianFilter,
            onFilterChange = {
                viewModel.setCatalogFilter(HomeViewModel.IRANIAN_SERVICE_ID, it)
            },
            onRefresh = { refreshCatalog() },
            onOpen = { openCatalogItem(it) }
        )
        Spacer(Modifier.height(16.dp))

        CatalogSectionRow(
            title = "تازه‌های خارجی",
            section = catalogSections.firstOrNull {
                it.serviceId == HomeViewModel.INTERNATIONAL_SERVICE_ID
            },
            filter = internationalFilter,
            onFilterChange = {
                viewModel.setCatalogFilter(HomeViewModel.INTERNATIONAL_SERVICE_ID, it)
            },
            onRefresh = { refreshCatalog() },
            onOpen = { openCatalogItem(it) }
        )
        Spacer(Modifier.height(16.dp))

        // ---------- Recently Opened ----------
        if (recents.isNotEmpty()) {
            SectionRow("Recently Opened") {
                recents.forEach { item ->
                    PosterCard(
                        item = item,
                        onClick = { openItem(item) },
                        onLongClick = { viewModel.toggleFavorite(item.id) }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // ---------- Favorites ----------
        if (favorites.isNotEmpty()) {
            SectionRow("Favorites") {
                favorites.forEach { item ->
                    PosterCard(
                        item = item,
                        onClick = { openItem(item) },
                        onLongClick = { viewModel.toggleFavorite(item.id) }
                    )
                }
            }
        }

        if (continueWatching.isEmpty() && recents.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                Text(
                    "Open a service to start building your library.\nLong-press any card to add it to Favorites.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        Spacer(Modifier.height(32.dp))
    }
    }
}
