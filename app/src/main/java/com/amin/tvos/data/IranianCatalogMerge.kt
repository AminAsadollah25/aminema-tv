package com.amin.tvos.data

import com.amin.tvos.data.model.CatalogItem
import com.amin.tvos.data.model.CatalogSection
import java.text.Normalizer
import java.util.Locale

/** Combines independently cached provider rows without guessing transliterations. */
internal object IranianCatalogMerge {
    fun section(primary: CatalogSection?, secondary: CatalogSection?): CatalogSection? {
        val base = primary ?: secondary ?: return null
        return base.copy(
            all = latest(primary?.all.orEmpty(), secondary?.all.orEmpty()),
            movies = latest(primary?.movies.orEmpty(), secondary?.movies.orEmpty()),
            series = latest(primary?.series.orEmpty(), secondary?.series.orEmpty()),
            featured = latest(primary?.featured.orEmpty(), secondary?.featured.orEmpty()),
            syncedAt = maxOf(primary?.syncedAt ?: 0L, secondary?.syncedAt ?: 0L),
            hasMoreAll = primary?.hasMoreAll == true || secondary?.hasMoreAll == true,
            hasMoreMovies = primary?.hasMoreMovies == true || secondary?.hasMoreMovies == true,
            hasMoreSeries = primary?.hasMoreSeries == true || secondary?.hasMoreSeries == true,
            error = if (listOf(primary, secondary).filterNotNull().any {
                    it.all.isNotEmpty() || it.movies.isNotEmpty() || it.series.isNotEmpty()
                }) "" else listOfNotNull(primary?.error, secondary?.error)
                    .filter { it.isNotBlank() }.joinToString(" • ")
        )
    }
    private fun titleKey(item: CatalogItem): String {
        val title = Normalizer.normalize(item.title, Normalizer.Form.NFKC)
            .lowercase(Locale.ROOT).replace('ي', 'ی').replace('ك', 'ک')
            .replace(Regex("[^\\p{L}\\p{Nd}]"), "")
        // A title alone can identify different remakes. Without a verified year,
        // leave both cards visible rather than sending the user to the wrong film.
        val rawYear = item.year.trim()
        if (!rawYear.matches(Regex("[0-9۰-۹٠-٩]{4}"))) return ""
        val year = CanonicalText.normalizeYear(rawYear)
        return if (title.isBlank()) "" else "${item.kind}:$title:$year"
    }

    fun latest(primary: List<CatalogItem>, secondary: List<CatalogItem>): List<CatalogItem> {
        val ids = mutableSetOf<String>()
        val names = mutableMapOf<String, String?>()
        val pairs = mutableMapOf<String, String?>()
        val urls = mutableSetOf<String>()
        fun add(item: CatalogItem, result: MutableList<CatalogItem>) {
            val id = item.imdbId.trim().lowercase(Locale.ROOT)
                .takeIf { it.matches(Regex("tt[0-9]+")) }?.let { "${item.kind}:$it" }
            val name = titleKey(item)
            val pair = IranianVerifiedPairs.key(item)
            val url = ContentMetadataPolicy.canonicalContentUrl(item.contentUrl)
            fun agrees(index: Map<String, String?>, key: String): Boolean =
                index.containsKey(key) && (id == null || index[key] == null || id == index[key])
            if ((id != null && id in ids) || (name.isNotEmpty() && agrees(names, name)) ||
                (pair != null && agrees(pairs, pair)) || (url.isNotBlank() && url in urls)) return
            if (id != null) ids.add(id)
            if (name.isNotEmpty()) names[name] = id
            if (pair != null) pairs[pair] = id
            if (url.isNotBlank()) urls.add(url)
            result.add(item)
        }
        // Seed all primary identities first, so a later primary match still wins over
        // an earlier secondary match. Keep the provider's own detail URL unchanged.
        val accepted = mutableListOf<CatalogItem>()
        primary.forEach { add(it, accepted) }
        val supplement = mutableListOf<CatalogItem>()
        secondary.forEach { add(it, supplement) }
        return buildList {
            for (i in 0 until maxOf(accepted.size, supplement.size)) {
                accepted.getOrNull(i)?.let(::add)
                supplement.getOrNull(i)?.let(::add)
            }
        }
    }
}
