package com.amin.tvos.data

import com.amin.tvos.data.model.CanonicalMatchConfidence
import com.amin.tvos.data.model.CanonicalMedia
import com.amin.tvos.data.model.CatalogItem
import com.amin.tvos.data.model.CatalogKind
import com.amin.tvos.data.model.PersonRef
import com.amin.tvos.data.model.SearchResult
import com.amin.tvos.data.model.SourceVariant
import com.amin.tvos.data.model.TitleMetadata
import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale

/**
 * Conservative, provider-agnostic identity matching for the canonical Aminema library.
 *
 * A title string alone is never enough to merge two providers. The matcher requires a stable
 * public id, title+year+kind, or title+kind plus verified credit overlap. Ambiguous records stay
 * separate, which is less pretty than a false merge but far safer on a TV library.
 */
object CanonicalLibrary {

    fun fromSearchResults(
        results: List<SearchResult>,
        metadataByUrl: Map<String, TitleMetadata> = emptyMap(),
        providerNames: Map<String, String> = emptyMap()
    ): List<CanonicalMedia> = canonicalize(
        items = results.map { result ->
            CatalogItem(
                title = CanonicalText.displayTitle(result.title),
                kind = result.kind,
                contentUrl = result.contentUrl,
                posterUrl = result.posterUrl,
                serviceId = result.serviceId,
                year = result.year.ifBlank { CanonicalText.extractYear(result.title) },
                imdbId = result.imdbId,
                hasPersianDub = result.hasPersianDub,
                hasPersianSubtitle = result.hasPersianSubtitle,
                maxQualityHeight = result.maxQualityHeight,
                qualityLabel = result.qualityLabel
            )
        },
        metadataByUrl = metadataByUrl,
        providerNames = providerNames
    )

    /**
     * Interleaves each provider's own latest order, then collapses verified duplicates.
     * Home therefore gives both providers a fair chance without claiming a cross-site
     * publication timestamp that neither catalogue exposes consistently.
     */
    fun mergeLatest(
        providerLists: List<List<CatalogItem>>,
        metadataByUrl: Map<String, TitleMetadata> = emptyMap(),
        providerNames: Map<String, String> = emptyMap(),
        limit: Int = 24
    ): List<CanonicalMedia> {
        val balanced = buildList {
            val maxSize = providerLists.maxOfOrNull(List<CatalogItem>::size) ?: 0
            for (index in 0 until maxSize) {
                providerLists.forEach { list -> list.getOrNull(index)?.let(::add) }
            }
        }
        return canonicalize(balanced, metadataByUrl, providerNames).take(limit)
    }

    fun canonicalize(
        items: List<CatalogItem>,
        metadataByUrl: Map<String, TitleMetadata> = emptyMap(),
        providerNames: Map<String, String> = emptyMap()
    ): List<CanonicalMedia> {
        val variants = items
            .filter { it.title.isNotBlank() && it.contentUrl.isNotBlank() }
            .map { raw ->
                val urlKey = ContentMetadataPolicy.canonicalContentUrl(raw.contentUrl)
                val metadata = metadataByUrl[urlKey]
                SourceVariant(
                    providerId = raw.serviceId,
                    providerName = providerNames[raw.serviceId].orEmpty(),
                    item = raw.withMetadata(metadata),
                    imdbId = raw.imdbId.ifBlank { metadata?.imdbId.orEmpty() }
                )
            }

        val groups = mutableListOf<MutableGroup>()
        variants.forEach { candidate ->
            val best = groups
                .mapNotNull { group ->
                    val confidence = group.bestConfidence(candidate)
                    confidence.takeIf { it != CanonicalMatchConfidence.INDEPENDENT }
                        ?.let { group to it }
                }
                .maxByOrNull { (_, confidence) -> confidence.rank }

            if (best == null) {
                groups += MutableGroup(mutableListOf(candidate))
            } else {
                val (group, confidence) = best
                group.variants += candidate
                group.confidence = maxConfidence(group.confidence, confidence)
            }
        }

        return groups.map(MutableGroup::toCanonical)
    }

    private class MutableGroup(
        val variants: MutableList<SourceVariant>,
        var confidence: CanonicalMatchConfidence = CanonicalMatchConfidence.INDEPENDENT
    ) {
        fun bestConfidence(candidate: SourceVariant): CanonicalMatchConfidence {
            val candidateImdb = candidate.imdbId.lowercase(Locale.ROOT)
            val groupImdbIds = variants.map { it.imdbId.lowercase(Locale.ROOT) }
                .filter { it.isNotBlank() }
                .toSet()
            // Never allow a no-id bridge to transitively join two contradictory IMDb ids.
            if (candidateImdb.isNotBlank() && groupImdbIds.any { it != candidateImdb }) {
                return CanonicalMatchConfidence.INDEPENDENT
            }
            return variants
                .map { existing -> match(existing, candidate) }
                .maxByOrNull { confidence -> confidence.rank }
                ?: CanonicalMatchConfidence.INDEPENDENT
        }

        fun toCanonical(): CanonicalMedia {
            val distinct = variants.distinctBy {
                ContentMetadataPolicy.canonicalContentUrl(it.item.contentUrl)
            }
            val preferred = distinct.maxWithOrNull(
                compareBy<SourceVariant>(
                    { if (it.item.kind == CatalogKind.MOVIE && it.item.hasPersianDub) 1 else 0 },
                    { autoQualityRank(it.item.maxQualityHeight) },
                    { if (it.providerId == FILMROOZ_ID) 1 else 0 },
                    ::variantQuality
                )
            ) ?: variants.first()
            val representative = distinct
                .filterNot { it === preferred }
                .fold(preferred.item) { merged, variant -> merged.mergeDisplay(variant.item) }
            val imdbId = distinct.map { it.imdbId }.firstOrNull { it.isNotBlank() }.orEmpty()
            val year = representative.year.ifBlank {
                distinct.map { it.item.year }.firstOrNull { it.isNotBlank() }.orEmpty()
            }
            val normalized = CanonicalText.normalizeTitle(representative.title)
            val identity = when {
                imdbId.isNotBlank() -> "imdb:${imdbId.lowercase(Locale.ROOT)}"
                year.isNotBlank() -> "${representative.kind}:$normalized:$year"
                distinct.size > 1 -> "${representative.kind}:$normalized:${creditFingerprint(distinct)}"
                else -> ContentMetadataPolicy.canonicalContentUrl(preferred.item.contentUrl)
            }
            return CanonicalMedia(
                canonicalId = "aminema:${identity.sha1()}",
                representative = representative,
                normalizedTitle = normalized,
                year = year,
                imdbId = imdbId,
                variants = distinct,
                matchConfidence = confidence.takeIf { distinct.size > 1 }
                    ?: CanonicalMatchConfidence.INDEPENDENT
            )
        }
    }

    private fun match(
        first: SourceVariant,
        second: SourceVariant
    ): CanonicalMatchConfidence {
        val firstUrl = ContentMetadataPolicy.canonicalContentUrl(first.item.contentUrl)
        val secondUrl = ContentMetadataPolicy.canonicalContentUrl(second.item.contentUrl)
        if (firstUrl == secondUrl) return CanonicalMatchConfidence.SAME_SOURCE_PAGE
        if (first.item.kind != second.item.kind) return CanonicalMatchConfidence.INDEPENDENT

        val firstImdb = first.imdbId.lowercase(Locale.ROOT)
        val secondImdb = second.imdbId.lowercase(Locale.ROOT)
        if (firstImdb.isNotBlank() && secondImdb.isNotBlank()) {
            return if (firstImdb == secondImdb) {
                CanonicalMatchConfidence.IMDb
            } else {
                CanonicalMatchConfidence.INDEPENDENT
            }
        }
        // Different pages from the same provider can represent remakes, cuts or duplicates;
        // title/year alone is not enough to collapse a provider's own distinct records.
        if (first.providerId == second.providerId) {
            return CanonicalMatchConfidence.INDEPENDENT
        }

        val firstTitle = CanonicalText.normalizeTitle(first.item.title)
        val secondTitle = CanonicalText.normalizeTitle(second.item.title)
        val titleAlias = CanonicalText.isClearSubtitleAlias(
            first.item.title,
            second.item.title
        )
        if (firstTitle.isBlank() || (firstTitle != secondTitle && !titleAlias)) {
            return CanonicalMatchConfidence.INDEPENDENT
        }

        val firstYear = CanonicalText.normalizeYear(first.item.year)
        val secondYear = CanonicalText.normalizeYear(second.item.year)
        if (firstYear.isNotBlank() && secondYear.isNotBlank()) {
            val firstYearNumber = firstYear.toIntOrNull()
            val secondYearNumber = secondYear.toIntOrNull()
            if (firstYearNumber == null || secondYearNumber == null) {
                return CanonicalMatchConfidence.INDEPENDENT
            }
            val yearDifference = kotlin.math.abs(firstYearNumber - secondYearNumber)
            return when {
                !titleAlias && yearDifference == 0 -> CanonicalMatchConfidence.TITLE_YEAR_KIND
                !titleAlias && yearDifference == 1 -> CanonicalMatchConfidence.TITLE_YEAR_DRIFT
                titleAlias && yearDifference == 0 -> CanonicalMatchConfidence.TITLE_ALIAS_YEAR
                titleAlias && yearDifference == 1 ->
                    CanonicalMatchConfidence.TITLE_ALIAS_YEAR_DRIFT
                else -> CanonicalMatchConfidence.INDEPENDENT
            }
        }

        return if (hasStrongCreditOverlap(first.item, second.item)) {
            CanonicalMatchConfidence.TITLE_CREDITS_KIND
        } else {
            CanonicalMatchConfidence.INDEPENDENT
        }
    }

    private fun hasStrongCreditOverlap(first: CatalogItem, second: CatalogItem): Boolean {
        val firstDirectors = first.directors.normalizedNames()
        val secondDirectors = second.directors.normalizedNames()
        if (firstDirectors.isNotEmpty() && (firstDirectors intersect secondDirectors).isNotEmpty()) {
            return true
        }
        val sharedCast = first.cast.normalizedNames() intersect second.cast.normalizedNames()
        return sharedCast.size >= 2
    }

    private fun List<PersonRef>.normalizedNames(): Set<String> =
        map { CanonicalText.normalizeTitle(it.name) }.filter { it.isNotBlank() }.toSet()

    private fun CatalogItem.withMetadata(metadata: TitleMetadata?): CatalogItem {
        if (metadata == null) return copy(
            title = CanonicalText.displayTitle(title),
            year = year.ifBlank { CanonicalText.extractYear(title) }
        )
        return copy(
            title = CanonicalText.displayTitle(title),
            posterUrl = posterUrl.ifBlank { metadata.posterUrl },
            backdropUrl = backdropUrl.ifBlank { metadata.backdropUrl },
            summary = metadata.summary.ifBlank { summary },
            year = metadata.year.ifBlank { year.ifBlank { CanonicalText.extractYear(title) } },
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
    }

    private fun CatalogItem.mergeDisplay(other: CatalogItem): CatalogItem = copy(
        imdbId = imdbId.ifBlank { other.imdbId },
        posterUrl = posterUrl.ifBlank { other.posterUrl },
        backdropUrl = backdropUrl.ifBlank { other.backdropUrl },
        episodeLabel = episodeLabel.ifBlank { other.episodeLabel },
        summary = listOf(summary, other.summary).maxByOrNull(String::length).orEmpty(),
        year = year.ifBlank { other.year },
        genres = genres.ifEmpty { other.genres },
        rating = rating.ifBlank { other.rating },
        runtime = runtime.ifBlank { other.runtime },
        country = country.ifBlank { other.country },
        language = language.ifBlank { other.language },
        hasPersianDub = hasPersianDub || other.hasPersianDub,
        hasPersianSubtitle = hasPersianSubtitle || other.hasPersianSubtitle,
        maxQualityHeight = maxOf(maxQualityHeight, other.maxQualityHeight),
        qualityLabel = qualityLabel.ifBlank { other.qualityLabel },
        directors = directors.ifEmpty { other.directors },
        cast = cast.ifEmpty { other.cast }
    )

    private fun variantQuality(variant: SourceVariant): Int = with(variant.item) {
        (if (posterUrl.isNotBlank()) 2 else 0) +
            (if (backdropUrl.isNotBlank()) 2 else 0) +
            (if (summary.isNotBlank()) 3 else 0) +
            (if (year.isNotBlank()) 1 else 0) +
            (if (directors.isNotEmpty()) 2 else 0) +
            minOf(cast.size, 3) +
            (if (hasPersianDub) 1 else 0)
    }

    private fun autoQualityRank(height: Int): Int = when {
        height >= 2160 -> 1080 // 4K remains manual; ordinary 1080 is the auto ceiling.
        height >= 1080 -> 1080
        height >= 720 -> 720
        height >= 480 -> 480
        else -> 0
    }

    private fun creditFingerprint(variants: List<SourceVariant>): String = variants
        .flatMap { it.item.directors + it.item.cast.take(2) }
        .map { CanonicalText.normalizeTitle(it.name) }
        .filter { it.isNotBlank() }
        .distinct()
        .sorted()
        .joinToString("|")

    private val CanonicalMatchConfidence.rank: Int
        get() = when (this) {
            CanonicalMatchConfidence.IMDb -> 4
            CanonicalMatchConfidence.TITLE_YEAR_KIND -> 3
            CanonicalMatchConfidence.TITLE_ALIAS_YEAR -> 3
            CanonicalMatchConfidence.TITLE_YEAR_DRIFT -> 2
            CanonicalMatchConfidence.TITLE_ALIAS_YEAR_DRIFT -> 2
            CanonicalMatchConfidence.TITLE_CREDITS_KIND -> 2
            CanonicalMatchConfidence.SAME_SOURCE_PAGE -> 1
            CanonicalMatchConfidence.INDEPENDENT -> 0
        }

    private fun maxConfidence(
        first: CanonicalMatchConfidence,
        second: CanonicalMatchConfidence
    ): CanonicalMatchConfidence = if (first.rank >= second.rank) first else second

    private fun String.sha1(): String = MessageDigest.getInstance("SHA-1")
        .digest(toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }

    private const val FILMROOZ_ID = "filmrooz"
}

/** Text-only utilities shared by matching and search presentation. */
object CanonicalText {
    private val trailingYear = Regex("""\s*[\[(（]\s*([۱۲۳۴۵۶۷۸۹۰١٢٣٤٥٦٧٨٩٠0-9]{4})\s*[\])）]\s*$""")

    fun displayTitle(value: String): String = value
        .replace(trailingYear, "")
        .replace(Regex("""\s+"""), " ")
        .trim()

    fun extractYear(value: String): String = trailingYear.find(value)
        ?.groupValues?.getOrNull(1)
        .orEmpty()
        .toLatinDigits()

    fun normalizeYear(value: String): String = value.toLatinDigits()
        .filter(Char::isDigit)
        .take(4)

    fun normalizeTitle(value: String): String {
        val folded = Normalizer.normalize(displayTitle(value), Normalizer.Form.NFKD)
            .lowercase(Locale.ROOT)
            .toLatinDigits()
            .replace("&", " and ")
            .replace('ي', 'ی')
            .replace('ى', 'ی')
            .replace('ك', 'ک')
            .replace('ة', 'ه')
            .replace('ۀ', 'ه')
        return folded.filter { it.isLetterOrDigit() }
    }

    /**
     * Detects a clear editorial subtitle alias such as
     * `Young Washington: A Founder's Story` vs `Young Washington`.
     *
     * This intentionally avoids fuzzy similarity: only an explicit subtitle delimiter may
     * be removed, and the shorter title must be the complete prefix of the longer one.
     */
    fun isClearSubtitleAlias(first: String, second: String): Boolean {
        val firstTitle = normalizeTitle(first)
        val secondTitle = normalizeTitle(second)
        if (firstTitle.isBlank() || secondTitle.isBlank() || firstTitle == secondTitle) {
            return false
        }
        val firstCore = subtitleCore(first)
        val secondCore = subtitleCore(second)
        return firstCore.isNotBlank() && secondCore.isNotBlank() &&
            (firstCore == secondTitle || secondCore == firstTitle)
    }

    private fun subtitleCore(value: String): String {
        val core = value.trim().split(
            Regex("\\s*[:：]\\s*|\\s+[–—-]\\s+")
        ).firstOrNull().orEmpty()
        return normalizeTitle(core)
    }

    private fun String.toLatinDigits(): String = map { character ->
        when (character) {
            in '۰'..'۹' -> ('0'.code + character.code - '۰'.code).toChar()
            in '٠'..'٩' -> ('0'.code + character.code - '٠'.code).toChar()
            else -> character
        }
    }.joinToString("")
}
