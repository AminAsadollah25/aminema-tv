package com.amin.tvos.data

import android.content.Context
import android.util.AtomicFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/** Small independent cache; never replaces catalog, account history or journey matches. */
class GenreArchiveRepository(context: Context) {
    private val file = AtomicFile(File(context.filesDir, "genre_archive_pages.json"))
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()
    @Volatile private var pages = emptyList<GenreArchivePage>()
    private var writable = true

    suspend fun load() = withContext(Dispatchers.IO) { mutex.withLock {
        if (file.baseFile.exists() || File(file.baseFile.path + ".bak").exists()) {
            runCatching { json.decodeFromString<List<GenreArchivePage>>(file.openRead().use { it.reader().readText() }) }
                .onSuccess { pages = it.sortedByDescending { p -> p.checkedAt }.take(GenreArchivePolicy.MAX_CACHE_PAGES) }
                .onFailure { writable = false } // Do not overwrite corrupt user cache.
        }
    } }
    fun find(query: GenreArchiveQuery, number: Int) = pages.firstOrNull { it.query == query && it.number == number }

    suspend fun save(page: GenreArchivePage) = withContext(Dispatchers.IO) { mutex.withLock {
        if (!writable) return@withLock
        val updated = GenreArchivePolicy.retain(pages, page)
        val output = file.startWrite()
        try {
            output.write(json.encodeToString(updated).toByteArray(Charsets.UTF_8))
            file.finishWrite(output)
            pages = updated
        } catch (error: Exception) { file.failWrite(output); throw error }
    } }
}
