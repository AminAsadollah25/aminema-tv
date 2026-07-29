package com.amin.tvos.data.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectPlayConfigTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun oldServiceConfigKeepsPlaybackPageAutoplayDisabled() {
        val config = json.decodeFromString<DirectPlayConfig>("{}")

        assertFalse(config.autoPlayOnPlaybackPage)
    }

    @Test
    fun providerCanExplicitlyEnablePlaybackPageAutoplay() {
        val config = json.decodeFromString<DirectPlayConfig>(
            """{"autoPlayOnPlaybackPage":true}"""
        )

        assertTrue(config.autoPlayOnPlaybackPage)
    }
}
