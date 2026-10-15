package com.amin.tvos.data

import android.content.Context
import com.amin.tvos.data.model.CatalogItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class MarathonFamily(
    val id: String, val title: String, val sourceLabel: String,
    val groups: List<String>, val queries: List<String>, val referenceId: String = ""
)

data class MarathonGroup(val id: String, val title: String, val hue: Float)

/** Research identities, not invented work memberships or availability assertions. */
object MarathonAtlas {
    val groups = listOf(
        MarathonGroup("comic", "ابرقهرمانی و کمیک", 350f),
        MarathonGroup("fantasy", "فانتزی و دنیاهای جادویی", 275f),
        MarathonGroup("scifi", "علمی‌تخیلی و فضا", 215f),
        MarathonGroup("adventure", "ماجراجویی", 145f),
        MarathonGroup("action", "اکشن و جاسوسی", 25f),
        MarathonGroup("crime", "جنایی و معمایی", 185f),
        MarathonGroup("martial", "رزمی و سامورایی", 5f),
        MarathonGroup("horror", "ترسناک", 325f),
        MarathonGroup("animation", "انیمیشن و خانواده", 265f),
        MarathonGroup("anime", "انیمه", 195f),
        MarathonGroup("games", "اقتباس بازی", 120f),
        MarathonGroup("comedy", "کمدی", 40f),
        MarathonGroup("romance", "عاشقانه و موزیکال", 340f),
        MarathonGroup("drama", "درام و سینمای هنری", 235f),
        MarathonGroup("sport", "ورزش و ماشین", 45f),
        MarathonGroup("world", "سینمای جهان", 165f),
        MarathonGroup("iran", "ایرانی و فارسی", 170f)
    )
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }
    @Volatile private var cached: List<MarathonFamily>? = null
    fun parse(text: String): List<MarathonFamily> {
        val result = json.decodeFromString<List<MarathonFamily>>(text)
        require(result.isNotEmpty() && result.size <= 1000)
        require(result.map { it.id }.toSet().size == result.size)
        require(result.all { it.id.isNotBlank() && it.title.isNotBlank() &&
            it.groups.isNotEmpty() && it.groups.all { group -> groups.any { it.id == group } } &&
            it.queries.isNotEmpty() && it.queries.size <= 3 && it.queries.all { q -> q.isNotBlank() && q.length <= 160 } })
        return result
    }
    suspend fun load(context: Context): List<MarathonFamily> = withContext(Dispatchers.IO) {
        cached ?: mutex.withLock {
            cached ?: parse(context.assets.open("marathon_atlas.json").bufferedReader().use { it.readText() })
                .also { cached = it }
        }
    }

    /** Discovery relevance only. These results are never labelled a verified continuity. */
    fun related(family: MarathonFamily, item: CatalogItem): Boolean {
        val name = " ${words(item.title)} "
        return family.queries.any { query ->
            val phrase = words(query.replace(Regex("\\s(?:19|20)\\d{2}$"), ""))
            phrase.isNotBlank() && name.contains(" $phrase ")
        }
    }
    fun matchesFilter(family:MarathonFamily,query:String):Boolean = query.isBlank() ||
        words(family.title+" "+family.sourceLabel+" "+family.queries.joinToString(" ")).contains(words(query))
    private fun words(value:String)=java.text.Normalizer.normalize(value,java.text.Normalizer.Form.NFKD)
        .lowercase(java.util.Locale.ROOT).replace(Regex("\\p{M}+"),"")
        .replace('ي','ی').replace('ك','ک').replace('&',' ')
        .replace(Regex("[^\\p{L}\\p{N}]+")," ").trim()
}
