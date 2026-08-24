package com.amin.tvos.data.model

import kotlinx.serialization.Serializable

/**
 * One ordinary title page that can satisfy a canonical Aminema title.
 *
 * The complete [item] is intentionally kept here instead of only a URL: Spotlight can switch
 * provider without issuing another catalog search, while the only navigable value remains the
 * provider's normal detail page. No media URL, token, manifest or DRM value belongs here.
 */
@Serializable
data class SourceVariant(
    val providerId: String,
    val providerName: String = "",
    val item: CatalogItem,
    /** Public title identifier, when already verified from normal metadata. */
    val imdbId: String = ""
)

/** Why two provider records were considered the same title. */
@Serializable
enum class CanonicalMatchConfidence {
    /** One provider repeated the exact same normal detail page in multiple lists. */
    SAME_SOURCE_PAGE,

    /** Both normal title pages expose the same public IMDb title id. */
    IMDb,

    /** Normalised title, release year and movie/series kind all agree. */
    TITLE_YEAR_KIND,

    /** Exact title/kind match with one provider's verified public IMDb id and one missing year. */
    TITLE_KIND_PUBLIC_ID,

    /** Exact title/kind match where providers differ by at most one publication year. */
    TITLE_YEAR_DRIFT,

    /** One provider adds a clear subtitle while title, year and kind agree. */
    TITLE_ALIAS_YEAR,

    /** Clear subtitle alias with the same small one-year publication drift. */
    TITLE_ALIAS_YEAR_DRIFT,

    /** Title/kind agree and verified director/cast evidence overlaps. */
    TITLE_CREDITS_KIND,

    /** No safe cross-provider match was found; the record stays independent. */
    INDEPENDENT
}

/**
 * Aminema's in-memory identity for one movie or series across providers.
 *
 * v0.16.6 derives this model from the existing caches and search results. It deliberately does
 * not migrate `library.json`, cookies, Continue Watching or any other persistent owner data.
 */
data class CanonicalMedia(
    val canonicalId: String,
    val representative: CatalogItem,
    val normalizedTitle: String,
    val year: String = "",
    val imdbId: String = "",
    val variants: List<SourceVariant>,
    val matchConfidence: CanonicalMatchConfidence = CanonicalMatchConfidence.INDEPENDENT
)
