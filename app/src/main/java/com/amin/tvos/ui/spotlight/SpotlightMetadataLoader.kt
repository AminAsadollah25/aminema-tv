package com.amin.tvos.ui.spotlight

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import com.amin.tvos.AminTvApp
import com.amin.tvos.data.model.PersonRef
import com.amin.tvos.data.model.SpotlightItem
import com.amin.tvos.data.model.TitleMetadata
import com.amin.tvos.data.model.UserAgentMode
import org.json.JSONArray
import org.json.JSONObject

/**
 * Reads ordinary, visible metadata from the provider's normal title page.
 *
 * This is deliberately not a scraper or stream resolver: it reuses the signed-in WebView session,
 * stays on the configured service host and never reads media URLs, tokens or protected sources.
 */
class SpotlightMetadataLoader(
    private val activity: ComponentActivity,
    private val app: AminTvApp,
    private val onLoaded: (TitleMetadata) -> Unit,
    private val onFailed: () -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private var webView: WebView? = null
    private var expectedUrl = ""
    private var expectedHost = ""
    private var delivered = false
    private val timeout = Runnable { finish(null) }

    @SuppressLint("SetJavaScriptEnabled")
    fun load(item: SpotlightItem) {
        destroy()
        val service = app.servicesRepository.findById(item.serviceId) ?: run {
            onFailed()
            return
        }
        val contentUri = Uri.parse(item.contentUrl)
        val serviceHost = Uri.parse(service.url).host.orEmpty()
        if (
            contentUri.scheme !in setOf("http", "https") ||
            !contentUri.host.equals(serviceHost, ignoreCase = true)
        ) {
            onFailed()
            return
        }

        expectedUrl = item.contentUrl
        expectedHost = serviceHost
        delivered = false

        val view = WebView(activity).apply {
            alpha = 0.01f
            isFocusable = false
            isFocusableInTouchMode = false
            isClickable = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            overScrollMode = View.OVER_SCROLL_NEVER
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                @Suppress("DEPRECATION")
                databaseEnabled = true
                cacheMode = WebSettings.LOAD_DEFAULT
                allowFileAccess = false
                allowContentAccess = false
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                val mode = service.userAgent
                    ?.let { value -> UserAgentMode.entries.firstOrNull { it.name == value } }
                userAgentString = mode?.value ?: WebSettings.getDefaultUserAgent(activity)
            }
        }
        webView = view
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(view, true)
        }
        view.addJavascriptInterface(MetadataBridge(view), BRIDGE_NAME)
        view.webViewClient = object : WebViewClient() {
            override fun onPageFinished(source: WebView, url: String) {
                CookieManager.getInstance().flush()
                if (
                    delivered ||
                    source !== webView ||
                    !Uri.parse(url).host.equals(expectedHost, ignoreCase = true)
                ) return
                // Both providers hydrate parts of their title page after onPageFinished.
                source.postDelayed(
                    {
                        if (source === webView && !delivered) {
                            source.evaluateJavascript(EXTRACT_SCRIPT, null)
                        }
                    },
                    if (item.serviceId == "parsiflix") 2_200L else 1_350L
                )
            }
        }
        activity.addContentView(
            view,
            FrameLayout.LayoutParams(2, 2, Gravity.BOTTOM or Gravity.END)
        )
        handler.postDelayed(timeout, TIMEOUT_MS)
        view.loadUrl(item.contentUrl)
    }

    private inner class MetadataBridge(private val source: WebView) {
        @JavascriptInterface
        fun result(payload: String?) {
            handler.post {
                if (source !== webView || delivered) return@post
                finish(parse(payload.orEmpty()))
            }
        }
    }

    private fun parse(payload: String): TitleMetadata? {
        val root = runCatching { JSONObject(payload) }.getOrNull() ?: return null

        fun people(key: String): List<PersonRef> {
            val array = root.optJSONArray(key) ?: JSONArray()
            return buildList {
                for (index in 0 until minOf(array.length(), 8)) {
                    val value = array.optJSONObject(index)
                    val name = (value?.optString("name") ?: array.optString(index))
                        .replace(Regex("""\s+"""), " ")
                        .trim()
                        .take(80)
                    if (name.isBlank()) continue
                    val profile = value?.optString("profileUrl").orEmpty().take(2_000)
                    val safeProfile = profile.takeIf {
                        it.startsWith("http", true) &&
                            Uri.parse(it).host.equals(expectedHost, true)
                    }.orEmpty()
                    add(
                        PersonRef(
                            name = name,
                            providerId = value?.optString("providerId").orEmpty().take(80),
                            profileUrl = safeProfile
                        )
                    )
                }
            }.distinctBy { it.name.lowercase() }
        }

        val metadata = TitleMetadata(
            contentUrl = expectedUrl,
            summary = root.optString("summary").clean(520),
            year = root.optString("year")
                .replace(Regex("""[^0-9۰-۹]"""), "")
                .take(4),
            genres = root.optJSONArray("genres")?.let { values ->
                buildList {
                    for (index in 0 until minOf(values.length(), 4)) {
                        values.optString(index).clean(28)
                            .takeIf { it.isNotBlank() }
                            ?.let(::add)
                    }
                }
            }.orEmpty(),
            rating = root.optString("rating")
                .replace(Regex("""[^0-9۰-۹.٫]"""), "")
                .replace('٫', '.')
                .take(5),
            runtime = root.optString("runtime").clean(24),
            country = root.optString("country").clean(60),
            language = root.optString("language").clean(60),
            hasPersianDub = root.optBoolean("hasPersianDub"),
            hasPersianSubtitle = root.optBoolean("hasPersianSubtitle"),
            directors = people("directors"),
            cast = people("cast")
        )
        return metadata.takeIf {
            it.summary.isNotBlank() ||
                it.year.isNotBlank() ||
                it.genres.isNotEmpty() ||
                it.rating.isNotBlank() ||
                it.runtime.isNotBlank() ||
                it.country.isNotBlank() ||
                it.language.isNotBlank() ||
                it.hasPersianDub ||
                it.hasPersianSubtitle ||
                it.directors.isNotEmpty() ||
                it.cast.isNotEmpty()
        }
    }

    private fun String.clean(limit: Int): String =
        replace(Regex("""\s+"""), " ").trim().take(limit)

    private fun finish(metadata: TitleMetadata?) {
        if (delivered) return
        delivered = true
        handler.removeCallbacks(timeout)
        if (metadata == null) onFailed() else onLoaded(metadata)
        releaseWebView()
    }

    fun destroy() {
        handler.removeCallbacksAndMessages(null)
        delivered = true
        releaseWebView()
    }

    private fun releaseWebView() {
        webView?.apply {
            stopLoading()
            removeJavascriptInterface(BRIDGE_NAME)
            (parent as? ViewGroup)?.removeView(this)
            destroy()
        }
        webView = null
    }

    private companion object {
        const val BRIDGE_NAME = "AminSpotlight"
        const val TIMEOUT_MS = 18_000L

        /**
         * Prefer schema.org data, then the smallest visible provider field.
         * Header/navigation/category links are excluded from Persian audio/subtitle detection,
         * avoiding the false positive caused by FilmRooz's global category menu.
         */
        val EXTRACT_SCRIPT = """
            (async function() {
              function clean(value) {
                return String(value || '').replace(/\s+/g, ' ').trim();
              }
              function array(value) {
                if (!value) return [];
                return Array.isArray(value) ? value : [value];
              }
              function person(value) {
                return array(value).map(function(entry) {
                  if (!entry) return null;
                  if (typeof entry === 'string') return {name: clean(entry).slice(0, 80)};
                  var name = clean(entry.name || entry.title);
                  if (!name) return null;
                  var rawUrl = entry.url || entry.sameAs || '';
                  try {
                    var url = rawUrl ? new URL(rawUrl, location.href) : null;
                    rawUrl = url && url.origin === location.origin ? url.href : '';
                  } catch (_) { rawUrl = ''; }
                  return {
                    name: name.slice(0, 80),
                    providerId: String(entry['@id'] || entry.id || '').slice(0, 80),
                    profileUrl: rawUrl
                  };
                }).filter(Boolean);
              }
              function schemaObjects() {
                var found = [];
                document.querySelectorAll('script[type="application/ld+json"]').forEach(
                  function(node) {
                    try {
                      var parsed = JSON.parse(node.textContent || '{}');
                      function visit(value) {
                        if (!value) return;
                        if (Array.isArray(value)) return value.forEach(visit);
                        if (typeof value !== 'object') return;
                        var type = array(value['@type']).join(' ');
                        if (/Movie|TVSeries|TVEpisode|CreativeWork/i.test(type)) found.push(value);
                        if (value['@graph']) visit(value['@graph']);
                      }
                      visit(parsed);
                    } catch (_) {}
                  }
                );
                return found;
              }
              function smallestField(pattern) {
                return Array.from(document.querySelectorAll(
                  'article .col-12,main .col-12,article p,main p,' +
                  'article li,main li,[class*="detail" i] p,[class*="meta" i] .col-12'
                )).filter(function(node) {
                  if (node.closest('header,nav,footer')) return false;
                  var text = clean(node.textContent);
                  return text.length > 1 && text.length < 700 && pattern.test(text);
                }).sort(function(a, b) {
                  return clean(a.textContent).length - clean(b.textContent).length;
                })[0] || null;
              }
              function fieldText(pattern, labels) {
                var node = smallestField(pattern);
                if (!node) return '';
                var linked = Array.from(node.querySelectorAll('a')).map(function(link) {
                  return clean(link.textContent);
                }).filter(Boolean);
                if (linked.length) return linked.join('، ').slice(0, 100);
                var text = clean(node.textContent);
                labels.forEach(function(label) { text = text.replace(label, ''); });
                return clean(text.replace(/^[:：\-–|،\s]+/, '')).slice(0, 100);
              }
              function peopleField(pattern, labels) {
                var node = smallestField(pattern);
                if (!node) return [];
                var linked = Array.from(node.querySelectorAll('a')).map(function(link) {
                  var name = clean(link.textContent);
                  if (!name) return null;
                  var url;
                  try { url = new URL(link.getAttribute('href') || '', location.href); }
                  catch (_) { return null; }
                  return {
                    name: name.slice(0, 80),
                    profileUrl: url.origin === location.origin ? url.href : ''
                  };
                }).filter(Boolean);
                if (linked.length) return linked.slice(0, 8);
                var text = clean(node.textContent);
                labels.forEach(function(label) { text = text.replace(label, ''); });
                return text.replace(/^[:：\-–|،\s]+/, '').split(/[،,|]/)
                  .map(function(name) {
                    name = clean(name);
                    return name ? {name: name.slice(0, 80)} : null;
                  }).filter(Boolean).slice(0, 8);
              }
              function localAudioText() {
                var roots = Array.from(document.querySelectorAll(
                  'article,.single-post,.postMeta,.post-content,.entry-content,' +
                  '[class*="detail" i],[class*="download" i]'
                ));
                var values = [];
                roots.forEach(function(root) {
                  var clone = root.cloneNode(true);
                  clone.querySelectorAll(
                    'header,nav,footer,a[href*="/archive/category/"],script,style'
                  ).forEach(function(node) { node.remove(); });
                  values.push(clean(clone.textContent));
                });
                return values.join(' ').slice(0, 12000);
              }
              function summary(schema) {
                var candidates = [
                  schema && schema.description,
                  document.querySelector('meta[name="description"]') &&
                    document.querySelector('meta[name="description"]').content,
                  document.querySelector('meta[property="og:description"]') &&
                    document.querySelector('meta[property="og:description"]').content
                ];
                var visible = document.querySelector(
                  '.text-justify.mt-2.p-2,.postExcerpt,.excerpt,.summary,' +
                  '[class*="synopsis" i],[class*="plot" i],main [class*="summary" i],' +
                  'main [class*="description" i]'
                );
                if (visible) candidates.unshift(visible.textContent);
                return clean(candidates.find(function(value) {
                  var text = clean(value);
                  return text.length >= 35 &&
                    !/تماشای آنلاین فیلم و سریال|دانلود فیلم و سریال رایگان/i.test(text);
                }) || '').slice(0, 520);
              }

              // Give client-rendered title pages one final short hydration window.
              await new Promise(function(resolve) { setTimeout(resolve, 650); });
              var schema = schemaObjects()[0] || {};
              var titleText = clean(
                (document.querySelector('main h1,main h2,.postMeta h1,.postMeta h2') || {}).textContent
              ) || clean(document.title);
              var publishedText = clean(
                schema.datePublished || schema.dateCreated || titleText
              );
              var yearMatch = publishedText.match(/(?:19|20)\d{2}/);
              var rating = clean(
                schema.aggregateRating && schema.aggregateRating.ratingValue
              );
              if (!rating) {
                var ratingNode = smallestField(/امتیاز.*از\s*۱۰|IMDb/i);
                var ratingMatch = ratingNode
                  ? clean(ratingNode.textContent).match(
                      /([۰-۹0-9]+(?:[.٫][۰-۹0-9]+)?)\s*از\s*۱۰/
                    )
                  : null;
                rating = ratingMatch ? ratingMatch[1] : '';
              }
              var runtime = clean(schema.duration);
              if (/^PT/i.test(runtime)) {
                var hours = Number((runtime.match(/(\d+)H/i) || [0, 0])[1]);
                var minutes = Number((runtime.match(/(\d+)M/i) || [0, 0])[1]);
                runtime = String(hours * 60 + minutes) + ' دقیقه';
              }
              if (!runtime) {
                var runtimeNode = smallestField(/^(?:مدت|زمان|Runtime)\s*[:：]/i);
                var runtimeMatch = runtimeNode
                  ? clean(runtimeNode.textContent).match(
                      /([۰-۹0-9]{2,3})\s*(?:دقیقه|min)/i
                    )
                  : null;
                runtime = runtimeMatch ? runtimeMatch[1] + ' دقیقه' : '';
              }
              var genres = array(schema.genre).map(function(value) {
                return clean(typeof value === 'string' ? value : value.name);
              }).filter(Boolean);
              if (!genres.length) {
                var genreNode = smallestField(/^(?:ژانر|Genre)\s*[:：]/i);
                if (genreNode) {
                  genres = Array.from(genreNode.querySelectorAll('a')).map(function(link) {
                    return clean(link.textContent);
                  }).filter(Boolean);
                }
              }
              var country = clean(
                person(schema.countryOfOrigin || schema.contentLocation)
                  .map(function(value) { return value.name; }).join('، ')
              );
              if (!country) {
                country = fieldText(/^(?:کشور|محصول|Country)\s*[:：]/i, [
                  /^(?:کشور|محصول|Country)\s*[:：]\s*/i
                ]);
              }
              var language = clean(array(schema.inLanguage).map(function(value) {
                return typeof value === 'string' ? value : (value.name || '');
              }).join('، '));
              if (!language) {
                language = fieldText(/^(?:زبان|Language)\s*[:：]/i, [
                  /^(?:زبان|Language)\s*[:：]\s*/i
                ]);
              }
              var directors = person(schema.director);
              if (!directors.length) {
                directors = peopleField(/^(?:کارگردان|Director)\s*[:：]/i, [
                  /^(?:کارگردان|Director)\s*[:：]\s*/i
                ]);
              }
              var cast = person(schema.actor);
              if (!cast.length) {
                cast = peopleField(/^(?:بازیگران|ستارگان|Cast|Actors)\s*[:：]/i, [
                  /^(?:بازیگران|ستارگان|Cast|Actors)\s*[:：]\s*/i
                ]);
              }
              var audioText = localAudioText();
              AminSpotlight.result(JSON.stringify({
                summary: summary(schema),
                year: yearMatch ? yearMatch[0] : '',
                genres: genres.slice(0, 4),
                rating: rating.replace(/[^0-9۰-۹.٫]/g, '').slice(0, 5),
                runtime: runtime.slice(0, 24),
                country: country.slice(0, 60),
                language: language.slice(0, 60),
                hasPersianDub: /دوبله\s*(?:اختصاصی\s*)?فارسی/i.test(audioText),
                hasPersianSubtitle:
                  /زیرنویس\s*(?:چسبیده\s*)?فارسی|با\s*زیرنویس\s*فارسی/i.test(audioText),
                directors: directors.slice(0, 3),
                cast: cast.slice(0, 8)
              }));
            })();
        """.trimIndent()
    }
}
