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

/** One application-owned, natively muted probe. Screen navigation never resets its queue. */
class LiveChannelHealthCoordinator(application: Application) {

    private val context = application
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val repository = LiveChannelHealthRepository(application)
    private var probe: LiveChannelHealthProbe? = null
    private var refreshJob: Job? = null
    private var appVisible = true
    private var queue: List<LiveChannelSource> = emptyList()
    private var nextIndex = 0
    private var failureGuard = LiveScanFailureGuard()

    val health = repository.health
    val refreshState = repository.refreshState

    /** Pause only when the entire app leaves the foreground, not when a screen changes. */
    fun setAppVisible(visible: Boolean) {
        if (appVisible == visible) return
        appVisible = visible
        if (!visible) {
            refreshJob?.cancel()
            refreshJob = null
            probe?.cancel()
            if (nextIndex < queue.size && !refreshState.value.inconclusive &&
                !refreshState.value.silenceUnavailable) {
                repository.setRefreshState(refreshState.value.copy(running = false, waitingForApp = true))
            }
        } else if (refreshState.value.waitingForApp) {
            drainQueue()
        }
    }

    fun start(services: List<StreamingService>, force: Boolean = false) {
        if (refreshJob?.isActive == true) return
        // Recreating LiveTvActivity must not restart or discard a pending run.
        if (!force && nextIndex < queue.size) return
        val sources = interleaveLiveSources(liveChannelSources(services))
        queue = if (force) sources else sources.filterNot { repository.isFresh(liveChannelKey(it)) }
        nextIndex = 0
        failureGuard = LiveScanFailureGuard()
        if (queue.isEmpty()) {
            repository.setRefreshState(LiveHealthRefreshState())
        } else {
            drainQueue()
        }
    }

    private fun drainQueue() {
        if (!appVisible) {
            repository.setRefreshState(LiveHealthRefreshState(
                completed = nextIndex, total = queue.size, waitingForApp = true
            ))
            return
        }
        refreshJob = scope.launch {
            val channelProbe = probe ?: createProbe(context).also { probe = it }
            if (!channelProbe.ensureSilent()) {
                repository.setRefreshState(LiveHealthRefreshState(
                    completed = nextIndex, total = queue.size, silenceUnavailable = true
                ))
                return@launch
            }
            repository.setRefreshState(LiveHealthRefreshState(
                running = true, completed = nextIndex, total = queue.size
            ))
            while (nextIndex < queue.size && isActive && appVisible) {
                val source = queue[nextIndex]
                val status = channelProbe.check(source)
                repository.record(liveChannelKey(source), status)
                nextIndex += 1
                val inconclusive = failureGuard.shouldPause(status)
                repository.setRefreshState(LiveHealthRefreshState(
                    running = nextIndex < queue.size && !inconclusive,
                    completed = nextIndex,
                    total = queue.size,
                    inconclusive = inconclusive
                ))
                if (inconclusive) return@launch
                // Yield between channels; never open several players in parallel.
                kotlinx.coroutines.delay(1_000L)
            }
        }
    }

    /** Real user playback is stronger evidence than an off-screen probe. */
    fun recordPlayback(key: String) {
        scope.launch { repository.record(key, LiveHealthStatus.ACTIVE) }
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
