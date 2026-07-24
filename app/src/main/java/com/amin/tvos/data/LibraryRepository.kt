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
        posterUrl: String,
        resumePosition: Long = 0L,
        duration: Long = 0L,
        isPlayable: Boolean = false
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
            resumePosition = resumePosition.takeIf { it > 0L }
                ?: existing?.resumePosition
                ?: 0L,
            duration = duration.takeIf { it > 0L }
                ?: existing?.duration
                ?: 0L,
            isPlayable = isPlayable || existing?.isPlayable == true,
            isFavorite = existing?.isFavorite ?: false
        )
        persist(trimmed(listOf(item) + _items.value.filterNot { it.id == id }))
    }

    suspend fun toggleFavorite(id: String) = withContext(Dispatchers.IO) {
        persist(_items.value.map { if (it.id == id) it.copy(isFavorite = !it.isFavorite) else it })
    }

    /**
     * Saves the current rendered page (if needed) and toggles its favorite state.
     * Returns the new state for a TV-friendly confirmation message.
     */
    suspend fun toggleFavoriteForPage(
        serviceId: String,
        serviceName: String,
        url: String,
        title: String,
        posterUrl: String,
        resumePosition: Long,
        duration: Long,
        isPlayable: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        val id = sha1(url)
        val existing = _items.value.firstOrNull { it.id == id }
        val favorite = !(existing?.isFavorite ?: false)
        val item = MovieItem(
            id = id,
            title = title.ifBlank { existing?.title ?: serviceName },
            posterUrl = posterUrl.ifBlank { existing?.posterUrl.orEmpty() },
            serviceId = serviceId,
            serviceName = serviceName,
            url = url,
            lastOpened = System.currentTimeMillis(),
            resumePosition = resumePosition.takeIf { it > 0L }
                ?: existing?.resumePosition
                ?: 0L,
            duration = duration.takeIf { it > 0L }
                ?: existing?.duration
                ?: 0L,
            isPlayable = isPlayable || existing?.isPlayable == true,
            isFavorite = favorite
        )
        persist(trimmed(listOf(item) + _items.value.filterNot { it.id == id }))
        favorite
    }

    fun isFavorite(url: String): Boolean {
        val id = sha1(url)
        return _items.value.firstOrNull { it.id == id }?.isFavorite == true
    }

    suspend fun remove(id: String) = withContext(Dispatchers.IO) {
        persist(_items.value.filterNot { it.id == id })
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        persist(_items.value.filter { it.isFavorite })
    }

    /** Playable pages only — login, profile, and service home pages stay out. */
    fun continueWatching(list: List<MovieItem>): List<MovieItem> =
        list.asSequence()
            .filter { it.isPlayable || it.resumePosition > 0L }
            .sortedByDescending { it.lastOpened }
            .distinctBy { it.id }
            .take(12)
            .toList()

    private fun persist(list: List<MovieItem>) {
        file.writeText(json.encodeToString(list))
        _items.value = list
    }

    private fun trimmed(list: List<MovieItem>): List<MovieItem> {
        // Favorites are kept until the user removes them; history remains small
        // for fast startup and low RAM use on Android boxes.
        val sorted = list.distinctBy { it.id }.sortedByDescending { it.lastOpened }
        val favorites = sorted.filter { it.isFavorite }
        val recents = sorted.filterNot { it.isFavorite }.take(50)
        return (favorites + recents)
            .distinctBy { it.id }
            .sortedByDescending { it.lastOpened }
    }

    private fun sha1(s: String): String =
        MessageDigest.getInstance("SHA-1").digest(s.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
