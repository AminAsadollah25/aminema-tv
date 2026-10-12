package com.amin.tvos.data

import com.amin.tvos.data.model.CatalogItem
import com.amin.tvos.data.model.CatalogKind
import com.amin.tvos.data.model.CatalogSection
import com.amin.tvos.data.model.TitleMetadata
import java.util.Locale

enum class DiscoveryCategory(val label: String, val atlasIndex: Int, val aliases: Set<String>) {
    HORROR("ترسناک", 0, setOf("horror", "ترسناک", "وحشت")),
    FAMILY("خانوادگی", 1, setOf("family", "خانوادگی")),
    MYSTERY("معمایی", 2, setOf("mystery", "معمایی", "رازآلود", "راز آلود")),
    ACTION("اکشن", 3, setOf("action", "اکشن")),
    ANIMATION("انیمیشن", 4, setOf("animation", "انیمیشن", "پویانمایی")),
    ROMANCE("عاشقانه", 5, setOf("romance", "romantic", "عاشقانه", "رمانتیک")),
    COMEDY("کمدی", 6, setOf("comedy", "کمدی")),
    SCI_FI("علمی‌تخیلی", 7, setOf("sci fi", "science fiction", "علمی تخیلی")),
    FANTASY("فانتزی", 8, setOf("fantasy", "فانتزی")),
    DOCUMENTARY("مستند", 9, setOf("documentary", "مستند")),
    CLASSIC("کلاسیک", 10, emptySet()),
    MARATHON("ماراتون", 11, emptySet());

    val libraryTitle: String get() = when (this) {
        CLASSIC -> "کلاسیک · ساخته‌شده تا سال ۲۰۰۰"
        MARATHON -> "ماراتون · انتخاب سریال"
        FAMILY -> "خانوادگی · ژانر سایت، نه رده‌بندی کودک"
        else -> label
    }
}

/** No synopsis guessing, provider calls or playback inference. Runs only on selection. */
object GenreDiscovery {
    private fun normalize(value: String): String = value.lowercase(Locale.ROOT)
        .replace('ي', 'ی').replace('ك', 'ک').replace('\u200c', ' ')
        .replace('-', ' ').replace(Regex("\\s+"), " ").trim()

    fun matches(category: DiscoveryCategory, item: CatalogItem, metadata: TitleMetadata? = null): Boolean {
        if (category == DiscoveryCategory.MARATHON) return item.kind == CatalogKind.SERIES
        if (category == DiscoveryCategory.CLASSIC) {
            val year = publicMatchYear(item.serviceId, item.year, metadata?.year.orEmpty())
            return year != null && year <= 2000
        }
        return (item.genres + metadata?.genres.orEmpty()).any { genre ->
            normalize(genre) in category.aliases
        }
    }

    fun select(
        category: DiscoveryCategory,
        sections: List<CatalogSection>,
        metadata: Map<String, TitleMetadata>
    ): List<CatalogItem> {
        val lists = sections.filter { it.serviceId in setOf("parsiflix", "filmrooz", "mymoviz") }
            .map { section ->
                (section.all + section.movies + section.series + section.popularSeries + section.featured)
                    .filter { matches(category, it, metadata[ContentMetadataPolicy.canonicalContentUrl(it.contentUrl)]) }
                    .distinctBy { ContentMetadataPolicy.canonicalContentUrl(it.contentUrl) }
            }
        return CanonicalLibrary.mergeLatest(lists, metadataByUrl = metadata, limit = 2048)
            .map { it.representative }
    }
}
