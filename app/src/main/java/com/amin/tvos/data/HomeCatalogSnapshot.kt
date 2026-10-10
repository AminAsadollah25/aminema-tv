package com.amin.tvos.data

import com.amin.tvos.data.model.CatalogSection
import kotlinx.serialization.Serializable

/** Disposable projection, never the source of truth for Library or pagination. */
@Serializable
internal data class HomeCatalogSnapshot(
    val schemaVersion: Int = 1,
    val sourceLength: Long,
    val sourceModified: Long,
    val sections: List<CatalogSection>
) {
    fun matchesSource(length: Long, modified: Long): Boolean =
        schemaVersion == 1 && sourceLength == length && sourceModified == modified
}

/** Keeps provider order, kind, artwork and metadata; nothing in the full cache is trimmed. */
internal fun compactHomeSections(sections: List<CatalogSection>): List<CatalogSection> =
    sections.map { section ->
        section.copy(
            all = section.all.take(24),
            movies = section.movies.take(24),
            series = section.series.take(24),
            popularSeries = section.popularSeries.take(12),
            featured = section.featured.take(6)
        )
    }
