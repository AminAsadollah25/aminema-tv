package com.amin.tvos.data.model

import kotlinx.serialization.Serializable

/** Movie or series, as reported by the service's own catalog. */
@Serializable
enum class CatalogKind { MOVIE, SERIES }

/** The `همه | فیلم | سریال` selector above each latest row. */
@Serializable
enum class CatalogFilter(val label: String) {
    ALL("همه"),
    MOVIE("فیلم"),
    SERIES("سریال")
}

/**
 * One title in a "latest" row.
 *
 * [contentUrl] is always the service's own normal detail page. No media URL, stream URL,
 * DRM value or token is ever stored here.
 */
@Serializable
data class CatalogItem(
    val title: String,
    val kind: CatalogKind,
    val contentUrl: String,
    val posterUrl: String = "",
    val serviceId: String
)

/**
 * The cached "latest" catalog of a single service.
 *
 * The three lists are kept separately because each one is the service's own ordering:
 * ParsiFlix has a real combined «جدیدترین‌ها» section plus per-type catalog pages, and
 * FilmRooz has separate new-films / new-tv-show pages whose union forms [all].
 */
@Serializable
data class CatalogSection(
    val serviceId: String,
    val all: List<CatalogItem> = emptyList(),
    val movies: List<CatalogItem> = emptyList(),
    val series: List<CatalogItem> = emptyList(),
    val syncedAt: Long = 0L,
    /** Adapter failure text for this service only; the other row stays usable. */
    val error: String = ""
) {
    fun items(filter: CatalogFilter): List<CatalogItem> = when (filter) {
        CatalogFilter.ALL -> all
        CatalogFilter.MOVIE -> movies
        CatalogFilter.SERIES -> series
    }
}
