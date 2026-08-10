package com.amin.tvos.data

import com.amin.tvos.data.model.CatalogItem
import com.amin.tvos.data.model.CatalogKind
import com.amin.tvos.data.model.PersonRef
import com.amin.tvos.data.model.SourceVariant
import com.amin.tvos.data.model.SpotlightItem
import com.amin.tvos.data.model.withPlaybackSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpotlightSourceSelectionTest {

    @Test
    fun switchingProviderChangesPlaybackRouteButKeepsCanonicalPanelStable() {
        val current = SpotlightItem(
            title = "Spider-Man 3",
            kind = CatalogKind.MOVIE,
            contentUrl = "https://film.test/title/1",
            posterUrl = "https://images.test/canonical-poster.jpg",
            backdropUrl = "https://images.test/canonical-backdrop.jpg",
            serviceId = "filmrooz",
            serviceName = "FilmRooz",
            summary = "Canonical synopsis",
            year = "2007",
            genres = listOf("اکشن"),
            rating = "6.3",
            runtime = "139 دقیقه",
            country = "آمریکا",
            language = "انگلیسی، فرانسوی",
            hasPersianDub = true,
            hasPersianSubtitle = true,
            directors = listOf(PersonRef("Sam Raimi")),
            cast = listOf(PersonRef("Tobey Maguire")),
            canonicalId = "aminema:spider-man-3"
        )
        val source = SourceVariant(
            providerId = "mymoviz",
            providerName = "MyMoviz",
            item = CatalogItem(
                title = "Spider-Man 3",
                kind = CatalogKind.MOVIE,
                contentUrl = "https://movie.test/_modern/title/349/spider-man-3-2007",
                posterUrl = "https://images.test/provider-poster.jpg",
                serviceId = "mymoviz",
                summary = "Different provider synopsis",
                year = "2007",
                rating = "9.2",
                hasPersianDub = false
            )
        )

        val switched = current.withPlaybackSource(source, providerSupportsDirectPlay = false)

        assertEquals("mymoviz", switched.serviceId)
        assertEquals(source.item.contentUrl, switched.contentUrl)
        assertEquals(source.item.contentUrl, switched.browserStartUrl)
        assertFalse(switched.directPlay)
        assertEquals(current.title, switched.title)
        assertEquals(current.posterUrl, switched.posterUrl)
        assertEquals(current.backdropUrl, switched.backdropUrl)
        assertEquals(current.summary, switched.summary)
        assertEquals("6.3", switched.rating)
        assertEquals(current.directors, switched.directors)
        assertEquals(current.cast, switched.cast)
        assertTrue(switched.hasPersianDub)
        assertTrue(switched.hasPersianSubtitle)
        assertEquals(current.canonicalId, switched.canonicalId)
    }
}
