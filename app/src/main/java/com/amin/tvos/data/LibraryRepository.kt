package com.amin.tvos.data

import android.content.Context
import com.amin.tvos.data.model.MovieItem
import com.amin.tvos.data.model.PlaybackSession
import com.amin.tvos.data.model.ResumeStrategy
import com.amin.tvos.data.model.CatalogItem
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
import java.security.MessageDigest

/**
 * Smart Resume + Recents + Favorites store.
 * Simple JSON persistence in app-private storage — fast and RAM-friendly
 * for Android boxes. Swappable for Room later without touching the UI.
 */
class LibraryRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val file: File get() = File(context.filesDir, "library.json")
    private val playbackFile: File get() = File(context.filesDir, "playback_sessions.json")
    private val storageMutex = Mutex()

    private val _items = MutableStateFlow<List<MovieItem>>(emptyList())
    val items: StateFlow<List<MovieItem>> = _items.asStateFlow()
    private val _playbackSessions = MutableStateFlow<List<PlaybackSession>>(emptyList())
    val playbackSessions: StateFlow<List<PlaybackSession>> =
        _playbackSessions.asStateFlow()

    suspend fun load() = withStorageLock {
        val loadedItems = runCatching {
            json.decodeFromString<List<MovieItem>>(file.readText())
        }.getOrDefault(emptyList())
        val loadedSessions = runCatching {
            json.decodeFromString<List<PlaybackSession>>(playbackFile.readText())
        }.getOrDefault(emptyList())
        if (_items.value != loadedItems) _items.value = loadedItems
        if (_playbackSessions.value != loadedSessions) _playbackSessions.value = loadedSessions
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
    ) = withStorageLock {
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

    suspend fun toggleFavorite(id: String) = withStorageLock {
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
    ): Boolean = withStorageLock {
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

    fun hasLocalPoster(url: String): Boolean {
        return localPoster(url) != null
    }

    fun localPoster(url: String): String? {
        val id = sha1(url)
        return _items.value.firstOrNull { it.id == id }
            ?.posterUrl
            ?.takeIf { it.startsWith("file:") }
    }

    suspend fun saveLocalPoster(
        contentUrl: String,
        mimeType: String,
        bytes: ByteArray
    ): String? = withStorageLock {
        if (
            contentUrl.isBlank() ||
            bytes.isEmpty() ||
            bytes.size > 1_500_000 ||
            mimeType !in setOf("image/webp", "image/jpeg", "image/png")
        ) return@withStorageLock null
        val extension = when (mimeType) {
            "image/png" -> "png"
            "image/jpeg" -> "jpg"
            else -> "webp"
        }
        val directory = File(context.filesDir, "posters").apply { mkdirs() }
        val posterFile = File(directory, "${sha1(contentUrl)}.$extension")
        posterFile.writeBytes(bytes)
        val localUrl = android.net.Uri.fromFile(posterFile).toString()
        val id = sha1(contentUrl)
        persist(
            _items.value.map {
                if (it.id == id) it.copy(posterUrl = localUrl) else it
            }
        )
        persistPlayback(
            _playbackSessions.value.map {
                if (it.id == id) it.copy(posterUrl = localUrl) else it
            }
        )
        localUrl
    }

    suspend fun remove(id: String) = withStorageLock {
        persist(_items.value.filterNot { it.id == id })
    }

    suspend fun clearHistory() = withStorageLock {
        persist(_items.value.filter { it.isFavorite })
        persistPlayback(emptyList())
    }

    /**
     * Repairs legacy SPA race-condition entries from the provider catalog.
     * A generic service-shell title must never replace a real catalog title.
     */
    suspend fun repairMetadata(catalogItems: List<CatalogItem>) =
        withStorageLock {
            if (catalogItems.isEmpty() || _items.value.isEmpty()) return@withStorageLock
            val byUrl = catalogItems.associateBy {
                ContentMetadataPolicy.canonicalContentUrl(it.contentUrl)
            }
            var changed = false
            val repaired = _items.value.map { item ->
                val catalog = byUrl[
                    ContentMetadataPolicy.canonicalContentUrl(item.url)
                ] ?: return@map item
                val titleIsGeneric = ContentMetadataPolicy.isGenericShellTitle(
                    item.title,
                    item.serviceId,
                    item.serviceName
                )
                val next = item.copy(
                    title = if (titleIsGeneric) catalog.title else item.title,
                    posterUrl = catalog.posterUrl.ifBlank { item.posterUrl }
                )
                if (next != item) changed = true
                next
            }
            if (changed) persist(repaired)
        }

    /**
     * Called only after a real HTML5 video play/timeupdate event.
     * The saved playbackUrl is the top-level browser page, never the media src.
     */
    suspend fun recordPlayback(
        serviceId: String,
        serviceName: String,
        contentUrl: String,
        playbackUrl: String,
        title: String,
        posterUrl: String,
        resumePosition: Long,
        duration: Long,
        resumeStrategy: ResumeStrategy
    ) = withStorageLock {
        if (contentUrl.isBlank() || playbackUrl.isBlank()) return@withStorageLock
        val id = sha1(contentUrl)
        val existingSession = _playbackSessions.value.firstOrNull { it.id == id }
        val existingItem = _items.value.firstOrNull { it.id == id }
        val resolvedTitle = title.ifBlank {
            existingSession?.title ?: existingItem?.title ?: serviceName
        }
        val resolvedPoster = posterUrl.ifBlank {
            existingSession?.posterUrl ?: existingItem?.posterUrl.orEmpty()
        }
        val now = System.currentTimeMillis()
        val session = PlaybackSession(
            id = id,
            title = resolvedTitle,
            posterUrl = resolvedPoster,
            serviceId = serviceId,
            serviceName = serviceName,
            contentUrl = contentUrl,
            playbackUrl = playbackUrl,
            lastPlayed = now,
            resumePosition = resumePosition.takeIf { it > 0L }
                ?: existingSession?.resumePosition
                ?: 0L,
            duration = duration.takeIf { it > 0L }
                ?: existingSession?.duration
                ?: 0L,
            resumeStrategy = resumeStrategy,
            actionButtonTextPatterns = emptyList(),
            syncedFromAccount = false
        )
        persistPlayback(
            (listOf(session) + _playbackSessions.value.filterNot { it.id == id })
                .sortedByDescending { it.lastPlayed }
                .take(30)
        )

        val contentItem = MovieItem(
            id = id,
            title = resolvedTitle,
            posterUrl = resolvedPoster,
            serviceId = serviceId,
            serviceName = serviceName,
            url = contentUrl,
            lastOpened = now,
            isFavorite = existingItem?.isFavorite ?: false
        )
        persist(trimmed(listOf(contentItem) + _items.value.filterNot { it.id == id }))
    }

    /**
     * Reconciles one provider against its signed-in account Continue/Recent row.
     *
     * The account list is authoritative for membership, so a successful sync
     * produces the same rail on every Aminema device. A matching local session
     * still enriches that remote item with its stable player page and position.
     * Sessions from other services are untouched. No media URL is accepted.
     */
    suspend fun syncAccountSessions(
        serviceId: String,
        incoming: List<PlaybackSession>
    ) = withStorageLock {
        if (serviceId.isBlank()) return@withStorageLock
        val previous = _playbackSessions.value
        val reconciled = incoming
            .asSequence()
            .filter { it.serviceId == serviceId && it.contentUrl.isNotBlank() }
            .take(30)
            .map { remote ->
                val id = sha1(remote.contentUrl)
                val existing = previous.firstOrNull { it.id == id }
                if (existing != null && !existing.syncedFromAccount) {
                    existing.copy(
                        title = remote.title.ifBlank { existing.title },
                        posterUrl = existing.posterUrl.ifBlank { remote.posterUrl },
                        lastPlayed = remote.lastPlayed
                    )
                } else {
                    remote.copy(
                        id = id,
                        title = remote.title.ifBlank {
                            existing?.title ?: remote.serviceName
                        },
                        posterUrl = remote.posterUrl.ifBlank {
                            existing?.posterUrl.orEmpty()
                        },
                        resumePosition = remote.resumePosition.takeIf { it > 0L }
                            ?: existing?.resumePosition
                            ?: 0L,
                        duration = remote.duration.takeIf { it > 0L }
                            ?: existing?.duration
                            ?: 0L,
                        syncedFromAccount = true
                    )
                }
            }
            .toList()
        persistPlayback(
            (
                previous.filterNot { it.serviceId == serviceId } + reconciled
            )
                .sortedByDescending { it.lastPlayed }
                .take(40)
        )
    }

    /** Real playback sessions only; nearly completed titles leave Continue. */
    fun continueWatching(list: List<PlaybackSession>): List<PlaybackSession> =
        list.asSequence()
            .filter {
                it.duration <= 0L ||
                    it.resumePosition <= 0L ||
                    it.resumePosition.toDouble() / it.duration.toDouble() < 0.96
            }
            .sortedByDescending { it.lastPlayed }
            .distinctBy { it.id }
            .take(20)
            .toList()

    private fun persist(list: List<MovieItem>) {
        file.writeText(json.encodeToString(list))
        _items.value = list
    }


    private fun persistPlayback(list: List<PlaybackSession>) {
        playbackFile.writeText(json.encodeToString(list))
        _playbackSessions.value = list
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

    /** Keeps visits, playback sync and favorites from overwriting one another. */
    private suspend fun <T> withStorageLock(block: () -> T): T =
        withContext(Dispatchers.IO) { storageMutex.withLock { block() } }
}
