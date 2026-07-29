package com.amin.tvos.data.model

import kotlinx.serialization.Serializable

/** The single dominant action shown on a native Aminema title page. */
@Serializable
enum class SpotlightAction {
    WATCH,
    CONTINUE
}

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
    val posterUrl: String = "",
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
    val primaryAction: SpotlightAction = SpotlightAction.WATCH,
    /** Normal top-level page BrowserActivity should open after the second click. */
    val browserStartUrl: String = contentUrl,
    val resumePosition: Long = 0L,
    val duration: Long = 0L,
    val autoResume: Boolean = false,
    val directPlay: Boolean = false,
    val resumeStrategy: ResumeStrategy? = null,
    val actionButtonTextPatterns: List<String> = emptyList()
)

/** Cached visible metadata read from one normal provider detail page. */
@Serializable
data class TitleMetadata(
    val contentUrl: String,
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
    val fetchedAt: Long = System.currentTimeMillis()
)

fun SpotlightItem.withMetadata(metadata: TitleMetadata): SpotlightItem = copy(
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
    cast = metadata.cast.ifEmpty { cast }
)
