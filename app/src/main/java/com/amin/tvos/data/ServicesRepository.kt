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
        if (!file.exists()) {
            context.assets.open("services.json").use { input ->
                file.outputStream().use { input.copyTo(it) }
            }
        }
        _services.value = runCatching {
            json.decodeFromString<List<StreamingService>>(file.readText())
        }.getOrDefault(emptyList())
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
