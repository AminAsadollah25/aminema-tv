package com.amin.tvos.data

import com.amin.tvos.data.model.CatalogItem
import com.amin.tvos.data.model.CatalogKind
import com.amin.tvos.data.model.CatalogSection
import com.amin.tvos.data.model.catalogKindFromUrl
import java.net.URI

/** Repair typed rails using ordinary provider routes, never title/name guesses. */
object CatalogClassification {
    fun classify(item: CatalogItem): CatalogItem {
        val path = runCatching { URI(item.contentUrl).path }.getOrNull().orEmpty()
        val kind = when {
            item.serviceId == "mymoviz" && path.startsWith("/tvshows/") -> CatalogKind.SERIES
            item.serviceId == "mymoviz" && path.startsWith("/movies/") -> CatalogKind.MOVIE
            // The legacy generic route has no type. A provider episode badge is positive
            // series evidence; absence of one is not enough to override its stored kind.
            item.serviceId == "mymoviz" && path.startsWith("/_modern/title/") &&
                item.episodeLabel.isNotBlank() -> CatalogKind.SERIES
            else -> catalogKindFromUrl(item.contentUrl) ?: item.kind
        }
        return if (kind == item.kind) item else item.copy(kind = kind)
    }

    fun normalize(section: CatalogSection): CatalogSection {
        val all = section.all.map(::classify)
        val movies = section.movies.map(::classify)
        val series = section.series.map(::classify)
        fun typed(primary: List<CatalogItem>, secondary: List<CatalogItem>, kind: CatalogKind) =
            (primary + secondary.filter { it.kind == kind })
                .filter { it.kind == kind }
                .distinctBy { ContentMetadataPolicy.canonicalContentUrl(it.contentUrl) }
        return section.copy(
            all = all,
            movies = typed(movies, series + all, CatalogKind.MOVIE),
            series = typed(series, movies + all, CatalogKind.SERIES),
            popularSeries = section.popularSeries.map(::classify).filter { it.kind == CatalogKind.SERIES },
            featured = section.featured.map(::classify)
        )
    }
}
