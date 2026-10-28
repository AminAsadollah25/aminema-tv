package com.amin.tvos.browser

import java.net.URI

/** Exact host boundaries; never log URLs or treat ad redirects as playback. */
object FarsiLandPolicy {
    const val ID = "farsiland"
    private val hosts = setOf("farsiland.com", "www.farsiland.com", "farsiplex.com", "www.farsiplex.com", "farsiland.buzz", "www.farsiland.buzz")
    private val ads = setOf("doubleclick.net", "googlesyndication.com", "googletagmanager.com",
        "webpushr.com", "qortex.ai", "datacygnal.io", "equityradarbrief.info")
    fun allowedPage(url:String):Boolean = runCatching {
        val u=URI(url)
        u.scheme.equals("https",true) && u.host?.lowercase() in hosts && u.userInfo==null &&
            (u.host?.lowercase() !in setOf("farsiland.buzz", "www.farsiland.buzz") ||
                u.path.orEmpty() in setOf("", "/", "/redirect.php", "/season", "/season/", "/smart-play", "/smart-play/")) &&
            (u.port==-1 || u.port==443) && !Regex("\\.(?:mp4|m3u8|mpd|webm|mkv|zip|apk|exe|js|css|png|jpg|webp)$",RegexOption.IGNORE_CASE)
                .containsMatchIn(u.path.orEmpty())
    }.getOrDefault(false)
    fun blockedResourceHost(host:String?):Boolean = host?.lowercase()?.let { h ->
        ads.any { h==it || h.endsWith(".$it") }
    } ?: false
    fun isEpisode(url:String):Boolean = allowedPage(url) &&
        runCatching { URI(url).host.orEmpty().endsWith("farsiland.com") &&
            Regex("^/episodes/[^/]+/?$").matches(URI(url).path.orEmpty()) }.getOrDefault(false)
    fun isTitle(url:String):Boolean = allowedPage(url) && runCatching {
        Regex("^/(?:movies|tvshows)/[^/]+/?$").matches(URI(url).path.orEmpty())
    }.getOrDefault(false)
    fun isPlayablePage(url:String):Boolean = allowedPage(url) && runCatching {
        val path=URI(url).path.orEmpty()
        Regex("^/(movies|episodes|movie|episode)/[^/]+/?$").matches(path) ||
            path.trimEnd('/') in setOf("/season","/smart-play") ||
            (URI(url).host.lowercase() in setOf("farsiplex.com","www.farsiplex.com") && path.startsWith("/play-"))
    }.getOrDefault(false)
    fun qualityIndex(labels:List<String>, live:Boolean=false):Int? {
        val clean=labels.map { it.trim().lowercase().replace(Regex("\\s+")," ") }
        val preferred=if(live)listOf("hd","1080","1080p","720","720p","480","480p")
            else listOf("1080","1080p","720","720p","480","480p")
        preferred.forEach { label -> clean.indexOf(label).takeIf { it>=0 }?.let { return it } }
        return clean.indexOfFirst { it in setOf("server1","server 1","live on youtube","backup server") }
            .takeIf { it>=0 }
    }
    /** Only verified official controls, each at most once per playback attempt. */
    fun alternatePlayer(available:List<String>, tried:Set<String>):String? =
        listOf("Player 1", "Player 2").firstOrNull { it in available && it !in tried }
}
