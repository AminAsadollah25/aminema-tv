package com.amin.tvos.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.amin.tvos.data.model.LiveChannel
import com.amin.tvos.data.model.StreamingService
import com.amin.tvos.ui.components.FocusableCard
import com.amin.tvos.ui.components.RailNavigationControls
import com.amin.tvos.ui.theme.CinemaRed
import com.amin.tvos.ui.theme.SurfaceElevated
import com.amin.tvos.ui.theme.TextSecondary

/**
 * Native, TV-first Live rail. The cards are native; playback still uses only the
 * service's normal signed-in page in BrowserActivity.
 */
@Composable
fun LiveTvSectionRow(
    sources: List<Pair<StreamingService, LiveChannel>>,
    onOpen: (StreamingService, LiveChannel) -> Unit
) {
    if (sources.isEmpty()) return
    val scrollState = rememberScrollState()

    Column(Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp, vertical = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.LiveTv,
                contentDescription = null,
                tint = CinemaRed,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text("پخش زنده", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.weight(1f))
            RailNavigationControls(scrollState)
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier
                .horizontalScroll(scrollState)
                .padding(horizontal = 48.dp, vertical = 8.dp)
        ) {
            sources.forEach { (service, channel) ->
                LiveChannelCard(
                    channel = channel,
                    onClick = { onOpen(service, channel) }
                )
            }
        }
    }
}

@Composable
private fun LiveChannelCard(
    channel: LiveChannel,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val logoModel = remember(channel.logoUrl) {
        // Logos occupy a small TV card. Bound the decode size so a needlessly
        // large source image cannot consume box memory at its original resolution.
        ImageRequest.Builder(context)
            .data(channel.logoUrl)
            .size(320, 140)
            .crossfade(180)
            .build()
    }
    FocusableCard(
        modifier = Modifier
            .width(214.dp)
            .height(142.dp),
        shape = RoundedCornerShape(18.dp),
        onClick = onClick
    ) { focused ->
        val focusTint by animateColorAsState(
            targetValue = if (focused) CinemaRed.copy(alpha = 0.22f) else Color.Transparent,
            animationSpec = tween(160),
            label = "liveCardTint"
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF292932), SurfaceElevated, Color(0xFF111116))
                    )
                )
                .background(focusTint)
        ) {
            AsyncImage(
                model = logoModel,
                contentDescription = channel.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp, start = 24.dp, end = 24.dp)
                    .fillMaxWidth()
                    .height(68.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(9.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.72f))
                    .padding(horizontal = 7.dp, vertical = 4.dp)
            ) {
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(CinemaRed)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    "LIVE",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.92f))
                        )
                    )
                    .padding(horizontal = 13.dp, vertical = 11.dp)
            ) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (focused) {
                    Text(
                        text = "پخش فوری",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
