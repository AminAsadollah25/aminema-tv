package com.amin.tvos.data

import com.amin.tvos.data.model.CatalogItem
import com.amin.tvos.data.model.CatalogKind
import com.amin.tvos.data.model.SearchResult
import org.junit.Assert.assertEquals
import org.junit.Test

class IranianCatalogMergeTest {
    private fun item(name: String, source: String, id: String = "", kind: CatalogKind = CatalogKind.MOVIE) =
        CatalogItem(name, kind, "https://example.test/$source/$name", serviceId = source, imdbId = id)

    @Test fun primaryWinsEvenWhenMatchingLaterInItsList() {
        val primary = listOf(item("اول", "parsiflix"), item("فیلم", "parsiflix", "tt123"))
        val secondary = listOf(item("Film", "farsiland", "tt123"), item("دیگر", "farsiland"))
        val result = IranianCatalogMerge.latest(primary, secondary)
        assertEquals(listOf("اول", "دیگر", "فیلم"), result.map { it.title })
        assertEquals("parsiflix", result.last().serviceId)
    }

    @Test fun ambiguousTransliterationsAndDifferentKindsRemainSeparate() {
        val result = IranianCatalogMerge.latest(
            listOf(item("سرخ و سیاه", "parsiflix")),
            listOf(item("Sorkho Siah", "farsiland"), item("سرخ و سیاه", "farsiland", kind = CatalogKind.SERIES))
        )
        assertEquals(3, result.size)
    }

    @Test fun persianArabicLetterVariantsMatchWithoutChangingProviderUrl() {
        val a = item("كيان", "parsiflix").copy(year = "2025")
        assertEquals(listOf(a), IranianCatalogMerge.latest(listOf(a), listOf(item("کیان", "farsiland").copy(year = "2025"))))
        assertEquals(listOf(a), IranianCatalogMerge.latest(listOf(a), listOf(item("کیان", "farsiland").copy(year = "۲۰۲۵"))))
        assertEquals(2, IranianCatalogMerge.latest(listOf(a), listOf(item("کیان", "farsiland").copy(year = "2024"))).size)
    }

    @Test fun eitherProviderCanBeUnavailable() {
        val a = item("تست", "farsiland")
        assertEquals(listOf(a), IranianCatalogMerge.latest(emptyList(), listOf(a)))
    }

    @Test fun verifiedPersianAndFinglishPagesCollapseToPrimaryInHome() {
        val primary = item("نیم‌رخ", "parsiflix", kind = CatalogKind.SERIES)
            .copy(contentUrl = "https://app.parsiflix.com/medias/series/377")
        val secondary = item("NimRokh", "farsiland", kind = CatalogKind.SERIES)
            .copy(contentUrl = "https://farsiland.com/tvshows/nimrokh/")
        assertEquals(listOf(primary), IranianCatalogMerge.latest(listOf(primary), listOf(secondary)))
        assertEquals(listOf(secondary), IranianCatalogMerge.latest(emptyList(), listOf(secondary)))
        assertEquals(2, IranianCatalogMerge.latest(
            listOf(primary), listOf(secondary.copy(contentUrl = "https://evil.test/tvshows/nimrokh/"))
        ).size)
        assertEquals(2, IranianCatalogMerge.latest(
            listOf(primary), listOf(secondary.copy(kind = CatalogKind.MOVIE))
        ).size)
    }

    @Test fun searchPreservesBothVerifiedSourcesAndPrefersParsiflixRegardlessOfOrder() {
        val p = SearchResult("نیم‌رخ", CatalogKind.SERIES, "https://app.parsiflix.com/medias/series/377", "", "parsiflix")
        val f = SearchResult("NimRokh", CatalogKind.SERIES, "https://farsiland.com/tvshows/nimrokh/", "", "farsiland", hasPersianDub = true)
        val result = CanonicalLibrary.fromSearchResults(listOf(f, p))
        assertEquals(1, result.size)
        assertEquals("parsiflix", result.single().representative.serviceId)
        assertEquals(2, result.single().variants.size)
    }

    @Test fun verifiedPairDoesNotOverrideConflictingPublicIdsInSearch() {
        val p = SearchResult("نیم‌رخ", CatalogKind.SERIES, "https://app.parsiflix.com/medias/series/377", "", "parsiflix", imdbId = "tt123")
        val f = SearchResult("NimRokh", CatalogKind.SERIES, "https://farsiland.com/tvshows/nimrokh/", "", "farsiland", imdbId = "tt456")
        assertEquals(2, CanonicalLibrary.fromSearchResults(listOf(f, p)).size)
    }

    @Test fun verifiedPairDoesNotHideConflictingPublicIdsAtHome() {
        val p = item("نیم‌رخ", "parsiflix", "tt123", CatalogKind.SERIES)
            .copy(contentUrl = "https://app.parsiflix.com/medias/series/377")
        val f = item("NimRokh", "farsiland", "tt456", CatalogKind.SERIES)
            .copy(contentUrl = "https://farsiland.com/tvshows/nimrokh/")
        assertEquals(2, IranianCatalogMerge.latest(listOf(p), listOf(f)).size)
    }
}
