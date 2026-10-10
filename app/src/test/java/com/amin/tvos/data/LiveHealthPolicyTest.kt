package com.amin.tvos.data

import com.amin.tvos.data.model.LiveChannel
import com.amin.tvos.data.model.LiveChannelHealth
import com.amin.tvos.data.model.LiveHealthStatus
import com.amin.tvos.data.model.StreamingService
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class LiveHealthPolicyTest {
    @Test fun timeoutWithoutPreviousSuccessIsUnknown() {
        val result = reconcileLiveHealth(null, LiveHealthStatus.UNKNOWN, 100L)
        assertEquals(LiveHealthStatus.UNKNOWN, result.status)
        assertEquals(100L, result.lastAttemptAt)
    }

    @Test fun unsuccessfulObservationIsNotProofOfOutage() {
        assertEquals(LiveHealthStatus.UNKNOWN,
            reconcileLiveHealth(null, LiveHealthStatus.INACTIVE, 100L).status)
    }

    @Test fun timeoutPreservesLastSuccessAndItsOriginalTimestamp() {
        val previous = LiveChannelHealth(LiveHealthStatus.ACTIVE, 100L)
        val next = reconcileLiveHealth(previous, LiveHealthStatus.UNKNOWN, 200L)
        assertEquals(LiveHealthStatus.ACTIVE, next.status)
        assertEquals(100L, next.checkedAt)
        assertEquals(200L, next.lastAttemptAt)
    }

    @Test fun actualPlaybackRehabilitatesAnOldInactiveRecord() {
        val next = reconcileLiveHealth(LiveChannelHealth(LiveHealthStatus.INACTIVE, 1L),
            LiveHealthStatus.ACTIVE, 200L)
        assertEquals(LiveChannelHealth(LiveHealthStatus.ACTIVE, 200L, 200L), next)
    }

    @Test fun legacyTimeoutRecordIsReinterpretedWithoutDeletingIt() {
        val old = Json.decodeFromString<LiveChannelHealth>("""{"status":"INACTIVE","checkedAt":123}""")
        val next = normalizeStoredLiveHealth(old)
        assertEquals(LiveHealthStatus.UNKNOWN, next.status)
        assertEquals(123L, next.checkedAt)
    }

    @Test fun legacySuccessRemainsUnchanged() {
        val old = LiveChannelHealth(LiveHealthStatus.ACTIVE, 123L)
        assertEquals(old, normalizeStoredLiveHealth(old))
    }

    @Test fun addedAttemptTimestampRoundTrips() {
        val value = LiveChannelHealth(LiveHealthStatus.ACTIVE, 100L, 200L)
        assertEquals(value, Json.decodeFromString<LiveChannelHealth>(Json.encodeToString(value)))
    }

    @Test fun freshnessUsesLatestAttemptWithoutFabricatingNewSuccess() {
        val value = LiveChannelHealth(LiveHealthStatus.ACTIVE, 1L, 2_000_000L)
        assertTrue(liveHealthIsFresh(value, 2_000_100L))
        assertFalse(liveHealthIsFresh(value, 3_800_000L))
        assertFalse(liveHealthIsFresh(value, 1_999_999L))
        assertFalse(liveHealthIsFresh(null, 100L))
    }

    @Test fun threeInconclusiveChecksPauseTheScan() {
        val guard = LiveScanFailureGuard()
        assertFalse(guard.shouldPause(LiveHealthStatus.UNKNOWN))
        assertFalse(guard.shouldPause(LiveHealthStatus.UNKNOWN))
        assertTrue(guard.shouldPause(LiveHealthStatus.UNKNOWN))
    }

    @Test fun confirmedPlaybackResetsTheFailureStreak() {
        val guard = LiveScanFailureGuard()
        repeat(2) { assertFalse(guard.shouldPause(LiveHealthStatus.UNKNOWN)) }
        assertFalse(guard.shouldPause(LiveHealthStatus.ACTIVE))
        repeat(2) { assertFalse(guard.shouldPause(LiveHealthStatus.UNKNOWN)) }
        assertTrue(guard.shouldPause(LiveHealthStatus.UNKNOWN))
    }

    @Test fun firstChecksSampleProvidersWithoutLosingOrDuplicatingChannels() {
        fun source(provider: String, id: String) = LiveChannelSource(
            StreamingService(id = provider, name = provider, url = "https://example.test"),
            LiveChannel(id = id, name = id, path = "/$id", logoUrl = "")
        )
        val sources = listOf(source("a", "1"), source("a", "2"), source("a", "3"),
            source("b", "1"), source("b", "2"), source("c", "1"))
        val ordered = interleaveLiveSources(sources)
        assertEquals(listOf("a:1", "b:1", "c:1", "a:2", "b:2", "a:3"), ordered.map(::liveChannelKey))
        assertEquals(sources.toSet(), ordered.toSet())
        assertTrue(interleaveLiveSources(emptyList()).isEmpty())
    }
}
