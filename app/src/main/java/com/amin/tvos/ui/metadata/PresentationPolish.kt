package com.amin.tvos.ui.metadata

import com.amin.tvos.data.ContentMetadataPolicy
import com.amin.tvos.data.model.SourceVariant

private fun String.latinDigits() = map { c -> when(c) {
    in '۰'..'۹' -> ('0'.code+c.code-'۰'.code).toChar()
    in '٠'..'٩' -> ('0'.code+c.code-'٠'.code).toChar()
    else -> c
} }.joinToString("")

fun displayTitle(title: String, year: String): String {
    val digits=year.latinDigits().filter(Char::isDigit).take(4)
    if (digits.length != 4) return title.trim()
    val suffix=Regex("""\s*[（(]\s*([0-9۰-۹٠-٩]{4})\s*[)）]\s*$""").find(title)
    return if(suffix?.groupValues?.get(1)?.latinDigits()==digits)
        title.substring(0,suffix.range.first).trim().ifBlank { title } else title.trim()
}

/** Presentation only: no episode is inferred for playback. */
fun compactEpisodeLabel(value: String): String {
    val text=value.latinDigits().replace('\u200c',' ')
    val episode=Regex("""قسمت\s*[:：]?\s*(\d+)""").find(text)?.groupValues?.get(1)
    val seasonToken=Regex("""فصل\s*[:：]?\s*(\d+|اول|دوم|سوم|چهارم|پنجم|ششم|هفتم|هشتم|نهم|دهم|یازدهم|دوازدهم)""")
        .find(text)?.groupValues?.get(1)
    val words=listOf("اول","دوم","سوم","چهارم","پنجم","ششم","هفتم","هشتم","نهم","دهم","یازدهم","دوازدهم")
    val season=seasonToken?.toIntOrNull() ?: words.indexOf(seasonToken).takeIf { it>=0 }?.plus(1)
    val result=buildList {
        episode?.let { add("قسمت $it") }
        season?.let { add("فصل $it") }
    }.joinToString(" • ").ifBlank {
        text.replace(Regex("""(?:در\s*حال\s*پخش|پایان\s*فصل|پایان\s*سریال)"""), "")
            .replace(Regex("""\s+"""), " ").trim(' ', '•', '-', '|')
    }
    return result.map { if(it in '0'..'9') ('۰'.code+it.code-'0'.code).toChar() else it }.joinToString("")
}

fun displayLanguages(value: String): String = displayTokens(value, mapOf(
    "english" to "انگلیسی", "british english" to "انگلیسی", "american english" to "انگلیسی",
    "انگلیسی بریتانیایی" to "انگلیسی", "انگلیسی آمریکایی" to "انگلیسی",
    "persian" to "فارسی", "farsi" to "فارسی", "french" to "فرانسوی",
    "arabic" to "عربی", "korean" to "کره‌ای", "japanese" to "ژاپنی",
    "german" to "آلمانی", "turkish" to "ترکی", "spanish" to "اسپانیایی"
), "زبان")

fun displayCountries(value: String): String = displayTokens(value, mapOf(
    "united states" to "آمریکا", "united states of america" to "آمریکا", "usa" to "آمریکا",
    "ایالات متحده آمریکا" to "آمریکا", "united kingdom" to "بریتانیا", "uk" to "بریتانیا",
    "انگلستان" to "بریتانیا", "iran" to "ایران"
), "محصول")

private fun displayTokens(value: String, aliases: Map<String,String>, prefix: String) =
    value.split(Regex("[,،;؛|/]+" )).map {
        val token=it.trim().replace(Regex("^(?:${Regex.escape(prefix)}\\s+)+"),"").trim().replace(Regex("\\s+")," ")
        aliases[token.lowercase()] ?: token
    }.filter { it.isNotBlank() }.distinct().joinToString("، ")

/** Keep all underlying routes; show a provider only once, retaining its current page. */
fun visibleSources(sources: List<SourceVariant>, currentUrl: String): List<SourceVariant> =
    sources.groupBy { it.providerId }.values.map { variants ->
        variants.firstOrNull { ContentMetadataPolicy.isSameTopLevelPage(it.item.contentUrl,currentUrl) }
            ?: variants.firstOrNull { it.item.contentUrl.contains("/_modern/title/") }
            ?: variants.first()
    }
