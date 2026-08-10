package com.amin.tvos.data.model

import kotlinx.serialization.Serializable

/** Service type. LIVE_TV keeps the service layer open to dedicated sources later. */
@Serializable
enum class ServiceType { STREAMING, LIVE_TV }

@Serializable
enum class ResumeStrategy {
    /** Open a stable, normal browser player page (FilmRooz). */
    OPEN_PLAYBACK_PAGE,

    /** Open the detail page, then activate the website's own Continue button. */
    CLICK_SITE_CONTINUE
}

/**
 * Optional "one step further" behaviour: instead of stopping on a title's detail page,
 * follow the website's own **Play online** control to its normal player page.
 *
 * Only the site's own visible options are read — the quality/language choices it renders
 * on the page. No media URL, stream file, DRM value or token is inspected or stored.
 */
@Serializable
data class DirectPlayConfig(
    /** Language keys as the site itself names them, best first — a dub wins when present. */
    val preferredLanguages: List<String> = emptyList(),
    /**
     * Accepted heights, best first. A height that is not listed is never auto-selected,
     * which is how 2160p stays excluded.
     */
    val preferredHeights: List<Int> = listOf(1080, 720, 480),
    /**
     * Fallback for services that expose a single visible watch button instead of a list
     * of quality options: the site's own button is clicked, exactly as the user would.
     */
    val buttonTextPatterns: List<String> = emptyList(),
    /**
     * Buttons whose text matches any of these are never clicked, even when they also match
     * an include pattern. A page can offer an unrelated player — live TV, for instance —
     * whose label starts with the same verb as the title's own watch button.
     */
    val excludeButtonTextPatterns: List<String> = emptyList(),
    /**
     * After Aminema reaches the provider's normal top-level player page, ask that page's
     * own HTML5/player control to start. This never reads a media URL and is opt-in per
     * provider because ParsiFlix already starts correctly without it.
     */
    val autoPlayOnPlaybackPage: Boolean = false
)

/**
 * A named shortcut into one specific page the service's own site already builds and
 * maintains — Live TV, a YouTube tab, or similar. Opens as a normal page in the browser;
 * no direct-play or resume logic applies.
 */
@Serializable
data class QuickLink(
    val id: String,
    val label: String,
    /** Appended to the service's own `url`. */
    val path: String,
    /** Prominent links surface near the top of Home; others sit lower in the scroll. */
    val prominent: Boolean = false
)

/**
 * One normal, signed-in website page that starts a live channel.
 *
 * `path` is a page route, never a media/stream URL. Logos are presentation metadata
 * discovered from the service's own visible channel cards.
 */
@Serializable
data class LiveChannel(
    val id: String,
    val name: String,
    /** Appended to the owning service's `url`. */
    val path: String,
    val logoUrl: String
)

/**
 * Optional native Live TV rail backed by the service's ordinary browser pages.
 * New channels or future sources can be added in services.json without UI changes.
 */
@Serializable
data class LiveTvConfig(
    val channels: List<LiveChannel> = emptyList()
)

/** A streaming service configured via services.json — never hardcoded. */
@Serializable
data class StreamingService(
    val id: String,
    val name: String,
    val url: String,
    val icon: String = "",
    /** Optional short label displayed on the cinematic home card. */
    val subtitle: String = "",
    /** Explicit provider label used only when a canonical title offers multiple sources. */
    val sourceLabel: String = "",
    /** Drawable resource name or remote image URL for the service card. */
    val artwork: String = "",
    val color: String = "#E50914",
    val type: ServiceType = ServiceType.STREAMING,
    /** Supplemental providers enrich/search the library but do not need a large Home doorway. */
    val showOnHome: Boolean = true,
    /** Optional service-specific scale used only on login / QR pages. */
    val loginZoomPercent: Int? = null,
    /** Optional UserAgentMode enum name: TV, DESKTOP, or MOBILE. */
    val userAgent: String? = null,
    /** Site-specific fullscreen controls to try from a TV remote shortcut. */
    val fullscreenSelectors: List<String> = emptyList(),
    /** Elements that identify an embedded player on this service. */
    val playerSelectors: List<String> = emptyList(),
    /** Native website search inputs/buttons that the quick menu can focus. */
    val searchSelectors: List<String> = emptyList(),
    /** Regex patterns that identify movie, series, episode, or watch routes. */
    val contentUrlPatterns: List<String> = emptyList(),
    /** Regex patterns that identify normal top-level player pages. */
    val playbackUrlPatterns: List<String> = emptyList(),
    /** Regex patterns that must never appear in the personal library. */
    val excludedUrlPatterns: List<String> = emptyList(),
    /** Optional: follow the site's own Play-online control instead of stopping on detail. */
    val directPlay: DirectPlayConfig? = null,
    /** How Continue Watching should re-enter this service. */
    val resumeStrategy: ResumeStrategy = ResumeStrategy.OPEN_PLAYBACK_PAGE,
    /** Text patterns for the website's own Continue/Resume button. */
    val resumeButtonTextPatterns: List<String> = emptyList(),
    /** Named shortcuts into pages the site already builds — Live TV, YouTube tab, etc. */
    val quickLinks: List<QuickLink> = emptyList(),
    /** Optional native channel rail; every channel still opens a normal website page. */
    val liveTv: LiveTvConfig? = null
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
    val duration: Long = 0L,
    /** True for a detected player or a service-adapter content route. */
    val isPlayable: Boolean = false,
    val isFavorite: Boolean = false
)

/**
 * A real playback event, separate from a recently opened detail page.
 * playbackUrl is a normal top-level browser page; no media/stream URL is stored.
 */
@Serializable
data class PlaybackSession(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val posterUrl: String = "",
    val serviceId: String,
    val serviceName: String,
    val contentUrl: String,
    val playbackUrl: String,
    val lastPlayed: Long,
    val resumePosition: Long = 0L,
    val duration: Long = 0L,
    val resumeStrategy: ResumeStrategy = ResumeStrategy.OPEN_PLAYBACK_PAGE,
    /** Optional per-item normal website action, e.g. FilmRooz "پخش آنلاین". */
    val actionButtonTextPatterns: List<String> = emptyList(),
    /** True when imported from the signed-in account rather than local playback. */
    val syncedFromAccount: Boolean = false
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
