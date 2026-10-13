package com.amin.tvos.data

import com.amin.tvos.data.model.*
import org.junit.Assert.*
import org.junit.Test

class JourneyLookupPlannerTest {
    @Test fun oneSearchForMultipleSeasonsAndOnlySupportedProviders() {
        val list = JourneyCatalog.works.filter { it.title == "Loki" }
        val tasks = JourneyLookupPlanner.tasks(list, setOf("filmrooz", "mymoviz", "parsiflix", "unknown"))
        assertEquals(2, tasks.size)
        assertEquals(setOf("filmrooz", "mymoviz"), tasks.map { it.serviceId }.toSet())
    }
    @Test fun remakesReuseSearchButDoNotLoseReferenceYears() {
        val list = JourneyCatalog.works.filter { it.title == "Fantastic Four" }
        assertEquals(2, list.size)
        assertEquals(1, JourneyLookupPlanner.tasks(list, setOf("filmrooz")).size)
        assertEquals(setOf("2005", "2015"), list.map { it.premiereYear }.toSet())
    }
    @Test fun verifiedAliasesHaveDistinctCheckpoints() {
        val work = JourneyCatalog.works.single { it.title == "X2" }
        val tasks = JourneyLookupPlanner.tasks(listOf(work), setOf("filmrooz"))
        assertEquals(3, tasks.size)
        assertEquals(tasks.size, tasks.map { it.key }.toSet().size)
    }
    @Test fun failedExpiredAndFutureCheckpointsAreRetried() {
        val now = 100_000_000L
        val record = JourneyLookupRecord("key", "filmrooz", "Loki", now - 1000)
        assertTrue(JourneyLookupPlanner.isFresh(record, now))
        assertFalse(JourneyLookupPlanner.isFresh(record.copy(error = "failure"), now))
        assertFalse(JourneyLookupPlanner.isFresh(record.copy(checkedAt = now + 1), now))
        assertFalse(JourneyLookupPlanner.isFresh(record.copy(checkedAt = now - 86_400_000), now))
    }
    @Test fun publicSearchProjectionNeverInventsYearOrPlaybackPage() {
        val result = SearchResult("Iron Man (2008)", CatalogKind.MOVIE, "https://site.test/post/film/1/", "", "filmrooz")
        val item = JourneyLookupPlanner.catalogItem(result)
        assertEquals("Iron Man", item.title)
        assertEquals("2008", item.year)
        assertEquals(result.contentUrl, item.contentUrl)
        assertEquals("", JourneyLookupPlanner.catalogItem(result.copy(title = "Iron Man")).year)
    }
    @Test fun unrelatedOrSearchHitsDoNotInflateJourneyCache() {
        val base = SearchResult("Iron Man", CatalogKind.MOVIE, "https://site.test/post/film/1/", "", "filmrooz", "2008")
        val unrelated = base.copy(title = "The Man in the Iron Mask", year = "1998", contentUrl = "https://site.test/post/film/2/")
        val work = JourneyCatalog.works.single { it.title == "Iron Man" }
        val result = JourneyLookupPlanner.relevantItems(listOf(base, unrelated, base.copy(year = "1951")), listOf(work))
        assertEquals(1, result.size)
        assertEquals("2008", result.single().year)
    }
    @Test fun directQueryOutranksIncidentalOlderYearOnSamePage() {
        val item = CatalogItem("The Gifted", CatalogKind.SERIES, "https://site.test/post/series/1", serviceId = "filmrooz", year = "2017")
        val indirect = JourneyLookupRecord("a", "filmrooz", "The Avengers", 200, items = listOf(item.copy(year = "2018")))
        val direct = JourneyLookupRecord("b", "filmrooz", "The Gifted", 100, items = listOf(item))
        assertEquals("2017", JourneyLookupPlanner.bestItems(listOf(indirect, direct)).single().year)
    }
    @Test fun verifiedIdentitySurvivesFreshCacheProductionYearDifference() {
        val work = JourneyCatalog.works.single { it.title == "Ghost Rider: Spirit of Vengeance" }
        val hit = SearchResult(work.title, CatalogKind.MOVIE, "https://site.test/post/film/1", "", "mymoviz", "2011", "tt1071875")
        val stored = JourneyLookupPlanner.relevantItems(listOf(hit), listOf(work))
        assertEquals(1, stored.size)
        assertNotNull(JourneyCatalog.resolve(listOf(work), listOf(CatalogSection("mymoviz", movies = stored))).single().media)
    }
}
