package com.amin.tvos.browser

import org.junit.Assert.*
import org.junit.Test
import com.amin.tvos.data.RetiredParsiFlix

class FarsiLandPolicyTest {
    @Test fun failedPlayerOneCanTryVerifiedPlayerTwo(){
        assertEquals("Player 2",FarsiLandPolicy.alternatePlayer(listOf("Player 2"),setOf("Player 1")))
    }
    @Test fun playerFallbackNeverBouncesToPreviouslyTriedEngine(){
        assertEquals("Player 1",FarsiLandPolicy.alternatePlayer(listOf("Player 1"),setOf("Player 2")))
        assertNull(FarsiLandPolicy.alternatePlayer(listOf("Player 1","Player 2"),setOf("Player 1","Player 2")))
    }
    @Test fun unknownHiddenOrAbsentPlayerIsNotInvented(){
        assertNull(FarsiLandPolicy.alternatePlayer(listOf("Player 3","Advertisement","Player 10"),emptySet()))
        assertNull(FarsiLandPolicy.alternatePlayer(emptyList(),emptySet()))
    }
    @Test fun approvedIntermediaryHasRestrictedPaths(){
        assertTrue(FarsiLandPolicy.allowedPage("https://farsiland.buzz/"))
        assertTrue(FarsiLandPolicy.allowedPage("https://farsiland.buzz/redirect.php"))
        assertTrue(FarsiLandPolicy.allowedPage("https://farsiland.buzz/smart-play"))
        assertFalse(FarsiLandPolicy.allowedPage("https://farsiland.buzz/movies/unrelated/"))
        assertFalse(FarsiLandPolicy.allowedPage("https://farsiland.buzz.evil.test/"))
        assertFalse(FarsiLandPolicy.allowedPage("http://farsiland.buzz/"))
    }
    @Test fun exactHttpsProviderBoundaries(){
        assertTrue(FarsiLandPolicy.allowedPage("https://farsiland.com/movies/khabnama/"))
        assertTrue(FarsiLandPolicy.allowedPage("https://farsiplex.com/season/?id=opaque"))
        assertTrue(FarsiLandPolicy.allowedPage("https://farsiplex.com/smart-play"))
        listOf("http://farsiland.com/","https://farsiland.com.evil.test/","https://evilfarsiland.com/",
            "https://farsiland.com@evil.test/","https://a:f@farsiland.com/","https://farsiland.com:8443/",
            "https://farsiplex.com/test.mp4","javascript:alert(1)","intent://player").forEach {
            assertFalse(it,FarsiLandPolicy.allowedPage(it))
        }
    }
    @Test fun qualityIsNumericNotReleaseOrder(){
        assertEquals(2,FarsiLandPolicy.qualityIndex(listOf("480","720","1080","2160")))
        assertEquals(1,FarsiLandPolicy.qualityIndex(listOf("2160","720")))
        assertNull(FarsiLandPolicy.qualityIndex(listOf("2160","4K")))
        assertEquals(0,FarsiLandPolicy.qualityIndex(listOf("Server1")))
        assertNull(FarsiLandPolicy.qualityIndex(listOf("Watch Advert","Download","1080 advertisement")))
    }
    @Test fun liveHasSeparateServerPolicy(){
        assertEquals(0,FarsiLandPolicy.qualityIndex(listOf("HD","480p","Backup Server"),true))
        assertEquals(0,FarsiLandPolicy.qualityIndex(listOf("Live on Youtube"),true))
        assertNull(FarsiLandPolicy.qualityIndex(listOf("HD")))
    }
    @Test fun adHostMatchingCannotBlockUnrelatedPlayerCdn(){
        assertTrue(FarsiLandPolicy.blockedResourceHost("googleads.g.doubleclick.net"))
        assertTrue(FarsiLandPolicy.blockedResourceHost("cdn.webpushr.com"))
        assertFalse(FarsiLandPolicy.blockedResourceHost("doubleclick.net.example.com"))
        assertFalse(FarsiLandPolicy.blockedResourceHost("cdn.jsdelivr.net"))
        assertFalse(FarsiLandPolicy.blockedResourceHost("fundingchoicesmessages.google.com"))
    }
    @Test fun episodeActionsAreOrdinaryFarsiLandPagesOnly(){
        assertTrue(FarsiLandPolicy.isEpisode("https://farsiland.com/episodes/bamdade-khomar-se02-ep13/"))
        assertFalse(FarsiLandPolicy.isEpisode("https://farsiplex.com/episodes/fake/"))
        assertFalse(FarsiLandPolicy.isEpisode("https://farsiland.com/episodes/page/2/"))
        assertFalse(FarsiLandPolicy.isEpisode("https://evil.test/episodes/title/"))
    }
    @Test fun retirementNeverConfusesFarsiPlexWithParsiFlix(){
        assertTrue(RetiredParsiFlix.service("parsiflix"))
        assertFalse(RetiredParsiFlix.service("farsiland"))
        assertFalse(RetiredParsiFlix.service("parsatv"))
        assertTrue(RetiredParsiFlix.page("https://app.parsiflix.com/medias/movies/1"))
        assertFalse(RetiredParsiFlix.page("https://farsiplex.com/smart-play"))
        assertFalse(RetiredParsiFlix.page("https://parsiflix.com.evil.test/"))
    }
}
