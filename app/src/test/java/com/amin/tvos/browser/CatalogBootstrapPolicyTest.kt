package com.amin.tvos.browser

import com.amin.tvos.data.model.CatalogItem
import com.amin.tvos.data.model.CatalogKind
import com.amin.tvos.data.model.CatalogSection
import org.junit.Assert.*
import org.junit.Test

class CatalogBootstrapPolicyTest {
    private val iranian = setOf("farsiland", "filmrooz", "mymoviz")
    private fun needed(ready: Boolean = true, attempted: Boolean = false,
                       ids: Set<String> = iranian, sections: List<CatalogSection> = emptyList()) =
        CatalogBootstrapPolicy.needsBootstrap(ready, attempted, ids, sections)

    @Test fun migrationWithOnlyForeignCacheNeedsIranianBootstrap() {
        assertTrue(needed(sections = listOf(CatalogSection("filmrooz"))))
    }
    @Test fun waitForCompleteCacheAndConfiguredProvider() {
        assertFalse(needed(ready = false))
        assertFalse(needed(ids = setOf("filmrooz")))
    }
    @Test fun oneAttemptEvenWhenNetworkFails() {
        assertFalse(needed(attempted = true))
        assertTrue(needed(sections = listOf(CatalogSection("farsiland", error = "timeout"))))
    }
    @Test fun existingRowsInAnyCatalogWindowMustNotTriggerScan() {
        val item = CatalogItem("Cached", CatalogKind.MOVIE,
            "https://farsiland.com/movies/cached/", serviceId = "farsiland")
        listOf(CatalogSection("farsiland", all = listOf(item)),
            CatalogSection("farsiland", movies = listOf(item)),
            CatalogSection("farsiland", series = listOf(item.copy(kind = CatalogKind.SERIES))))
            .forEach { assertFalse(needed(sections = listOf(it))) }
    }
}
