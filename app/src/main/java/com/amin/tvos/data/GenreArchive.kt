package com.amin.tvos.data

import com.amin.tvos.data.model.CatalogItem
import kotlinx.serialization.Serializable
import java.net.URI
import java.net.URLDecoder

@Serializable enum class ArchiveSort(val label: String, val parameter: String) {
    LATEST("جدیدترینِ سایت", "latest"), IMDB("امتیاز IMDb سایت", "imdb")
}
@Serializable enum class ArchiveType(val label: String, val parameter: String) {
    MOVIES("فیلم", "movie"), SERIES("سریال", "tv")
}
/** Values read from the provider's genre[] form, not inferred from translated labels. */
@Serializable enum class ArchiveGenre(val category: DiscoveryCategory, val parameter: String) {
    HORROR(DiscoveryCategory.HORROR, "horror"), FAMILY(DiscoveryCategory.FAMILY, "family"),
    MYSTERY(DiscoveryCategory.MYSTERY, "mystery"), ACTION(DiscoveryCategory.ACTION, "action"),
    ANIMATION(DiscoveryCategory.ANIMATION, "animation"), ROMANCE(DiscoveryCategory.ROMANCE, "romance"),
    COMEDY(DiscoveryCategory.COMEDY, "comedy"), SCI_FI(DiscoveryCategory.SCI_FI, "science-fiction"),
    FANTASY(DiscoveryCategory.FANTASY, "fantasy"), DOCUMENTARY(DiscoveryCategory.DOCUMENTARY, "documentary");
    companion object {
        fun forCategory(category: DiscoveryCategory) = entries.firstOrNull { it.category == category }
    }
}
@Serializable data class GenreArchiveQuery(
    val sort: ArchiveSort = ArchiveSort.LATEST,
    val type: ArchiveType = ArchiveType.MOVIES,
    val genre: ArchiveGenre = ArchiveGenre.ROMANCE
) {
    val key: String get() = "mymoviz:${genre.parameter}:${type.parameter}:${sort.parameter}"
    fun pageUrl(origin: String, page: Int): String {
        require(page in 1..10_000)
        val base = URI(origin)
        require(base.scheme == "https" && base.host != null)
        return URI("https", null, base.host, base.port, "/search",
            "genre=${genre.parameter}&type=${type.parameter}&sort=${sort.parameter}" +
                if (page > 1) "&p=$page" else "", null).toASCIIString()
    }

    /** Only the observed, same-origin search contract can become a pagination cursor. */
    fun acceptsPage(url: String, origin: String, expectedPage: Int): Boolean = runCatching {
        val uri = URI(url)
        val base = URI(origin)
        if (uri.scheme != "https" || !uri.host.equals(base.host, true) ||
            uri.port != base.port || uri.rawUserInfo != null || uri.fragment != null || uri.path != "/search") return false
        val pairs = uri.rawQuery.orEmpty().split('&').filter(String::isNotEmpty).map {
            val pair = it.split('=', limit = 2)
            URLDecoder.decode(pair[0], "UTF-8") to URLDecoder.decode(pair.getOrElse(1) { "" }, "UTF-8")
        }
        if (pairs.map { it.first }.distinct().size != pairs.size) return false
        val params = pairs.toMap()
        params.keys.all { it in setOf("genre", "type", "sort", "p") } &&
            params["genre"] == genre.parameter && params["type"] == type.parameter &&
            (params["sort"] ?: "latest") == sort.parameter &&
            (params["p"]?.toIntOrNull() ?: if (params["p"] == null) 1 else -1) == expectedPage
    }.getOrDefault(false)
}

@Serializable data class GenreArchivePage(
    val query: GenreArchiveQuery, val number: Int, val items: List<CatalogItem>,
    val nextUrl: String = "", val totalCount: Int? = null, val checkedAt: Long = 0L
)

object GenreArchivePolicy {
    const val MAX_CACHE_PAGES = 12
    const val MAX_PAGE_ITEMS = 24
    const val FRESH_MS = 6 * 60 * 60 * 1000L
    fun fresh(page: GenreArchivePage, now: Long) = page.checkedAt <= now && now - page.checkedAt < FRESH_MS
    fun belongsToOrigin(page: GenreArchivePage, origin: String): Boolean = runCatching {
        val base = URI(origin)
        page.number in 1..10_000 && page.items.size <= MAX_PAGE_ITEMS && page.items.all { item ->
            val uri = URI(item.contentUrl)
            val path = if (page.query.type == ArchiveType.MOVIES) "^/movies/[0-9]+/[^/?#]+/?$" else "^/tvshows/[0-9]+/[^/?#]+/?$"
            item.serviceId == "mymoviz" && uri.scheme == "https" && uri.host.equals(base.host, true) &&
                uri.port == base.port && uri.rawUserInfo == null && uri.rawQuery == null && uri.fragment == null &&
                Regex(path).matches(uri.path) &&
                (item.kind == com.amin.tvos.data.model.CatalogKind.MOVIE) == (page.query.type == ArchiveType.MOVIES)
        } && (page.nextUrl.isBlank() || page.query.acceptsPage(page.nextUrl, origin, page.number + 1))
    }.getOrDefault(false)
    fun retain(old: List<GenreArchivePage>, page: GenreArchivePage): List<GenreArchivePage> =
        (old.filterNot { it.query == page.query && it.number == page.number } + page)
            .sortedByDescending { it.checkedAt }.take(MAX_CACHE_PAGES)
}
