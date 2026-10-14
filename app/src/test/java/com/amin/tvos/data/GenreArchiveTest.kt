package com.amin.tvos.data

import org.junit.Assert.*
import org.junit.Test

class GenreArchiveTest {
    private val origin = "https://mymoviz.co/"
    @Test fun allTenGenresHaveDistinctKeysAndPreserveGenreOnNextPage() {
        assertEquals(10, ArchiveGenre.entries.size)
        val keys = mutableSetOf<String>()
        for (genre in ArchiveGenre.entries) for (type in ArchiveType.entries) for (sort in ArchiveSort.entries) {
            val q = GenreArchiveQuery(genre = genre, type = type, sort = sort)
            assertTrue(keys.add(q.key))
            assertTrue(q.acceptsPage(q.pageUrl(origin, 2), origin, 2))
            assertFalse(q.acceptsPage(q.pageUrl(origin, 2), origin, 1))
            val other = ArchiveGenre.entries.first { it != genre }
            assertFalse(q.copy(genre = other).acceptsPage(q.pageUrl(origin, 2), origin, 2))
        }
        assertEquals(40, keys.size)
        assertEquals("science-fiction", ArchiveGenre.SCI_FI.parameter)
        assertNull(ArchiveGenre.forCategory(DiscoveryCategory.CLASSIC))
        assertNull(ArchiveGenre.forCategory(DiscoveryCategory.MARATHON))
    }
    @Test fun verifiedFilterContractAndQueryKeysAreSeparate() {
        val query = GenreArchiveQuery()
        assertEquals("https://mymoviz.co/search?genre=romance&type=movie&sort=latest", query.pageUrl(origin, 1))
        assertNotEquals(query.key, query.copy(sort = ArchiveSort.IMDB).key)
        assertNotEquals(query.key, query.copy(type = ArchiveType.SERIES).key)
    }
    @Test fun followsOnlyNextPageWithSameFilterAndOrigin() {
        val query = GenreArchiveQuery(sort = ArchiveSort.IMDB)
        assertTrue(query.acceptsPage("https://mymoviz.co/search?genre=romance&sort=imdb&type=movie&p=2", origin, 2))
        assertFalse(query.acceptsPage("https://evil.test/search?genre=romance&sort=imdb&type=movie&p=2", origin, 2))
        assertFalse(query.acceptsPage("https://mymoviz.co/search?genre=horror&sort=imdb&type=movie&p=2", origin, 2))
        assertFalse(query.acceptsPage("https://mymoviz.co/search?genre=romance&sort=latest&type=movie&p=2", origin, 2))
        assertFalse(query.acceptsPage("https://mymoviz.co/search?genre=romance&sort=imdb&type=movie&p=3", origin, 2))
    }
    @Test fun rejectsDuplicateParametersCredentialsUnknownQueriesAndFragments() {
        val q = GenreArchiveQuery()
        val base = q.pageUrl(origin, 1)
        assertFalse(q.acceptsPage("$base&genre=horror", origin, 1))
        assertFalse(q.acceptsPage("$base&token=secret", origin, 1))
        assertFalse(q.acceptsPage("$base#", origin, 1))
        assertFalse(q.acceptsPage(base.replace("https://", "https://user:pass@"), origin, 1))
        assertFalse(q.acceptsPage(base.replace("https://", "http://"), origin, 1))
        assertFalse(q.acceptsPage("$base&p=broken", origin, 1))
    }
    @Test fun latestCanUseTheSitesOmittedDefaultSort() {
        assertTrue(GenreArchiveQuery().acceptsPage("https://mymoviz.co/search?genre=romance&type=movie&p=2", origin, 2))
        assertFalse(GenreArchiveQuery(sort = ArchiveSort.IMDB).acceptsPage("https://mymoviz.co/search?genre=romance&type=movie&p=2", origin, 2))
    }
    @Test fun cacheBoundedAcrossQueriesAndPagesWithoutDuplicatingOnePage() {
        val q = GenreArchiveQuery()
        val old = (1..20).map { GenreArchivePage(q, it, emptyList(), checkedAt = it.toLong()) }
        val newest = GenreArchivePage(q, 20, emptyList(), checkedAt = 100)
        val retained = GenreArchivePolicy.retain(old, newest)
        assertEquals(12, retained.size)
        assertEquals(1, retained.count { it.number == 20 })
        assertEquals(100L, retained.first().checkedAt)
        assertFalse(retained.any { it.number == 1 })
    }
    @Test fun expirationAndFutureTimestampsNeedRevalidation() {
        val page = GenreArchivePage(GenreArchiveQuery(), 1, emptyList(), checkedAt = 1000)
        assertTrue(GenreArchivePolicy.fresh(page, 1001))
        assertFalse(GenreArchivePolicy.fresh(page, 999))
        assertFalse(GenreArchivePolicy.fresh(page, 1000 + GenreArchivePolicy.FRESH_MS))
    }
    @Test fun oldProviderHostCannotSupplyACachedTitleToNewOrigin() {
        val item = com.amin.tvos.data.model.CatalogItem(title = "Example",
            contentUrl = "https://mymoviz.co/movies/123/example-2020", serviceId = "mymoviz",
            kind = com.amin.tvos.data.model.CatalogKind.MOVIE)
        val page = GenreArchivePage(GenreArchiveQuery(), 1, listOf(item))
        assertTrue(GenreArchivePolicy.belongsToOrigin(page, origin))
        assertFalse(GenreArchivePolicy.belongsToOrigin(page, "https://other.test/"))
        assertFalse(GenreArchivePolicy.belongsToOrigin(page.copy(items = listOf(item.copy(contentUrl = item.contentUrl + "?token=value"))), origin))
        assertFalse(GenreArchivePolicy.belongsToOrigin(page.copy(query = GenreArchiveQuery(type = ArchiveType.SERIES)), origin))
        assertFalse(GenreArchivePolicy.belongsToOrigin(page.copy(nextUrl = "https://other.test/"), origin))
    }
}
