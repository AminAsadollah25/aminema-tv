package com.amin.tvos.data

import android.app.Application
import android.view.View
import android.webkit.WebView
import com.amin.tvos.browser.LiveChannelHealthProbe
import com.amin.tvos.data.model.LiveHealthStatus
import com.amin.tvos.data.model.StreamingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Owns the Live TV scan for the process, while allowing the visible LiveTvActivity
 * to gate it. The coordinator survives Activity recreation, but scanning is stopped
 * whenever Live TV is no longer resumed so a hidden probe can never become a second
 * player behind Home, Spotlight or BrowserActivity.
 */
class LiveChannelHealthCoordinator(application: Application) {

    private val context = application
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val repository = LiveChannelHealthRepository(application)
    private var probe: LiveChannelHealthProbe? = null
    private var refreshJob: Job? = null
    private var scanningEnabled = false
    private var pendingServices: List<StreamingService>? = null

    val health = repository.health
    val refreshState = repository.refreshState

    /**
     * Health checks are only allowed while the Live TV screen is resumed. A hidden
     * WebView is still a real browser/player; leaving it scanning while another screen
     * is visible can consume the UI thread and, more importantly, create a second player.
     */
    fun setScanningEnabled(enabled: Boolean) {
        if (scanningEnabled == enabled) return
        scanningEnabled = enabled
        if (!enabled) {
            refreshJob?.cancel()
            refreshJob = null
            probe?.cancel()
            repository.setRefreshState(LiveHealthRefreshState())
            return
        }
        pendingServices?.let { services ->
            pendingServices = null
            start(services, force = false)
        }
    }

    fun start(services: List<StreamingService>, force: Boolean = false) {
        pendingServices = services
        if (!scanningEnabled) return
        if (refreshJob?.isActive == true) return
        val sources = liveChannelSources(services)
        val candidates = if (force) {
            sources
        } else {
            sources.filterNot { repository.isFresh(liveChannelKey(it)) }
        }
        if (candidates.isEmpty()) {
            repository.setRefreshState(LiveHealthRefreshState())
            return
        }

        refreshJob = scope.launch {
            repository.setRefreshState(
                LiveHealthRefreshState(running = true, completed = 0, total = candidates.size)
            )
            val channelProbe = probe ?: createProbe(context).also { probe = it }
            candidates.forEachIndexed { index, source ->
                if (!isActive || !scanningEnabled) return@launch
                val status = channelProbe.check(source)
                repository.record(liveChannelKey(source), status)
                repository.setRefreshState(
                    LiveHealthRefreshState(
                        running = true,
                        completed = index + 1,
                        total = candidates.size
                    )
                )
            }
            repository.setRefreshState(
                LiveHealthRefreshState(
                    running = false,
                    completed = candidates.size,
                    total = candidates.size
                )
            )
        }
    }

    fun close() {
        refreshJob?.cancel()
        scope.cancel()
        probe?.close()
        probe = null
    }

    private fun createProbe(application: Application): LiveChannelHealthProbe =
        LiveChannelHealthProbe(
            WebView(application).apply {
                // The WebView is intentionally not added to a visible Activity. It is kept
                // transparent and off-screen because it only performs page/player health checks.
                alpha = 0.01f
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        )
}
