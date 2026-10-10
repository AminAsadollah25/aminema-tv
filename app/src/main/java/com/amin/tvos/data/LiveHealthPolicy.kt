package com.amin.tvos.data

import com.amin.tvos.data.model.LiveChannelHealth
import com.amin.tvos.data.model.LiveHealthStatus

/** A hidden browser can confirm playback, but failure to observe it is not an outage. */
internal fun reconcileLiveHealth(
    previous: LiveChannelHealth?,
    observed: LiveHealthStatus,
    now: Long
): LiveChannelHealth = when {
    observed == LiveHealthStatus.ACTIVE -> LiveChannelHealth(observed, now, now)
    previous?.status == LiveHealthStatus.ACTIVE -> previous.copy(lastAttemptAt = now)
    else -> LiveChannelHealth(LiveHealthStatus.UNKNOWN, now, now)
}

/** Old INACTIVE records were produced by timeouts, not verified channel outages. */
internal fun normalizeStoredLiveHealth(value: LiveChannelHealth): LiveChannelHealth =
    if (value.status == LiveHealthStatus.INACTIVE) value.copy(status = LiveHealthStatus.UNKNOWN)
    else value

internal fun liveHealthIsFresh(value: LiveChannelHealth?, now: Long): Boolean {
    val timestamp = value?.let { maxOf(it.checkedAt, it.lastAttemptAt) } ?: return false
    return timestamp > 0L && now - timestamp in 0 until 30 * 60 * 1_000L
}

/** Sample different providers before treating repeated failures as a scanner problem. */
internal fun interleaveLiveSources(sources: List<LiveChannelSource>): List<LiveChannelSource> {
    val groups = sources.groupBy { it.service.id }.values.toList()
    return buildList {
        repeat(groups.maxOfOrNull { it.size } ?: 0) { index ->
            groups.forEach { group -> group.getOrNull(index)?.let { add(it) } }
        }
    }
}

internal class LiveScanFailureGuard {
    private var consecutiveUnknown = 0

    fun shouldPause(status: LiveHealthStatus): Boolean {
        consecutiveUnknown = if (status == LiveHealthStatus.ACTIVE) 0 else consecutiveUnknown + 1
        return consecutiveUnknown >= 3
    }
}
