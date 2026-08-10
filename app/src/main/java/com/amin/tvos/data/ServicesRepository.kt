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

    private val legacyGenericContentPattern =
        "(/|#)(movie|film|series|episode|watch|play|title|content)(/|$|[?#])"
    private val legacyBroadParsiExcludedPattern =
        "/medias/(movies|series|search|live)($|[?#/])"

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
        val enrichedExisting = saved.map { current ->
            val defaults = bundled.firstOrNull { it.id == current.id }
            if (defaults == null) current else current.copy(
                name = if (current.name in setOf("ParsiFlix", "FilmRooz")) {
                    defaults.name
                } else {
                    current.name
                },
                url = if (
                    current.url.contains("example.com/CHANGE_ME", ignoreCase = true)
                ) defaults.url else current.url,
                subtitle = if (
                    current.subtitle.isBlank() ||
                    current.subtitle in setOf("Iranian Cinema", "International Cinema")
                ) {
                    defaults.subtitle
                } else {
                    current.subtitle
                },
                sourceLabel = current.sourceLabel.ifBlank { defaults.sourceLabel },
                artwork = if (
                    current.artwork.isBlank() ||
                    current.artwork in setOf("service_parsiflix", "service_filmrooz")
                ) {
                    defaults.artwork
                } else {
                    current.artwork
                },
                loginZoomPercent = current.loginZoomPercent ?: defaults.loginZoomPercent,
                userAgent = current.userAgent ?: defaults.userAgent,
                fullscreenSelectors = (
                    current.fullscreenSelectors + defaults.fullscreenSelectors
                    ).distinct(),
                playerSelectors = (
                    current.playerSelectors + defaults.playerSelectors
                    ).distinct(),
                searchSelectors = (
                    current.searchSelectors + defaults.searchSelectors
                    ).distinct(),
                // v0.5 used one broad fallback rule. On ParsiFlix that rule
                // classified /play as content and replaced the stable detail
                // URL needed by Continue Watching. Remove only that legacy
                // rule while preserving any user-added, service-specific ones.
                contentUrlPatterns = (
                    current.contentUrlPatterns
                        .filterNot { it == legacyGenericContentPattern } +
                        defaults.contentUrlPatterns
                    ).distinct(),
                playbackUrlPatterns = (
                    current.playbackUrlPatterns + defaults.playbackUrlPatterns
                    ).distinct(),
                excludedUrlPatterns = (
                    current.excludedUrlPatterns
                        .filterNot { it == legacyBroadParsiExcludedPattern } +
                        defaults.excludedUrlPatterns
                    ).distinct(),
                // Adapter capability, not user content: the bundled version always wins,
                // exactly like resumeStrategy below. Keeping a stored copy would pin every
                // existing install to the rules it was first shipped with — which is how a
                // fix to those rules would never reach the device.
                directPlay = defaults.directPlay ?: current.directPlay,
                resumeStrategy = defaults.resumeStrategy,
                resumeButtonTextPatterns = (
                    current.resumeButtonTextPatterns + defaults.resumeButtonTextPatterns
                    ).distinct(),
                // Same rule as directPlay above: adapter capability, bundled version wins.
                quickLinks = defaults.quickLinks.ifEmpty { current.quickLinks },
                // Live channel routes/logos are shipped adapter metadata. Bundled data
                // must reach existing installs instead of staying pinned to an old file.
                liveTv = defaults.liveTv ?: current.liveTv
            )
        }
        // Provider definitions are shipped adapter capabilities. Existing installs keep
        // every customised service untouched, while a newly bundled provider is appended
        // once instead of requiring an app-data reset or a manual JSON edit.
        val enriched = enrichedExisting + bundled.filter { bundledService ->
            enrichedExisting.none { it.id == bundledService.id }
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
