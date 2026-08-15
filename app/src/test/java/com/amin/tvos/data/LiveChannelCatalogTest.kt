package com.amin.tvos.data

import com.amin.tvos.data.model.LiveChannel
import com.amin.tvos.data.model.LiveChannelHealth
import com.amin.tvos.data.model.LiveHealthStatus
import com.amin.tvos.data.model.LiveTvConfig
import com.amin.tvos.data.model.StreamingService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveChannelCatalogTest {

    @Test
    fun activeDuplicateWinsOverAnUnverifiedCopy() {
        val sources = listOf(
            source("parsatv", "GEM River", "parsa-river"),
            source("parsiflix", "GEM River", "flx-river")
        )
        val health = mapOf(
            liveChannelKey(sources[0]) to LiveChannelHealth(LiveHealthStatus.UNKNOWN, 1L),
            liveChannelKey(sources[1]) to LiveChannelHealth(LiveHealthStatus.ACTIVE, 1L)
        )

        val result = deduplicateLiveChannels(sources, health)

        assertEquals("parsiflix", result.single().service.id)
        assertTrue(isLiveActive(result.single(), health))
    }

    @Test
    fun equalActiveDuplicatesPreferParsiFlixThenParsaTv() {
        val sources = listOf(
            source("babaktv", "GEM Series", "babak-series"),
            source("parsatv", "GEM Series", "parsa-series"),
            source("parsiflix", "GEM Series", "flx-series")
        )
        val health = sources.associate { liveChannelKey(it) to LiveChannelHealth(LiveHealthStatus.ACTIVE, 1L) }

        assertEquals("parsiflix", deduplicateLiveChannels(sources, health).single().service.id)
    }

    @Test
    fun namesWithDifferentSeparatorsAreRecognisedAsDuplicates() {
        val sources = listOf(
            source("parsatv", "GEM-Series", "series-a"),
            source("parsiflix", "gem series", "series-b")
        )

        assertEquals(1, deduplicateLiveChannels(sources, emptyMap()).size)
    }

    private fun source(serviceId: String, name: String, channelId: String) = LiveChannelSource(
        service = StreamingService(
            id = serviceId,
            name = serviceId,
            url = "https://example.test",
            liveTv = LiveTvConfig()
        ),
        channel = LiveChannel(
            id = channelId,
            name = name,
            path = "/channel/$channelId",
            logoUrl = ""
        )
    )
}
