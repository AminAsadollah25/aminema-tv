package com.amin.tvos.browser

import android.net.Uri
import com.amin.tvos.data.model.DirectPlayConfig
import com.amin.tvos.data.model.ResumeStrategy
import com.amin.tvos.data.model.StreamingService

/**
 * Keeps site-specific knowledge outside BrowserActivity.
 *
 * Every rule comes from services.json. The adapter only identifies the current
 * rendered page and normal browser controls; it never discovers catalogs,
 * extracts streams, or bypasses authentication.
 */
class ServiceAdapter(private val service: StreamingService) {

    val playerSelectors: List<String> =
        service.playerSelectors.ifEmpty {
            listOf(
                "video",
                "iframe[allowfullscreen]",
                ".video-js",
                ".plyr",
                "[class*='video-player' i]",
                "[class*='player-wrapper' i]"
            )
        }

    val searchSelectors: List<String> =
        service.searchSelectors.ifEmpty {
            listOf(
                "input[type='search']",
                "input[placeholder*='search' i]",
                "input[placeholder*='جست' i]",
                "button[aria-label*='search' i]",
                "a[href*='search' i]",
                "[class*='search' i] input"
            )
        }

    val resumeStrategy: ResumeStrategy get() = service.resumeStrategy

    /** Present only for services that expose their own online-play options. */
    val directPlay: DirectPlayConfig? get() = service.directPlay

    val resumeButtonTextPatterns: List<String> =
        service.resumeButtonTextPatterns.ifEmpty {
            listOf("ادامه تماشا", "Continue Watching", "Resume")
        }

    fun isContentUrl(url: String): Boolean =
        !isPlaybackUrl(url) &&
            service.contentUrlPatterns.any { pattern -> matches(pattern, url) }

    fun isPlaybackUrl(url: String): Boolean =
        service.playbackUrlPatterns.any { pattern -> matches(pattern, url) }

    fun isExcluded(url: String): Boolean {
        if (Regex(
                """\.(?:jpe?g|png|gif|webp|svg|css|js|woff2?)(?:$|[?#])""",
                RegexOption.IGNORE_CASE
            ).containsMatchIn(url)
        ) return true
        if (isServiceRoot(url)) return true
        return service.excludedUrlPatterns.any { pattern -> matches(pattern, url) }
    }

    fun shouldRecord(
        url: String,
        title: String,
        hasPoster: Boolean,
        playerDetected: Boolean
    ): Boolean {
        if (url.isBlank() || isExcluded(url)) return false
        // Playback sessions are stored separately after a real <video> play
        // event; player pages must never appear in Recently Opened.
        if (isPlaybackUrl(url)) return false
        if (title.isBlank() || title.equals(service.name, ignoreCase = true)) {
            return isContentUrl(url)
        }
        if (service.contentUrlPatterns.isNotEmpty()) return isContentUrl(url)

        // Fallback for user-added services without adapter rules.
        val pathDepth = runCatching {
            Uri.parse(url).pathSegments.count { it.isNotBlank() }
        }.getOrDefault(0)
        return !playerDetected && hasPoster && pathDepth >= 2
    }

    private fun isServiceRoot(url: String): Boolean = runCatching {
        val current = Uri.parse(url)
        val home = Uri.parse(service.url)
        current.host.equals(home.host, ignoreCase = true) &&
            normalizedPath(current.path) == normalizedPath(home.path) &&
            current.query.isNullOrBlank() &&
            current.fragment.isNullOrBlank()
    }.getOrDefault(false)

    private fun normalizedPath(path: String?): String =
        path.orEmpty().trim().trimEnd('/').ifBlank { "/" }

    private fun matches(pattern: String, value: String): Boolean =
        runCatching { Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(value) }
            .getOrDefault(false)
}
