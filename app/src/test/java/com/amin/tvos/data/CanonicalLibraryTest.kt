package com.amin.tvos.data

import com.amin.tvos.data.model.CanonicalMatchConfidence
import com.amin.tvos.data.model.CatalogItem
import com.amin.tvos.data.model.CatalogKind
import com.amin.tvos.data.model.PersonRef
import com.amin.tvos.data.model.SearchResult
import com.amin.tvos.data.model.TitleMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CanonicalLibraryTest {

    @Test
    fun persianSpacingAndArabicLettersNormalizeToOneIdentity() {
        assertEquals(
            CanonicalText.normalizeTitle("لیسانسه‌ها"),
            CanonicalText.normalizeTitle("ليسانسه ها")
        )
    }

    @Test
    fun punctuationAndSpacingDoNotBreakLatinSearchIdentity() {
        assertEquals(
            CanonicalText.normalizeTitle("Spider-Man"),
            CanonicalText.normalizeTitle("spiderman")
        )
    }

    @Test
    fun visibleTrailingYearIsSeparatedFromSearchTitle() {
        assertEquals("لیسانسه ها", CanonicalText.displayTitle("لیسانسه ها (2016)"))
        assertEquals("2016", CanonicalText.extractYear("لیسانسه ها (۲۰۱۶)"))
    }

    @Test
    fun titleNumberWithoutYearBracketsIsNotRemoved() {
        assertEquals("Blade Runner 2049", CanonicalText.displayTitle("Blade Runner 2049"))
        assertEquals("", CanonicalText.extractYear("Blade Runner 2049"))
    }

    @Test
    fun exactImdbAndKindMergeAcrossProviders() {
        val first = item("A Title", "https://one.test/title/1", "one")
        val second = item("عنوان دیگر", "https://two.test/title/8", "two")
        val metadata = mapOf(
            first.contentUrl to TitleMetadata(first.contentUrl, imdbId = "tt1234567"),
            second.contentUrl to TitleMetadata(second.contentUrl, imdbId = "tt1234567")
        )

        val result = CanonicalLibrary.canonicalize(listOf(first, second), metadata)

        assertEquals(1, result.size)
        assertEquals(2, result.single().variants.size)
        assertEquals(CanonicalMatchConfidence.IMDb, result.single().matchConfidence)
    }

    @Test
    fun titleOnlyNeverAutoMerges() {
        val first = item("The Office", "https://one.test/title/1", "one")
        val second = item("The Office", "https://two.test/title/2", "two")

        val result = CanonicalLibrary.canonicalize(listOf(first, second))

        assertEquals(2, result.size)
        assertNotEquals(result[0].canonicalId, result[1].canonicalId)
    }

    @Test
    fun conflictingYearsStaySeparateEvenWhenTitlesMatch() {
        val first = item("Same Name", "https://one.test/title/1", "one", year = "2019")
        val second = item("Same Name", "https://two.test/title/2", "two", year = "2024")

        assertEquals(2, CanonicalLibrary.canonicalize(listOf(first, second)).size)
    }

    @Test
    fun sameProviderPagesDoNotMergeFromTitleAndYearAlone() {
        val first = item("Same Name", "https://one.test/title/1", "one", year = "2024")
        val second = item("Same Name", "https://one.test/title/2", "one", year = "2024")

        assertEquals(2, CanonicalLibrary.canonicalize(listOf(first, second)).size)
    }

    @Test
    fun verifiedLicenseesExampleMergesByTitleKindAndCredits() {
        val parsiUrl = "https://app.parsiflix.com/medias/series/338"
        val filmRoozUrl = "https://sean.robert-redford.net/post/series/56308/لیسانسه-ها"
        val searchResults = listOf(
            SearchResult(
                title = "لیسانسه ها",
                kind = CatalogKind.SERIES,
                contentUrl = parsiUrl,
                posterUrl = "https://images.test/parsi.webp",
                serviceId = "parsiflix"
            ),
            SearchResult(
                title = "لیسانسه ها (2016)",
                kind = CatalogKind.SERIES,
                contentUrl = filmRoozUrl,
                posterUrl = "https://images.test/filmrooz.webp",
                serviceId = "filmrooz"
            )
        )
        val metadata = mapOf(
            parsiUrl to TitleMetadata(
                contentUrl = parsiUrl,
                directors = listOf(PersonRef("سروش صحت")),
                cast = listOf(PersonRef("هوتن شکیبا"), PersonRef("امیر کاظمی")),
                imdbId = "tt9191330"
            ),
            filmRoozUrl to TitleMetadata(
                contentUrl = filmRoozUrl,
                year = "2016",
                directors = listOf(PersonRef("سروش صحت")),
                cast = listOf(PersonRef("هوتن شکیبا"), PersonRef("امیر کاظمی"))
            )
        )

        val result = CanonicalLibrary.fromSearchResults(
            searchResults,
            metadata,
            mapOf("parsiflix" to "فیلم ایرانی", "filmrooz" to "فیلم خارجی")
        )

        assertEquals(1, result.size)
        assertEquals(2, result.single().variants.size)
        assertEquals("tt9191330", result.single().imdbId)
        assertEquals("2016", result.single().year)
        assertEquals(
            CanonicalMatchConfidence.TITLE_CREDITS_KIND,
            result.single().matchConfidence
        )
        assertTrue(result.single().canonicalId.startsWith("aminema:"))
    }

    private fun item(
        title: String,
        url: String,
        serviceId: String,
        year: String = ""
    ) = CatalogItem(
        title = title,
        kind = CatalogKind.SERIES,
        contentUrl = url,
        serviceId = serviceId,
        year = year
    )
}
