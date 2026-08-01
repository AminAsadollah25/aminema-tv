package com.amin.tvos.data

import com.amin.tvos.data.model.CatalogKind
import com.amin.tvos.data.model.PersonRef
import com.amin.tvos.data.model.SpotlightItem
import com.amin.tvos.data.model.TitleMetadata
import com.amin.tvos.data.model.isDecisionComplete
import com.amin.tvos.data.model.mergePrefer
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.Normalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Fills gaps left by a provider from public Wikipedia/Wikidata metadata.
 *
 * Provider data always wins. This class never touches IMDb pages (IMDb has no anonymous
 * public metadata API); it only uses an IMDb title id when the normal provider page exposes
 * one, making the Wikidata match exact. Without that id, title + release year are validated
 * before accepting a result. No media URL, cookie or authentication value is sent.
 */
class PublicTitleMetadataEnricher {

    private val filmnet = FilmnetPublicMetadataEnricher()

    suspend fun lookup(
        item: SpotlightItem,
        known: TitleMetadata?
    ): TitleMetadata? = withContext(Dispatchers.IO) {
        val needsIranianSolarYear = item.serviceId.equals("parsiflix", true) &&
            known?.year?.filter(Char::isDigit)?.take(4)?.toIntOrNull() !in 1300..1499
        if (known?.isDecisionComplete() == true && !needsIranianSolarYear) {
            return@withContext null
        }

        val cleanTitle = cleanDisplayTitle(item.title, item.year.ifBlank { known?.year.orEmpty() })
        if (cleanTitle.length < 2) return@withContext null
        val rawExpectedYear = item.year.ifBlank { known?.year.orEmpty() }
            .filter(Char::isDigit)
            .take(4)
            .toIntOrNull()
        // Parsiflix frequently publishes the catalog/upload year for older Iranian titles.
        // Treating that as the production year rejects an otherwise exact Wikidata match
        // (for example a 1393 film newly added in 1405). A verified Solar year remains useful.
        val expectedYear = rawExpectedYear.takeUnless {
            item.serviceId.equals("parsiflix", true) && it in 1900..2200
        }
        val expectedImdb = known?.imdbId.orEmpty()
        val now = System.currentTimeMillis()

        // Iranian titles first try an exact match on Filmnet's public, server-rendered
        // title page. It is the most useful source for Solar year and Persian credits.
        val iranianMetadata = filmnet.lookup(item, known)
        val effectiveKnown = when {
            known != null && iranianMetadata != null -> known.mergePrefer(iranianMetadata)
            iranianMetadata != null -> iranianMetadata
            else -> known
        }
        if (effectiveKnown?.isDecisionComplete() == true) {
            return@withContext iranianMetadata
        }

        val match = listOf("fa", "en").firstNotNullOfOrNull { language ->
            findWikipediaMatch(
                language = language,
                title = cleanTitle,
                kind = item.kind,
                expectedYear = expectedYear,
                expectedImdb = expectedImdb
            )
        } ?: return@withContext iranianMetadata

        val claims = match.entity.optJSONObject("claims") ?: JSONObject()
        val directorIds = entityIds(claims, "P57").take(3)
        val castIds = entityIds(claims, "P161").take(8)
        val genreIds = entityIds(claims, "P136").take(4)
        val countryIds = entityIds(claims, "P495").take(3)
        val languageIds = entityIds(claims, "P364").take(3)
        val labels = labelsFor(
            (directorIds + castIds + genreIds + countryIds + languageIds).distinct()
        )
        val releaseYear = claimTimeYears(claims, "P577").firstOrNull()?.toString().orEmpty()
        val imdbId = claimStrings(claims, "P345")
            .firstOrNull { IMDB_ID.matches(it) }
            .orEmpty()
        val runtimeMinutes = claimQuantity(claims, "P2047")?.toInt()

        // Return only missing fields. CatalogRepository's field-wise merge therefore keeps
        // the provider's own Persian synopsis/credits whenever it has them.
        val wikipediaMetadata = TitleMetadata(
            contentUrl = item.contentUrl,
            summary = if (effectiveKnown?.summary.isNullOrBlank()) {
                match.extract.take(520)
            } else "",
            year = if (effectiveKnown?.year.isNullOrBlank()) releaseYear else "",
            genres = if (effectiveKnown?.genres.isNullOrEmpty()) {
                genreIds.mapNotNull(labels::get)
            } else emptyList(),
            runtime = if (effectiveKnown?.runtime.isNullOrBlank() && runtimeMinutes != null) {
                "$runtimeMinutes دقیقه"
            } else "",
            country = if (effectiveKnown?.country.isNullOrBlank()) {
                countryIds.mapNotNull(labels::get).joinToString("، ")
            } else "",
            language = if (effectiveKnown?.language.isNullOrBlank()) {
                languageIds.mapNotNull(labels::get).joinToString("، ")
            } else "",
            directors = if (effectiveKnown?.directors.isNullOrEmpty()) {
                directorIds.mapNotNull { id -> labels[id]?.let { PersonRef(it, id) } }
            } else emptyList(),
            cast = if (effectiveKnown?.cast.isNullOrEmpty()) {
                castIds.mapNotNull { id -> labels[id]?.let { PersonRef(it, id) } }
            } else emptyList(),
            imdbId = expectedImdb.ifBlank { imdbId },
            externalLookupAt = now,
            externalLookupVersion = LOOKUP_VERSION,
            fetchedAt = now
        )
        if (iranianMetadata != null) {
            iranianMetadata.mergePrefer(wikipediaMetadata)
        } else {
            wikipediaMetadata
        }
    }

    private fun findWikipediaMatch(
        language: String,
        title: String,
        kind: CatalogKind,
        expectedYear: Int?,
        expectedImdb: String
    ): WikiMatch? {
        val hint = when {
            language == "fa" && kind == CatalogKind.SERIES -> " مجموعه تلویزیونی"
            language == "fa" -> " فیلم"
            kind == CatalogKind.SERIES -> " television series"
            else -> " film"
        }
        val response = getJson(
            "https://$language.wikipedia.org/w/api.php",
            mapOf(
                "action" to "query",
                "generator" to "search",
                "gsrsearch" to "\"$title\"$hint",
                "gsrnamespace" to "0",
                "gsrlimit" to "5",
                "prop" to "extracts|pageprops",
                "exintro" to "1",
                "explaintext" to "1",
                "exsentences" to "5",
                "redirects" to "1",
                "format" to "json",
                "formatversion" to "2"
            )
        ) ?: return null
        val pages = response.optJSONObject("query")?.optJSONArray("pages") ?: JSONArray()
        val candidates = buildList {
            for (index in 0 until pages.length()) {
                val page = pages.optJSONObject(index) ?: continue
                val qid = page.optJSONObject("pageprops")
                    ?.optString("wikibase_item")
                    .orEmpty()
                if (!QID.matches(qid)) continue
                val pageTitle = page.optString("title")
                val score = titleScore(title, pageTitle)
                add(
                    WikiCandidate(
                        qid = qid,
                        title = pageTitle,
                        extract = cleanText(page.optString("extract"), 900),
                        score = score
                    )
                )
            }
        }.sortedByDescending { it.score }

        for (candidate in candidates.take(3)) {
            val entity = entity(candidate.qid) ?: continue
            val claims = entity.optJSONObject("claims") ?: JSONObject()
            val verifiedTitleScore = maxOf(candidate.score, entityTitleScore(title, entity))
            if (verifiedTitleScore < 0.55) continue
            val imdbIds = claimStrings(claims, "P345")
            if (expectedImdb.isNotBlank() && expectedImdb !in imdbIds) continue
            if (expectedImdb.isBlank() && !kindMatches(kind, claims)) continue

            val years = claimTimeYears(claims, "P577")
            if (
                expectedImdb.isBlank() &&
                expectedYear != null &&
                years.isNotEmpty() &&
                years.none { kotlin.math.abs(it - expectedYear) <= 1 }
            ) continue

            // A title without year/IMDb evidence must be an exceptionally close title match.
            if (expectedImdb.isBlank() && expectedYear == null && verifiedTitleScore < 0.88) continue
            return WikiMatch(candidate.extract, entity)
        }
        return null
    }

    private fun entity(qid: String): JSONObject? = getJson(
        WIKIDATA_API,
        mapOf(
            "action" to "wbgetentities",
            "ids" to qid,
            "props" to "claims|labels|aliases",
            "format" to "json"
        )
    )?.optJSONObject("entities")?.optJSONObject(qid)

    private fun labelsFor(ids: List<String>): Map<String, String> {
        if (ids.isEmpty()) return emptyMap()
        val entities = getJson(
            WIKIDATA_API,
            mapOf(
                "action" to "wbgetentities",
                "ids" to ids.take(45).joinToString("|"),
                "props" to "labels",
                "languages" to "fa|en",
                "languagefallback" to "1",
                "format" to "json"
            )
        )?.optJSONObject("entities") ?: return emptyMap()
        return buildMap {
            ids.forEach { id ->
                val labels = entities.optJSONObject(id)?.optJSONObject("labels")
                val label = labels?.optJSONObject("fa")?.optString("value")
                    ?.takeIf { it.isNotBlank() }
                    ?: labels?.optJSONObject("en")?.optString("value")
                label?.let { put(id, cleanText(it, 80)) }
            }
        }
    }

    private fun entityTitleScore(expected: String, entity: JSONObject): Double {
        val names = buildList {
            val labels = entity.optJSONObject("labels")
            labels?.optJSONObject("fa")?.optString("value")?.let(::add)
            labels?.optJSONObject("en")?.optString("value")?.let(::add)
            val aliases = entity.optJSONObject("aliases")
            listOf("fa", "en").forEach { language ->
                val values = aliases?.optJSONArray(language) ?: JSONArray()
                for (index in 0 until minOf(values.length(), 12)) {
                    values.optJSONObject(index)?.optString("value")?.let(::add)
                }
            }
        }.filter { it.isNotBlank() }
        return names.maxOfOrNull { titleScore(expected, it) } ?: 0.0
    }

    private fun kindMatches(kind: CatalogKind, claims: JSONObject): Boolean {
        val types = entityIds(claims, "P31").toSet()
        return when (kind) {
            CatalogKind.MOVIE -> types.any { it in MOVIE_TYPES }
            CatalogKind.SERIES -> types.any { it in SERIES_TYPES }
        }
    }

    private fun entityIds(claims: JSONObject, property: String): List<String> =
        claimValues(claims, property).mapNotNull { value -> value.optString("id") }
            .filter(QID::matches)
            .distinct()

    private fun claimStrings(claims: JSONObject, property: String): List<String> =
        claimValues(claims, property).mapNotNull { value ->
            value.takeIf { it.has("value") }?.optString("value")
        }.filter { it.isNotBlank() }.distinct()

    private fun claimTimeYears(claims: JSONObject, property: String): List<Int> =
        claimValues(claims, property).mapNotNull { value ->
            Regex("""[+-](\d{4,})-""").find(value.optString("time"))
                ?.groupValues?.getOrNull(1)?.toIntOrNull()
        }.distinct()

    private fun claimQuantity(claims: JSONObject, property: String): Double? =
        claimValues(claims, property).firstNotNullOfOrNull { value ->
            value.optString("amount").removePrefix("+").toDoubleOrNull()
        }

    private fun claimValues(claims: JSONObject, property: String): List<JSONObject> {
        val statements = claims.optJSONArray(property) ?: JSONArray()
        return buildList {
            for (index in 0 until statements.length()) {
                val value = statements.optJSONObject(index)
                    ?.optJSONObject("mainsnak")
                    ?.optJSONObject("datavalue")
                    ?.opt("value")
                when (value) {
                    is JSONObject -> add(value)
                    is String -> add(JSONObject().put("value", value))
                    is Number -> add(JSONObject().put("amount", value.toString()))
                }
            }
        }
    }

    private fun getJson(base: String, parameters: Map<String, String>): JSONObject? {
        val query = parameters.entries.joinToString("&") { (key, value) ->
            "${encode(key)}=${encode(value)}"
        }
        val connection = (URL("$base?$query").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 6_000
            readTimeout = 8_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty(
                "User-Agent",
                "Aminema/0.16.2 (personal Android TV metadata; " +
                    "https://github.com/AminAsadollah25/aminema-tv)"
            )
        }
        return runCatching {
            if (connection.responseCode !in 200..299) return@runCatching null
            connection.inputStream.bufferedReader().use { reader ->
                JSONObject(reader.readText())
            }
        }.getOrNull().also { connection.disconnect() }
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")

    private fun titleScore(expected: String, candidate: String): Double {
        val left = normalizedTitle(expected)
        val right = normalizedTitle(candidate)
        if (left.isBlank() || right.isBlank()) return 0.0
        if (left == right) return 1.0
        if (right.contains(left) || left.contains(right)) return 0.92
        val leftTokens = left.split(' ').filter { it.length > 1 }.toSet()
        val rightTokens = right.split(' ').filter { it.length > 1 }.toSet()
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) return 0.0
        return leftTokens.intersect(rightTokens).size.toDouble() /
            leftTokens.union(rightTokens).size.toDouble()
    }

    private fun cleanDisplayTitle(value: String, year: String): String = value
        .replace(Regex("""\s*[（(]\s*(?:19|20)?\d{2}\s*[)）]\s*$"""), "")
        .replace(year, "")
        .replace(Regex("""\s+"""), " ")
        .trim()
        .take(140)

    private fun normalizedTitle(value: String): String = Normalizer
        .normalize(cleanDisplayTitle(value, ""), Normalizer.Form.NFKD)
        .replace(Regex("""\p{M}+"""), "")
        .lowercase()
        .replace('ي', 'ی')
        .replace('ك', 'ک')
        .replace(Regex("""\b(?:film|movie|tv|television|series|miniseries)\b"""), " ")
        .replace(Regex("""[^\p{L}\p{N}]+"""), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()

    private fun cleanText(value: String, limit: Int): String =
        value.replace(Regex("""\s+"""), " ").trim().take(limit)

    private data class WikiCandidate(
        val qid: String,
        val title: String,
        val extract: String,
        val score: Double
    )

    private data class WikiMatch(val extract: String, val entity: JSONObject)

    companion object {
        const val WIKIDATA_API = "https://www.wikidata.org/w/api.php"
        val QID = Regex("""Q\d+""")
        val IMDB_ID = Regex("""tt\d{5,12}""")
        val MOVIE_TYPES = setOf("Q11424", "Q24869", "Q506240", "Q93204", "Q24862")
        val SERIES_TYPES = setOf(
            "Q5398426", "Q1259759", "Q15416", "Q526877", "Q581714",
            "Q1366112", "Q63952888"
        )
        const val EXTERNAL_RETRY_MS = 30L * 24L * 60L * 60L * 1_000L
        // v5 adds the official public Sheyda title UI as an Iranian credits fallback.
        const val LOOKUP_VERSION = 5

        fun shouldLookup(metadata: TitleMetadata?, item: SpotlightItem? = null): Boolean {
            val needsIranianSolarYear = item?.serviceId?.equals("parsiflix", true) == true &&
                metadata?.year?.filter(Char::isDigit)?.take(4)?.toIntOrNull() !in 1300..1499
            return (metadata?.isDecisionComplete() != true || needsIranianSolarYear) &&
                ((metadata?.externalLookupVersion ?: 0) < LOOKUP_VERSION ||
                    System.currentTimeMillis() - (metadata?.externalLookupAt ?: 0L) >
                    EXTERNAL_RETRY_MS)
        }
    }
}
