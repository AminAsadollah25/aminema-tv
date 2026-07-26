package com.amin.tvos.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.amin.tvos.ui.components.FocusableCard
import com.amin.tvos.ui.theme.CinemaRed
import com.amin.tvos.ui.theme.SurfaceElevated
import com.amin.tvos.ui.theme.TextSecondary
import com.amin.tvos.update.ReleaseInfo
import com.amin.tvos.update.UpdateState

/**
 * A compact Home banner, shown only when a newer release is actually available.
 *
 * Installing is never one click away from silent: the download step is visible with
 * progress, and the final install step always hands off to Android's own system installer
 * screen — this banner cannot bypass that confirmation.
 */
@Composable
fun UpdateBanner(
    state: UpdateState,
    onInstall: (ReleaseInfo) -> Unit,
    onSkip: (ReleaseInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    when (state) {
        is UpdateState.Available -> Surface(
            color = SurfaceElevated,
            shape = RoundedCornerShape(14.dp),
            modifier = modifier.fillMaxWidth().padding(horizontal = 48.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Icon(Icons.Filled.SystemUpdate, contentDescription = null, tint = CinemaRed)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "نسخه ${state.release.versionName} موجود است",
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (state.release.notes.isNotBlank()) {
                        Text(
                            state.release.notes.lineSequence().firstOrNull().orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FocusableCard(
                        shape = RoundedCornerShape(50),
                        onClick = { onSkip(state.release) }
                    ) {
                        Text(
                            "بعداً",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                        )
                    }
                    FocusableCard(
                        shape = RoundedCornerShape(50),
                        onClick = { onInstall(state.release) }
                    ) {
                        Text(
                            "بروزرسانی",
                            style = MaterialTheme.typography.labelLarge,
                            color = CinemaRed,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }

        is UpdateState.Downloading -> Surface(
            color = SurfaceElevated,
            shape = RoundedCornerShape(14.dp),
            modifier = modifier.fillMaxWidth().padding(horizontal = 48.dp)
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "در حال دانلود نسخه ${state.release.versionName}… ${state.percent}%",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(Modifier.height(10.dp))
                ProgressTrack(percent = state.percent)
            }
        }

        is UpdateState.Failed -> Surface(
            color = SurfaceElevated,
            shape = RoundedCornerShape(14.dp),
            modifier = modifier.fillMaxWidth().padding(horizontal = 48.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text("بروزرسانی انجام نشد", style = MaterialTheme.typography.titleMedium)
                    Text(state.message, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                }
                if (state.release != null) {
                    FocusableCard(
                        shape = RoundedCornerShape(50),
                        onClick = { onInstall(state.release) }
                    ) {
                        Text(
                            "تلاش دوباره",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }

        UpdateState.Idle, UpdateState.Checking -> Unit
    }
}

@Composable
private fun ProgressTrack(percent: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(50))
            .background(TextSecondary.copy(alpha = 0.25f))
    ) {
        Row(
            Modifier
                .fillMaxWidth(percent.coerceIn(0, 100) / 100f)
                .height(6.dp)
                .background(CinemaRed)
        ) {}
    }
}
