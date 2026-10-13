package com.amin.tvos.data

import com.amin.tvos.data.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class JourneyCatalogTest {
    private val now = LocalDate.of(2026, 10, 13)
    @Test fun mcuIncludes38FilmsWithoutCountingSeasonAsFilm() {
        assertEquals(38, JourneyCatalog.select("mcu", JourneyFormat.MOVIE, now).size)
        assertEquals(5, JourneyCatalog.select("mcu", JourneyFormat.SHORT, now).count { it.season == 0 })
        assertEquals(3, JourneyCatalog.select("mcu", JourneyFormat.SPECIAL, now).size)
    }
    @Test fun seasonsInterleaveByReleaseNotStoryOrder() {
        val list = JourneyCatalog.select("mcu", today = now)
        val names = list.filter { it.release.startsWith("2021") }.map { it.title }
        assertTrue(names.indexOf("Loki") < names.indexOf("Black Widow"))
        assertTrue(names.indexOf("Black Widow") < names.indexOf("What If...?"))
        assertEquals(listOf(1, 2), list.filter { it.title == "Loki" }.map { it.season })
    }
    @Test fun sharedMembershipKeepsOneIdentityInLargestJourney() {
        val work = JourneyCatalog.select("mcu", today = now).single { it.title == "Spider-Man: Homecoming" }
        assertTrue(JourneyCatalog.select("spider", today = now).any { it.id == work.id })
        val full = JourneyCatalog.select("complete", today = now)
        assertEquals(full.size, full.map { it.id }.toSet().size)
        assertTrue(full.any { it.title == "Blade" })
        assertFalse(JourneyCatalog.select("doomsday", today = now).any { it.title == "Blade" })
    }
    @Test fun futureReleaseExcludedAtDateBoundary() {
        assertFalse(JourneyCatalog.select("mcu", today = LocalDate.of(2025, 7, 24)).any { it.title == "The Fantastic Four: First Steps" })
        assertTrue(JourneyCatalog.select("mcu", today = LocalDate.of(2025, 7, 25)).any { it.title == "The Fantastic Four: First Steps" })
    }
    private fun film(title: String, year: String, kind: CatalogKind = CatalogKind.MOVIE, url: String = "https://provider.test/post/film/1") =
        CatalogItem(title, kind, url, serviceId = "filmrooz", year = year)
    @Test fun exactTitleYearAndKindRequiredAndNoInventedMissingUrls() {
        val work = JourneyCatalog.select("mcu", today = now).single { it.title == "Iron Man" }
        fun resolve(item: CatalogItem) = JourneyCatalog.resolve(listOf(work), listOf(CatalogSection("filmrooz", movies = listOf(item)))).single()
        assertNotNull(resolve(film("Iron Man", "2008")).media)
        assertNull(resolve(film("Iron Man 2", "2008")).media)
        assertNull(resolve(film("Iron Man", "2010")).media)
        assertNull(resolve(film("Iron Man", "")).media)
        assertNull(resolve(film("Iron Man", "2008", CatalogKind.SERIES)).media)
        assertNull(JourneyCatalog.resolve(listOf(work), emptyList()).single().media)
    }
    @Test fun laterSeasonMatchesSeriesPremiereYearNotSeasonReleaseYear() {
        val work = JourneyCatalog.select("mcu", today = now).single { it.title == "Loki" && it.season == 2 }
        val section = CatalogSection("filmrooz", series = listOf(film("Loki", "2021", CatalogKind.SERIES)))
        assertNotNull(JourneyCatalog.resolve(listOf(work), listOf(section)).single().media)
    }
    @Test fun differentVerifiedPublicIdsAreAmbiguousNotAutomaticallyChosen() {
        val work = JourneyCatalog.select("mcu", today = now).single { it.title == "Iron Man" }
        val a = film("Iron Man", "2008").copy(imdbId = "tt0371746")
        val b = film("Iron Man", "2008", url = "https://other.test/post/film/2").copy(imdbId = "tt1234567", serviceId = "mymoviz")
        assertNull(JourneyCatalog.resolve(listOf(work), listOf(CatalogSection("filmrooz", movies = listOf(a, b)))).single().media)
    }
    @Test fun marathonBranchesAreSeparateFromMarvel() {
        val list = JourneyCatalog.select("middleearth", today = now)
        assertEquals(6, list.size)
        assertFalse(list.any { "mcu" in it.collections })
        assertEquals(4, JourneyCatalog.marathonCollections.size)
        assertEquals(6, JourneyCatalog.marvelCollections.size)
    }
    @Test fun oneShotFullTitleMapsWithoutConfusingSameNameSeries() {
        val work = JourneyCatalog.works.single { it.title == "The Consultant" }
        val short = film("Marvel One-Shot: The Consultant", "2011").copy(imdbId = "tt2011118")
        val series = film("The Consultant", "2023", CatalogKind.SERIES, "https://site.test/post/series/2")
        val hit = JourneyCatalog.resolve(listOf(work), listOf(CatalogSection("filmrooz", all = listOf(short, series)))).single()
        assertEquals(short.contentUrl, hit.media?.representative?.contentUrl)
    }
    @Test fun curatedAliasesMergeSourcesButConflictingImdbStillDoesNot() {
        val work = JourneyCatalog.works.single { it.title == "X2" }
        val a = film("X2", "2003")
        val b = film("X-Men 2", "2003", url = "https://other.test/post/film/2").copy(serviceId = "mymoviz")
        val result = JourneyCatalog.resolve(listOf(work), listOf(CatalogSection("filmrooz", movies = listOf(a, b)))).single()
        assertEquals(2, result.media?.variants?.size)
        assertNull(JourneyCatalog.resolve(listOf(work), listOf(CatalogSection("filmrooz", movies = listOf(
            a.copy(imdbId = "tt0290334"), b.copy(imdbId = "tt1234567"))))).single().media)
    }
    @Test fun staleVol2IdentityCannotBlockCorrectGuardians2014Source() {
        val work = JourneyCatalog.works.single { it.title == "Guardians of the Galaxy" }
        val wrong = film(work.title, "2014")
        val good = film(work.title, "2014", url = "https://other.test/post/film/2").copy(serviceId = "mymoviz", imdbId = "tt2015381")
        val metadata = mapOf(ContentMetadataPolicy.canonicalContentUrl(wrong.contentUrl) to
            TitleMetadata(wrong.contentUrl, year = "2017", imdbId = "tt3896198"))
        val result = JourneyCatalog.resolve(listOf(work), listOf(CatalogSection("filmrooz", movies = listOf(wrong, good))), metadata).single()
        assertEquals(good.contentUrl, result.media?.representative?.contentUrl)
        assertEquals(1, result.media?.variants?.size)
    }
    @Test fun detailYearSeparatesSameNameSeriesWithIncidentalCardYear() {
        val work = JourneyCatalog.works.single { it.title == "The Gifted" }
        val original = film(work.title, "2017", CatalogKind.SERIES)
        val other = original.copy(contentUrl = "https://site.test/post/series/2")
        val metadata = mapOf(ContentMetadataPolicy.canonicalContentUrl(other.contentUrl) to
            TitleMetadata(other.contentUrl, year = "2018", imdbId = "tt1234567"))
        val result = JourneyCatalog.resolve(listOf(work), listOf(CatalogSection("filmrooz", series = listOf(original, other))), metadata).single()
        assertEquals(original.contentUrl, result.media?.representative?.contentUrl)
        assertEquals(1, result.media?.variants?.size)
    }
    @Test fun verifiedPublicIdentityHandlesProductionVsReleaseYearWithoutMovingTimeline() {
        val work = JourneyCatalog.works.single { it.title == "Ghost Rider: Spirit of Vengeance" }
        val item = film(work.title, "2011").copy(imdbId = "tt1071875")
        assertNotNull(JourneyCatalog.resolve(listOf(work), listOf(CatalogSection("filmrooz", movies = listOf(item)))).single().media)
        assertEquals("2012", work.release)
    }
    @Test fun sameProviderDuplicateSeriesRequiresIdentityAndKeepsOnlyUsGifted() {
        val work = JourneyCatalog.works.single { it.title == "The Gifted" }
        val us = film(work.title, "2017", CatalogKind.SERIES, "https://site.test/post/series/1")
        val thai = us.copy(contentUrl = "https://site.test/post/series/2")
        fun resolve(items: List<CatalogItem>) = JourneyCatalog.resolve(listOf(work), listOf(CatalogSection("filmrooz", series = items))).single()
        assertNull(resolve(listOf(us, thai)).media)
        val result = resolve(listOf(us.copy(imdbId = "tt4396630"), thai.copy(imdbId = "tt9136312")))
        assertEquals(us.contentUrl, result.media?.representative?.contentUrl)
        assertEquals(1, result.media?.variants?.size)
    }
}
