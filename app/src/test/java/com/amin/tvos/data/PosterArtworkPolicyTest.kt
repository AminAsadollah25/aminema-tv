package com.amin.tvos.data

import com.amin.tvos.data.model.CatalogKind
import com.amin.tvos.data.model.SpotlightItem
import com.amin.tvos.data.model.TitleMetadata
import com.amin.tvos.data.model.mergePrefer
import com.amin.tvos.data.model.withMetadata
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Test

class PosterArtworkPolicyTest {
    private val url = "https://images.example.com/title.jpg"

    @Test fun rejectsThumbnailsLandscapesSquaresAndUnsafeUrls() {
        assertEquals("", PosterArtworkPolicy.verifiedPortrait(url, 190, 281))
        assertEquals("", PosterArtworkPolicy.verifiedPortrait(url, 1920, 1080))
        assertEquals("", PosterArtworkPolicy.verifiedPortrait(url, 640, 640))
        assertEquals("", PosterArtworkPolicy.verifiedPortrait("https://secret@images.example.com/a", 364, 536))
        assertEquals("", PosterArtworkPolicy.verifiedPortrait(url, 0, 0))
        assertEquals("", PosterArtworkPolicy.verifiedPortrait("data:image/svg+xml,placeholder", 381, 571))
        assertEquals(url, PosterArtworkPolicy.verifiedPortrait(url, 364, 536))
        assertEquals(url, PosterArtworkPolicy.verifiedPortrait(url, 762, 1142))
    }

    @Test fun cachedDetailArtSurvivesPartialRefreshWithoutReplacingThumbnail() {
        val old = TitleMetadata("https://example.com/title", detailPosterUrl = url, artworkLookupVersion = 1)
        val merged = old.mergePrefer(TitleMetadata(old.contentUrl, summary = "summary"))
        val item = SpotlightItem("Title", CatalogKind.SERIES, old.contentUrl,
            posterUrl = "https://example.com/small.jpg", serviceId = "mymoviz").withMetadata(merged)
        assertEquals(url, item.detailPosterUrl)
        assertEquals("https://example.com/small.jpg", item.posterUrl)
        assertEquals(1, merged.artworkLookupVersion)
    }

    @Test fun legacyCacheHasSafeEmptyArtworkDefaults() {
        val old = Json.decodeFromString<TitleMetadata>("""{"contentUrl":"https://example.com/title"}""")
        assertEquals("", old.detailPosterUrl)
        assertEquals(0, old.artworkLookupVersion)
    }
    @Test fun sourceRefreshDoesNotReplaceCanonicalArtworkAlreadyOnScreen() {
        val item=SpotlightItem("Title",CatalogKind.SERIES,"https://example.com/title",
            posterUrl=url,detailPosterUrl="https://example.com/large.jpg",serviceId="filmrooz")
        val next=item.withMetadata(TitleMetadata(item.contentUrl,detailPosterUrl="https://other.example.com/large.jpg"))
        assertEquals(item.detailPosterUrl,next.detailPosterUrl)
    }
}
