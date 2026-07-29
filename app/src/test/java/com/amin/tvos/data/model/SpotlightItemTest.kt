package com.amin.tvos.data.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpotlightItemTest {
    private val json = Json { encodeDefaults = true }

    @Test
    fun activityPayloadPreservesExactContinueRequest() {
        val original = SpotlightItem(
            title = "نمونه سریال",
            kind = CatalogKind.SERIES,
            contentUrl = "https://example.test/post/series/example",
            posterUrl = "https://example.test/poster.webp",
            serviceId = "example",
            episodeLabel = "قسمت ۴ فصل سوم",
            primaryAction = SpotlightAction.CONTINUE,
            browserStartUrl = "https://example.test/watch/example",
            resumePosition = 125_000L,
            duration = 2_700_000L,
            autoResume = true,
            resumeStrategy = ResumeStrategy.OPEN_PLAYBACK_PAGE,
            actionButtonTextPatterns = listOf("پخش آنلاین"),
            directors = listOf(PersonRef("نمونه کارگردان", providerId = "42")),
            cast = listOf(PersonRef("نمونه بازیگر"))
        )

        val restored = json.decodeFromString<SpotlightItem>(json.encodeToString(original))

        assertEquals(original, restored)
        assertTrue(restored.autoResume)
        assertEquals(SpotlightAction.CONTINUE, restored.primaryAction)
        assertEquals("نمونه کارگردان", restored.directors.single().name)
    }

    @Test
    fun catalogMovieDefaultsToNormalTitlePageBeforeDirectPlay() {
        val item = SpotlightItem(
            title = "نمونه فیلم",
            kind = CatalogKind.MOVIE,
            contentUrl = "https://example.test/post/film/example",
            serviceId = "example",
            directPlay = true
        )

        assertEquals(item.contentUrl, item.browserStartUrl)
        assertEquals(SpotlightAction.WATCH, item.primaryAction)
    }

    @Test
    fun detailMetadataCompletesOldRecentWithoutChangingPlaybackRequest() {
        val oldRecent = SpotlightItem(
            title = "Spider Man Homecoming",
            kind = CatalogKind.MOVIE,
            contentUrl = "https://example.test/post/film/spider-man",
            serviceId = "example",
            directPlay = true,
            browserStartUrl = "https://example.test/post/film/spider-man"
        )
        val metadata = TitleMetadata(
            contentUrl = oldRecent.contentUrl,
            summary = "یک معرفی کوتاه و بدون اسپویل.",
            year = "2017",
            genres = listOf("اکشن"),
            rating = "7.4",
            runtime = "133 دقیقه",
            country = "آمریکا",
            language = "انگلیسی",
            hasPersianDub = true,
            directors = listOf(PersonRef("Jon Watts")),
            cast = listOf(PersonRef("Tom Holland"))
        )

        val enriched = oldRecent.withMetadata(metadata)

        assertEquals("آمریکا", enriched.country)
        assertEquals("2017", enriched.year)
        assertEquals("7.4", enriched.rating)
        assertTrue(enriched.hasPersianDub)
        assertEquals("Jon Watts", enriched.directors.single().name)
        assertEquals(oldRecent.browserStartUrl, enriched.browserStartUrl)
        assertEquals(oldRecent.directPlay, enriched.directPlay)
    }

    @Test
    fun metadataPayloadPreservesPersianAvailabilityBadges() {
        val metadata = TitleMetadata(
            contentUrl = "https://example.test/post/film/example",
            hasPersianDub = true,
            hasPersianSubtitle = true
        )

        val restored = json.decodeFromString<TitleMetadata>(json.encodeToString(metadata))

        assertTrue(restored.hasPersianDub)
        assertTrue(restored.hasPersianSubtitle)
    }
}
