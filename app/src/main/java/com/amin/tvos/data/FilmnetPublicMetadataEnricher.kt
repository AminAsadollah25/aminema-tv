package com.amin.tvos.data

import com.amin.tvos.data.model.CatalogKind
import com.amin.tvos.data.model.PersonRef
import com.amin.tvos.data.model.SpotlightItem
import com.amin.tvos.data.model.TitleMetadata
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.Normalizer
import org.json.JSONArray
import org.json.JSONObject

/**
 * Public Iranian-title fallback backed by Filmnet's server-rendered ordinary title pages.
 *
 * No account, cookie, player request or media URL is used. Search results are accepted only
 * after a strict normalised-title and movie/series-kind match; the matching public detail page
 * then supplies solar year, synopsis, credits and editorial artwork.
 */
class FilmnetPublicMetadataEnricher {

    fun lookup(item: SpotlightItem, known: TitleMetadata?): TitleMetadata? {
        if (!isIranian(item)) return null
        val title = cleanTitle(item.title)
        if (title.length < 2) return null

        val search = nextData(
            getText("$BASE_URL/contents?query=${encode(title)}") ?: return null
        ) ?: return null
        val searchProps = search.optJSONObject("props")?.optJSONObject("pageProps")
            ?: return null
        val results = searchProps.optJSONObject("ssrData")?.optJSONArray("data")
            ?: searchProps.optJSONArray("contents")
            ?: JSONArray()

        val candidate = (0 until results.length())
            .mapNotNull(results::optJSONObject)
            .filter { kindMatches(item.kind, it.optString("type")) }
            .map { it to titleScore(title, it.optString("title")) }
            .filter { (_, score) -> score >= 0.82 }
            .maxByOrNull { (_, score) -> score }
            ?.first ?: return null

        val shortId = candidate.optString("short_id").takeIf { it.isNotBlank() }
            ?: return null
        val slug = candidate.optString("slug").takeIf { it.isNotBlank() }
            ?: return null
        val detail = nextData(
            getText("$BASE_URL/contents/${encodePath(shortId)}/${encodePath(slug)}")
                ?: return null
        ) ?: return null
        val detailProps = detail.optJSONObject("props")?.optJSONObject("pageProps")
            ?: return null
        val aggregate = detailProps.optJSONObject("aggregate")
        val video = aggregate?.optJSONObject("video_content")
            ?: detailProps.optJSONObject("content")
            ?: candidate
        val now = System.currentTimeMillis()

        val directors = mutableListOf<PersonRef>()
        val cast = mutableListOf<PersonRef>()
        val castGroups = aggregate?.optJSONArray("cast") ?: JSONArray()
        for (index in 0 until castGroups.length()) {
            val group = castGroups.optJSONObject(index) ?: continue
            val target = if (group.optString("role").contains("کارگردان")) {
                directors
            } else if (group.optString("role").contains("بازیگر")) {
                cast
            } else continue
            val people = group.optJSONArray("artists") ?: JSONArray()
            for (personIndex in 0 until people.length()) {
                people.optString(personIndex)
                    .cleanText(80)
                    .takeIf { it.isNotBlank() }
                    ?.let { target += PersonRef(name = it) }
            }
        }
        // Filmnet's TV/Android rendering exposes the same public people as a flat
        // `artists` array rather than grouped `cast`. Support both server layouts.
        val artists = detailProps.optJSONArray("artists") ?: JSONArray()
        for (index in 0 until artists.length()) {
            val entry = artists.optJSONObject(index) ?: continue
            val role = entry.optJSONObject("person_role")?.optString("title").orEmpty()
            val person = entry.optJSONObject("person") ?: continue
            val name = person.optString("name").cleanText(80)
            if (name.isBlank()) continue
            val reference = PersonRef(
                name = name,
                providerId = person.optString("id").take(80),
                profileUrl = ""
            )
            when {
                role.contains("کارگردان") -> directors += reference
                role.contains("بازیگر") -> cast += reference
            }
        }

        val summary = cleanHtml(video.optString("summary"), 620)
        val genres = categoryTitles(video, "genre").take(4)
        val countries = categoryTitles(video, "territory").take(2)
        val solarYear = video.optString("year")
            .filter(Char::isDigit)
            .take(4)
            .takeIf { it.toIntOrNull() in 1300..1499 }
            .orEmpty()

        return TitleMetadata(
            contentUrl = item.contentUrl,
            posterUrl = if (item.posterUrl.isBlank() && known?.posterUrl.isNullOrBlank()) {
                imagePath(video, "poster_image")
            } else "",
            backdropUrl = if (
                item.backdropUrl.isBlank() && known?.backdropUrl.isNullOrBlank()
            ) {
                imagePath(video, "cover_image").ifBlank {
                    imagePath(video, "alter_cover_image")
                }
            } else "",
            summary = if (known?.summary.isNullOrBlank()) summary else "",
            // An exact Iranian-platform match is authoritative for the Solar Hijri year.
            year = solarYear,
            genres = if (known?.genres.isNullOrEmpty()) genres else emptyList(),
            country = if (known?.country.isNullOrBlank()) {
                countries.joinToString("، ").ifBlank { "ایران" }
            } else "",
            language = if (known?.language.isNullOrBlank()) "فارسی" else "",
            hasPersianSubtitle = Regex("""زیرنویس\s*:?[\s‌]*فارسی""")
                .containsMatchIn(summary),
            directors = if (known?.directors.isNullOrEmpty()) directors.distinct() else emptyList(),
            cast = if (known?.cast.isNullOrEmpty()) cast.distinct().take(8) else emptyList(),
            externalLookupAt = now,
            externalLookupVersion = PublicTitleMetadataEnricher.LOOKUP_VERSION,
            fetchedAt = now
        )
    }

    private fun categoryTitles(video: JSONObject, expectedType: String): List<String> {
        val groups = video.optJSONArray("categories") ?: JSONArray()
        return buildList {
            for (index in 0 until groups.length()) {
                val group = groups.optJSONObject(index) ?: continue
                if (!group.optString("type").equals(expectedType, true)) continue
                val items = group.optJSONArray("items") ?: JSONArray()
                for (itemIndex in 0 until items.length()) {
                    items.optJSONObject(itemIndex)?.optString("title")
                        ?.cleanText(40)
                        ?.takeIf { it.isNotBlank() }
                        ?.let(::add)
                }
            }
        }.distinct()
    }

    private fun imagePath(video: JSONObject, key: String): String =
        video.optJSONObject(key)?.optString("path")
            ?.takeIf { it.startsWith("https://", true) }
            .orEmpty()

    private fun nextData(html: String): JSONObject? {
        val payload = Regex(
            """<script[^>]+id=[\"']__NEXT_DATA__[\"'][^>]*>(.*?)</script>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        ).find(html)?.groupValues?.getOrNull(1) ?: return null
        return runCatching { JSONObject(payload) }.getOrNull()
    }

    private fun kindMatches(kind: CatalogKind, value: String): Boolean = when (kind) {
        CatalogKind.MOVIE -> value.contains("movie", true) || value.contains("film", true)
        CatalogKind.SERIES -> value.contains("series", true)
    }

    private fun isIranian(item: SpotlightItem): Boolean =
        item.serviceId.equals("parsiflix", true) ||
            item.country.contains("ایران") || item.country.contains("Iran", true)

    private fun titleScore(expected: String, candidate: String): Double {
        val left = normalize(expected)
        val right = normalize(candidate)
        if (left.isBlank() || right.isBlank()) return 0.0
        if (left == right) return 1.0
        if (left.contains(right) || right.contains(left)) return 0.94
        val leftTokens = left.split(' ').filter { it.length > 1 }.toSet()
        val rightTokens = right.split(' ').filter { it.length > 1 }.toSet()
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) return 0.0
        return leftTokens.intersect(rightTokens).size.toDouble() /
            leftTokens.union(rightTokens).size.toDouble()
    }

    private fun cleanTitle(value: String): String = value
        .replace(Regex("""\s*[（(]\s*\d{4}\s*[)）]\s*$"""), "")
        .cleanText(140)

    private fun normalize(value: String): String = Normalizer
        .normalize(cleanTitle(value), Normalizer.Form.NFKD)
        .replace(Regex("""\p{M}+"""), "")
        .lowercase()
        .replace('ي', 'ی')
        .replace('ك', 'ک')
        .replace(Regex("""\b(?:film|movie|tv|television|series)\b"""), " ")
        .replace(Regex("""(?:فیلم|سریال|مجموعه)"""), " ")
        .replace(Regex("""[^\p{L}\p{N}]+"""), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()

    private fun cleanHtml(value: String, limit: Int): String = value
        .replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("""<[^>]+>"""), " ")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .cleanText(limit)

    private fun String.cleanText(limit: Int): String =
        replace(Regex("""\s+"""), " ").trim().take(limit)

    private fun getText(url: String): String? {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 6_000
            readTimeout = 9_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "text/html,application/xhtml+xml")
            setRequestProperty(
                "User-Agent",
                "Aminema/0.16.2 (personal Android TV public title metadata; " +
                    "https://github.com/AminAsadollah25/aminema-tv)"
            )
        }
        return runCatching {
            if (connection.responseCode !in 200..299) return@runCatching null
            connection.inputStream.bufferedReader().use { it.readText().take(4_000_000) }
        }.getOrNull().also { connection.disconnect() }
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")

    private fun encodePath(value: String): String = encode(value).replace("%2F", "")

    private companion object {
        const val BASE_URL = "https://filmnet.ir"
    }
}
