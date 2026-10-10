package com.amin.tvos.data

import com.amin.tvos.data.model.CatalogItem
import com.amin.tvos.data.model.CatalogKind
import com.amin.tvos.data.model.CatalogSection
import org.junit.Assert.*
import org.junit.Test

class CatalogClassificationTest {
    private fun item(path: String, kind: CatalogKind) = CatalogItem(
        title = "Title", kind = kind, contentUrl = "https://movie.test$path", serviceId = "mymoviz"
    )

    @Test fun repairsMisfiledSeriesAndKeepsMovie() {
        val movie = item("/movies/1/movie/", CatalogKind.MOVIE)
        val show = item("/tvshows/2/show/", CatalogKind.MOVIE)
        val result = CatalogClassification.normalize(CatalogSection(
            "mymoviz", all = listOf(movie, show), movies = listOf(movie, show)
        ))
        assertEquals(listOf(movie), result.movies)
        assertEquals(listOf(show.copy(kind = CatalogKind.SERIES)), result.series)
        assertEquals(2, result.all.size)
        assertEquals(result, CatalogClassification.normalize(result))
    }

    @Test fun repairsMisfiledMovieWithoutChangingProviderOrder() {
        val show = item("/tvshows/2/show/", CatalogKind.SERIES)
        val movie = item("/movies/1/movie/", CatalogKind.SERIES)
        val result = CatalogClassification.normalize(CatalogSection(
            "mymoviz", series = listOf(show, movie)
        ))
        assertEquals(listOf(show), result.series)
        assertEquals(listOf(movie.copy(kind = CatalogKind.MOVIE)), result.movies)
    }

    @Test fun legacyRouteRequiresPositiveEpisodeEvidence() {
        val unknown = item("/_modern/title/7/title/", CatalogKind.MOVIE)
        assertEquals(CatalogKind.MOVIE, CatalogClassification.classify(unknown).kind)
        assertEquals(CatalogKind.SERIES, CatalogClassification.classify(
            unknown.copy(episodeLabel = "قسمت ۴ فصل ۱")
        ).kind)
    }

    @Test fun queryDoesNotChangeRouteIdentity() {
        assertEquals(CatalogKind.MOVIE, CatalogClassification.classify(
            item("/movies/1/movie/?redirect=/tvshows/", CatalogKind.MOVIE)
        ).kind)
    }

    @Test fun homeWindowIsBoundedButLibraryDefaultIsNot() {
        val entries = (1..30).map { item("/movies/$it/title/", CatalogKind.MOVIE) }
        assertEquals(8, CanonicalLibrary.mergeLatest(
            listOf(entries), limit = 20, providerItemLimit = 8
        ).size)
        assertEquals(20, CanonicalLibrary.mergeLatest(listOf(entries), limit = 20).size)
        assertTrue(CanonicalLibrary.mergeLatest(listOf(entries), limit = 0).isEmpty())
    }

    @Test fun conflictingKindsNeverMergeEvenForSamePageOrImdb() {
        val movie = item("/_modern/title/1/title/", CatalogKind.MOVIE).copy(imdbId = "tt1234567")
        val show = movie.copy(kind = CatalogKind.SERIES)
        val result = CanonicalLibrary.canonicalize(listOf(movie, show))
        assertEquals(2, result.size)
        assertNotEquals(result[0].canonicalId, result[1].canonicalId)
    }
}
