package com.amin.tvos.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AwardEvent(
    val event: String,
    val awards: List<String> = emptyList()
)

/** The single dominant action shown on a native Aminema title page. */
@Serializable
enum class SpotlightAction {
    WATCH,
    CONTINUE,
    SELECT_EPISODE,
    LATEST_EPISODE
}

/** Safe default when a title has no provider-specific resume action. */
fun CatalogKind.defaultSpotlightAction(): SpotlightAction =
    if (this == CatalogKind.SERIES) SpotlightAction.SELECT_EPISODE else SpotlightAction.WATCH

/**
 * Everything the native title page needs.
 *
 * Only ordinary title metadata and normal provider page URLs live here. The exact browser
 * request is carried with the item so opening Spotlight never changes the already-tested
 * direct-play or Continue Watching behaviour.
 */
@Serializable
data class SpotlightItem(
    val title: String,
    val kind: CatalogKind,
    val contentUrl: String,
    /** Portrait artwork, ~2:3 — see [CatalogItem.posterUrl] on why the two are not swappable. */
    val posterUrl: String = "",
    /** The provider's own wide key art, ~16:9, when it publishes one. */
    val backdropUrl: String = "",
    val serviceId: String,
    val serviceName: String = "",
    val summary: String = "",
    val year: String = "",
    val genres: List<String> = emptyList(),
    val rating: String = "",
    val runtime: String = "",
    val episodeLabel: String = "",
    val country: String = "",
    val language: String = "",
    val hasPersianDub: Boolean = false,
    val hasPersianSubtitle: Boolean = false,
    val directors: List<PersonRef> = emptyList(),
    val cast: List<PersonRef> = emptyList(),
    val awards: List<AwardEvent> = emptyList(),
    val primaryAction: SpotlightAction = SpotlightAction.WATCH,
    /** Normal top-level page BrowserActivity should open after the second click. */
    val browserStartUrl: String = contentUrl,
    val resumePosition: Long = 0L,
    val duration: Long = 0L,
    val editionTimelineId: String = "",
    val autoResume: Boolean = false,
    val directPlay: Boolean = false,
    val resumeStrategy: ResumeStrategy? = null,
    val actionButtonTextPatterns: List<String> = emptyList(),
    /** Stable in-memory identity shared by provider variants; empty for legacy entries. */
    val canonicalId: String = "",
    /**
     * Alternative normal provider title pages for the same verified movie/series.
     * Defaults preserve every old Intent/cache payload and no persistent library migration is
     * performed in v0.16.6.
     */
    val sourceVariants: List<SourceVariant> = emptyList()
)

/** Cached visible metadata read from one normal provider detail page. */
@Serializable
data class TitleMetadata(
    val contentUrl: String,
    /** Public/provider portrait and wide artwork used only when the catalog has a gap. */
    val posterUrl: String = "",
    val backdropUrl: String = "",
    val summary: String = "",
    val year: String = "",
    val genres: List<String> = emptyList(),
    val rating: String = "",
    val runtime: String = "",
    val country: String = "",
    val language: String = "",
    val hasPersianDub: Boolean = false,
    val hasPersianSubtitle: Boolean = false,
    val directors: List<PersonRef> = emptyList(),
    val cast: List<PersonRef> = emptyList(),
    val awards: List<AwardEvent> = emptyList(),
    /** Public title identifier exposed by the normal provider page, never a media id. */
    val imdbId: String = "",
    /** Prevents repeatedly querying a public fallback when no matching article exists. */
    val externalLookupAt: Long = 0L,
    /** Bump when a new trusted metadata source is added so old negative cache can retry. */
    val externalLookupVersion: Int = 0,
    val fetchedAt: Long = System.currentTimeMillis()
)

/**
 * Merge field by field so a partial provider refresh never erases richer cached data.
 * [newer] wins only where it actually carries a value; booleans are affirmative evidence.
 */
fun TitleMetadata.mergePrefer(newer: TitleMetadata): TitleMetadata = copy(
    contentUrl = newer.contentUrl.ifBlank { contentUrl },
    posterUrl = newer.posterUrl.ifBlank { posterUrl },
    backdropUrl = newer.backdropUrl.ifBlank { backdropUrl },
    summary = newer.summary.ifBlank { summary },
    year = newer.year.ifBlank { year },
    genres = newer.genres.ifEmpty { genres },
    rating = newer.rating.ifBlank { rating },
    runtime = newer.runtime.ifBlank { runtime },
    country = newer.country.ifBlank { country },
    language = newer.language.ifBlank { language },
    hasPersianDub = hasPersianDub || newer.hasPersianDub,
    hasPersianSubtitle = hasPersianSubtitle || newer.hasPersianSubtitle,
    directors = newer.directors.ifEmpty { directors },
    cast = newer.cast.ifEmpty { cast },
    awards = newer.awards.ifEmpty { awards },
    imdbId = newer.imdbId.ifBlank { imdbId },
    externalLookupAt = maxOf(externalLookupAt, newer.externalLookupAt),
    externalLookupVersion = maxOf(externalLookupVersion, newer.externalLookupVersion),
    fetchedAt = maxOf(fetchedAt, newer.fetchedAt)
)

/** The decision-making fields requested for a useful, spoiler-safe title page. */
fun TitleMetadata.isDecisionComplete(): Boolean =
    summary.isNotBlank() && year.isNotBlank() && directors.isNotEmpty() && cast.isNotEmpty()

fun SpotlightItem.withMetadata(metadata: TitleMetadata): SpotlightItem = copy(
    posterUrl = posterUrl.ifBlank { metadata.posterUrl },
    backdropUrl = backdropUrl.ifBlank { metadata.backdropUrl },
    summary = metadata.summary.ifBlank { summary },
    year = metadata.year.ifBlank { year },
    genres = metadata.genres.ifEmpty { genres },
    rating = metadata.rating.ifBlank { rating },
    runtime = metadata.runtime.ifBlank { runtime },
    country = metadata.country.ifBlank { country },
    language = metadata.language.ifBlank { language },
    hasPersianDub = hasPersianDub || metadata.hasPersianDub,
    hasPersianSubtitle = hasPersianSubtitle || metadata.hasPersianSubtitle,
    directors = metadata.directors.ifEmpty { directors },
    cast = metadata.cast.ifEmpty { cast },
    awards = metadata.awards.ifEmpty { awards }
)

/**
 * Changes only the provider route behind a canonical title page.
 *
 * The title, artwork, synopsis, IMDb rating, language badges and credits belong to the
 * canonical Aminema panel. They must not jump when the user merely chooses another place to
 * watch the same verified title. Provider-specific metadata can still improve the canonical
 * item before this screen opens, but source selection itself is a playback decision only.
 */
fun SpotlightItem.withPlaybackSource(
    source: SourceVariant,
    providerSupportsDirectPlay: Boolean
): SpotlightItem {
    val variant = source.item
    return copy(
        kind = variant.kind,
        contentUrl = variant.contentUrl,
        serviceId = source.providerId,
        serviceName = source.providerName,
        primaryAction = variant.kind.defaultSpotlightAction(),
        browserStartUrl = variant.contentUrl,
        resumePosition = 0L,
        duration = 0L,
        editionTimelineId = "",
        autoResume = false,
        directPlay = variant.kind == CatalogKind.MOVIE && providerSupportsDirectPlay,
        resumeStrategy = null,
        actionButtonTextPatterns = emptyList()
    )
}
