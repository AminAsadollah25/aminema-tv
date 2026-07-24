package com.amin.tvos.data.model

import kotlinx.serialization.Serializable

/**
 * Service type. LIVE_TV exists for future expansion — the architecture supports it,
 * but no Live TV implementation ships yet.
 */
@Serializable
enum class ServiceType { STREAMING, LIVE_TV }

/** A streaming service configured via services.json — never hardcoded. */
@Serializable
data class StreamingService(
    val id: String,
    val name: String,
    val url: String,
    val icon: String = "",
    val color: String = "#E50914",
    val type: ServiceType = ServiceType.STREAMING,
    /** Optional service-specific scale used only on login / QR pages. */
    val loginZoomPercent: Int? = null,
    /** Optional UserAgentMode enum name: TV, DESKTOP, or MOBILE. */
    val userAgent: String? = null,
    /** Site-specific fullscreen controls to try from a TV remote shortcut. */
    val fullscreenSelectors: List<String> = emptyList()
)

/**
 * A content item the user has opened — powers Continue Watching,
 * Recently Opened, and Favorites. Poster architecture is ready for
 * future TMDB / manual catalog integration.
 */
@Serializable
data class MovieItem(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val posterUrl: String = "",
    val serviceId: String,
    val serviceName: String,
    val url: String,
    val lastOpened: Long,
    val resumePosition: Long = 0L,
    val isFavorite: Boolean = false
)

/** User-agent modes for the embedded browser. */
enum class UserAgentMode(val label: String, val value: String?) {
    TV("Android TV (default)", null), // null = WebView default UA
    DESKTOP(
        "Desktop Chrome",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
    ),
    MOBILE(
        "Mobile Chrome",
        "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
    )
}
