package com.amin.tvos.data

import com.amin.tvos.data.model.*
import java.time.LocalDate

enum class JourneyFormat(val label: String) {
    MOVIE("فیلم"), SERIES("سریال"), ANIMATION("انیمیشن"), SHORT("کوتاه"), SPECIAL("ویژه")
}

/** Reference identity is independent from provider availability and from list membership. */
data class JourneyWork(
    val id: String, val title: String, val release: String,
    val format: JourneyFormat = JourneyFormat.MOVIE,
    val season: Int = 0, val premiereYear: String = release.take(4),
    val collections: Set<String>, val aliases: Set<String> = emptySet(),
    val imdbId: String = ""
) {
    val kind get() = if (season > 0 || format == JourneyFormat.SERIES) CatalogKind.SERIES else CatalogKind.MOVIE
    val dateLabel get() = if (release.length == 4) "$release · ترتیب داخل سال ممیزی نشده" else release
}

data class JourneyCollection(val id: String, val title: String, val description: String)
data class JourneyMatch(val work: JourneyWork, val media: CanonicalMedia?)

/** Curated research v0.1; not an assertion of complete Marvel/DC history or site inventory. */
object JourneyCatalog {
    val marvelCollections = listOf(
        JourneyCollection("vast", "مسیر عظیم مارول", "گسترده‌ترین فهرست فعلی · تکمیل اقتباس‌های قدیمی ادامه دارد"),
        JourneyCollection("doomsday", "به‌سوی Doomsday — گسترده", "MCU، مرد عنکبوتی و ایکس‌من · مسیر پیشنهادی، نه پیش‌نیاز رسمی"),
        JourneyCollection("mcu", "MCU", "فیلم‌ها، فصل‌ها، کوتاه‌ها و ویژه‌برنامه‌ها"),
        JourneyCollection("spider", "Spider-Man Universe", "شاخه‌های MCU، سونی و انیمیشن"),
        JourneyCollection("xmen", "X-Men Universe", "فیلم‌ها و سریال‌های خانوادهٔ جهش‌یافته‌ها"),
        JourneyCollection("complete", "Marvel کامل", "همهٔ شاخه‌های ثبت‌شده، از جمله قدیمی‌ها · مرجع در حال تکمیل")
    )
    val marathonCollections = listOf(
        JourneyCollection("middleearth", "سرزمین میانه", "ارباب حلقه‌ها و هابیت · ترتیب انتشار"),
        JourneyCollection("starwars", "جنگ ستارگان", "فیلم‌ها و فصل‌های ثبت‌شده · ترتیب انتشار"),
        JourneyCollection("dc", "DC · دنیای سینمایی", "DCEU · جهان‌های دیگر در مرحلهٔ تکمیل"),
        JourneyCollection("potter", "دنیای جادوگری", "هری پاتر و جانوران شگفت‌انگیز")
    )

    // Construction is lazy: Home only needs the two entry cards, never this registry.
    val works: List<JourneyWork> by lazy {
        buildList {
            fun films(collection: String, rows: String, extra: Set<String> = emptySet()) {
                rows.trimIndent().lines().filter { it.isNotBlank() }.forEach { row ->
                    val p = row.split('|')
                    val memberships = setOf(collection) + extra + p.getOrNull(2).orEmpty().split(',').filter { it.isNotBlank() }
                    add(JourneyWork("${collection}:${p[0]}:${key(p[1])}", p[1], p[0], collections = memberships))
                }
            }
            fun show(collection: String, title: String, dates: String, format: JourneyFormat = JourneyFormat.SERIES) {
                val starts = dates.split(',')
                starts.forEachIndexed { i, date ->
                    add(JourneyWork("$collection:${key(title)}:s${i + 1}", title, date, format,
                        i + 1, starts.first().take(4), setOf(collection)))
                }
            }
            films("mcu", """
                2008-05-02|Iron Man
                2008-06-13|The Incredible Hulk
                2010-05-07|Iron Man 2
                2011-05-06|Thor
                2011-07-22|Captain America: The First Avenger
                2012-05-04|The Avengers
                2013-05-03|Iron Man 3
                2013-11-08|Thor: The Dark World
                2014-04-04|Captain America: The Winter Soldier
                2014-08-01|Guardians of the Galaxy
                2015-05-01|Avengers: Age of Ultron
                2015-07-17|Ant-Man
                2016-05-06|Captain America: Civil War|spider
                2016-11-04|Doctor Strange
                2017-05-05|Guardians of the Galaxy Vol. 2
                2017-07-07|Spider-Man: Homecoming|spider
                2017-11-03|Thor: Ragnarok
                2018-02-16|Black Panther
                2018-04-27|Avengers: Infinity War|spider
                2018-07-06|Ant-Man and the Wasp
                2019-03-08|Captain Marvel
                2019-04-26|Avengers: Endgame|spider
                2019-07-02|Spider-Man: Far From Home|spider
                2021-07-09|Black Widow
                2021-09-03|Shang-Chi and the Legend of the Ten Rings
                2021-11-05|Eternals
                2021-12-17|Spider-Man: No Way Home|spider
                2022-05-06|Doctor Strange in the Multiverse of Madness
                2022-07-08|Thor: Love and Thunder
                2022-11-11|Black Panther: Wakanda Forever
                2023-02-17|Ant-Man and the Wasp: Quantumania
                2023-05-05|Guardians of the Galaxy Vol. 3
                2023-11-10|The Marvels
                2024-07-26|Deadpool & Wolverine|xmen
                2025-02-14|Captain America: Brave New World
                2025-05-02|Thunderbolts*
                2025-07-25|The Fantastic Four: First Steps
                2026-07-31|Spider-Man: Brand New Day|spider
            """)
            show("mcu", "WandaVision", "2021-01-15")
            show("mcu", "The Falcon and the Winter Soldier", "2021-03-19")
            show("mcu", "Loki", "2021-06-09,2023-10-05")
            show("mcu", "What If...?", "2021-08-11,2023-12-22,2024-12-22", JourneyFormat.ANIMATION)
            show("mcu", "Hawkeye", "2021-11-24")
            show("mcu", "Moon Knight", "2022-03-30")
            show("mcu", "Ms. Marvel", "2022-06-08")
            show("mcu", "I Am Groot", "2022-08-10,2023-09-06", JourneyFormat.SHORT)
            show("mcu", "She-Hulk: Attorney at Law", "2022-08-18")
            show("mcu", "Secret Invasion", "2023-06-21")
            show("mcu", "Echo", "2024-01-09")
            show("mcu", "Agatha All Along", "2024-09-18")
            show("mcu", "Daredevil: Born Again", "2025-03-04,2026-03-24")
            show("mcu", "Ironheart", "2025-06-24")
            show("mcu", "Eyes of Wakanda", "2025-08-01", JourneyFormat.ANIMATION)
            show("mcu", "Marvel Zombies", "2025-09-24", JourneyFormat.ANIMATION)
            show("mcu", "Wonder Man", "2026-01-27")
            show("spider", "Your Friendly Neighborhood Spider-Man", "2025-01-29", JourneyFormat.ANIMATION)
            listOf("2022-10-07|Werewolf by Night", "2022-11-25|The Guardians of the Galaxy Holiday Special",
                "2026-05-12|The Punisher: One Last Kill").forEach { row ->
                val p = row.split('|'); add(JourneyWork("mcu:${key(p[1])}", p[1], p[0], JourneyFormat.SPECIAL, collections = setOf("mcu")))
            }
            listOf("2011-09-13|The Consultant", "2011-10-25|A Funny Thing Happened on the Way to Thor's Hammer",
                "2012-09-25|Item 47", "2013-09-03|Agent Carter", "2014-02-04|All Hail the King").forEach { row ->
                val p = row.split('|'); add(JourneyWork("mcu:short:${key(p[1])}", p[1], p[0], JourneyFormat.SHORT, collections = setOf("mcu")))
            }
            show("legacy", "Agents of S.H.I.E.L.D.", "2013-09-24,2014-09-23,2015-09-29,2016-09-20,2017-12-01,2019-05-10,2020-05-27")
            show("legacy", "Agent Carter", "2015-01-06,2016-01-19")
            show("legacy", "Daredevil", "2015-04-10,2016-03-18,2018-10-19")
            show("legacy", "Jessica Jones", "2015-11-20,2018-03-08,2019-06-14")
            show("legacy", "Luke Cage", "2016-09-30,2018-06-22")
            show("legacy", "Iron Fist", "2017-03-17,2018-09-07")
            show("legacy", "The Defenders", "2017-08-18")
            show("legacy", "Inhumans", "2017-09-29")
            show("legacy", "The Punisher", "2017-11-17,2019-01-18")
            show("legacy", "Runaways", "2017-11-21,2018-12-21,2019-12-13")
            show("legacy", "Cloak & Dagger", "2018-06-07,2019-04-04")
            films("spider", """
                2002|Spider-Man
                2004|Spider-Man 2
                2007|Spider-Man 3
                2012|The Amazing Spider-Man
                2014|The Amazing Spider-Man 2
                2018|Venom
                2021|Venom: Let There Be Carnage
                2022|Morbius
                2024|Madame Web
                2024|Venom: The Last Dance
                2024|Kraven the Hunter
            """)
            listOf("2018|Spider-Man: Into the Spider-Verse", "2023|Spider-Man: Across the Spider-Verse").forEach {
                val p = it.split('|'); add(JourneyWork("spider:${key(p[1])}", p[1], p[0], JourneyFormat.ANIMATION, collections = setOf("spider")))
            }
            films("xmen", """
                2000-07-14|X-Men
                2003-05-02|X2
                2006-05-26|X-Men: The Last Stand
                2009-05-01|X-Men Origins: Wolverine
                2011-06-03|X-Men: First Class
                2013-07-26|The Wolverine
                2014-05-23|X-Men: Days of Future Past
                2016-02-12|Deadpool
                2016-05-27|X-Men: Apocalypse
                2017-03-03|Logan
                2018-05-18|Deadpool 2
                2019-06-07|Dark Phoenix
                2020-08-28|The New Mutants
            """)
            // Only premiere years verified here; do not invent season start dates.
            show("xmen", "Legion", "2017")
            show("xmen", "The Gifted", "2017")
            show("xmen", "X-Men '97", "2024", JourneyFormat.ANIMATION)
            films("legacy", """
                1986|Howard the Duck
                1989|The Punisher
                1990|Captain America
                1998|Blade
                2002|Blade II
                2004|Blade: Trinity
                2003|Daredevil
                2003|Hulk
                2004|The Punisher
                2005|Elektra
                2005|Fantastic Four
                2007|Fantastic Four: Rise of the Silver Surfer
                2015|Fantastic Four
                2007|Ghost Rider
                2012|Ghost Rider: Spirit of Vengeance
                2008|Punisher: War Zone
            """)
            films("middleearth", """
                2001|The Lord of the Rings: The Fellowship of the Ring
                2002|The Lord of the Rings: The Two Towers
                2003|The Lord of the Rings: The Return of the King
                2012|The Hobbit: An Unexpected Journey
                2013|The Hobbit: The Desolation of Smaug
                2014|The Hobbit: The Battle of the Five Armies
            """)
            films("starwars", """
                1977|Star Wars: Episode IV - A New Hope
                1980|Star Wars: Episode V - The Empire Strikes Back
                1983|Star Wars: Episode VI - Return of the Jedi
                1999|Star Wars: Episode I - The Phantom Menace
                2002|Star Wars: Episode II - Attack of the Clones
                2005|Star Wars: Episode III - Revenge of the Sith
                2015|Star Wars: Episode VII - The Force Awakens
                2016|Rogue One: A Star Wars Story
                2017|Star Wars: Episode VIII - The Last Jedi
                2018|Solo: A Star Wars Story
                2019|Star Wars: Episode IX - The Rise of Skywalker
            """)
            show("starwars", "The Mandalorian", "2019-11-12,2020-10-30,2023-03-01")
            show("starwars", "The Book of Boba Fett", "2021-12-29")
            show("starwars", "Obi-Wan Kenobi", "2022-05-27")
            show("starwars", "Andor", "2022-09-21,2025-04-22")
            show("starwars", "Ahsoka", "2023-08-22")
            show("starwars", "The Acolyte", "2024-06-04")
            show("starwars", "Skeleton Crew", "2024-12-02")
            show("starwars", "Star Wars: The Clone Wars", "2008-10-03,2009-10-02,2010-09-17,2011-09-16,2012-09-29,2014-03-07,2020-02-21", JourneyFormat.ANIMATION)
            show("starwars", "Star Wars Rebels", "2014-10-03,2015-06-20,2016-09-24,2017-10-16", JourneyFormat.ANIMATION)
            show("starwars", "Star Wars Resistance", "2018-10-07,2019-10-06", JourneyFormat.ANIMATION)
            show("starwars", "Star Wars: The Bad Batch", "2021-05-04,2023-01-04,2024-02-21", JourneyFormat.ANIMATION)
            films("dc", """
                2013|Man of Steel
                2016|Batman v Superman: Dawn of Justice
                2016|Suicide Squad
                2017|Wonder Woman
                2017|Justice League
                2018|Aquaman
                2019|Shazam!
                2020|Birds of Prey
                2020|Wonder Woman 1984
                2021|The Suicide Squad
                2022|Black Adam
                2023|Shazam! Fury of the Gods
                2023|The Flash
                2023|Blue Beetle
                2023|Aquaman and the Lost Kingdom
            """)
            films("potter", """
                2001|Harry Potter and the Sorcerer's Stone
                2002|Harry Potter and the Chamber of Secrets
                2004|Harry Potter and the Prisoner of Azkaban
                2005|Harry Potter and the Goblet of Fire
                2007|Harry Potter and the Order of the Phoenix
                2009|Harry Potter and the Half-Blood Prince
                2010|Harry Potter and the Deathly Hallows: Part 1
                2011|Harry Potter and the Deathly Hallows: Part 2
                2016|Fantastic Beasts and Where to Find Them
                2018|Fantastic Beasts: The Crimes of Grindelwald
                2022|Fantastic Beasts: The Secrets of Dumbledore
            """)
        }.map { work ->
            // These full titles/public ids were verified in the sites' ordinary search
            // results on 2026-10-13. The 2023 series The Consultant is NOT this short.
            val shortIds = mapOf("The Consultant" to "tt2011118",
                "A Funny Thing Happened on the Way to Thor's Hammer" to "tt2011109",
                "Item 47" to "tt2247732", "Agent Carter" to "tt3067038",
                "All Hail the King" to "tt3438640")
            if (work.format == JourneyFormat.SHORT && work.season == 0 && work.title in shortIds) {
                return@map work.copy(aliases = setOf("Marvel One-Shot: ${work.title}"), imdbId = shortIds.getValue(work.title))
            }
            when (work.title) {
                "Your Friendly Neighborhood Spider-Man" -> work.copy(collections = work.collections + "mcu")
                "Harry Potter and the Sorcerer's Stone" -> work.copy(aliases = setOf("Harry Potter and the Philosopher's Stone"))
                "X2" -> work.copy(aliases = setOf("X2: X-Men United", "X-Men 2"))
                "Guardians of the Galaxy" -> work.copy(imdbId = "tt2015381")
                "Ghost Rider: Spirit of Vengeance" -> work.copy(imdbId = "tt1071875")
                "Skeleton Crew" -> work.copy(aliases = setOf("Star Wars: Skeleton Crew"), imdbId = "tt20600980")
                "The Gifted" -> work.copy(imdbId = "tt4396630") // US 2017 detail page; not Thai tt9136312.
                else -> work
            }
        }.distinctBy { it.id }
    }

    fun select(collection: String, format: JourneyFormat? = null, today: LocalDate = LocalDate.now()): List<JourneyWork> =
        works.filter { work ->
            val belongs = when (collection) {
                "complete", "vast" -> work.collections.any { it in setOf("mcu", "spider", "xmen", "legacy") }
                "doomsday" -> work.collections.any { it in setOf("mcu", "spider", "xmen") }
                else -> collection in work.collections
            }
            belongs && (format == null || work.format == format) &&
                work.release.take(4).toInt() <= today.year &&
                (work.release.length != 10 || !LocalDate.parse(work.release).isAfter(today))
        }.sortedWith(compareBy<JourneyWork> { it.release }.thenBy { it.title }.thenBy { it.season })

    private fun key(value: String): String = CanonicalText.normalizeTitle(value)

    /** Exact curated title+original year+kind only; never fuzzy-prefix a franchise. */
    fun resolve(works: List<JourneyWork>, sections: List<CatalogSection>, metadata: Map<String, TitleMetadata> = emptyMap(),
        providerNames: Map<String, String> = emptyMap()): List<JourneyMatch> {
        val raw = sections.flatMap { it.all + it.movies + it.series + it.featured + it.popularSeries }
            .distinctBy { ContentMetadataPolicy.canonicalContentUrl(it.contentUrl) }
        val index = raw.groupBy { item ->
            val meta = metadata[ContentMetadataPolicy.canonicalContentUrl(item.contentUrl)]
            // Verified detail identity/year outranks a result card's incidental year.
            val detailYear = meta?.year?.takeIf { meta.imdbId.isNotBlank() && it.isNotBlank() }
            Triple(key(item.title), publicMatchYear(item.serviceId, detailYear ?: item.year, meta?.year.orEmpty()).toString(), item.kind)
        }
        val imdbIndex = raw.groupBy { item -> item.imdbId.ifBlank {
            metadata[ContentMetadataPolicy.canonicalContentUrl(item.contentUrl)]?.imdbId.orEmpty()
        }.lowercase(java.util.Locale.ROOT) }
        return works.map { work ->
            val candidates = (setOf(work.title) + work.aliases).flatMap { title ->
                index[Triple(key(title), work.premiereYear, work.kind)].orEmpty()
            }.plus(if (work.imdbId.isBlank()) emptyList() else imdbIndex[work.imdbId].orEmpty().filter { it.kind == work.kind })
                .distinctBy { it.contentUrl }
                .filter { candidate ->
                    val id = candidate.imdbId.ifBlank { metadata[ContentMetadataPolicy.canonicalContentUrl(candidate.contentUrl)]?.imdbId.orEmpty() }
                    work.imdbId.isBlank() || id.isBlank() || id.equals(work.imdbId, true)
                }
            // Multiple conflicting canonical identities are ambiguous, not a playable guess.
            val matches = CanonicalLibrary.canonicalize(candidates.map { it.copy(title = work.title) }, metadata, providerNames)
            val media = matches.singleOrNull()?.takeUnless { canonical ->
                // Same provider, same title/year, different pages can be two unrelated
                // series. Require detail ids rather than hiding the ambiguity in a merge.
                canonical.variants.groupBy { it.providerId }.values.any { variants ->
                    variants.size > 1 && variants.any { it.imdbId.isBlank() }
                }
            }
            JourneyMatch(work, media)
        }
    }
}
