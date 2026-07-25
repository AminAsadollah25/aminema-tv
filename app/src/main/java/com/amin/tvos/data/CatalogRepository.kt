package com.amin.tvos.data

import android.content.Context
import com.amin.tvos.data.model.CatalogSection
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
 * the cache instantly at startup and only a user-triggered sync goes to the network.
 * Each service is stored independently, so a broken adapter can never empty the other row.
 */
class CatalogRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val file: File get() = File(context.filesDir, "catalog.json")

    private val _sections = MutableStateFlow<List<CatalogSection>>(emptyList())
    val sections: StateFlow<List<CatalogSection>> = _sections.asStateFlow()

    suspend fun load() = withContext(Dispatchers.IO) {
        _sections.value = runCatching {
            json.decodeFromString<List<CatalogSection>>(file.readText())
        }.getOrDefault(emptyList())
    }

    fun section(serviceId: String): CatalogSection? =
        _sections.value.firstOrNull { it.serviceId == serviceId }

    /** Replaces one service's cached row, leaving every other service untouched. */
    suspend fun save(section: CatalogSection) = withContext(Dispatchers.IO) {
        val merged = _sections.value.filterNot { it.serviceId == section.serviceId } + section
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
}
