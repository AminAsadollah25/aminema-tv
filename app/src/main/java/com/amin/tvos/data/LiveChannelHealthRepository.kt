package com.amin.tvos.data

import android.content.Context
import com.amin.tvos.data.model.LiveChannelHealth
import com.amin.tvos.data.model.LiveHealthStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class LiveHealthRefreshState(
    val running: Boolean = false,
    val completed: Int = 0,
    val total: Int = 0,
    val inconclusive: Boolean = false,
    val waitingForApp: Boolean = false,
    val silenceUnavailable: Boolean = false
)

/** Persists only channel health and timestamps; no page, media or token data is stored. */
class LiveChannelHealthRepository(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences(
        "live_channel_health",
        Context.MODE_PRIVATE
    )
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()
    private val _health = MutableStateFlow(loadHealth())
    private val _refreshState = MutableStateFlow(LiveHealthRefreshState())

    val health: StateFlow<Map<String, LiveChannelHealth>> = _health.asStateFlow()
    val refreshState: StateFlow<LiveHealthRefreshState> = _refreshState.asStateFlow()

    suspend fun record(key: String, status: LiveHealthStatus, now: Long = System.currentTimeMillis()) {
        mutex.withLock {
            val next = _health.value.toMutableMap().apply {
                this[key] = reconcileLiveHealth(this[key], status, now)
            }.toMap()
            withContext(Dispatchers.IO) {
                preferences.edit()
                    .putString(HEALTH_KEY, json.encodeToString(next))
                    .apply()
            }
            // Publish under the same lock as persistence so an older probe result
            // cannot overwrite a concurrently confirmed user playback.
            _health.value = next
        }
    }

    fun isFresh(key: String, now: Long = System.currentTimeMillis()): Boolean =
        liveHealthIsFresh(_health.value[key], now)

    fun setRefreshState(state: LiveHealthRefreshState) {
        _refreshState.value = state
    }

    private fun loadHealth(): Map<String, LiveChannelHealth> = runCatching {
        json.decodeFromString<Map<String, LiveChannelHealth>>(
            preferences.getString(HEALTH_KEY, "{}").orEmpty()
        ).mapValues { (_, value) -> normalizeStoredLiveHealth(value) }
    }.getOrDefault(emptyMap())

    private companion object {
        const val HEALTH_KEY = "channel_health_v1"
    }
}
