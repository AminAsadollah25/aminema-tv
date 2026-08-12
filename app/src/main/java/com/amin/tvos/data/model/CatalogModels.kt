package com.amin.tvos.data.model

import kotlinx.serialization.Serializable

/** Movie or series, as reported by the service's own catalog. */
@Serializable
enum class CatalogKind { MOVIE, SERIES }

/**
 * A provider-supplied person reference, deliberately small enough for the local catalog.
 *
 * [providerId] and [profileUrl] make the model ready for a future native Person page without
 * forcing Aminema to guess that two people with the same display name are identical.
 */
@Serializable
data class PersonRef(
    val name: String,
    val providerId: String = "",
    val profileUrl: String = ""
)

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
    /**
     * The portrait poster, roughly 2:3 — the shape every card in a rail is built for.
     *
     * Both providers publish two separate artworks per title and they are not
     * interchangeable: the portrait one belongs here, and the wide one in [backdropUrl].
     * Filling this with the wide artwork is what used to crop a landscape image into a
     * narrow, magnified strip inside a poster card.
     */
    val posterUrl: String = "",
    /**
     * The provider's own wide key art, roughly 16:9 — what their site puts in its banner
     * carousel. Empty when a provider does not publish one for this title.
     */
    val backdropUrl: String = "",
    val serviceId: String,
    /** Public IMDb title id exposed by the provider's ordinary catalog/detail page. */
    val imdbId: String = "",
    /**
     * Ordinary, spoiler-safe release metadata such as `قسمت ۰۴ فصل سوم`.
     *
     * This describes what the provider has published. It deliberately does not claim that
     * the user has or has not watched the episode.
     */
    val episodeLabel: String = "",
    /** Spoiler-safe title synopsis supplied on the provider's catalog card/API. */
    val summary: String = "",
    /** Compact metadata used by the TV hover preview. */
    val year: String = "",
    val genres: List<String> = emptyList(),
    val rating: String = "",
    val runtime: String = "",
    val country: String = "",
    val language: String = "",
    val hasPersianDub: Boolean = false,
    val hasPersianSubtitle: Boolean = false,
    /** Best ordinary catalogue quality; 2160/4K is retained but never auto-selected. */
    val maxQualityHeight: Int = 0,
    val qualityLabel: String = "",
    /** Ordinary public title credits, when the provider exposes them in its catalog card/API. */
    val directors: List<PersonRef> = emptyList(),
    val cast: List<PersonRef> = emptyList()
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
    /**
     * What the provider itself puts in the banner carousel on its own home page: an
     * editorial pick rather than anything recency-ordered, and the only list whose items
     * are guaranteed to carry wide [CatalogItem.backdropUrl] artwork.
     */
    val featured: List<CatalogItem> = emptyList(),
    val syncedAt: Long = 0L,
    /** Adapter failure text for this service only; the other row stays usable. */
    val error: String = "",
    /** Largest provider page window successfully stored for this service. */
    val loadedPageLimit: Int = 0,
    /** True only when the provider confirmed another page/window is available. */
    val hasMoreAll: Boolean = false,
    val hasMoreMovies: Boolean = false,
    val hasMoreSeries: Boolean = false,
    val hasMorePopularSeries: Boolean = false
) {
    fun items(filter: CatalogFilter): List<CatalogItem> = when (filter) {
        CatalogFilter.ALL -> all
        CatalogFilter.MOVIE -> movies
        CatalogFilter.SERIES -> series
    }
}
