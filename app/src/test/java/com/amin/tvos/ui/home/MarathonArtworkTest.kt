package com.amin.tvos.ui.home

import com.amin.tvos.data.JourneyCatalog
import org.junit.Assert.*
import org.junit.Test

class MarathonArtworkTest {
    @Test fun everyMarathonHasItsOwnLocalArtworkAndUnknownHasSafeFallback() {
        assertEquals(JourneyCatalog.marathonCollections.map { it.id }.toSet(), MarathonArtwork.resources.keys)
        assertEquals(12, MarathonArtwork.resources.values.toSet().size)
        assertTrue(MarathonArtwork.resources.values.all { it != 0 })
        assertNull(MarathonArtwork.forCollection("unknown-future-collection"))
        assertFalse(MarathonArtwork.resources.keys.any { id -> JourneyCatalog.marvelCollections.any { it.id == id } })
    }
}
