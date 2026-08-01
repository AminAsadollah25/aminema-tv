package com.amin.tvos.ui.metadata

import com.amin.tvos.data.model.CatalogKind
import com.amin.tvos.data.model.SpotlightItem
import org.junit.Assert.assertEquals
import org.junit.Test

class DisplayMetadataTest {

    @Test
    fun `iranian gregorian release year is displayed as solar hijri`() {
        assertEquals("۱۴۰۵", displayReleaseYear(item("parsiflix", "2026")))
    }

    @Test
    fun `verified iranian solar year is preserved`() {
        assertEquals("۱۴۰۴", displayReleaseYear(item("parsiflix", "۱۴۰۴")))
    }

    @Test
    fun `international year remains gregorian`() {
        assertEquals("2026", displayReleaseYear(item("filmrooz", "2026")))
    }

    private fun item(serviceId: String, year: String) = SpotlightItem(
        title = "Test",
        kind = CatalogKind.MOVIE,
        contentUrl = "https://example.com/title",
        serviceId = serviceId,
        year = year
    )
}
