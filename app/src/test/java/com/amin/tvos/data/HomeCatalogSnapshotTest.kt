package com.amin.tvos.data

import com.amin.tvos.data.model.CatalogItem
import com.amin.tvos.data.model.CatalogKind
import com.amin.tvos.data.model.CatalogSection
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class HomeCatalogSnapshotTest {
    private fun items(kind: CatalogKind, size: Int) = (0 until size).map {
        CatalogItem(title = "Title $it", kind = kind, serviceId = "provider",
            contentUrl = "https://provider.test/${kind.name}/$it",
            posterUrl = "https://provider.test/poster/$it.jpg", year = "2026")
    }

    @Test fun projectionIsBoundedWithoutMutatingFullArchive() {
        val movies = items(CatalogKind.MOVIE, 100)
        val series = items(CatalogKind.SERIES, 100)
        val full = CatalogSection(serviceId = "provider", all = movies + series,
            movies = movies, series = series, popularSeries = series, featured = movies,
            syncedAt = 123L, loadedPageLimit = 10, error = "stale")
        val home = compactHomeSections(listOf(full)).single()
        assertEquals(24, home.all.size)
        assertEquals(24, home.movies.size)
        assertEquals(24, home.series.size)
        assertEquals(12, home.popularSeries.size)
        assertEquals(6, home.featured.size)
        assertEquals(full.movies.take(24), home.movies)
        assertEquals(full.series.first(), home.series.first())
        assertEquals(200, full.all.size)
        assertEquals(full.syncedAt, home.syncedAt)
        assertEquals(full.loadedPageLimit, home.loadedPageLimit)
        assertEquals(full.error, home.error)
    }

    @Test fun staleOrUnsupportedSnapshotsAreRejected() {
        val cache = HomeCatalogSnapshot(sourceLength = 100, sourceModified = 123, sections = emptyList())
        assertTrue(cache.matchesSource(100, 123))
        assertFalse(cache.matchesSource(101, 123))
        assertFalse(cache.matchesSource(100, 124))
        assertFalse(cache.copy(schemaVersion = 2).matchesSource(100, 123))
    }

    @Test fun serializationPreservesOrdinaryMetadataAndProviderOrder() {
        val sections = listOf(CatalogSection(serviceId = "first", movies = items(CatalogKind.MOVIE, 2)),
            CatalogSection(serviceId = "second", series = items(CatalogKind.SERIES, 2)))
        val cache = HomeCatalogSnapshot(sourceLength = 300, sourceModified = 456,
            sections = compactHomeSections(sections))
        assertEquals(cache, Json.decodeFromString<HomeCatalogSnapshot>(Json.encodeToString(cache)))
        assertEquals(listOf("first", "second"), cache.sections.map { it.serviceId })
    }

    @Test fun optimizedHexRetainsExistingCanonicalIdentity() {
        val item = items(CatalogKind.MOVIE, 1).single().copy(year = "", imdbId = "")
        val expected = java.security.MessageDigest.getInstance("SHA-1")
            .digest(ContentMetadataPolicy.canonicalContentUrl(item.contentUrl).toByteArray())
            .joinToString("") { "%02x".format(it) }
        assertEquals("aminema:$expected", CanonicalLibrary.canonicalize(listOf(item)).single().canonicalId)
    }
}
