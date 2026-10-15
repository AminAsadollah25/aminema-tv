package com.amin.tvos.data

import com.amin.tvos.data.model.CatalogItem
import com.amin.tvos.data.model.CatalogKind
import java.io.File
import org.junit.Assert.*
import org.junit.Test

class MarathonAtlasTest {
    private fun atlas():List<MarathonFamily> {
        val file=listOf(File("src/main/assets/marathon_atlas.json"),File("app/src/main/assets/marathon_atlas.json"))
            .first {it.exists()}
        return MarathonAtlas.parse(file.readText())
    }
    @Test fun allResearchGroupsHaveVisibleFamiliesWithStableIdsAndRealQuerySeeds() {
        val rows=atlas()
        assertEquals(594,rows.size)
        assertEquals(rows.size,rows.map {it.id}.toSet().size)
        assertEquals(MarathonAtlas.groups.map {it.id}.toSet(),rows.flatMap {it.groups}.toSet())
        assertTrue(rows.all {it.queries.isNotEmpty() && it.queries.size<=3})
    }
    @Test fun exactReferenceCardsRemainBackedByExistingWorksNotSearchGuesses() {
        val refs=atlas().filter {it.referenceId.isNotBlank()}
        assertEquals(16,refs.size)
        refs.forEach { assertTrue(it.id,JourneyCatalog.select(it.referenceId).isNotEmpty()) }
        assertEquals(12,JourneyCatalog.marathonCollections.size)
        assertEquals(6,JourneyCatalog.marvelCollections.size)
    }
    @Test fun importantPreviouslyMissingBranchesAreNotOmitted() {
        val rows=atlas()
        listOf("terminator","matrix","rambo","hunger-games","divergent","alien","predator",
            "saw","scream","pixar","ghibli","toy-story","justice-league-unlimited","نون-خ","زیرخاکی")
            .forEach { id -> assertTrue(id,rows.any {it.id==id}) }
    }
    @Test fun relatedSearchCannotTreatOneSharedWordOrSubstringAsMembership() {
        val family=MarathonFamily("terminator","ترمیناتور","Terminator",listOf("scifi"),listOf("Terminator"))
        fun item(title:String)=CatalogItem(title,CatalogKind.MOVIE,"https://provider.test/post/film/1",serviceId="filmrooz")
        assertTrue(MarathonAtlas.related(family,item("Terminator 2: Judgment Day")))
        assertTrue(MarathonAtlas.related(family,item("The Terminator")))
        assertFalse(MarathonAtlas.related(family,item("The Exterminator")))
        assertFalse(MarathonAtlas.related(family,item("Judgment Day")))
    }
    @Test fun discoveryCacheKeysAreIsolatedFromVerifiedJourneyLookupsAndOtherFamilies() {
        val a=MarathonDiscoveryRepository.key("terminator","filmrooz","Terminator")
        assertTrue(a.startsWith(MarathonDiscoveryRepository.prefix("terminator")))
        assertFalse(a.startsWith(MarathonDiscoveryRepository.prefix("matrix")))
        assertNotEquals(a,MarathonDiscoveryRepository.key("terminator","mymoviz","Terminator"))
        assertEquals(60,MarathonDiscoveryRepository.MAX_RECORDS)
    }
    @Test fun corruptDuplicateManifestIsRejectedInsteadOfRenderingUnstableKeys() {
        val json="""[{"id":"x","title":"X","sourceLabel":"X","groups":["action"],"queries":["X"]},{"id":"x","title":"Y","sourceLabel":"Y","groups":["action"],"queries":["Y"]}]"""
        assertTrue(runCatching {MarathonAtlas.parse(json)}.isFailure)
    }
    @Test fun localFamilyFilterHandlesPersianAndOriginalNamesWithoutNetwork() {
        val rows=atlas()
        assertEquals("terminator",rows.single {it.id=="terminator"}.takeIf {MarathonAtlas.matchesFilter(it,"ترمیناتور")}?.id)
        assertTrue(rows.filter {MarathonAtlas.matchesFilter(it,"Terminator")}.any {it.id=="terminator"})
        assertFalse(MarathonAtlas.matchesFilter(rows.first(),"definitely-no-family-with-this-title"))
    }
}
