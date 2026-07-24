package com.amin.tvos.data

import android.content.Context
import com.amin.tvos.data.model.MovieItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest

/**
 * Smart Resume + Recents + Favorites store.
 * Simple JSON persistence in app-private storage — fast and RAM-friendly
 * for Android boxes. Swappable for Room later without touching the UI.
 */
class LibraryRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val file: File get() = File(context.filesDir, "library.json")

    private val _items = MutableStateFlow<List<MovieItem>>(emptyList())
    val items: StateFlow<List<MovieItem>> = _items.asStateFlow()

    suspend fun load() = withContext(Dispatchers.IO) {
        _items.value = runCatching {
            json.decodeFromString<List<MovieItem>>(file.readText())
        }.getOrDefault(emptyList())
    }

    /** Called by the browser layer whenever the user lands on a content page. */
    suspend fun recordVisit(
        serviceId: String,
        serviceName: String,
        url: String,
        title: String,
        posterUrl: String
    ) = withContext(Dispatchers.IO) {
        val id = sha1(url)
        val existing = _items.value.firstOrNull { it.id == id }
        val item = MovieItem(
            id = id,
            title = title.ifBlank { existing?.title ?: serviceName },
            posterUrl = posterUrl.ifBlank { existing?.posterUrl.orEmpty() },
            serviceId = serviceId,
            serviceName = serviceName,
            url = url,
            lastOpened = System.currentTimeMillis(),
            isFavorite = existing?.isFavorite ?: false
        )
        val updated = (listOf(item) + _items.value.filterNot { it.id == id })
            .let { list ->
                // keep favorites forever, trim non-favorites to 30 entries
                val favorites = list.filter { it.isFavorite }
                val recents = list.filterNot { it.isFavorite }.take(30)
                (favorites + recents).distinctBy { it.id }
            }
        persist(updated.sortedByDescending { it.lastOpened })
    }

    suspend fun toggleFavorite(id: String) = withContext(Dispatchers.IO) {
        persist(_items.value.map { if (it.id == id) it.copy(isFavorite = !it.isFavorite) else it })
    }

    suspend fun remove(id: String) = withContext(Dispatchers.IO) {
        persist(_items.value.filterNot { it.id == id })
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        persist(_items.value.filter { it.isFavorite })
    }

    /** Latest item per service — the "Continue Watching" row. */
    fun continueWatching(list: List<MovieItem>): List<MovieItem> =
        list.sortedByDescending { it.lastOpened }
            .distinctBy { it.serviceId }

    private fun persist(list: List<MovieItem>) {
        file.writeText(json.encodeToString(list))
        _items.value = list
    }

    private fun sha1(s: String): String =
        MessageDigest.getInstance("SHA-1").digest(s.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
