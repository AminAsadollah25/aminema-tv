package com.amin.tvos.data

import android.content.Context
import android.util.AtomicFile
import com.amin.tvos.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class JourneyLookupRecord(
    val key: String, val serviceId: String, val query: String,
    val checkedAt: Long, val error: String = "", val items: List<CatalogItem> = emptyList()
)

data class JourneyLookupTask(val key: String, val serviceId: String, val query: String)

@Serializable
data class JourneyLookupAuditEntry(
    val workId: String, val title: String, val season: Int, val year: String,
    val matched: Boolean, val providers: List<String> = emptyList()
)

object JourneyLookupPlanner {
    val providers = setOf("filmrooz", "mymoviz")
    fun tasks(works: List<JourneyWork>, providerIds: Set<String>): List<JourneyLookupTask> =
        providerIds.filter { it in providers }.sorted().flatMap { service ->
            works.flatMap { work -> (listOf(work.title) + work.aliases.sorted()).map { query ->
                JourneyLookupTask("$service:${CanonicalText.normalizeTitle(query)}", service, query)
            } }.distinctBy { it.key }
        }

    fun isFresh(record: JourneyLookupRecord, now: Long): Boolean =
        record.error.isBlank() && record.checkedAt <= now && now - record.checkedAt < 24 * 60 * 60 * 1000L

    fun catalogItem(result: SearchResult) = CatalogItem(
        title = CanonicalText.displayTitle(result.title), kind = result.kind,
        contentUrl = result.contentUrl, posterUrl = result.posterUrl, serviceId = result.serviceId,
        year = result.year.ifBlank { CanonicalText.extractYear(result.title) }, imdbId = result.imdbId,
        hasPersianDub = result.hasPersianDub, hasPersianSubtitle = result.hasPersianSubtitle,
        maxQualityHeight = result.maxQualityHeight, qualityLabel = result.qualityLabel
    )

    /** Provider search may OR the query words. Don't grow the journey cache with unrelated hits. */
    fun relevantItems(results: List<SearchResult>, works: List<JourneyWork>): List<CatalogItem> {
        val identities = works.flatMap { work -> (setOf(work.title) + work.aliases).map { title ->
            Triple(CanonicalText.normalizeTitle(title), work.premiereYear, work.kind)
        } }.toSet()
        return results.map(::catalogItem).filter { item ->
            val title = CanonicalText.normalizeTitle(item.title)
            val year = publicMatchYear(item.serviceId, item.year, "")
            val verifiedId = item.imdbId.isNotBlank() && works.any { it.imdbId == item.imdbId && it.kind == item.kind }
            verifiedId || identities.any { identity -> identity.first == title && identity.third == item.kind &&
                (year == null || kotlin.math.abs(identity.second.toInt() - year) <= 1) }
        }.distinctBy { it.contentUrl }
    }

    /** Prefer the direct title query over incidental OR/recommendation hits for the same URL. */
    fun bestItems(records: List<JourneyLookupRecord>): List<CatalogItem> = records
        .flatMap { record -> record.items.map { item -> item to record } }
        .groupBy { ContentMetadataPolicy.canonicalContentUrl(it.first.contentUrl) }
        .values.map { candidates -> candidates.maxWith(compareBy<Pair<CatalogItem, JourneyLookupRecord>>(
            { if (CanonicalText.normalizeTitle(it.first.title) == CanonicalText.normalizeTitle(it.second.query)) 1 else 0 },
            { if (it.first.imdbId.isNotBlank()) 1 else 0 },
            { if (it.first.year.isNotBlank()) 1 else 0 },
            { it.second.checkedAt }
        )).first }
}

/** Separate additive search cache: never reorder or overwrite the Home/latest catalog. */
class JourneyLookupRepository(context: Context) {
    private val file = AtomicFile(File(context.filesDir, "journey_lookup.json"))
    private val auditFile = AtomicFile(File(context.filesDir, "journey_lookup_audit.json"))
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()
    private var loaded = false
    private val _records = MutableStateFlow<List<JourneyLookupRecord>>(emptyList())
    val records = _records.asStateFlow()

    suspend fun load() = withContext(Dispatchers.IO) { mutex.withLock {
        if (!loaded) {
            if (file.baseFile.exists() || File(file.baseFile.path + ".bak").exists()) {
                // A corrupt cache is not silently replaced: surface the error for diagnosis.
                _records.value = json.decodeFromString<List<JourneyLookupRecord>>(file.openRead().use { it.reader().readText() })
            }
            loaded = true
        }
    } }

    suspend fun save(record: JourneyLookupRecord) = withContext(Dispatchers.IO) { mutex.withLock {
        check(loaded) { "Load journey cache first" }
        val old = _records.value.firstOrNull { it.key == record.key }
        // Failure/no new result must not destroy a previously discovered ordinary title page.
        val safe = record.copy(items = (record.items + old?.items.orEmpty()).distinctBy { it.contentUrl }.take(48))
        val updated = _records.value.filterNot { it.key == record.key } + safe
        val stream = file.startWrite()
        try {
            stream.write(json.encodeToString(updated).toByteArray(Charsets.UTF_8))
            file.finishWrite(stream)
            _records.value = updated
        } catch (error: Exception) {
            file.failWrite(stream)
            throw error
        }
    } }

    suspend fun saveAudit(matches: List<JourneyMatch>) = withContext(Dispatchers.IO) { mutex.withLock {
        val entries = matches.map { match -> JourneyLookupAuditEntry(match.work.id, match.work.title,
            match.work.season, match.work.premiereYear, match.media != null,
            match.media?.variants.orEmpty().map { it.providerId }.distinct()) }
        val stream = auditFile.startWrite()
        try {
            stream.write(json.encodeToString(entries).toByteArray(Charsets.UTF_8))
            auditFile.finishWrite(stream)
        } catch (error: Exception) { auditFile.failWrite(stream); throw error }
    } }
}
