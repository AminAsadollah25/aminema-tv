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
    val total: Int = 0
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
        val updated = mutex.withLock {
            val next = _health.value.toMutableMap().apply {
                this[key] = LiveChannelHealth(status, now)
            }.toMap()
            withContext(Dispatchers.IO) {
                preferences.edit()
                    .putString(HEALTH_KEY, json.encodeToString(next))
                    .apply()
            }
            next
        }
        _health.value = updated
    }

    fun isFresh(key: String, now: Long = System.currentTimeMillis()): Boolean =
        _health.value[key]?.checkedAt?.let { now - it < FRESH_FOR_MS } == true

    fun setRefreshState(state: LiveHealthRefreshState) {
        _refreshState.value = state
    }

    private fun loadHealth(): Map<String, LiveChannelHealth> = runCatching {
        json.decodeFromString<Map<String, LiveChannelHealth>>(
            preferences.getString(HEALTH_KEY, "{}").orEmpty()
        )
    }.getOrDefault(emptyMap())

    private companion object {
        const val HEALTH_KEY = "channel_health_v1"
        const val FRESH_FOR_MS = 30 * 60 * 1_000L
    }
}
