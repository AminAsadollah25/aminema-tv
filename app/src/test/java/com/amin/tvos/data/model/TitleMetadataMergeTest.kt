package com.amin.tvos.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TitleMetadataMergeTest {

    @Test
    fun partialRefreshDoesNotEraseRicherCachedFields() {
        val cached = TitleMetadata(
            contentUrl = "https://example.com/title/1",
            summary = "خلاصه فارسی",
            year = "2022",
            directors = listOf(PersonRef("کارگردان")),
            cast = listOf(PersonRef("بازیگر")),
            imdbId = "tt1234567",
            externalLookupAt = 20L,
            fetchedAt = 30L
        )
        val partial = TitleMetadata(
            contentUrl = cached.contentUrl,
            rating = "8.3",
            fetchedAt = 40L
        )

        val merged = cached.mergePrefer(partial)

        assertEquals("خلاصه فارسی", merged.summary)
        assertEquals("کارگردان", merged.directors.single().name)
        assertEquals("بازیگر", merged.cast.single().name)
        assertEquals("8.3", merged.rating)
        assertEquals("tt1234567", merged.imdbId)
        assertEquals(40L, merged.fetchedAt)
    }

    @Test
    fun externalFallbackFillsOnlyMissingDecisionFields() {
        val provider = TitleMetadata(
            contentUrl = "https://example.com/title/2",
            summary = "خلاصه سرویس",
            year = "2026"
        )
        val fallback = TitleMetadata(
            contentUrl = provider.contentUrl,
            summary = "خلاصه ویکی‌پدیا",
            directors = listOf(PersonRef("کارگردان فارسی", "Q1")),
            cast = listOf(PersonRef("بازیگر فارسی", "Q2")),
            externalLookupAt = 100L
        )

        val merged = provider.mergePrefer(
            fallback.copy(summary = if (provider.summary.isBlank()) fallback.summary else "")
        )

        assertEquals("خلاصه سرویس", merged.summary)
        assertEquals("کارگردان فارسی", merged.directors.single().name)
        assertEquals("بازیگر فارسی", merged.cast.single().name)
        assertTrue(merged.isDecisionComplete())
    }
}
