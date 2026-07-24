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
import com.amin.tvos.browser.BrowserActivity
import com.amin.tvos.data.model.MovieItem
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
        context.startActivity(BrowserActivity.intent(context, item.serviceId, item.url))

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
                painter = painterResource(R.drawable.ic_logo),
                contentDescription = null,
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text("AMIN ", style = MaterialTheme.typography.displayMedium)
            Text(
                "TV OS",
                style = MaterialTheme.typography.displayMedium,
                color = CinemaRed
            )
            Spacer(Modifier.weight(1f))
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

        // ---------- Continue Watching ----------
        if (continueWatching.isNotEmpty()) {
            SectionRow("Continue Watching") {
                continueWatching.forEach { item ->
                    PosterCard(
                        item = item,
                        showContinueBadge = true,
                        onClick = { openItem(item) },
                        onLongClick = { viewModel.toggleFavorite(item.id) }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // ---------- My Services ----------
        SectionRow("My Services") {
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
