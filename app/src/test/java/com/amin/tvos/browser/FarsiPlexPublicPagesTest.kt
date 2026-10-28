package com.amin.tvos.browser

import org.junit.Assert.*
import org.junit.Test

class FarsiPlexPublicPagesTest {
    private fun result(title:String,page:String)="""<div class="result-item"><div class="details"><div class="title"><a href="$page">$title</a></div></div></div>"""
    private fun episode(season:Int,number:Int,page:String)="""<li><div class="numerando">$season - $number</div><div class="episodiotitle"><a href="$page">Episode $number</a></div></li>"""
    @Test fun sourceBoundaryNeverAcceptsMediaOrUnrelatedHosts(){
        assertTrue(FarsiPlexPublicPages.isSource("https://farsiland.com/movies/a/"))
        assertTrue(FarsiPlexPublicPages.isSource("https://farsiland.com/episodes/a/"))
        listOf("https://evil.test/movies/a/","https://farsiplex.com/movie/a/","http://farsiland.com/movies/a/",
            "https://farsiland.com@evil.test/movies/a/","https://farsiland.com/movies/a.mp4").forEach {
            assertFalse(it,FarsiPlexPublicPages.isSource(it))
        }
    }
    @Test fun movieTitleComesFromRealHeadingNotUrlSlug(){
        assertEquals(FarsiPlexPublicPages.Title("Harchi To Begi"),FarsiPlexPublicPages.describe(
            "https://farsiland.com/movies/unrelated-slug/","<h1>Harchi To Begi</h1>"))
    }
    @Test fun episodeAndSeasonComeFromCharacterizedHeadings(){
        assertEquals(FarsiPlexPublicPages.Title("HamKalaam",11),FarsiPlexPublicPages.describe(
            "https://farsiland.com/episodes/a/","<h1>HamKalaam Episode 11</h1>"))
        assertEquals(FarsiPlexPublicPages.Title("Zane Rooz",20,4),FarsiPlexPublicPages.describe(
            "https://farsiland.com/episodes/a/","<h1>Other heading</h1><h2>Zane Rooz SE04 EP20</h2>"))
    }
    @Test fun noEpisodeNumberMeansNoGuessedFirstEpisode(){
        assertNull(FarsiPlexPublicPages.describe("https://farsiland.com/episodes/a/","<h1>HamKalaam</h1>"))
    }
    @Test fun taxiEpisodeTwoUsesItsRealSecondaryHeadingAndActualEpisodeLink(){
        val source="https://farsiland.com/episodes/taxi-ep02/"
        val wanted=FarsiPlexPublicPages.describe(source,"""<h1>Season  Episode</h1><h2>Taxi EP02</h2><a href="https://farsiland.com/tvshows/taxi/" title="Taxi">ALL</a>""")
        assertEquals(FarsiPlexPublicPages.Title("Taxi",2),wanted)
        assertEquals("https://farsiplex.com/episode/taxi-ep2/",FarsiPlexPublicPages.matchEpisode(wanted!!,
            episode(1,1,"https://farsiplex.com/episode/taxi-ep01-95081968/")+episode(1,2,"https://farsiplex.com/episode/taxi-ep2/")))
    }
    @Test fun explicitSeasonIsPreferredAndConflictingAllTitleIsRejected(){
        val source="https://farsiland.com/episodes/a/"
        assertEquals(4,FarsiPlexPublicPages.describe(source,"<h1>Zane Rooz Episode 20</h1><h2>Zane Rooz SE04 EP20</h2>")?.season)
        assertNull(FarsiPlexPublicPages.describe(source,"""<h1>Wrong Show Episode 1</h1><a href="https://farsiland.com/tvshows/real-show/" title="Real Show">ALL</a>"""))
    }
    @Test fun matchRequiresExactNormalizedTitleAndCorrectKind(){
        val wanted=FarsiPlexPublicPages.Title("Harchi To Begi")
        val page="https://farsiplex.com/movie/harchi-to-begi/"
        assertEquals(page,FarsiPlexPublicPages.matchTitle(wanted,result("Harchi To Begi",page)))
        assertNull(FarsiPlexPublicPages.matchTitle(wanted,result("Harchi To Begi 2",page)))
        assertNull(FarsiPlexPublicPages.matchTitle(wanted,result("Harchi To Begi","https://farsiplex.com/tvshow/harchi/")))
    }
    @Test fun unsafeOrSignedResultLinksAreNeverKept(){
        val wanted=FarsiPlexPublicPages.Title("Test")
        listOf("https://evil.test/movie/a/","https://farsiplex.com.evil.test/movie/a/",
            "https://farsiplex.com/movie/a/?token=private","https://farsiplex.com:8443/movie/a/",
            "https://user:password@farsiplex.com/movie/a/","https://farsiplex.com/file.mp4").forEach {
            assertNull(it,FarsiPlexPublicPages.matchTitle(wanted,result("Test",it)))
        }
    }
    @Test fun ambiguousRemakesAreNotAutoSelected(){
        assertNull(FarsiPlexPublicPages.matchTitle(FarsiPlexPublicPages.Title("Test"),
            result("Test","https://farsiplex.com/movie/a/")+result("Test","https://farsiplex.com/movie/b/")))
    }
    @Test fun onlyRequestedEpisodeOfMatchedSeriesIsChosen(){
        val page="https://farsiplex.com/episode/hamkalaam-ep11/"
        assertEquals(page,FarsiPlexPublicPages.matchEpisode(FarsiPlexPublicPages.Title("HamKalaam",11),
            episode(1,10,"https://farsiplex.com/episode/a/")+episode(1,11,page)))
    }
    @Test fun unknownSeasonIsRejectedWhenEpisodeNumberIsAmbiguous(){
        val html=episode(1,2,"https://farsiplex.com/episode/one/")+episode(2,2,"https://farsiplex.com/episode/two/")
        assertNull(FarsiPlexPublicPages.matchEpisode(FarsiPlexPublicPages.Title("Series",2),html))
        assertEquals("https://farsiplex.com/episode/two/",FarsiPlexPublicPages.matchEpisode(FarsiPlexPublicPages.Title("Series",2,2),html))
    }
    @Test fun encodedTitleEntitiesRemainComparable(){
        assertEquals(FarsiPlexPublicPages.normalized("Rock & Roll"),FarsiPlexPublicPages.normalized("Rock &#38; Roll"))
    }
    @Test fun boundedLookupUsesOnlyPublicSearchAndActualEpisodePageLinks(){
        val source="https://farsiland.com/episodes/hamkalaam-ep11/"
        val series="https://farsiplex.com/tvshow/hamkalaam-8470fe1c/"
        val page="https://farsiplex.com/episode/hamkalaam-ep11/"
        val requested=mutableListOf<String>()
        val resolved=FarsiPlexPublicPages.resolve(source){url->requested.add(url);when(url){
            source->"<h1>HamKalaam Episode 11</h1>"
            "https://farsiplex.com/?s=HamKalaam"->result("HamKalaam",series)
            series->episode(1,11,page)
            else->error("Unexpected request")
        }}
        assertEquals(page,resolved);assertEquals(3,requested.size)
        assertFalse(requested.any{it.contains("smart-play")||it.contains("play-elite-player")})
    }
    @Test fun missingPublicMatchDoesNotGuessAPlayerOrEpisodeUrl(){
        val requested=mutableListOf<String>()
        assertNull(FarsiPlexPublicPages.resolve("https://farsiland.com/movies/a/"){url->
            requested.add(url);if(requested.size==1)"<h1>Not Indexed</h1>" else "<p>No results</p>"
        });assertEquals(2,requested.size)
    }
}
