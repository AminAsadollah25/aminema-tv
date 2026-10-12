package com.amin.tvos.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PublicMatchYearTest {
    @Test fun solarYearOverridesUploadYear() {
        assertEquals(2022, publicMatchYear("parsiflix", "2026", "۱۴۰۱"))
        assertEquals(2020, publicMatchYear("parsiflix", "١٣٩٩", ""))
    }
    @Test fun uploadYearIsNotProductionEvidence() {
        assertNull(publicMatchYear("parsiflix", "2026", "2026"))
        assertNull(publicMatchYear("parsiflix", "", ""))
    }
    @Test fun foreignGregorianYearsRemainUnchanged() {
        assertEquals(2020, publicMatchYear("filmrooz", "2020", ""))
        assertEquals(2021, publicMatchYear("mymoviz", "", "۲۰۲۱"))
        assertNull(publicMatchYear("filmrooz", "unknown", ""))
    }
}
