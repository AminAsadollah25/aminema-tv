package com.amin.tvos.data.model

import kotlinx.serialization.Serializable

/** Movie or series, as reported by the service's own catalog. */
@Serializable
enum class CatalogKind { MOVIE, SERIES }

/**
 * Recognises movie vs. series from a normal detail-page URL alone.
 *
 * Recently Opened and Favorites store only [MovieItem], which predates the catalog/search
 * kind field, so this is how those two rows learn whether direct-play is safe for a given
 * saved URL: `/medias/movies/`, `/medias/series/` (ParsiFlix) and `/post/film/`,
 * `/post/series/` (FilmRooz) are the same path segments the adapters already match on.
 */
fun catalogKindFromUrl(url: String): CatalogKind? = when {
    Regex("""/medias/movies/""").containsMatchIn(url) -> CatalogKind.MOVIE
    Regex("""/medias/series/""").containsMatchIn(url) -> CatalogKind.SERIES
    Regex("""/post/film/""").containsMatchIn(url) -> CatalogKind.MOVIE
    Regex("""/post/series/""").containsMatchIn(url) -> CatalogKind.SERIES
    else -> null
}

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
    val serviceId: String,
    /**
     * Ordinary, spoiler-safe release metadata such as `قسمت ۰۴ فصل سوم`.
     *
     * This describes what the provider has published. It deliberately does not claim that
     * the user has or has not watched the episode.
     */
    val episodeLabel: String = ""
)

/**
 * The cached "latest" catalog of a single service.
 *
 * The lists are kept separately because each one is the service's own ordering:
 * ParsiFlix has a real combined «جدیدترین‌ها» section plus per-type catalog pages, and
 * FilmRooz has new-film, episode-release-ordered series and curated-series pages whose
 * interleaved latest movie/series result forms [all].
 */
@Serializable
data class CatalogSection(
    val serviceId: String,
    val all: List<CatalogItem> = emptyList(),
    val movies: List<CatalogItem> = emptyList(),
    val series: List<CatalogItem> = emptyList(),
    /** Provider-curated series; kept separate from release-ordered [series]. */
    val popularSeries: List<CatalogItem> = emptyList(),
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
