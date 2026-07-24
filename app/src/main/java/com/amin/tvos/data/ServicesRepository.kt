package com.amin.tvos.data

import android.content.Context
import com.amin.tvos.data.model.StreamingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Loads services from a user-editable JSON file.
 * On first launch the bundled assets/services.json is copied to
 * filesDir/services.json — edit that file (or use Settings) to
 * add/remove services. No websites are hardcoded.
 */
class ServicesRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val file: File get() = File(context.filesDir, "services.json")

    private val _services = MutableStateFlow<List<StreamingService>>(emptyList())
    val services: StateFlow<List<StreamingService>> = _services.asStateFlow()

    suspend fun load() = withContext(Dispatchers.IO) {
        val bundled = runCatching {
            context.assets.open("services.json").bufferedReader().use {
                json.decodeFromString<List<StreamingService>>(it.readText())
            }
        }.getOrDefault(emptyList())

        if (!file.exists()) {
            context.assets.open("services.json").use { input ->
                file.outputStream().use { input.copyTo(it) }
            }
        }
        val saved = runCatching {
            json.decodeFromString<List<StreamingService>>(file.readText())
        }.getOrDefault(emptyList())

        // Add newly shipped presentation metadata to existing installs without
        // replacing the user's URL, service order, or custom services.
        val enriched = saved.map { current ->
            val defaults = bundled.firstOrNull { it.id == current.id }
            if (defaults == null) current else current.copy(
                url = if (
                    current.url.contains("example.com/CHANGE_ME", ignoreCase = true)
                ) defaults.url else current.url,
                subtitle = current.subtitle.ifBlank { defaults.subtitle },
                artwork = current.artwork.ifBlank { defaults.artwork },
                loginZoomPercent = current.loginZoomPercent ?: defaults.loginZoomPercent,
                userAgent = current.userAgent ?: defaults.userAgent,
                fullscreenSelectors = current.fullscreenSelectors
                    .ifEmpty { defaults.fullscreenSelectors }
            )
        }
        if (enriched != saved) file.writeText(json.encodeToString(enriched))
        _services.value = enriched
    }

    suspend fun addService(service: StreamingService) = withContext(Dispatchers.IO) {
        val updated = _services.value.filterNot { it.id == service.id } + service
        persist(updated)
    }

    suspend fun removeService(id: String) = withContext(Dispatchers.IO) {
        persist(_services.value.filterNot { it.id == id })
    }

    fun findById(id: String): StreamingService? = _services.value.firstOrNull { it.id == id }

    private fun persist(list: List<StreamingService>) {
        file.writeText(json.encodeToString(list))
        _services.value = list
    }
}
