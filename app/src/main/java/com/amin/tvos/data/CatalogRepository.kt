package com.amin.tvos.data

import android.content.Context
import com.amin.tvos.data.model.CatalogSection
import com.amin.tvos.data.model.CatalogItem
import com.amin.tvos.data.model.TitleMetadata
import com.amin.tvos.data.model.mergePrefer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Cache of each service's "latest" row.
 *
 * Same lightweight JSON-in-app-storage approach as [LibraryRepository]: Home renders from
 * the cache instantly at startup while cold-start/manual provider jobs refresh behind Home.
 * Each service is stored independently, so a broken adapter can never empty the other row.
 */
class CatalogRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val file: File get() = File(context.filesDir, "catalog.json")
    private val metadataFile: File get() = File(context.filesDir, "title_metadata.json")

    private val _sections = MutableStateFlow<List<CatalogSection>>(emptyList())
    val sections: StateFlow<List<CatalogSection>> = _sections.asStateFlow()
    private val _refreshingServices = MutableStateFlow<Set<String>>(emptySet())
    val refreshingServices: StateFlow<Set<String>> = _refreshingServices.asStateFlow()
    private val _titleMetadata = MutableStateFlow<Map<String, TitleMetadata>>(emptyMap())
    val titleMetadata: StateFlow<Map<String, TitleMetadata>> = _titleMetadata.asStateFlow()

    suspend fun load() = withContext(Dispatchers.IO) {
        val loaded = runCatching {
            json.decodeFromString<List<CatalogSection>>(file.readText())
        }.getOrDefault(emptyList())
        val normalized = loaded.map(::normalize)
        if (normalized != loaded) {
            runCatching { file.writeText(json.encodeToString(normalized)) }
        }
        _sections.value = normalized

        val metadata = runCatching {
            json.decodeFromString<List<TitleMetadata>>(metadataFile.readText())
        }.getOrDefault(emptyList())
            .map(::normalize)
            .filter { it.contentUrl.isNotBlank() }
            .sortedByDescending { it.fetchedAt }
            .take(MAX_TITLE_METADATA)
            .associateBy { ContentMetadataPolicy.canonicalContentUrl(it.contentUrl) }
        _titleMetadata.value = metadata
    }

    fun section(serviceId: String): CatalogSection? =
        _sections.value.firstOrNull { it.serviceId == serviceId }

    fun metadataFor(contentUrl: String): TitleMetadata? =
        _titleMetadata.value[ContentMetadataPolicy.canonicalContentUrl(contentUrl)]

    suspend fun saveTitleMetadata(metadata: TitleMetadata) = withContext(Dispatchers.IO) {
        val normalized = normalize(metadata)
        if (normalized.contentUrl.isBlank()) return@withContext
        val key = ContentMetadataPolicy.canonicalContentUrl(normalized.contentUrl)
        val complete = _titleMetadata.value[key]?.mergePrefer(normalized) ?: normalized
        val merged = (_titleMetadata.value + (key to complete))
            .values
            .sortedByDescending { it.fetchedAt }
            .take(MAX_TITLE_METADATA)
            .associateBy { ContentMetadataPolicy.canonicalContentUrl(it.contentUrl) }
        runCatching {
            metadataFile.writeText(json.encodeToString(merged.values.toList()))
        }
        _titleMetadata.value = merged
    }

    /** Ephemeral UI state; catalog data itself remains available throughout a refresh. */
    fun setRefreshing(serviceId: String, refreshing: Boolean) {
        _refreshingServices.value = if (refreshing) {
            _refreshingServices.value + serviceId
        } else {
            _refreshingServices.value - serviceId
        }
    }

    /** Replaces one service's cached row, leaving every other service untouched. */
    suspend fun save(section: CatalogSection) = withContext(Dispatchers.IO) {
        val normalized = normalize(section)
        val merged =
            _sections.value.filterNot { it.serviceId == normalized.serviceId } + normalized
        persist(merged)
    }

    /**
     * Records an adapter failure without discarding the items already cached — a failed
     * refresh should degrade to "stale but usable", not to an empty row.
     */
    suspend fun recordError(serviceId: String, message: String) = withContext(Dispatchers.IO) {
        val existing = section(serviceId)
        val updated = existing?.copy(error = message)
            ?: CatalogSection(serviceId = serviceId, error = message)
        persist(_sections.value.filterNot { it.serviceId == serviceId } + updated)
    }

    private fun persist(list: List<CatalogSection>) {
        runCatching { file.writeText(json.encodeToString(list)) }
        _sections.value = list
    }

    /** Keeps old JSON caches visually clean after parser rules improve. */
    private fun normalize(section: CatalogSection): CatalogSection =
        section.copy(
            all = section.all.map(::normalize),
            movies = section.movies.map(::normalize),
            series = section.series.map(::normalize),
            popularSeries = section.popularSeries.map(::normalize),
            featured = section.featured.map(::normalize)
        )

    private fun normalize(item: CatalogItem): CatalogItem =
        item.copy(
            country = cleanText(item.country, 60),
            language = cleanText(item.language, 60),
            genres = item.genres
                .map {
                    it.replace(Regex("""\s+"""), " ")
                        .trim()
                        .replace(
                            Regex(
                                """^(?:ژانر|Genre)\s*:\s*""",
                                RegexOption.IGNORE_CASE
                            ),
                            ""
                        )
                }
                .filter { it.isNotBlank() }
                .distinct()
                .take(4),
            directors = item.directors
                .map { it.copy(name = it.name.replace(Regex("""\s+"""), " ").trim()) }
                .filter { it.name.isNotBlank() }
                .distinctBy { it.name.lowercase() }
                .take(3),
            cast = item.cast
                .map { it.copy(name = it.name.replace(Regex("""\s+"""), " ").trim()) }
                .filter { it.name.isNotBlank() }
                .distinctBy { it.name.lowercase() }
                .take(6)
        )

    private fun normalize(metadata: TitleMetadata): TitleMetadata =
        metadata.copy(
            contentUrl = ContentMetadataPolicy.canonicalContentUrl(metadata.contentUrl),
            posterUrl = metadata.posterUrl.trim().take(2_000)
                .takeIf { it.startsWith("https://", ignoreCase = true) }
                .orEmpty(),
            backdropUrl = metadata.backdropUrl.trim().take(2_000)
                .takeIf { it.startsWith("https://", ignoreCase = true) }
                .orEmpty(),
            summary = cleanText(metadata.summary, 520),
            year = metadata.year.replace(Regex("""[^0-9۰-۹]"""), "").take(4),
            genres = metadata.genres
                .map { cleanText(it, 28) }
                .filter { it.isNotBlank() }
                .distinct()
                .take(4),
            rating = metadata.rating
                .replace(Regex("""[^0-9۰-۹.٫]"""), "")
                .replace('٫', '.')
                .take(5),
            runtime = cleanText(metadata.runtime, 24),
            country = cleanText(metadata.country, 60),
            language = cleanText(metadata.language, 60),
            imdbId = metadata.imdbId
                .trim()
                .takeIf { Regex("""tt\d{5,12}""").matches(it) }
                .orEmpty(),
            directors = normalizePeople(metadata.directors, 3),
            cast = normalizePeople(metadata.cast, 8)
        )

    private fun normalizePeople(people: List<com.amin.tvos.data.model.PersonRef>, limit: Int) =
        people
            .map {
                it.copy(
                    name = cleanText(it.name, 80),
                    providerId = it.providerId.take(80),
                    profileUrl = it.profileUrl.take(2_000)
                )
            }
            .filter { it.name.isNotBlank() }
            .distinctBy { it.name.lowercase() }
            .take(limit)

    private fun cleanText(value: String, limit: Int): String =
        value.replace(Regex("""\s+"""), " ").trim().take(limit)

    private companion object {
        const val MAX_TITLE_METADATA = 150
    }
}
