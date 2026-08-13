package com.amin.tvos.ui.metadata

import org.junit.Assert.assertEquals
import org.junit.Test

class PersianMetadataLabelsTest {

    @Test
    fun translatesCommonEnglishGenreWithoutChangingPersianOrUnknownLabels() {
        assertEquals("تاریخی", "History".toPersianMetadataLabel())
        assertEquals("علمی‌تخیلی", "SCI-FI".toPersianMetadataLabel())
        assertEquals("کمدی عاشقانه", "romantic comedy film".toPersianMetadataLabel())
        assertEquals("سریال درام", "drama television series".toPersianMetadataLabel())
        assertEquals("درام", "درام".toPersianMetadataLabel())
        assertEquals("Neo Noir", "Neo Noir".toPersianMetadataLabel())
    }
}
