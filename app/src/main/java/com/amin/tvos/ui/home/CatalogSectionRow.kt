package com.amin.tvos.ui.home

import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.amin.tvos.data.model.CatalogFilter
import com.amin.tvos.data.model.CatalogItem
import com.amin.tvos.data.model.CatalogSection
import com.amin.tvos.ui.components.CatalogCard
import com.amin.tvos.ui.components.FocusableCard
import com.amin.tvos.ui.components.RailNavigationControls
import com.amin.tvos.ui.theme.CinemaRed
import com.amin.tvos.ui.theme.TextSecondary
import java.util.concurrent.TimeUnit

/**
 * One "latest" row: title, `همه | فیلم | سریال` selector, refresh, and a TV-friendly
 * empty/error state. The row never shows the provider's brand name — only «ایرانی» or
 * «خارجی», as configured on Home.
 */
@Composable
fun CatalogSectionRow(
    title: String,
    section: CatalogSection?,
    filter: CatalogFilter,
    onFilterChange: (CatalogFilter) -> Unit,
    onRefresh: () -> Unit,
    onOpen: (CatalogItem) -> Unit,
    onPreviewStateChange: (CatalogItem, Boolean) -> Unit = { _, _ -> },
    itemsOverride: List<CatalogItem>? = null,
    showFilters: Boolean = true,
    isRefreshing: Boolean = false
) {
    val items = itemsOverride ?: section?.items(filter).orEmpty()
    val listState = rememberLazyListState()
    Column(Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp, vertical = 12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            if (showFilters) {
                Spacer(Modifier.width(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CatalogFilter.entries.forEach { entry ->
                        FocusableCard(
                            shape = RoundedCornerShape(50),
                            onClick = { onFilterChange(entry) }
                        ) {
                            Text(
                                entry.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (entry == filter) CinemaRed else TextSecondary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            section?.syncedAt?.takeIf { it > 0L }?.let { syncedAt ->
                Text(
                    lastSyncLabel(syncedAt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(Modifier.width(12.dp))
            }
            FocusableCard(
                shape = RoundedCornerShape(50),
                enabled = !isRefreshing,
                onClick = onRefresh
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            color = CinemaRed,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (isRefreshing) "در حال بروزرسانی" else "بروزرسانی",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            RailNavigationControls(listState, items.size)
        }

        if (section?.error?.isNotBlank() == true) {
            Text(
                "این بخش بروزرسانی نشد: ${section.error}",
                style = MaterialTheme.typography.bodyMedium,
                color = CinemaRed,
                modifier = Modifier.padding(horizontal = 48.dp, vertical = 4.dp)
            )
        }

        if (items.isEmpty()) {
            Box(
                Modifier.fillMaxWidth().padding(horizontal = 48.dp, vertical = 24.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    if (isRefreshing && (section == null || section.syncedAt == 0L)) {
                        "اولین بروزرسانی در پس‌زمینه انجام می‌شود…"
                    } else if (section == null || section.syncedAt == 0L) {
                        "هنوز بروزرسانی نشده است. دکمه بروزرسانی را بزنید."
                    } else {
                        "موردی در این بخش پیدا نشد."
                    },
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = 48.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(items, key = { it.contentUrl }) { item ->
                    CatalogCard(
                        item = item,
                        onClick = { onOpen(item) },
                        onPreviewStateChange = onPreviewStateChange
                    )
                }
            }
        }
    }
}

private fun lastSyncLabel(syncedAt: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(
        (System.currentTimeMillis() - syncedAt).coerceAtLeast(0L)
    )
    return when {
        minutes < 1 -> "بروزرسانی: هم‌اکنون"
        minutes < 60 -> "بروزرسانی: $minutes دقیقه پیش"
        minutes < 24 * 60 -> "بروزرسانی: ${minutes / 60} ساعت پیش"
        else -> "بروزرسانی: ${minutes / (24 * 60)} روز پیش"
    }
}
