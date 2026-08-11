package com.amin.tvos.ui.livetv

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width


import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import com.amin.tvos.AminTvApp
import com.amin.tvos.MainActivity
import com.amin.tvos.browser.BrowserActivity
import com.amin.tvos.data.model.LiveChannel
import com.amin.tvos.data.model.StreamingService
import com.amin.tvos.ui.components.FocusableCard
import com.amin.tvos.ui.theme.AminTvTheme
import com.amin.tvos.ui.theme.Ink
import com.amin.tvos.ui.theme.SurfaceDark
import com.amin.tvos.ui.theme.TextPrimary
import com.amin.tvos.ui.theme.TextSecondary
import java.net.URI

class LiveTvActivity : ComponentActivity() {

    private val app get() = application as AminTvApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUi()

        val compose = ComposeView(this).apply {
            setContent {
                AminTvTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Ink,
                        contentColor = TextPrimary
                    ) {
                        LiveTvScreen()
                    }
                }
            }
        }

        setContentView(
            compose,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = returnToHome()
        })
    }

    private fun returnToHome() {
        startActivity(
            Intent(this, MainActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        )
        finish()
    }

    private fun openLiveChannel(
        service: StreamingService,
        channel: LiveChannel,
        playerId: String? = null
    ) {
        val absoluteUrl = if (channel.path.startsWith("http")) {
            channel.path
        } else {
            URI(service.url).resolve(channel.path).toString()
        }
        startActivity(
            BrowserActivity.intent(
                context = this,
                serviceId = service.id,
                url = absoluteUrl,
                contentPoster = channel.logoUrl,
                liveTheaterMode = true,
                contentTitle = channel.name,
                livePlayerId = playerId
            )
        )
    }

    @Composable
    private fun LiveTvScreen() {
        val services by app.servicesRepository.services.collectAsState()

        val allChannels = remember(services) {
            services.flatMap { service ->
                service.liveTv?.channels?.map { channel ->
                    Pair(service, channel)
                } ?: emptyList()
            }
        }

        val categories = remember(allChannels) {
            val categoryNames = linkedMapOf(
                "persian" to "شبکه‌های فارسی",
                "irib" to "شبکه‌های سراسری و صداوسیما",
                "sport" to "شبکه‌های ورزشی",
                "other" to "سایر شبکه‌ها"
            )
            val grouped = allChannels.groupBy { item ->
                if (categoryNames.containsKey(item.second.category)) {
                    item.second.category
                } else {
                    "other"
                }
            }
            categoryNames.mapNotNull { (id, label) ->
                grouped[id]?.takeIf { it.isNotEmpty() }?.let { channels -> label to channels }
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(top = 20.dp, start = 48.dp, end = 48.dp)
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FocusableCard(
                        shape = RoundedCornerShape(50),
                        onClick = { returnToHome() }
                    ) { focused ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(
                                    if (focused) Color.White.copy(alpha = 0.16f)
                                    else SurfaceDark,
                                    RoundedCornerShape(50)
                                )
                                .padding(horizontal = 17.dp, vertical = 11.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("بازگشت", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    Spacer(Modifier.width(18.dp))
                    Column {
                        Text("پخش زنده", style = MaterialTheme.typography.displayMedium)
                        Text(
                            "شبکه‌های تلویزیونی زنده",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary
                        )
                    }
                }
            }
            Spacer(Modifier.height(32.dp))

            if (allChannels.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "هیچ شبکه زنده‌ای یافت نشد.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            } else {
                androidx.compose.runtime.CompositionLocalProvider(
                    LocalLayoutDirection provides LayoutDirection.Rtl
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(26.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(categories, key = { it.first }) { (catName, catChannels) ->
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text(
                                    text = catName,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 8.dp, start = 8.dp)
                                )
                                // Four compact cards fit a 1080p TV row at the current
                                // TV density. Extra channels continue on the next row;
                                // there is no hidden horizontal scroll state.
                                catChannels.chunked(4).forEach { rowChannels ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        rowChannels.forEach { (service, channel) ->
                                            LiveChannelCard(
                                                service = service,
                                                channel = channel,
                                                onClick = { openLiveChannel(service, channel) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun LiveChannelCard(
        service: StreamingService,
        channel: LiveChannel,
        onClick: () -> Unit
    ) {
        FocusableCard(
            modifier = Modifier.width(200.dp),
            shape = RoundedCornerShape(16.dp),
            onClick = onClick
        ) { focused ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (focused) Color.White.copy(alpha = 0.15f) else SurfaceDark,
                        RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (channel.logoUrl.isNotBlank()) {
                        AsyncImage(
                            model = channel.logoUrl,
                            contentDescription = channel.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(22.dp)
                        )
                    } else {
                        // Fallback text if no logo is available
                        Text(
                            text = channel.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = service.name.ifBlank { service.id },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
        }
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
}
