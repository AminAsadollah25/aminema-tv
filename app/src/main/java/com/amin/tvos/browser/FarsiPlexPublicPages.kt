package com.amin.tvos.browser

import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.text.Normalizer
import java.util.Locale

/** Public title/episode pages only. No cookies, player payloads, scripts or media addresses parsed. */
internal object FarsiPlexPublicPages {
    data class Title(val name:String,val episode:Int?=null,val season:Int?=null)
    private val htmlOptions=setOf(RegexOption.IGNORE_CASE,RegexOption.DOT_MATCHES_ALL)
    private val resultLink=Regex("""<div\b[^>]*class\s*=\s*["'][^"']*\btitle\b[^"']*["'][^>]*>\s*<a\b[^>]*href\s*=\s*["']([^"']+)["'][^>]*>(.*?)</a>""",htmlOptions)
    private val episodeSuffix=Regex("""^(.+?)\s+(?:(?:SE|S)(\d+)\s+)?EP(?:ISODE)?\s*0*(\d+)\s*$""",RegexOption.IGNORE_CASE)
    fun isSource(page:String):Boolean=runCatching {
        val u=URI(page)
        FarsiLandPolicy.allowedPage(page) && u.host.lowercase() in setOf("farsiland.com","www.farsiland.com") &&
            Regex("^/(movies|episodes)/[^/]+/?$").matches(u.path)
    }.getOrDefault(false)
    fun text(html:String):String {
        val stripped=html.replace(Regex("<[^>]*>")," ")
        return Regex("&#(x[0-9a-f]+|[0-9]+);",RegexOption.IGNORE_CASE).replace(stripped){m->
            val raw=m.groupValues[1]
            val n=if(raw.startsWith("x",true))raw.drop(1).toIntOrNull(16) else raw.toIntOrNull()
            if(n!=null && n in 1..0x10ffff && n !in 0xd800..0xdfff)String(Character.toChars(n)) else " "
        }.replace("&amp;","&").replace("&quot;","\"").replace("&apos;","'")
            .replace("&nbsp;"," ").replace(Regex("\\s+")," ").trim()
    }
    fun normalized(name:String)=Normalizer.normalize(text(name),Normalizer.Form.NFKD)
        .lowercase(Locale.ROOT).replace('ي','ی').replace('ك','ک')
        .replace(Regex("[^\\p{L}\\p{Nd}]"),"")
    fun describe(page:String,html:String):Title? {
        if(!isSource(page))return null
        if(URI(page).path.startsWith("/movies/")) {
            val name=Regex("<h1\\b[^>]*>(.*?)</h1>",htmlOptions).find(html)?.groupValues?.get(1)?.let(::text)
            return name?.takeIf {it.isNotBlank() && it.length<=200}?.let {Title(it)}
        }
        // Characterized FL h2: 'HamKalaam EP11', h1: 'HamKalaam Episode 11'.
        val headings=Regex("<h[12]\\b[^>]*>(.*?)</h[12]>",htmlOptions).findAll(html).take(12).map {text(it.groupValues[1])}
        val descriptions=headings.mapNotNull {heading->episodeSuffix.matchEntire(heading)?.let {m->
            val number=m.groupValues[3].toIntOrNull()?.takeIf {it>0}?:return@let null
            Title(m.groupValues[1].trim(),number,m.groupValues[2].toIntOrNull())
        }}.toList()
        val description=descriptions.firstOrNull {it.season!=null}?:descriptions.firstOrNull()?:return null
        // FL has known mislabeled episode headings. A real ALL-series title must agree.
        val parents=Regex("""<a\b([^>]*href\s*=\s*["']https://(?:www\.)?farsiland\.com/tvshows/[^"']+["'][^>]*)>""",htmlOptions)
            .findAll(html).mapNotNull {a->Regex("""\btitle\s*=\s*["']([^"']+)["']""",htmlOptions)
                .find(a.groupValues[1])?.groupValues?.get(1)?.let(::text)}.filter {it.isNotBlank()}.distinct().toList()
        if(parents.isNotEmpty() && parents.none {normalized(it)==normalized(description.name)})return null
        return description
    }
    private fun publicPage(value:String,kind:String):String?=runCatching {
        val u=URI(text(value))
        if(!u.scheme.equals("https",true) || u.host?.lowercase() !in setOf("farsiplex.com","www.farsiplex.com") ||
            u.userInfo!=null || u.port !in setOf(-1,443) || u.rawQuery!=null || u.rawFragment!=null ||
            !Regex("^/$kind/[^/]+/?$").matches(u.path.orEmpty()))null else u.toASCIIString()
    }.getOrNull()
    fun matchTitle(wanted:Title,html:String):String? {
        val kind=if(wanted.episode==null)"movie" else "tvshow"
        val pages=resultLink.findAll(html).filter {normalized(it.groupValues[2])==normalized(wanted.name)}
            .mapNotNull {publicPage(it.groupValues[1],kind)}.distinct().toList()
        return pages.singleOrNull()
    }
    fun matchEpisode(wanted:Title,html:String):String? {
        if(wanted.episode==null)return null
        val rows=Regex("<li\\b[^>]*>(.*?)</li>",htmlOptions).findAll(html)
        val matches=rows.mapNotNull {row->
            val content=row.groupValues[1]
            val number=Regex("""class\s*=\s*["'][^"']*\bnumerando\b[^"']*["'][^>]*>(.*?)</""",htmlOptions)
                .find(content)?.groupValues?.get(1)?.let(::text)
                ?.let {Regex("^(\\d+)\\s*-\\s*(\\d+)$").matchEntire(it)}?:return@mapNotNull null
            val season=number.groupValues[1].toIntOrNull();val episode=number.groupValues[2].toIntOrNull()
            if(episode!=wanted.episode || (wanted.season!=null && season!=wanted.season))return@mapNotNull null
            val href=Regex("""class\s*=\s*["'][^"']*\bepisodiotitle\b[^"']*["'][^>]*>\s*<a\b[^>]*href\s*=\s*["']([^"']+)["']""",htmlOptions)
                .find(content)?.groupValues?.get(1)?:return@mapNotNull null
            publicPage(href,"episode")
        }.distinct().toList()
        return matches.singleOrNull() // Unknown season + duplicated episode is ambiguous, never guess.
    }
    fun resolve(source:String,fetch:(String)->String):String?=runCatching {
        if(!isSource(source))return null
        val wanted=describe(source,fetch(source))?:return null
        val search="https://farsiplex.com/?s="+URLEncoder.encode(wanted.name,"UTF-8")
        val detail=matchTitle(wanted,fetch(search))?:return null
        if(wanted.episode==null)detail else matchEpisode(wanted,fetch(detail))
    }.getOrNull()
    /** At most 3 public requests / 12s checked budget, capped socket waits / 2MB per response. No WebView cookies. */
    fun resolve(source:String):String? {
        val deadline=System.nanoTime()+12_000_000_000L
        return resolve(source){page->
            val remaining=((deadline-System.nanoTime())/1_000_000L).coerceAtMost(4000L).toInt()
            check(remaining>0)
            val connection=(URL(page).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects=false;connectTimeout=remaining;readTimeout=remaining
                setRequestProperty("User-Agent",com.amin.tvos.data.model.UserAgentMode.DESKTOP.value)
                setRequestProperty("Accept","text/html")
            }
            try {
                check(connection.responseCode==200)
                connection.inputStream.use {input->
                    val output=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192)
                    while(true){check(System.nanoTime()<deadline);val n=input.read(buffer);if(n<0)break
                        check(output.size()+n<=2_000_000);output.write(buffer,0,n)}
                    output.toString("UTF-8")
                }
            } finally {connection.disconnect()}
        }
    }
}
