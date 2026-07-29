package com.amin.tvos.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentMetadataPolicyTest {

    @Test
    fun sameContentRoute_acceptsFragmentAndTrailingSlashChanges() {
        assertTrue(
            ContentMetadataPolicy.isSameTopLevelPage(
                "https://app.parsiflix.com/medias/movies/357",
                "https://app.parsiflix.com/medias/movies/357/#player"
            )
        )
    }

    @Test
    fun routeRace_rejectsHomepageMetadataForContentPage() {
        assertFalse(
            ContentMetadataPolicy.isSameTopLevelPage(
                "https://app.parsiflix.com/medias/movies/357",
                "https://app.parsiflix.com/"
            )
        )
    }

    @Test
    fun genericParsiShellTitle_isRejected() {
        assertTrue(
            ContentMetadataPolicy.isGenericShellTitle(
                "ParsiFlix - Watch Persian Tv Shows Online, Watch Persian Movies Online",
                "parsiflix"
            )
        )
    }

    @Test
    fun realMovieTitle_isAccepted() {
        assertFalse(
            ContentMetadataPolicy.isGenericShellTitle(
                "دو روز دیرتر",
                "parsiflix",
                "فیلم ایرانی"
            )
        )
    }
}
