package com.amin.tvos.data

import com.amin.tvos.data.model.CatalogItem
import com.amin.tvos.data.model.CatalogKind
import com.amin.tvos.data.model.CatalogSection
import com.amin.tvos.data.model.TitleMetadata
import org.junit.Assert.*
import org.junit.Test

class GenreDiscoveryTest {
    private fun item(genres: List<String> = emptyList()) = CatalogItem(
        title = "Example", kind = CatalogKind.MOVIE,
        contentUrl = "https://provider.test/post/film/1/example", serviceId = "filmrooz", genres = genres)

    @Test fun matchesRealGenreAliasesNotSynopsisOrSubstring() {
        assertTrue(GenreDiscovery.matches(DiscoveryCategory.HORROR, item(listOf("Horror"))))
        assertTrue(GenreDiscovery.matches(DiscoveryCategory.SCI_FI, item(listOf("Sci-Fi"))))
        assertTrue(GenreDiscovery.matches(DiscoveryCategory.SCI_FI, item(listOf("علمی‌تخیلی"))))
        assertTrue(GenreDiscovery.matches(DiscoveryCategory.COMEDY, item(listOf("كمدي"))))
        assertFalse(GenreDiscovery.matches(DiscoveryCategory.HORROR, item(listOf("horror comedy unknown"))))
        assertFalse(GenreDiscovery.matches(DiscoveryCategory.HORROR, item().copy(summary = "Horror")))
    }
    @Test fun missingGenreDoesNotBecomeFamilySafe() {
        assertFalse(GenreDiscovery.matches(DiscoveryCategory.FAMILY, item(listOf("Comedy", "Animation"))))
        assertFalse(GenreDiscovery.matches(DiscoveryCategory.FAMILY, item()))
        assertTrue(GenreDiscovery.matches(DiscoveryCategory.FAMILY, item(listOf("Family"))))
    }
    @Test fun classicUsesKnownCalendarNotUploadYear() {
        assertTrue(GenreDiscovery.matches(DiscoveryCategory.CLASSIC, item().copy(year = "1999")))
        assertFalse(GenreDiscovery.matches(DiscoveryCategory.CLASSIC, item().copy(year = "2001")))
        assertFalse(GenreDiscovery.matches(DiscoveryCategory.CLASSIC, item()))
        val iranian = item().copy(serviceId = "parsiflix", year = "2026")
        assertTrue(GenreDiscovery.matches(DiscoveryCategory.CLASSIC, iranian,
            TitleMetadata(iranian.contentUrl, year = "۱۳۷۲")))
        assertFalse(GenreDiscovery.matches(DiscoveryCategory.CLASSIC, iranian))
    }
    @Test fun marathonContainsOnlySeriesAndAtlasIdentityIsUnique() {
        assertFalse(GenreDiscovery.matches(DiscoveryCategory.MARATHON, item()))
        assertTrue(GenreDiscovery.matches(DiscoveryCategory.MARATHON, item().copy(kind = CatalogKind.SERIES)))
        assertEquals((0..11).toList(), DiscoveryCategory.entries.map { it.atlasIndex })
    }
    @Test fun selectionUsesMetadataAndDeduplicatesWithoutChangingArchive() {
        val film = item()
        val section = CatalogSection("filmrooz", all = listOf(film), movies = listOf(film))
        val metadata = mapOf(ContentMetadataPolicy.canonicalContentUrl(film.contentUrl) to
            TitleMetadata(film.contentUrl, genres = listOf("Romance")))
        assertEquals(1, GenreDiscovery.select(DiscoveryCategory.ROMANCE, listOf(section), metadata).size)
        assertTrue(GenreDiscovery.select(DiscoveryCategory.HORROR, listOf(section), metadata).isEmpty())
        assertTrue(section.movies.single().genres.isEmpty())
    }
}
