package com.amin.tvos.ui.livetv

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.amin.tvos.AminTvApp
import com.amin.tvos.MainActivity
import com.amin.tvos.browser.BrowserActivity
import com.amin.tvos.data.LiveChannelSource
import com.amin.tvos.data.deduplicateLiveChannels
import com.amin.tvos.data.isLiveActive
import com.amin.tvos.data.liveChannelKey
import com.amin.tvos.data.liveChannelSources
import com.amin.tvos.data.model.LiveChannel
import com.amin.tvos.data.model.LiveChannelHealth
import com.amin.tvos.data.model.LiveHealthStatus
import com.amin.tvos.data.model.StreamingService
import com.amin.tvos.ui.components.FocusableCard
import com.amin.tvos.ui.theme.AminTvTheme
import com.amin.tvos.ui.theme.Ink
import com.amin.tvos.ui.theme.SurfaceDark
import com.amin.tvos.ui.theme.TextPrimary
import com.amin.tvos.ui.theme.TextSecondary
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.net.URI

class LiveTvActivity : ComponentActivity() {

    private val app get() = application as AminTvApp

    override fun onResume() {
        super.onResume()
        app.liveChannelHealthCoordinator.setScanningEnabled(true)
    }

    override fun onPause() {
        // Opening a real channel or returning home must silence and cancel the hidden
        // health WebView immediately; it must never compete with the visible player.
        app.liveChannelHealthCoordinator.setScanningEnabled(false)
        super.onPause()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUi()

        val root = FrameLayout(this)
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
        root.addView(
            compose,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        setContentView(root)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = returnToHome()
        })

        lifecycleScope.launch {
            val services = app.servicesRepository.services
                .filter { it.any { service -> service.liveTv?.channels?.isNotEmpty() == true } }
                .first()
            app.liveChannelHealthCoordinator.start(services, force = false)
        }
    }

    private fun returnToHome() {
        startActivity(
            Intent(this, MainActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        )
        finish()
    }

    private fun openLiveChannel(source: LiveChannelSource, playerId: String? = null) {
        val absoluteUrl = if (source.channel.path.startsWith("http")) {
            source.channel.path
        } else {
            URI(source.service.url).resolve(source.channel.path).toString()
        }
        startActivity(
            BrowserActivity.intent(
                context = this,
                serviceId = source.service.id,
                url = absoluteUrl,
                contentPoster = source.channel.logoUrl,
                liveTheaterMode = true,
                contentTitle = source.channel.name,
                livePlayerId = playerId
            )
        )
    }

    @Composable
    private fun LiveTvScreen() {
        val services by app.servicesRepository.services.collectAsState()
        val health by app.liveChannelHealthCoordinator.health.collectAsState()
        val refreshState by app.liveChannelHealthCoordinator.refreshState.collectAsState()
        var showAll by rememberSaveable { mutableStateOf(false) }

        val allSources = remember(services) {
            liveChannelSources(services).sortedBy { source ->
                when {
                    source.channel.id.startsWith("babaktv-gem-") -> 0
                    source.service.id == "babaktv" -> 2
                    else -> 1
                }
            }
        }
        val uniqueSources = remember(allSources, health) {
            deduplicateLiveChannels(allSources, health)
        }
        val activeSources = remember(uniqueSources, health) {
            uniqueSources.filter { isLiveActive(it, health) }
        }
        val visibleSources = if (showAll) uniqueSources else activeSources
        val categories = remember(visibleSources) { categoryGroups(visibleSources) }

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(top = 20.dp, start = 48.dp, end = 48.dp)
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
                                    if (focused) Color.White.copy(alpha = 0.18f)
                                    else SurfaceDark.copy(alpha = 0.72f),
                                    RoundedCornerShape(50)
                                )
                                .padding(horizontal = 17.dp, vertical = 11.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("بازگشت", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    Spacer(Modifier.width(18.dp))
                    Column(Modifier.weight(1f)) {
                        Text("پخش زنده", style = MaterialTheme.typography.displayMedium)
                        Text(
                            if (refreshState.running) {
                                "در حال بررسی کانال‌ها ${refreshState.completed} از ${refreshState.total}"
                            } else {
                                "شبکه‌های فعال آماده تماشا"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary
                        )
                    }
                    FocusableCard(
                        shape = RoundedCornerShape(50),
                        onClick = {
                            app.liveChannelHealthCoordinator.start(services, force = true)
                        }
                    ) { focused ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(
                                    if (focused) Color.White.copy(alpha = 0.18f)
                                    else SurfaceDark.copy(alpha = 0.72f),
                                    RoundedCornerShape(50)
                                )
                                .padding(horizontal = 18.dp, vertical = 12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (refreshState.running) "در حال بررسی" else "بررسی دوباره",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LiveTab(
                        label = "فعال",
                        count = activeSources.size,
                        selected = !showAll,
                        onClick = { showAll = false }
                    )
                    LiveTab(
                        label = "همه کانال‌ها",
                        count = uniqueSources.size,
                        selected = showAll,
                        onClick = { showAll = true }
                    )
                }
                Spacer(Modifier.height(20.dp))

                if (visibleSources.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            if (refreshState.running) {
                                "در حال بررسی شبکه‌ها..."
                            } else if (showAll) {
                                "هیچ شبکه زنده‌ای یافت نشد."
                            } else {
                                "هنوز کانال فعالی تأیید نشده؛ تب «همه کانال‌ها» را ببینید."
                            },
                            color = TextSecondary,
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(26.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        categories.forEach { (catName, catChannels) ->
                            item(key = "header:$catName") {
                                Text(
                                    text = catName,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 8.dp, start = 8.dp)
                                )
                            }
                            items(
                                items = catChannels.chunked(CHANNELS_PER_ROW),
                                key = { row -> "row:" + row.joinToString("|") { liveChannelKey(it) } }
                            ) { rowChannels ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    rowChannels.forEach { source ->
                                        LiveChannelCard(
                                            service = source.service,
                                            channel = source.channel,
                                            status = health[liveChannelKey(source)]?.status,
                                            onClick = { openLiveChannel(source) }
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

    private fun categoryGroups(sources: List<LiveChannelSource>): List<Pair<String, List<LiveChannelSource>>> {
        val categoryNames = linkedMapOf(
            "persian" to "شبکه‌های فارسی",
            "irib" to "شبکه‌های سراسری و صداوسیما",
            "sport" to "شبکه‌های ورزشی",
            "movies" to "فیلم و سرگرمی جهان",
            "documentary" to "مستند و دانستنی",
            "news" to "خبرهای جهان",
            "kids" to "کودک و خانواده",
            "other" to "سایر شبکه‌ها"
        )
        val grouped = sources.groupBy { source ->
            if (categoryNames.containsKey(source.channel.category)) source.channel.category else "other"
        }
        return categoryNames.mapNotNull { (id, label) -> grouped[id]?.let { label to it } }
    }

    @Composable
    private fun LiveTab(
        label: String,
        count: Int,
        selected: Boolean,
        onClick: () -> Unit
    ) {
        FocusableCard(
            modifier = Modifier,
            shape = RoundedCornerShape(50),
            onClick = onClick
        ) { focused ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(
                        when {
                            selected -> Color(0xFFE50914)
                            focused -> Color.White.copy(alpha = 0.16f)
                            else -> SurfaceDark.copy(alpha = 0.72f)
                        },
                        RoundedCornerShape(50)
                    )
                    .padding(horizontal = 22.dp, vertical = 12.dp)
            ) {
                Text(label, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text("$count", color = if (selected) Color.White else TextSecondary)
            }
        }
    }

    @Composable
    private fun LiveChannelCard(
        service: StreamingService,
        channel: LiveChannel,
        status: LiveHealthStatus?,
        onClick: () -> Unit
    ) {
        val logoModel = remember(channel.logoUrl) {
            ImageRequest.Builder(this)
                .data(channel.logoUrl)
                .size(320, 180)
                .crossfade(160)
                .build()
        }
        FocusableCard(
            modifier = Modifier.width(200.dp),
            shape = RoundedCornerShape(16.dp),
            onClick = onClick
        ) { focused ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (focused) Color.White.copy(alpha = 0.18f)
                        else SurfaceDark.copy(alpha = 0.72f),
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
                            model = logoModel,
                            contentDescription = channel.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().padding(22.dp)
                        )
                    } else {
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
                    text = service.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = when (status) {
                        LiveHealthStatus.ACTIVE -> "فعال"
                        LiveHealthStatus.INACTIVE -> "فعلاً فعال نیست"
                        LiveHealthStatus.UNKNOWN, null -> "در صف بررسی"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = when (status) {
                        LiveHealthStatus.ACTIVE -> Color(0xFF38D39F)
                        LiveHealthStatus.INACTIVE -> Color(0xFFFF8B8B)
                        LiveHealthStatus.UNKNOWN, null -> Color(0xFFFFC857)
                    },
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    private fun hideSystemUi() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUi()
    }

    private companion object {
        const val CHANNELS_PER_ROW = 4
    }
}
