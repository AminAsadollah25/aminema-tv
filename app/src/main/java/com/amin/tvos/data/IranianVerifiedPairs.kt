package com.amin.tvos.data

import com.amin.tvos.data.model.CatalogItem
import com.amin.tvos.data.model.CatalogKind
import java.net.URI
import java.util.Locale

/**
 * Explicit page pairs checked against the Persian original title on FarsiLand's
 * public detail page and ParsiFlix's own catalogue on 2026-11-01.
 * This is a partial crosswalk, not a transliteration algorithm or complete archive.
 */
internal object IranianVerifiedPairs {
    private val series = mapOf(
        "ostokhan-sooz" to "392", "taxi" to "393", "yal" to "390",
        "nimrokh" to "377", "shah-gol" to "367", "eteraf-mikonam" to "363",
        "hamkalaam" to "366", "koori" to "359", "jimy-jaam" to "348",
        "befarmaeed-jaam" to "349", "kalagh" to "345", "man-too-man" to "344",
        "bist-o-yek" to "328", "delroba" to "317", "manshoor" to "315",
        "1001-nights" to "282", "bamdade-khomar" to "265"
    )
    private val movies = mapOf(
        "mahoor" to "384", "antik" to "388", "sarriz" to "375",
        "rokhe-niloo" to "380", "kamion" to "381", "dokhtar-barghi" to "371",
        "peperoni-baraye-diktator" to "373", "zaabet" to "369",
        "nekbat" to "368", "vahshy" to "370", "outcry" to "364",
        "a-hundred-years-to-these-years" to "362", "cloture" to "361",
        "harekate-akhar" to "358", "two-days-later" to "357",
        "the-man-who-became-a-horse" to "354", "in-the-form-of-love" to "353",
        "woman-and-child" to "350", "high-copy" to "347"
    )
    fun key(item: CatalogItem): String? = runCatching {
        val u = URI(item.contentUrl)
        if (!u.scheme.equals("https", true) || u.userInfo != null || u.port !in listOf(-1, 443))
            return null
        val host = u.host?.lowercase(Locale.ROOT).orEmpty()
        val path = u.path.orEmpty().trimEnd('/')
        val isSeries = item.kind == CatalogKind.SERIES
        val pairs = if (isSeries) series else movies
        val id = when {
            item.serviceId == "farsiland" && host in setOf("farsiland.com", "www.farsiland.com") -> {
                val prefix = if (isSeries) "/tvshows/" else "/movies/"
                if (!path.startsWith(prefix)) null else pairs[path.removePrefix(prefix)]
            }
            item.serviceId == "parsiflix" && host == "app.parsiflix.com" -> {
                val prefix = if (isSeries) "/medias/series/" else "/medias/movies/"
                val candidate = path.removePrefix(prefix)
                candidate.takeIf { path.startsWith(prefix) && it in pairs.values }
            }
            else -> null
        }
        id?.let { "${item.kind}:$it" }
    }.getOrNull()

    fun matches(a: CatalogItem, b: CatalogItem): Boolean =
        a.kind == b.kind && a.serviceId != b.serviceId &&
            key(a)?.let { it == key(b) } == true
}
