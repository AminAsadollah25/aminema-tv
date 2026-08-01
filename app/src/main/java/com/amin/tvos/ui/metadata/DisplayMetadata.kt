package com.amin.tvos.ui.metadata

import com.amin.tvos.data.model.CatalogKind
import com.amin.tvos.data.model.SpotlightItem

/** Display-only localisation; the provider's original year remains untouched in storage. */
fun displayReleaseYear(item: SpotlightItem): String {
    val year = item.year.toLatinDigits().filter(Char::isDigit).take(4).toIntOrNull()
        ?: return ""
    return if (item.isIranianTitle()) {
        val solarYear = when (year) {
            in 1300..1499 -> year
            in 1900..2200 -> year - 621
            else -> return ""
        }
        solarYear.toPersianDigits()
    } else {
        year.toString()
    }
}

fun spotlightCategory(item: SpotlightItem): String = buildString {
    append(if (item.kind == CatalogKind.SERIES) "سریال" else "فیلم")
    append(if (item.isIranianTitle()) " ایرانی" else " خارجی")
}

fun SpotlightItem.isIranianTitle(): Boolean =
    serviceId.equals("parsiflix", ignoreCase = true) ||
        country.contains("ایران", ignoreCase = true) ||
        country.contains("Iran", ignoreCase = true)

private fun String.toLatinDigits(): String = map { character ->
    when (character) {
        in '۰'..'۹' -> ('0'.code + (character.code - '۰'.code)).toChar()
        in '٠'..'٩' -> ('0'.code + (character.code - '٠'.code)).toChar()
        else -> character
    }
}.joinToString("")

private fun Int.toPersianDigits(): String = toString().map { character ->
    if (character in '0'..'9') {
        ('۰'.code + (character.code - '0'.code)).toChar()
    } else character
}.joinToString("")
