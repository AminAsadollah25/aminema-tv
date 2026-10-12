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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import com.amin.tvos.diagnostics.StartupDiagnostics

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
    private val homeFile: File get() = File(context.filesDir, "home_catalog_snapshot.json")
    private val storageMutex = Mutex()
    private var loadedSourceFingerprint: Pair<Long, Long>? = null
    private fun sourceFingerprint() = file.length() to file.lastModified()

    private val _sections = MutableStateFlow<List<CatalogSection>>(emptyList())
    val sections: StateFlow<List<CatalogSection>> = _sections.asStateFlow()
    private val _homeSections = MutableStateFlow<List<CatalogSection>>(emptyList())
    val homeSections: StateFlow<List<CatalogSection>> = _homeSections.asStateFlow()
    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()
    private val _refreshingServices = MutableStateFlow<Set<String>>(emptySet())
    val refreshingServices: StateFlow<Set<String>> = _refreshingServices.asStateFlow()
    private val _titleMetadata = MutableStateFlow<Map<String, TitleMetadata>>(emptyMap())
    val titleMetadata: StateFlow<Map<String, TitleMetadata>> = _titleMetadata.asStateFlow()

    suspend fun load() = withStorageLock {
        StartupDiagnostics.mark("catalog.disk.start")
        // This tiny metadata cache is needed by Hero immediately. Waiting behind the
        // full archive caused redundant enrichment lookups and repeated slide transitions.
        loadTitleMetadata()
        // Browser/settings return can change metadata without changing the catalog.
        // Keep that reload, but avoid decoding the identical full archive again.
        if (_loaded.value && loadedSourceFingerprint == sourceFingerprint()) {
            StartupDiagnostics.mark("catalog.memory.reused")
            return@withStorageLock
        }
        val readFingerprint = sourceFingerprint()
        val snapshot = runCatching {
            json.decodeFromString<HomeCatalogSnapshot>(homeFile.readText())
        }.getOrNull()?.takeIf { it.matchesSource(file.length(), file.lastModified()) }
        // Returning to Home must not temporarily replace already-loaded full rows with
        // a smaller window. Only a genuinely cold repository needs the fast projection.
        if (_sections.value.isEmpty() && snapshot != null) {
            _homeSections.value = snapshot.sections
            StartupDiagnostics.mark("catalog.home.cache.ready")
        }
        val decoded = runCatching {
            json.decodeFromString<List<CatalogSection>>(file.readText())
        }
        val loaded = decoded.getOrDefault(emptyList())
        StartupDiagnostics.mark("catalog.decode.end")
        val normalized = loaded.map(::normalize)
        StartupDiagnostics.mark("catalog.normalize.end")
        if (normalized != loaded) {
            val saved = runCatching { file.writeText(json.encodeToString(normalized)) }.isSuccess
            loadedSourceFingerprint = if (saved) sourceFingerprint() else readFingerprint
        } else {
            loadedSourceFingerprint = readFingerprint.takeIf { decoded.isSuccess }
        }
        // Returning from Search/Browser calls load() again. Do not emit an equal, newly
        // allocated 2,000-item list: Home's canonical merge would otherwise rebuild every
        // rail and recompose hundreds of image nodes for no visible change.
        if (_sections.value != normalized) _sections.value = normalized
        if (_homeSections.value != normalized) _homeSections.value = normalized
        if (snapshot == null || !snapshot.matchesSource(file.length(), file.lastModified())) {
            persistHomeSnapshot(normalized)
        }

        _loaded.value = true
        StartupDiagnostics.mark("catalog.disk.end")
    }

    private fun loadTitleMetadata() {
        val metadata = runCatching {
            json.decodeFromString<List<TitleMetadata>>(metadataFile.readText())
        }.getOrDefault(emptyList())
            .map(::normalize)
            .filter { it.contentUrl.isNotBlank() }
            .sortedByDescending { it.fetchedAt }
            .take(MAX_TITLE_METADATA)
            .associateBy { ContentMetadataPolicy.canonicalContentUrl(it.contentUrl) }
        if (_titleMetadata.value != metadata) _titleMetadata.value = metadata
        StartupDiagnostics.mark("metadata.cache.ready")
    }

    fun section(serviceId: String): CatalogSection? =
        _sections.value.firstOrNull { it.serviceId == serviceId }

    fun metadataFor(contentUrl: String): TitleMetadata? =
        _titleMetadata.value[ContentMetadataPolicy.canonicalContentUrl(contentUrl)]

    suspend fun saveTitleMetadata(metadata: TitleMetadata) = withStorageLock {
        val normalized = normalize(metadata)
        if (normalized.contentUrl.isBlank()) return@withStorageLock
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

    /**
     * Updates one service without letting a later shallow refresh shrink a library window that
     * the user already loaded. Provider order is preserved; older cached tail items are appended.
     */
    suspend fun save(section: CatalogSection) = withStorageLock {
        // Incoming adapter output carries an authoritative end-page marker. normalize() also
        // repairs legacy on-disk caches, so retain the adapter marker here before merging.
        val normalized = normalize(section).copy(loadedPageLimit = section.loadedPageLimit)
        val existing = section(normalized.serviceId)
        fun keepLoadedTail(
            incoming: List<CatalogItem>,
            cached: List<CatalogItem>
        ): List<CatalogItem> = buildList {
            addAll(incoming)
            val seen = incoming
                .map { ContentMetadataPolicy.canonicalContentUrl(it.contentUrl) }
                .toMutableSet()
            cached.forEach { item ->
                if (seen.add(ContentMetadataPolicy.canonicalContentUrl(item.contentUrl))) {
                    add(item)
                }
            }
        }
        val complete = existing?.let { cached ->
            // Caches written before loadedPageLimit existed can already contain a deep
            // MyMoviz window. Its adapter contributes ten relevant titles per page, so infer
            // that one time rather than restarting at page 4 and repeatedly reloading data
            // already present in the library.
            val inferredMyMovizPageLimit = if (cached.serviceId == MYMOVIZ_SERVICE_ID) {
                val loadedItems = maxOf(
                    cached.movies.size,
                    cached.series.size
                )
                if (loadedItems == 0) 0 else {
                    ((loadedItems + MYMOVIZ_ITEMS_PER_PAGE - 1) / MYMOVIZ_ITEMS_PER_PAGE)
                        .coerceAtLeast(DEFAULT_PAGE_LIMIT)
                }
            } else {
                0
            }
            // Older builds could claim page 40 after receiving only 200 titles. Never start
            // the next batch beyond what the stored rows can substantiate.
            val migratedCachedPageLimit = if (cached.serviceId == MYMOVIZ_SERVICE_ID) {
                cached.loadedPageLimit
                    .takeIf { it > 0 }
                    ?.coerceAtMost(inferredMyMovizPageLimit)
                    ?: inferredMyMovizPageLimit
            } else cached.loadedPageLimit
            val extendsCachedWindow = normalized.loadedPageLimit > migratedCachedPageLimit
            fun mergeWindow(
                incoming: List<CatalogItem>,
                cachedItems: List<CatalogItem>
            ): List<CatalogItem> = if (extendsCachedWindow) {
                keepLoadedTail(cachedItems, incoming)
            } else {
                keepLoadedTail(incoming, cachedItems)
            }
            normalized.copy(
                all = mergeWindow(normalized.all, cached.all),
                movies = mergeWindow(normalized.movies, cached.movies),
                series = mergeWindow(normalized.series, cached.series),
                popularSeries = mergeWindow(
                    normalized.popularSeries,
                    cached.popularSeries
                ),
                featured = if (normalized.featured.isNotEmpty()) {
                    normalized.featured
                } else {
                    cached.featured
                },
                loadedPageLimit = maxOf(
                    normalized.loadedPageLimit,
                    migratedCachedPageLimit
                )
            )
        } ?: normalized
        val merged =
            _sections.value.filterNot { it.serviceId == complete.serviceId } + complete
        persist(merged)
    }

    /**
     * Records an adapter failure without discarding the items already cached — a failed
     * refresh should degrade to "stale but usable", not to an empty row.
     */
    suspend fun recordError(serviceId: String, message: String) = withStorageLock {
        val existing = section(serviceId)
        val updated = existing?.copy(error = message)
            ?: CatalogSection(serviceId = serviceId, error = message)
        persist(_sections.value.filterNot { it.serviceId == serviceId } + updated)
    }

    private fun persist(list: List<CatalogSection>) {
        val sourceSaved = runCatching { file.writeText(json.encodeToString(list)) }.isSuccess
        _sections.value = list
        _homeSections.value = list
        if (sourceSaved) {
            loadedSourceFingerprint = sourceFingerprint()
            persistHomeSnapshot(list)
        } else {
            loadedSourceFingerprint = null
        }
    }

    private fun persistHomeSnapshot(list: List<CatalogSection>) {
        // Best-effort derived cache. A missing/interrupted file simply falls back to the
        // original full cache; source timestamp/length reject stale projections.
        runCatching {
            homeFile.writeText(json.encodeToString(HomeCatalogSnapshot(
                sourceLength = file.length(),
                sourceModified = file.lastModified(),
                sections = compactHomeSections(list)
            )))
        }
    }

    /** Keeps old JSON caches visually clean after parser rules improve. */
    private fun normalize(section: CatalogSection): CatalogSection {
        val migratedPageLimit = if (section.serviceId == MYMOVIZ_SERVICE_ID) {
            val loadedItems = maxOf(
                section.movies.size,
                section.series.size
            )
            val inferred = if (loadedItems == 0) 0 else {
                ((loadedItems + MYMOVIZ_ITEMS_PER_PAGE - 1) / MYMOVIZ_ITEMS_PER_PAGE)
                    .coerceAtLeast(DEFAULT_PAGE_LIMIT)
            }
            // A cumulative MyMoviz cache contains about ten relevant rows per fetched page.
            // This repairs both old over-reported markers (40 pages / 200 rows) and an
            // interrupted write whose marker trails the rows already persisted.
            inferred
        } else {
            section.loadedPageLimit
        }
        return CatalogClassification.normalize(section.copy(
            all = section.all.map(::normalize),
            movies = section.movies.map(::normalize),
            series = section.series.map(::normalize),
            popularSeries = section.popularSeries.map(::normalize),
            featured = section.featured.map(::normalize),
            loadedPageLimit = migratedPageLimit
        ))
    }

    private fun normalize(item: CatalogItem): CatalogItem =
        item.copy(
            imdbId = item.imdbId.trim()
                .takeIf { IMDB_ID.matches(it) }
                .orEmpty(),
            maxQualityHeight = item.maxQualityHeight.coerceIn(0, 4_320),
            qualityLabel = cleanText(item.qualityLabel, 32),
            country = cleanText(item.country, 60),
            language = cleanText(item.language, 60),
            genres = item.genres
                .map {
                    it.replace(WHITESPACE, " ")
                        .trim()
                        .replace(
                            GENRE_PREFIX,
                            ""
                        )
                }
                .filter { it.isNotBlank() }
                .distinct()
                .take(4),
            directors = item.directors
                .map { it.copy(name = it.name.replace(WHITESPACE, " ").trim()) }
                .filter { it.name.isNotBlank() }
                .distinctBy { it.name.lowercase() }
                .take(3),
            cast = item.cast
                .map { it.copy(name = it.name.replace(WHITESPACE, " ").trim()) }
                .filter { it.name.isNotBlank() }
                .distinctBy { it.name.lowercase() }
                .take(6)
        )

    private fun normalize(metadata: TitleMetadata): TitleMetadata =
        metadata.copy(
            contentUrl = ContentMetadataPolicy.canonicalContentUrl(metadata.contentUrl),
            detailPosterUrl = PersonImagePolicy.safeUrl(metadata.detailPosterUrl),
            posterUrl = metadata.posterUrl.trim().take(2_000)
                .takeIf { it.startsWith("https://", ignoreCase = true) }
                .orEmpty(),
            backdropUrl = metadata.backdropUrl.trim().take(2_000)
                .takeIf { it.startsWith("https://", ignoreCase = true) }
                .orEmpty(),
            summary = cleanText(metadata.summary, 520),
            year = metadata.year.replace(NON_YEAR_DIGIT, "").take(4),
            genres = metadata.genres
                .map { cleanText(it, 28) }
                .filter { it.isNotBlank() }
                .distinct()
                .take(4),
            rating = metadata.rating
                .replace(NON_RATING_DIGIT, "")
                .replace('٫', '.')
                .take(5),
            runtime = cleanText(metadata.runtime, 24),
            country = cleanText(metadata.country, 60),
            language = cleanText(metadata.language, 60),
            imdbId = metadata.imdbId
                .trim()
                .takeIf { IMDB_ID.matches(it) }
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
                    profileUrl = it.profileUrl.take(2_000),
                    imageUrl = PersonImagePolicy.safeUrl(it.imageUrl)
                )
            }
            .filter { it.name.isNotBlank() }
            .distinctBy { it.name.lowercase() }
            .take(limit)

    private fun cleanText(value: String, limit: Int): String =
        value.replace(WHITESPACE, " ").trim().take(limit)

    /** Serializes read/modify/write cycles so concurrent provider jobs cannot lose a section. */
    private suspend fun <T> withStorageLock(block: () -> T): T =
        withContext(Dispatchers.IO) { storageMutex.withLock { block() } }

    private companion object {
        val WHITESPACE = Regex("""\s+""")
        val IMDB_ID = Regex("""tt\d{5,12}""")
        val GENRE_PREFIX = Regex("""^(?:ژانر|Genre)\s*:\s*""", RegexOption.IGNORE_CASE)
        val NON_YEAR_DIGIT = Regex("""[^0-9۰-۹]""")
        val NON_RATING_DIGIT = Regex("""[^0-9۰-۹.٫]""")
        const val MAX_TITLE_METADATA = 150
        const val MYMOVIZ_SERVICE_ID = "mymoviz"
        const val MYMOVIZ_ITEMS_PER_PAGE = 10
        const val DEFAULT_PAGE_LIMIT = 4
    }
}
