package com.amin.tvos.ui.home

import org.junit.Assert.*
import org.junit.Test

class JourneyArtworkTest {
    @Test fun everyGroupHasStableColoredArtworkEvenWithoutImage() {
        listOf("action","comic","horror","fantasy","scifi","animation","iran").forEach {group ->
            val colors=journeyColors("missing-poster",group)
            assertEquals(colors,journeyColors("missing-poster",group))
            assertNotEquals(colors.base,colors.accent)
            assertTrue(colors.accent.red+colors.accent.green+colors.accent.blue>.6f)
        }
    }
    @Test fun differentIdentitiesDoNotDependOnTheSameFlatBlackPlaceholder() {
        assertNotEquals(journeyColors("matrix","scifi"),journeyColors("terminator","scifi"))
        assertNotEquals(journeyColors("bond","action"),journeyColors("madmax","action"))
    }
}
