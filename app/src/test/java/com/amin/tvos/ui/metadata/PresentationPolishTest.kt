package com.amin.tvos.ui.metadata

import com.amin.tvos.data.model.CatalogItem
import com.amin.tvos.data.model.CatalogKind
import com.amin.tvos.data.model.SourceVariant
import com.amin.tvos.data.PersonPortraitPolicy
import com.amin.tvos.data.TrustedPortrait
import com.amin.tvos.data.model.PersonRef
import com.amin.tvos.ui.components.HeroContrastPolicy
import org.junit.Assert.*
import org.junit.Test

class PresentationPolishTest {
    @Test fun languageAliasesAndPrefixAreNotRepeated() {
        assertEquals("انگلیسی",displayLanguages("زبان انگلیسی، انگلیسی بریتانیایی، انگلیسی آمریکایی"))
        assertEquals("انگلیسی، فارسی",displayLanguages("English / American English | Farsi"))
        assertEquals("ناشناخته",displayLanguages("زبان زبان ناشناخته"))
    }
    @Test fun countryAliasesStayCompact() {
        assertEquals("آمریکا، بریتانیا",displayCountries("USA, United States of America, United Kingdom"))
        assertEquals("ایران",displayCountries("محصول Iran، ایران"))
    }
    @Test fun episodeDisplayIsCompactAndDoesNotInferMissingSeason() {
        assertEquals("قسمت ۲ • فصل ۴",compactEpisodeLabel("فصل 4 قسمت 2 در حال پخش"))
        assertEquals("قسمت ۱۰ • فصل ۴",compactEpisodeLabel("قسمت ۱۰ فصل چهارم پایان فصل"))
        assertEquals("قسمت ۶",compactEpisodeLabel("قسمت 6"))
        assertEquals("",compactEpisodeLabel("در حال پخش"))
    }
    @Test fun titleYearSuffixRemovedOnlyWhenSameYearIsKnown() {
        assertEquals("Ted Lasso",displayTitle("Ted Lasso (2020)","۲۰۲۰"))
        assertEquals("Ted Lasso (2021)",displayTitle("Ted Lasso (2021)","2020"))
        assertEquals("1917",displayTitle("1917","2019"))
    }
    private fun source(id:String,url:String) = SourceVariant(id,id,CatalogItem("Ted Lasso",CatalogKind.SERIES,url,serviceId=id))
    @Test fun sourceDisplayKeepsSelectedRouteAndAllUnderlyingRoutes() {
        val old=source("mymoviz","https://mymoviz.co/tvshows/14563/ted-lasso-2020")
        val modern=source("mymoviz","https://mymoviz.co/_modern/title/14563/ted-lasso-2020")
        val foreign=source("filmrooz","https://sean.robert-redford.net/post/series/5673834/ted-lasso")
        val raw=listOf(old,modern,foreign)
        assertEquals(listOf(modern,foreign),visibleSources(raw,modern.item.contentUrl))
        assertEquals(listOf(old,foreign),visibleSources(raw,old.item.contentUrl))
        assertEquals(3,raw.size)
    }
    @Test fun lightArtworkUsesDarkTextAndDarkArtworkKeepsWhite() {
        assertTrue(HeroContrastPolicy.useDarkText(1.0,1.0,1.0))
        assertTrue(HeroContrastPolicy.useDarkText(0.95,0.92,0.84))
        assertFalse(HeroContrastPolicy.useDarkText(0.10,0.12,0.25))
        assertTrue(HeroContrastPolicy.useDarkText(1.0,0.0,0.0))
        assertFalse(HeroContrastPolicy.isBrightArtwork(1.0,0.0,0.0))
    }
    @Test fun verifiedIranianTitleCreditFillsAliasesButNeverUnrelatedPerson() {
        val photo="https://tv.filmnet.ir/images/dd/61/person.jpg"
        val trusted=TrustedPortrait("filmnet:verified",setOf("شهاب حسینی","Shahab Hosseini"),photo)
        assertEquals(photo,PersonPortraitPolicy.fill(listOf(PersonRef("Shahab Hosseini")),listOf(trusted)).single().imageUrl)
        assertEquals("",PersonPortraitPolicy.fill(listOf(PersonRef("شهاب حسینیان")),listOf(trusted)).single().imageUrl)
    }
    @Test fun foregroundContrastRemainsAccessibleThroughoutGreyTransition() {
        for(step in 0..255) {
            val v=step/255.0
            val luminance=HeroContrastPolicy.luminance(v,v,v)
            val contrast=if(HeroContrastPolicy.useDarkText(v,v,v)) (luminance+0.05)/0.05
                else 1.05/(luminance+0.05)
            assertTrue("step=$step ratio=$contrast",contrast>=4.5)
        }
    }
}
