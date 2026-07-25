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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.amin.tvos.data.model.MovieItem
import com.amin.tvos.data.model.PlaybackSession
import com.amin.tvos.data.model.ResumeStrategy
import com.amin.tvos.data.model.StreamingService
import com.amin.tvos.ui.components.FocusableCard
import com.amin.tvos.ui.components.PosterCard
import com.amin.tvos.ui.components.SectionRow
import com.amin.tvos.ui.components.ServiceCard
import com.amin.tvos.ui.theme.CinemaRed
import com.amin.tvos.ui.theme.TextSecondary

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

        // ---------- Cinemas ----------
        SectionRow("امشب چی می‌بینیم؟") {
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
