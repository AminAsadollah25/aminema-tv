package com.amin.tvos.browser

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
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
import androidx.lifecycle.lifecycleScope
import com.amin.tvos.AminTvApp
import com.amin.tvos.data.model.CatalogItem
import com.amin.tvos.data.model.CatalogKind
import com.amin.tvos.data.model.CatalogSection
import com.amin.tvos.data.model.PlaybackSession
import com.amin.tvos.data.model.PersonRef
import com.amin.tvos.data.model.ResumeStrategy
import com.amin.tvos.data.model.StreamingService
import com.amin.tvos.data.model.UserAgentMode
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * Refreshes one provider rail at a time without opening a modal Activity.
 *
 * Tiny off-screen WebViews reuse the user's normal signed-in browser session. Cached cards stay
 * visible and Home remains fully interactive while a provider independently refreshes.
 */
class CatalogBackgroundSync(
    private val activity: ComponentActivity,
    private val app: AminTvApp
) {
    private data class Slot(
        val webView: WebView,
        val timeout: Runnable,
        val pageLimit: Int,
        val callbacks: MutableList<() -> Unit>
    )

    private data class PendingRefresh(
        var pageLimit: Int,
        val callbacks: MutableList<() -> Unit>
    )

    private val handler = Handler(Looper.getMainLooper())
    private val slots = mutableMapOf<String, Slot>()
    private val pendingRefreshes = mutableMapOf<String, PendingRefresh>()

    fun refresh(
        serviceId: String,
        pageLimit: Int = DEFAULT_PAGE_LIMIT,
        onFinished: (() -> Unit)? = null
    ) {
        if (serviceId !in SUPPORTED_IDS) {
            onFinished?.invoke()
            return
        }
        val rawRequestedLimit = pageLimit.coerceIn(DEFAULT_PAGE_LIMIT, MAX_PAGE_LIMIT)
        val cachedLimit = app.catalogRepository.section(serviceId)?.loadedPageLimit ?: 0
        // View All grows MyMoviz in small, reliable batches. Even if several end-of-list
        // signals arrive close together, never turn them into dozens of concurrent requests.
        val requestedLimit = if (serviceId == MYMOVIZ_ID && rawRequestedLimit > cachedLimit) {
            minOf(rawRequestedLimit, cachedLimit + MYMOVIZ_PAGE_BATCH)
        } else {
            rawRequestedLimit
        }
        val active = slots[serviceId]
        if (active != null) {
            if (requestedLimit <= active.pageLimit) {
                onFinished?.let(active.callbacks::add)
                Log.d(TAG, "join active service=$serviceId pages=${active.pageLimit}")
            } else {
                val pending = pendingRefreshes.getOrPut(serviceId) {
                    PendingRefresh(requestedLimit, mutableListOf())
                }
                pending.pageLimit = maxOf(pending.pageLimit, requestedLimit)
                onFinished?.let(pending.callbacks::add)
                Log.d(
                    TAG,
                    "queue service=$serviceId active=${active.pageLimit} next=${pending.pageLimit}"
                )
            }
            return
        }
        val service = app.servicesRepository.findById(serviceId)
        if (service == null) {
            activity.lifecycleScope.launch {
                app.catalogRepository.recordError(serviceId, "سرویس تنظیم نشده است")
                onFinished?.invoke()
            }
            return
        }

        app.catalogRepository.setRefreshing(serviceId, true)
        createWebView(
            service,
            requestedLimit,
            mutableListOf<() -> Unit>().apply { onFinished?.let(::add) }
        )
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(
        service: StreamingService,
        pageLimit: Int,
        callbacks: MutableList<() -> Unit>
    ) {
        val serviceId = service.id
        val serviceHost = Uri.parse(service.url).host.orEmpty()
        var scriptStarted = false
        val webView = WebView(activity).apply {
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
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }
        webView.addJavascriptInterface(CatalogBridge(serviceId, webView), "AminCatalog")
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                CookieManager.getInstance().flush()
                if (
                    scriptStarted ||
                    !Uri.parse(url).host.equals(serviceHost, ignoreCase = true)
                ) return
                scriptStarted = true
                val delay = when (serviceId) {
                    PARSI_ID -> 1_500L
                    MYMOVIZ_ID -> 700L
                    else -> 1_200L
                }
                view.postDelayed({
                    if (slots[serviceId]?.webView === view) {
                        val cachedLimit = app.catalogRepository.section(serviceId)
                            ?.loadedPageLimit
                            ?: 0
                        val pageStart = if (
                            serviceId == MYMOVIZ_ID && pageLimit > cachedLimit
                        ) {
                            cachedLimit + 1
                        } else {
                            1
                        }
                        view.evaluateJavascript(
                            scriptFor(serviceId, pageLimit, pageStart),
                            null
                        )
                    }
                }, delay)
            }
        }

        val timeout = Runnable { fail(serviceId, webView, "timeout") }
        slots[serviceId] = Slot(webView, timeout, pageLimit, callbacks)
        Log.d(TAG, "start service=$serviceId pages=$pageLimit")
        activity.addContentView(
            webView,
            FrameLayout.LayoutParams(2, 2, Gravity.BOTTOM or Gravity.END)
        )
        handler.postDelayed(timeout, TIMEOUT_MS)
        webView.loadUrl(
            if (serviceId == FILMROOZ_ID) {
                service.url.trimEnd('/') + "/user/panel/"
            } else {
                service.url
            }
        )
    }

    private inner class CatalogBridge(
        private val expectedServiceId: String,
        private val source: WebView
    ) {
        @JavascriptInterface
        fun section(serviceId: String?, payload: String?) {
            if (serviceId != expectedServiceId) return
            handler.post {
                if (slots[expectedServiceId]?.webView !== source) return@post
                val service = app.servicesRepository.findById(expectedServiceId)
                if (service == null) {
                    fail(expectedServiceId, source, "سرویس تنظیم نشده است")
                    return@post
                }
                val pageLimit = slots[expectedServiceId]?.pageLimit ?: DEFAULT_PAGE_LIMIT
                val section = parseSection(service, payload.orEmpty(), pageLimit)
                val accountSessions = parseAccountSessions(service, payload.orEmpty())
                Log.d(
                    TAG,
                    "result service=$expectedServiceId pages=$pageLimit " +
                        "movies=${section.movies.size} series=${section.series.size} " +
                        "moreMovies=${section.hasMoreMovies} moreSeries=${section.hasMoreSeries}"
                )
                activity.lifecycleScope.launch {
                    app.catalogRepository.save(section)
                    // `null` means the account row was unavailable, so stale Continue data
                    // is preserved. An explicit empty array is authoritative for this service.
                    accountSessions?.let {
                        app.libraryRepository.syncAccountSessions(service.id, it)
                    }
                    complete(expectedServiceId, source)
                }
            }
        }

        @JavascriptInterface
        fun failed(serviceId: String?, reason: String?) {
            if (serviceId != expectedServiceId) return
            handler.post {
                fail(expectedServiceId, source, reason.orEmpty().take(80))
            }
        }
    }

    private fun fail(serviceId: String, source: WebView, reason: String) {
        if (slots[serviceId]?.webView !== source) return
        activity.lifecycleScope.launch {
            app.catalogRepository.recordError(
                serviceId,
                reason.ifBlank { "بروزرسانی انجام نشد" }
            )
            complete(serviceId, source)
        }
    }

    private fun complete(serviceId: String, source: WebView) {
        val slot = slots[serviceId] ?: return
        if (slot.webView !== source) return
        slots.remove(serviceId)
        handler.removeCallbacks(slot.timeout)
        app.catalogRepository.setRefreshing(serviceId, false)
        slot.callbacks.forEach { callback -> runCatching(callback) }
        source.apply {
            stopLoading()
            removeJavascriptInterface("AminCatalog")
            (parent as? ViewGroup)?.removeView(this)
            destroy()
        }
        pendingRefreshes.remove(serviceId)?.let { pending ->
            val service = app.servicesRepository.findById(serviceId)
            if (service == null) {
                pending.callbacks.forEach { callback -> runCatching(callback) }
            } else {
                app.catalogRepository.setRefreshing(serviceId, true)
                createWebView(service, pending.pageLimit, pending.callbacks)
            }
        }
    }

    fun destroy() {
        slots.toMap().forEach { (serviceId, slot) ->
            complete(serviceId, slot.webView)
        }
        handler.removeCallbacksAndMessages(null)
        pendingRefreshes.values
            .flatMap(PendingRefresh::callbacks)
            .forEach { callback -> runCatching(callback) }
        pendingRefreshes.clear()
    }

    /**
     * Every bridge value is untrusted page output. Only normal same-host content pages survive.
     */
    private fun parseSection(
        service: StreamingService,
        payload: String,
        loadedPageLimit: Int
    ): CatalogSection {
        val adapter = ServiceAdapter(service)
        val serviceHost = Uri.parse(service.url).host
        val root = runCatching { JSONObject(payload) }.getOrNull()
            ?: return CatalogSection(serviceId = service.id, error = "پاسخ نامعتبر")

        fun list(key: String): List<CatalogItem> {
            val array = root.optJSONArray(key) ?: return emptyList()
            return buildList {
                for (index in 0 until minOf(array.length(), MAX_ITEMS)) {
                    val entry = array.optJSONObject(index) ?: continue
                    val contentUrl = entry.optString("contentUrl").take(2_000)
                    if (!adapter.isContentUrl(contentUrl)) continue
                    if (!Uri.parse(contentUrl).host.equals(serviceHost, true)) continue
                    val poster = entry.optString("posterUrl").take(2_000)
                        .takeIf { it.startsWith("http", ignoreCase = true) }
                        .orEmpty()
                    val backdrop = entry.optString("backdropUrl").take(2_000)
                        .takeIf { it.startsWith("http", ignoreCase = true) }
                        .orEmpty()
                    val kind = if (
                        entry.optString("kind").equals("SERIES", true)
                    ) CatalogKind.SERIES else CatalogKind.MOVIE
                    fun people(key: String): List<PersonRef> {
                        val values = entry.optJSONArray(key) ?: return emptyList()
                        return buildList {
                            for (personIndex in 0 until minOf(values.length(), 8)) {
                                val person = values.optJSONObject(personIndex)
                                val name = (
                                    person?.optString("name")
                                        ?: values.optString(personIndex)
                                    )
                                    .replace(Regex("""\s+"""), " ")
                                    .trim()
                                    .take(80)
                                if (name.isBlank()) continue
                                val rawProfile = person?.optString("profileUrl").orEmpty()
                                    .take(2_000)
                                val safeProfile = rawProfile.takeIf {
                                    it.startsWith("http", true) &&
                                        Uri.parse(it).host.equals(serviceHost, true)
                                }.orEmpty()
                                add(
                                    PersonRef(
                                        name = name,
                                        providerId = person?.optString("providerId")
                                            .orEmpty()
                                            .take(80),
                                        profileUrl = safeProfile
                                    )
                                )
                            }
                        }.distinctBy { it.name.lowercase() }
                    }
                    add(
                        CatalogItem(
                            title = entry.optString("title").trim().take(140)
                                .ifBlank { service.name },
                            kind = kind,
                            contentUrl = contentUrl,
                            posterUrl = poster,
                            backdropUrl = backdrop,
                            serviceId = service.id,
                            imdbId = entry.optString("imdbId").trim().take(20),
                            episodeLabel = entry.optString("episodeLabel")
                                .replace(Regex("""\s+"""), " ")
                                .trim()
                                .take(72),
                            summary = entry.optString("summary")
                                .replace(Regex("""\s+"""), " ")
                                .trim()
                                .take(420),
                            year = entry.optString("year")
                                .replace(Regex("""[^0-9۰-۹]"""), "")
                                .take(4),
                            genres = entry.optJSONArray("genres")
                                ?.let { genres ->
                                    buildList {
                                        for (genreIndex in 0 until minOf(genres.length(), 4)) {
                                            genres.optString(genreIndex)
                                                .replace(Regex("""\s+"""), " ")
                                                .trim()
                                                .replace(
                                                    Regex(
                                                        """^(?:ژانر|Genre)\s*:\s*""",
                                                        RegexOption.IGNORE_CASE
                                                    ),
                                                    ""
                                                )
                                                .take(28)
                                                .takeIf { it.isNotBlank() }
                                                ?.let(::add)
                                        }
                                    }
                                }
                                .orEmpty(),
                            rating = entry.optString("rating")
                                .replace(Regex("""[^0-9۰-۹.٫]"""), "")
                                .replace('٫', '.')
                                .take(5),
                            runtime = entry.optString("runtime")
                                .replace(Regex("""\s+"""), " ")
                                .trim()
                                .take(24),
                            country = entry.optString("country")
                                .replace(Regex("""\s+"""), " ")
                                .trim()
                                .take(60),
                            language = entry.optString("language")
                                .replace(Regex("""\s+"""), " ")
                                .trim()
                                .take(60),
                            hasPersianDub = entry.optBoolean("hasPersianDub"),
                            hasPersianSubtitle =
                                entry.optBoolean("hasPersianSubtitle"),
                            maxQualityHeight = entry.optInt("maxQualityHeight")
                                .coerceIn(0, 4_320),
                            qualityLabel = entry.optString("qualityLabel")
                                .replace(Regex("""\s+"""), " ")
                                .trim()
                                .take(32),
                            directors = people("directors"),
                            cast = people("cast")
                        )
                    )
                }
            }.distinctBy { it.contentUrl }
        }

        return CatalogSection(
            serviceId = service.id,
            all = list("all"),
            movies = list("movies"),
            series = list("series"),
            popularSeries = list("popularSeries"),
            featured = list("featured"),
            syncedAt = System.currentTimeMillis(),
            error = "",
            loadedPageLimit = loadedPageLimit,
            hasMoreAll = root.optBoolean("hasMoreAll"),
            hasMoreMovies = root.optBoolean("hasMoreMovies"),
            hasMoreSeries = root.optBoolean("hasMoreSeries"),
            hasMorePopularSeries = root.optBoolean("hasMorePopularSeries")
        )
    }

    /**
     * Parses only normal account Continue/Recent detail links bundled with a catalog refresh.
     * No playback URL, media URL, token or protected stream value crosses the JS bridge.
     */
    private fun parseAccountSessions(
        service: StreamingService,
        payload: String
    ): List<PlaybackSession>? {
        val root = runCatching { JSONObject(payload) }.getOrNull() ?: return null
        if (!root.has("accountItems")) return null
        val array: JSONArray = root.optJSONArray("accountItems") ?: return emptyList()
        val adapter = ServiceAdapter(service)
        val serviceHost = Uri.parse(service.url).host
        val now = System.currentTimeMillis()
        val actionPatterns = if (service.id == FILMROOZ_ID) {
            listOf("پخش آنلاین", "پخش انلاین", "تماشای آنلاین", "Watch Online")
        } else {
            emptyList()
        }
        return buildList {
            for (index in 0 until minOf(array.length(), MAX_ACCOUNT_ITEMS)) {
                val item = array.optJSONObject(index) ?: continue
                val contentUrl = item.optString("contentUrl").take(2_000)
                if (!adapter.isContentUrl(contentUrl)) continue
                if (!Uri.parse(contentUrl).host.equals(serviceHost, true)) continue
                add(
                    PlaybackSession(
                        id = "",
                        title = item.optString("title").trim().take(140)
                            .ifBlank { service.name },
                        subtitle = "Synced from account",
                        posterUrl = item.optString("posterUrl").take(2_000),
                        serviceId = service.id,
                        serviceName = service.name,
                        contentUrl = contentUrl,
                        playbackUrl = "",
                        lastPlayed = now - index,
                        resumePosition = item.optLong("resumePosition")
                            .coerceIn(0L, 86_400_000L),
                        duration = 0L,
                        resumeStrategy = ResumeStrategy.CLICK_SITE_CONTINUE,
                        actionButtonTextPatterns = actionPatterns,
                        syncedFromAccount = true
                    )
                )
            }
        }
    }

    private companion object {
        const val TAG = "AminCatalogSync"
        const val PARSI_ID = "parsiflix"
        const val FILMROOZ_ID = "filmrooz"
        const val MYMOVIZ_ID = "mymoviz"
        // Home renders only the first few cards, while View All can use the larger cached set.
        // Keep the cached window larger than the Home rails. The library still renders cards
        // in small pages, while provider data can grow as the user keeps scrolling.
        const val MAX_ITEMS = 2048
        const val MAX_ACCOUNT_ITEMS = 20
        const val TIMEOUT_MS = 35_000L
        const val DEFAULT_PAGE_LIMIT = 4
        const val MYMOVIZ_PAGE_BATCH = 4
        // Keep a generous safety ceiling; the library requests the next window on demand.
        const val MAX_PAGE_LIMIT = 200
        val SUPPORTED_IDS = setOf(PARSI_ID, FILMROOZ_ID, MYMOVIZ_ID)

        fun scriptFor(
            serviceId: String,
            pageLimit: Int,
            pageStart: Int = 1
        ): String =
            (when (serviceId) {
                PARSI_ID -> PARSI_SCRIPT
                MYMOVIZ_ID -> MYMOVIZ_SCRIPT
                else -> FILMROOZ_SCRIPT
            }).replace("__AMIN_PAGE_LIMIT__", pageLimit.toString())
                .replace("__AMIN_PAGE_START__", pageStart.toString())
                .replace("__AMIN_MAX_ITEMS__", (pageLimit * 24).toString())

        val PARSI_SCRIPT = """
            (async function() {
              function map(item) {
                var kind = String(item.type || '').toUpperCase() === 'SERIES'
                  ? 'SERIES' : 'MOVIE';
                var path = kind === 'SERIES' ? 'series' : 'movies';
                function text(value) {
                  if (value == null) return '';
                  if (typeof value === 'string' || typeof value === 'number') {
                    return String(value);
                  }
                  return String(value.name || value.title || value.label || '');
                }
                function people(value) {
                  if (!value) return [];
                  if (!Array.isArray(value)) value = [value];
                  return value.map(function(person) {
                    if (person == null) return null;
                    if (typeof person === 'string') {
                      return {name: person.slice(0, 80)};
                    }
                    var name = text(person).replace(/\s+/g, ' ').trim();
                    if (!name) return null;
                    return {
                      name: name.slice(0, 80),
                      providerId: String(person.id || person.personId || '').slice(0, 80),
                      profileUrl: ''
                    };
                  }).filter(Boolean).slice(0, 8);
                }
                function joined(value) {
                  if (!value) return '';
                  if (!Array.isArray(value)) value = [value];
                  return value.map(text).filter(Boolean).join('، ');
                }
                var rawGenres = item.genres || item.genre || item.categories || [];
                if (!Array.isArray(rawGenres)) rawGenres = [rawGenres];
                var published = text(
                  item.releaseYear || item.year || item.publishedAt || item.published
                );
                var yearMatch = published.match(/(?:19|20)\d{2}/);
                var availabilityText = joined(
                  item.audioTypes || item.audioType || item.languages ||
                  item.language || item.tags || item.badges
                );
                return {
                  title: String(item.title || '').slice(0, 140),
                  kind: kind,
                  contentUrl: location.origin + '/medias/' + path + '/' + item.id,
                  // The provider's field names are the opposite of what they sound like:
                  // "thumbnail" is the PORTRAIT artwork (~0.8:1) and "cover" is the WIDE one
                  // (~1.8:1), both measured on live data.
                  //
                  // Deliberately no fallback to the wide image here. A poster frame is 2:3,
                  // so a landscape image dropped into it gets cropped to a magnified strip —
                  // which looks broken rather than merely imperfect. A title with no portrait
                  // artwork leaves this empty and the card falls back to its clean placeholder.
                  posterUrl: item.thumbnailLink || '',
                  backdropUrl: item.coverLink || '',
                  episodeLabel: '',
                  summary: text(
                    item.description || item.summary || item.overview || item.plot
                  ).replace(/\s+/g, ' ').slice(0, 420),
                  year: yearMatch ? yearMatch[0] : '',
                  genres: rawGenres.map(text).filter(Boolean).slice(0, 4),
                  rating: text(
                    item.imdbRating || item.rating || item.rate
                  ).replace(/[^0-9.]/g, '').slice(0, 4),
                  runtime: text(item.runtime || item.duration || ''),
                  country: joined(
                    item.countries || item.country || item.countryOfOrigin ||
                    item.productionCountries
                  ).slice(0, 60),
                  language: joined(
                    item.languages || item.language || item.originalLanguage
                  ).slice(0, 60),
                  hasPersianDub: Boolean(
                    item.isDubbed || item.dubbed || item.hasPersianDub
                  ) || /دوبله\s*(?:اختصاصی\s*)?فارسی/i.test(availabilityText),
                  hasPersianSubtitle: Boolean(
                    item.hasPersianSubtitle || item.persianSubtitle
                  ) || /زیرنویس\s*(?:چسبیده\s*)?فارسی/i.test(availabilityText),
                  directors: people(
                    item.directors || item.director || item.directorList || item.creators
                  ),
                  cast: people(
                    item.actors || item.cast || item.casts || item.stars || item.performers
                  )
                };
              }
              try {
                var token = localStorage.getItem('accessToken');
                if (!token) { AminCatalog.failed('parsiflix', 'Login required'); return; }
                try { token = JSON.parse(token); } catch (_) {}
                var headers = {
                  Authorization: 'Bearer ' + token,
                  appVersion: '1.0.0',
                  Accept: 'application/json, text/plain, */*'
                };
                var homeResponse = await fetch('https://api.parsiflix.com/app/home', {
                  headers: headers
                });
                if (!homeResponse.ok) {
                  AminCatalog.failed('parsiflix', 'Login expired');
                  return;
                }
                var home = await homeResponse.json();
                var latest = (home.sections || []).find(function(section) {
                  return /جدیدترین/.test(section.title || '');
                });
                var all = ((latest && latest.items) || []).slice(0, 24).map(map);
                var accountSection = (home.sections || []).find(function(section) {
                  return /ادامه\s*تماشا|continue\s*watching/i.test(section.title || '');
                });
                var accountItems = ((accountSection && accountSection.items) || [])
                  .slice(0, 20).map(function(item) {
                    var kind = String(item.type || '').toUpperCase() === 'MOVIE'
                      ? 'movies' : 'series';
                    return {
                      title: String(item.title || '').slice(0, 140),
                      contentUrl: location.origin + '/medias/' + kind + '/' + item.id,
                      // Portrait only: see the note on the catalog mapper above.
                      posterUrl: item.thumbnailLink || '',
                      resumePosition: 0
                    };
                  });

                async function typed(type, pageNumber) {
                  var response = await fetch(
                    'https://api.parsiflix.com/medias?type=' + type +
                      '&page=' + pageNumber + '&size=96',
                    {headers: headers}
                  );
                  if (!response.ok) return [];
                  var data = await response.json();
                  return (data.elements || []).slice(0, 96).map(map);
                }
                async function pagedTyped(type) {
                  var pages = [];
                  var pageCount = Math.ceil(__AMIN_MAX_ITEMS__ / 96) + 2;
                  for (var pageNumber = 1; pageNumber <= pageCount; pageNumber++) {
                    var result = await typed(type, pageNumber);
                    if (!result.length) break;
                    pages = pages.concat(result);
                    if (result.length < 96) break;
                  }
                  return {
                    items: pages.slice(0, __AMIN_MAX_ITEMS__),
                    hasMore: pages.length > __AMIN_MAX_ITEMS__
                  };
                }
                var movieResult = await pagedTyped('MOVIE');
                var seriesResult = await pagedTyped('SERIES');
                var movies = movieResult.items;
                var series = seriesResult.items;
                if (!all.length) {
                  all = movies.slice(0, 12).concat(series.slice(0, 12));
                }

                // The provider's own banner carousel. It is the one CUSTOM section, its items
                // are marked SLIDER, and — unlike every other row — they carry only the wide
                // artwork (their thumbnail field is empty) plus a ready-made link.
                var slider = (home.sections || []).find(function(section) {
                  return String(section.type || '').toUpperCase() === 'CUSTOM' &&
                    (section.items || []).some(function(entry) {
                      return String(entry.itemType || '').toUpperCase() === 'SLIDER';
                    });
                });
                var featured = ((slider && slider.items) || [])
                  .filter(function(entry) { return entry && entry.link && entry.cover; })
                  .slice(0, 14)
                  .map(function(entry) {
                    var link = String(entry.link || '');
                    var isSeries = /\/medias\/series\//.test(link);
                    return {
                      title: String(entry.title || '').slice(0, 140),
                      kind: isSeries ? 'SERIES' : 'MOVIE',
                      contentUrl: link,
                      // A SLIDER item ships only wide artwork, so it becomes the backdrop and
                      // the poster stays empty. The rejoin below fills the real portrait from
                      // the typed catalog; when even that has none, an empty poster is still
                      // better than a wide image cropped into a 2:3 frame.
                      posterUrl: '',
                      backdropUrl: entry.cover || '',
                      // Not the shared text() helper: that one is scoped inside map().
                      summary: String(entry.description || '')
                        .replace(/\s+/g, ' ').slice(0, 420)
                    };
                  });

                // Slider JSON intentionally contains only a wide cover. Rejoin it with the
                // typed catalog response by the same normal content URL so Spotlight/Hero
                // always receive the title's real portrait poster and full synopsis.
                var portraitByUrl = {};
                all.concat(movies).concat(series).forEach(function(item) {
                  portraitByUrl[item.contentUrl] = item;
                });
                featured = featured.map(function(slide) {
                  var titleItem = portraitByUrl[slide.contentUrl];
                  if (!titleItem || !titleItem.posterUrl) return slide;
                  return Object.assign({}, titleItem, {
                    title: slide.title || titleItem.title,
                    backdropUrl: slide.backdropUrl,
                    summary: titleItem.summary || slide.summary
                  });
                });

                AminCatalog.section('parsiflix', JSON.stringify({
                  all: all,
                  movies: movies,
                  series: series,
                  popularSeries: [],
                  featured: featured,
                  accountItems: accountItems,
                  hasMoreMovies: movieResult.hasMore,
                  hasMoreSeries: seriesResult.hasMore
                }));
              } catch (error) {
                AminCatalog.failed('parsiflix', 'Service unavailable');
              }
            })();
        """.trimIndent()

        val FILMROOZ_SCRIPT = """
            (async function() {
              function parse(html, expectedKind) {
                var doc = new DOMParser().parseFromString(html, 'text/html');
                var seen = {};
                var items = [];
                Array.from(doc.querySelectorAll('a[href*="/post/"]')).forEach(
                  function(anchor) {
                    var href = anchor.getAttribute('href') || '';
                    if (!href) return;
                    var url = new URL(href, location.origin);
                    if (url.origin !== location.origin) return;
                    var match = url.pathname.match(/^\/post\/(film|series)\/(\d+)\//);
                    if (!match || seen[url.pathname]) return;
                    var kind = match[1] === 'series' ? 'SERIES' : 'MOVIE';
                    if (expectedKind && kind !== expectedKind) return;
                    seen[url.pathname] = true;
                    var card = anchor.closest('.postMeta') || anchor.parentElement;
                    var links = card
                      ? Array.from(card.querySelectorAll('a[href*="/post/"]')) : [];
                    var titleLink = links.find(function(a) {
                      var text = (a.textContent || '').replace(/\s+/g, ' ').trim();
                      if (text.length < 2 || /^قسمت/.test(text)) return false;
                      var target = new URL(a.getAttribute('href') || '', location.origin);
                      return target.pathname === url.pathname;
                    });
                    var title = titleLink ? titleLink.textContent.trim() : '';
                    if (!title) {
                      var slug = url.pathname.split('/').filter(Boolean)[3] || '';
                      title = decodeURIComponent(slug)
                        .replace(/[-_]+/g, ' ')
                        .replace(/\b[a-z]/g, function(c) { return c.toUpperCase(); })
                        .trim();
                    }
                    // FilmRooz renders the publication status as plain text beside the
                    // title, not as a link. Read only this card — never a parent rail —
                    // so one show's episode cannot leak onto another show's card.
                    var status = card
                      ? (card.textContent || '').replace(/\s+/g, ' ').trim() : '';
                    var episodeMatch = status.match(
                      /قسمت\s*[۰-۹0-9]+\s*(?:فصل\s+[^\s،؛\-]+)?/
                    );
                    var image = anchor.querySelector('img');
                    var candidate = image
                      ? (image.getAttribute('data-src') || image.getAttribute('src') || '')
                      : '';
                    var poster = candidate && !/^data:/i.test(candidate)
                      ? new URL(candidate, location.origin).href : '';
                    var yearMatch = title.match(/(?:19|20)\d{2}/) ||
                      status.match(/(?:19|20)\d{2}/);
                    function field(pattern) {
                      return card ? Array.from(card.querySelectorAll('.col-12')).find(
                        function(node) {
                          return pattern.test((node.textContent || '').trim());
                        }
                      ) : null;
                    }
                    function peopleField(pattern) {
                      var node = field(pattern);
                      if (!node) return [];
                      var linked = Array.from(node.querySelectorAll('a')).map(
                        function(personLink) {
                          var name = (personLink.textContent || '')
                            .replace(/\s+/g, ' ').trim();
                          if (!name) return null;
                          var profile = new URL(
                            personLink.getAttribute('href') || '', location.origin
                          );
                          return {
                            name: name.slice(0, 80),
                            profileUrl: profile.origin === location.origin
                              ? profile.href : ''
                          };
                        }
                      ).filter(Boolean);
                      if (linked.length) return linked.slice(0, 8);
                      return (node.textContent || '')
                        .replace(/^(?:کارگردان|Director|بازیگران|ستارگان|Cast)\s*:\s*/i, '')
                        .split(/[،,|]/)
                        .map(function(name) {
                          name = name.replace(/\s+/g, ' ').trim();
                          return name ? {name: name.slice(0, 80)} : null;
                        }).filter(Boolean).slice(0, 8);
                    }
                    var ratingNode = field(/از\s*۱۰|IMDb/i);
                    var ratingMatch = ratingNode
                      ? (ratingNode.textContent || '').match(
                          /([۰-۹0-9]+(?:[.٫][۰-۹0-9]+)?)\s*از\s*۱۰/
                        )
                      : null;
                    var runtimeNode = field(/دقیقه|\bmin\b/i);
                    var runtimeMatch = runtimeNode
                      ? (runtimeNode.textContent || '').match(
                          /([۰-۹0-9]{2,3}\s*(?:دقیقه|min))/i
                        )
                      : null;
                    var genreNode = field(/^(?:ژانر|Genre)\s*:/i);
                    var genres = [];
                    if (genreNode) {
                      var genreHtml = genreNode.innerHTML.replace(
                        /<spl[^>]*><\/spl>/gi, '|'
                      );
                      var holder = document.createElement('div');
                      holder.innerHTML = genreHtml;
                      genres = (holder.textContent || '')
                        .replace(/\s+/g, ' ').trim()
                        .replace(/^(?:ژانر|Genre)\s*:\s*/i, '')
                        .split('|')
                        .map(function(value) {
                          return value.replace(/\s+/g, ' ').trim();
                        }).filter(Boolean).slice(0, 4);
                    }
                    var summary = '';
                    if (card) {
                      var summaryNode = card.querySelector(
                        '.text-justify.mt-2.p-2,.postExcerpt,.excerpt,.summary,' +
                        '[class*="excerpt" i],[class*="summary" i]'
                      );
                      var candidateSummary = summaryNode
                        ? (summaryNode.textContent || '').replace(/\s+/g, ' ').trim()
                        : '';
                      if (candidateSummary.length >= 35) {
                        summary = candidateSummary.slice(0, 420);
                      }
                    }
                    function plainField(pattern, labelPattern) {
                      var node = field(pattern);
                      return node
                        ? (node.textContent || '')
                            .replace(/\s+/g, ' ').trim()
                            .replace(labelPattern, '').trim().slice(0, 60)
                        : '';
                    }
                    var localCard = card ? card.cloneNode(true) : null;
                    if (localCard) {
                      localCard.querySelectorAll(
                        'header,nav,footer,a[href*="/archive/category/"]'
                      ).forEach(function(node) { node.remove(); });
                    }
                    var localText = localCard
                      ? (localCard.textContent || '').replace(/\s+/g, ' ').trim() : '';
                    var imdbAnchor = card ? card.querySelector(
                      'a[href*="imdb.com/title/tt"]'
                    ) : null;
                    var imdbMatch = imdbAnchor
                      ? String(imdbAnchor.getAttribute('href') || '').match(/tt\d{5,12}/i)
                      : null;
                    var qualityMatch = localText.match(/\b(2160|1080|720|480)p\b/i);
                    items.push({
                      title: title.slice(0, 140),
                      kind: kind,
                      contentUrl: url.origin + url.pathname,
                      imdbId: imdbMatch ? imdbMatch[0].toLowerCase() : '',
                      posterUrl: poster,
                      episodeLabel: episodeMatch ? episodeMatch[0].trim() : '',
                      summary: summary,
                      year: yearMatch ? yearMatch[0] : '',
                      genres: genres,
                      rating: ratingMatch ? ratingMatch[1] : '',
                      runtime: runtimeMatch ? runtimeMatch[1] : '',
                      country: plainField(
                        /^(?:کشور|محصول|Country)\s*:/i,
                        /^(?:کشور|محصول|Country)\s*:\s*/i
                      ),
                      language: plainField(
                        /^(?:زبان|Language)\s*:/i,
                        /^(?:زبان|Language)\s*:\s*/i
                      ),
                      hasPersianDub:
                        /دوبله(?:\s*(?:اختصاصی\s*)?فارسی)?|دو\s*زبانه|دوزبانه|صوت\s*فارسی/i
                          .test(localText.replace(
                            /بدون\s*(?:دوبله|صوت\s*فارسی)/gi, ''
                          )),
                      hasPersianSubtitle:
                        /زیرنویس\s*(?:چسبیده\s*)?فارسی|با\s*زیرنویس\s*فارسی/i
                          .test(localText),
                      maxQualityHeight: qualityMatch ? Number(qualityMatch[1]) : 0,
                      qualityLabel: qualityMatch ? qualityMatch[0] : '',
                      directors: peopleField(/^(?:کارگردان|Director)\s*:/i),
                      cast: peopleField(/^(?:بازیگران|ستارگان|Cast)\s*:/i)
                    });
                  }
                );
                return items.slice(0, 24);
              }
              async function page(path, kind) {
                try {
                  var response = await fetch(location.origin + path, {
                    credentials: 'include'
                  });
                  if (!response.ok) return [];
                  return parse(await response.text(), kind);
                } catch (_) { return []; }
              }
              function unique(items) {
                var seen = {};
                return items.filter(function(item) {
                  if (seen[item.contentUrl]) return false;
                  seen[item.contentUrl] = true;
                  return true;
                });
              }
              // The provider's own banner carousel, from its home page: a Bootstrap carousel
              // whose slides are plain links wrapping one wide image each. Measured on live
              // data at 1504x846, exactly 16:9. One extra page fetch, not one per title.
              async function featuredBanners() {
                try {
                  var response = await fetch(location.origin + '/', {
                    credentials: 'include'
                  });
                  if (!response.ok) return [];
                  var doc = new DOMParser().parseFromString(
                    await response.text(), 'text/html'
                  );
                  var box = doc.querySelector('.carousel-inner') ||
                    doc.querySelector('.carousel');
                  if (!box) return [];
                  var seen = {};
                  var items = [];
                  Array.from(box.querySelectorAll('a[href*="/post/"]')).forEach(
                    function(anchor) {
                      var href = anchor.getAttribute('href') || '';
                      if (!href) return;
                      var url = new URL(href, location.origin);
                      if (url.origin !== location.origin) return;
                      var match = url.pathname.match(/^\/post\/(film|series)\/(\d+)\//);
                      if (!match || seen[url.pathname]) return;
                      var img = anchor.querySelector('img');
                      var raw = img
                        ? (img.getAttribute('data-src') || img.getAttribute('src') || '')
                        : '';
                      if (!raw || /^data:/i.test(raw)) return;
                      var art = new URL(raw, location.origin);
                      if (art.origin !== location.origin) return;
                      seen[url.pathname] = true;
                      // The slides carry no alt text, so the title comes from the slug the
                      // provider already puts in its own content URL.
                      var slug = decodeURIComponent(url.pathname.split('/')[4] || '')
                        .replace(/[-_]+/g, ' ')
                        .replace(/\b[a-z]/g, function(c) { return c.toUpperCase(); })
                        .trim();
                      var alt = img ? (img.getAttribute('alt') || '').trim() : '';
                      items.push({
                        title: (alt || slug).slice(0, 140),
                        kind: match[1] === 'series' ? 'SERIES' : 'MOVIE',
                        contentUrl: url.origin + url.pathname,
                        // A slide publishes only wide artwork, so it serves as both.
                        posterUrl: art.href,
                        backdropUrl: art.href,
                        summary: ''
                      });
                    }
                  );
                  var selected = items.slice(0, 14);
                  // Carousel slides expose only 16:9 art. Resolve each selected title's
                  // ordinary detail page once during background sync and keep the 2:3 image
                  // separate; the banner remains backdrop-only.
                  return await Promise.all(selected.map(async function(item) {
                    try {
                      var detailResponse = await fetch(item.contentUrl, {
                        credentials: 'include'
                      });
                      if (!detailResponse.ok) return item;
                      var detailHtml = await detailResponse.text();
                      var detail = new DOMParser().parseFromString(
                        detailHtml, 'text/html'
                      );

                      // Reuse the same proven detail parser used by the normal catalog.
                      // Featured slides used to keep only poster+summary, which is why a
                      // title such as The Sheep Detectives lost its dub/year/credits chips.
                      var parsedDetail = parse(detailHtml, item.kind).find(function(value) {
                        return value.contentUrl.replace(/\/$/, '') ===
                          item.contentUrl.replace(/\/$/, '');
                      });
                      if (parsedDetail) {
                        item = Object.assign({}, parsedDetail, {
                          title: item.title || parsedDetail.title,
                          posterUrl: parsedDetail.posterUrl || item.posterUrl,
                          backdropUrl: item.backdropUrl,
                          summary: parsedDetail.summary || item.summary
                        });
                      }

                      var portrait = detail.querySelector(
                        'img[src*="/img/170-256/"],img[data-src*="/img/170-256/"],' +
                        '.postMeta img,.single-post img[class*="poster" i]'
                      );
                      var rawPoster = portrait ? (
                        portrait.getAttribute('data-src') ||
                        portrait.getAttribute('src') || ''
                      ) : '';
                      var poster = rawPoster ? new URL(rawPoster, location.origin) : null;
                      if (poster && poster.origin === location.origin) {
                        item.posterUrl = poster.href;
                      }
                      var summaryNode = detail.querySelector(
                        '.text-justify.mt-2.p-2,.postExcerpt,.excerpt,.summary,' +
                        '[class*="synopsis" i],[class*="plot" i]'
                      );
                      var summary = summaryNode
                        ? (summaryNode.textContent || '').replace(/\s+/g, ' ').trim()
                        : '';
                      if (summary.length >= 10) item.summary = summary.slice(0, 420);

                      // JSON-LD is the least brittle source when the provider publishes it.
                      // Only ordinary public title metadata is read — never player/download
                      // links. DOM-labelled fields below remain the fallback.
                      var schemas = [];
                      Array.from(detail.querySelectorAll('script[type="application/ld+json"]'))
                        .forEach(function(node) {
                          try {
                            var value = JSON.parse(node.textContent || '{}');
                            if (Array.isArray(value)) schemas = schemas.concat(value);
                            else if (value && Array.isArray(value['@graph'])) {
                              schemas = schemas.concat(value['@graph']);
                            } else schemas.push(value);
                          } catch (_) {}
                        });
                      var schema = schemas.find(function(value) {
                        var type = String(value && value['@type'] || '');
                        return /Movie|TVSeries|TVShow|CreativeWork/i.test(type);
                      });
                      function schemaPeople(value) {
                        if (!value) return [];
                        if (!Array.isArray(value)) value = [value];
                        return value.map(function(person) {
                          var name = typeof person === 'string'
                            ? person : String(person.name || '');
                          name = name.replace(/\s+/g, ' ').trim();
                          return name ? {name: name.slice(0, 80)} : null;
                        }).filter(Boolean).slice(0, 8);
                      }
                      if (schema) {
                        var schemaYear = String(
                          schema.datePublished || schema.dateCreated || ''
                        ).match(/(?:19|20)\d{2}/);
                        if (!item.year && schemaYear) item.year = schemaYear[0];
                        if (!item.directors || !item.directors.length) {
                          item.directors = schemaPeople(schema.director || schema.creator);
                        }
                        if (!item.cast || !item.cast.length) {
                          item.cast = schemaPeople(schema.actor || schema.actors);
                        }
                        if (!item.genres || !item.genres.length) {
                          var rawGenre = schema.genre || [];
                          if (!Array.isArray(rawGenre)) rawGenre = [rawGenre];
                          item.genres = rawGenre.map(String).filter(Boolean).slice(0, 4);
                        }
                        if (!item.runtime && schema.duration) {
                          var duration = String(schema.duration).match(/PT(?:(\d+)H)?(?:(\d+)M)?/i);
                          if (duration) {
                            item.runtime = String(
                              Number(duration[1] || 0) * 60 + Number(duration[2] || 0)
                            ) + ' دقیقه';
                          }
                        }
                      }

                      // Audio badges can live in the download/quality accordion outside
                      // `.postMeta`; use the whole ordinary detail document after removing
                      // navigation and recommendation rails. This is the real Sheep
                      // Detectives case where poster/summary were inside postMeta but the
                      // «دوبله فارسی» label was lower in the page.
                      var detailRoot = detail.body;
                      var detailClone = detailRoot ? detailRoot.cloneNode(true) : null;
                      if (detailClone) {
                        detailClone.querySelectorAll(
                          'header,nav,footer,[class*="related" i],[class*="similar" i],' +
                          '[class*="recommend" i],[class*="carousel" i]'
                        ).forEach(function(node) { node.remove(); });
                      }
                      var detailText = detailClone
                        ? (detailClone.textContent || '').replace(/\s+/g, ' ').trim() : '';
                      item.hasPersianDub = Boolean(item.hasPersianDub) ||
                        /دوبله(?:\s*(?:اختصاصی\s*)?فارسی)?|دو\s*زبانه|دوزبانه|صوت\s*فارسی/i
                          .test(detailText.replace(
                            /بدون\s*(?:دوبله|صوت\s*فارسی)/gi, ''
                          ));
                      item.hasPersianSubtitle = Boolean(item.hasPersianSubtitle) ||
                        /زیرنویس\s*(?:چسبیده\s*)?فارسی|با\s*زیرنویس\s*فارسی/i
                          .test(detailText);
                    } catch (_) {}
                    return item;
                  }));
                } catch (_) { return []; }
              }
              async function recentAccountItems() {
                function findBox() {
                  var heading = Array.from(
                    document.querySelectorAll('h1,h2,h3,h4,h5')
                  ).find(function(el) {
                    return /مشاهدات\s*اخیر|recent/i.test(el.innerText || '');
                  });
                  return heading && heading.parentElement
                    ? heading.parentElement.nextElementSibling : null;
                }
                var box = findBox();
                for (var attempt = 0; !box && attempt < 4; attempt++) {
                  await new Promise(function(resolve) { setTimeout(resolve, 900); });
                  box = findBox();
                }
                if (!box) return null;
                var seen = {};
                return Array.from(box.querySelectorAll('a[href*="/post/"]'))
                  .map(function(anchor) {
                    var url = new URL(anchor.href, location.href);
                    if (seen[url.pathname]) return null;
                    seen[url.pathname] = true;
                    var parts = url.pathname.split('/').filter(Boolean);
                    var contentId = parts.length > 2 ? parts[2] : '';
                    var slug = parts.length > 3 ? decodeURIComponent(parts[3]) : '';
                    var title = slug.replace(/[-_]+/g, ' ').replace(
                      /\b[a-z]/g, function(letter) {
                        return letter.toUpperCase();
                      }
                    ).trim();
                    var image = anchor.querySelector('img');
                    var candidate = image ? (
                      image.dataset.src ||
                      image.dataset.lazySrc ||
                      image.dataset.original ||
                      image.currentSrc ||
                      image.src ||
                      ''
                    ) : '';
                    var poster = candidate && !/^data:/i.test(candidate)
                      ? new URL(candidate, location.href).href : '';
                    var resumeKey = Object.keys(localStorage).find(function(key) {
                      return key.indexOf('rvd_') === 0 &&
                        contentId && key.endsWith('_' + contentId);
                    });
                    var seconds = resumeKey
                      ? Number(localStorage.getItem(resumeKey)) : 0;
                    return {
                      title: title || 'فیلم خارجی',
                      contentUrl: url.origin + url.pathname,
                      posterUrl: poster,
                      resumePosition: isFinite(seconds)
                        ? Math.max(0, Math.round(seconds * 1000)) : 0
                    };
                  }).filter(Boolean).slice(0, 20);
              }
              try {
                if (!document.querySelector('a[href*="/user/panel"]')) {
                  AminCatalog.failed('filmrooz', 'Login required');
                  return;
                }
                // Reconcile account history and public catalog metadata in the same
                // invisible provider pass. Home never gets replaced by a sync screen.
                var accountItems = await recentAccountItems();
                async function pagedArchive(basePath, kind) {
                  var pages = [];
                  var probeHasItems = false;
                  for (var pageNumber = 1; pageNumber <= __AMIN_PAGE_LIMIT__ + 1; pageNumber++) {
                    var path = pageNumber === 1
                      ? basePath
                      : basePath.replace(/\/$/, '') + '/page/' + pageNumber + '/';
                    var result = await page(path, kind);
                    if (!result.length) break;
                    if (pageNumber <= __AMIN_PAGE_LIMIT__) {
                      pages = pages.concat(result);
                    } else {
                      probeHasItems = true;
                    }
                  }
                  return {
                    items: unique(pages).slice(0, __AMIN_MAX_ITEMS__),
                    hasMore: probeHasItems
                  };
                }
                var movieResult = await pagedArchive('/archive/category/new-films/', 'MOVIE');
                var movies = movieResult.items;
                // Release-ordered: old series return to the top whenever a new episode lands.
                var seriesResult = await pagedArchive('/archive/series/', 'SERIES');
                var series = seriesResult.items;
                // Curated/trending, intentionally separate from new episode releases.
                var popularSeries = await page(
                  '/archive/playlist/show/most-popular-tv-shows/', 'SERIES'
                );
                if (!movies.length && !series.length && !popularSeries.length) {
                  AminCatalog.failed('filmrooz', 'Empty catalog');
                  return;
                }
                var all = [];
                for (var i = 0; i < Math.max(movies.length, series.length); i++) {
                  if (movies[i]) all.push(movies[i]);
                  if (series[i]) all.push(series[i]);
                }
                var payload = {
                  all: unique(all).slice(0, __AMIN_MAX_ITEMS__),
                  movies: unique(movies),
                  series: unique(series),
                  popularSeries: unique(popularSeries),
                  featured: await featuredBanners(),
                  hasMoreMovies: movieResult.hasMore,
                  hasMoreSeries: seriesResult.hasMore
                };
                if (accountItems !== null) payload.accountItems = accountItems;
                AminCatalog.section('filmrooz', JSON.stringify(payload));
              } catch (error) {
                AminCatalog.failed('filmrooz', 'Service unavailable');
              }
            })();
        """.trimIndent()

        /** Public MyMoviz catalogue only; no login or watch page is touched. */
        val MYMOVIZ_SCRIPT = """
            (async function() {
              function clean(value) {
                return String(value || '').replace(/\s+/g, ' ').trim();
              }
              function nodeText(root, selector) {
                var node = root ? root.querySelector(selector) : null;
                return node ? node.textContent : '';
              }
              function people(row, label) {
                var fact = Array.from(row.querySelectorAll('.mv-ritem__fact')).find(
                  function(node) {
                    return clean(node.querySelector('.mv-ritem__k')).indexOf(label) === 0;
                  }
                );
                if (!fact) return [];
                return Array.from(fact.querySelectorAll('a[href*="/_modern/person/"]'))
                  .map(function(anchor) {
                    var name = clean(anchor.textContent);
                    return name ? {
                      name: name.slice(0, 80),
                      providerId: (anchor.getAttribute('href') || '')
                        .split('/').filter(Boolean)[2] || '',
                      profileUrl: new URL(anchor.getAttribute('href'), location.origin).href
                    } : null;
                  }).filter(Boolean).slice(0, 8);
              }
              function parse(html, kind) {
                var doc = new DOMParser().parseFromString(html, 'text/html');
                var seen = {};
                return Array.from(
                  doc.querySelectorAll('.mv-results-grid .mv-ritem')
                ).map(function(row) {
                  var card = row.querySelector('a.mv-card[href*="/_modern/title/"]');
                  if (!card) return null;
                  var url = new URL(card.getAttribute('href') || '', location.origin);
                  var match = url.pathname.match(/^\/_modern\/title\/(\d+)\//);
                  if (!match || url.origin !== location.origin || seen[url.pathname]) {
                    return null;
                  }
                  seen[url.pathname] = true;
                  var title = clean(
                    nodeText(row, '.mv-ritem__t-en') ||
                    nodeText(card, '.mv-card__title') ||
                    card.getAttribute('title')
                  ).replace(/\s*[\[(（](?:19|20)\d{2}[\])）]\s*$/, '');
                  var cardSub = card.querySelector('.mv-card__sub');
                  var yearText = clean(
                    nodeText(row, '.mv-ritem__t-yr') ||
                    (cardSub && cardSub.childNodes[0]
                      ? cardSub.childNodes[0].textContent : '')
                  );
                  var yearMatch = yearText.match(/(?:19|20)\d{2}/);
                  var image = card.querySelector('.mv-card__img');
                  var rawPoster = image ? (
                    image.getAttribute('data-src') || image.getAttribute('src') || ''
                  ) : '';
                  var imdbAnchor = row.querySelector('a[href*="imdb.com/title/tt"]');
                  var imdbMatch = imdbAnchor
                    ? String(imdbAnchor.getAttribute('href') || '').match(/tt\d{5,12}/i)
                    : null;
                  var quality = clean(
                    nodeText(row, '.mv-ritem__q') ||
                    nodeText(card, '.mv-card__q')
                  );
                  var qualityMatch = quality.match(/(2160|1080|720|480)p/i);
                  var genres = Array.from(
                    row.querySelectorAll('.mv-ritem__genres a,.mv-card__genre')
                  ).map(function(node) { return clean(node.textContent); })
                    .filter(Boolean).slice(0, 4);
                  var rating = clean(
                    nodeText(row, '.mv-card__rate--imdb')
                  ).replace(/[^0-9.]/g, '').slice(0, 5);
                  var tags = clean(nodeText(row, '.mv-ritem__foot')) + ' ' +
                    clean(nodeText(card, '.mv-card__badges'));
                  var countryFact = Array.from(
                    row.querySelectorAll('.mv-ritem__fact')
                  ).find(function(node) {
                    return clean(node.querySelector('.mv-ritem__k')).indexOf('کشور') === 0;
                  });
                  var country = countryFact
                    ? clean(countryFact.textContent).replace(/^کشور\s*:\s*/, '') : '';
                  return {
                    title: title.slice(0, 140),
                    kind: kind,
                    contentUrl: url.origin + url.pathname,
                    posterUrl: rawPoster && !/^data:/i.test(rawPoster)
                      ? new URL(rawPoster, location.origin).href : '',
                    backdropUrl: '',
                    imdbId: imdbMatch ? imdbMatch[0].toLowerCase() : '',
                    episodeLabel: clean(
                      nodeText(card, '.mv-card__ep')
                    ).slice(0, 72),
                    summary: clean(
                      nodeText(row, '.mv-ritem__plot') ||
                      nodeText(card, '.mv-card__plot')
                    ).slice(0, 420),
                    year: yearMatch ? yearMatch[0] : '',
                    genres: genres,
                    rating: rating,
                    runtime: '',
                    country: country.slice(0, 60),
                    language: '',
                    hasPersianDub: /دوبله/.test(tags),
                    hasPersianSubtitle: /زیرنویس\s*فارسی|زیرنویس/.test(tags),
                    maxQualityHeight: qualityMatch ? Number(qualityMatch[1]) : 0,
                    qualityLabel: quality.slice(0, 32),
                    directors: people(row, 'کارگردان'),
                    cast: people(row, 'بازیگران')
                  };
                }).filter(Boolean).slice(0, 24);
              }
              async function page(type, pageNumber) {
                var query = '?sort=latest' +
                  (type === 'SERIES' ? '&type=tv' : '') +
                  (pageNumber > 1 ? '&p=' + pageNumber : '');
                var response = await fetch(location.origin + '/_modern/classic' + query, {
                  credentials: 'include'
                });
                if (!response.ok) return [];
                return parse(await response.text(), type);
              }
              function unique(items) {
                var seen = {};
                return items.filter(function(item) {
                  if (seen[item.contentUrl]) return false;
                  seen[item.contentUrl] = true;
                  return true;
                });
              }
              try {
                // Fetch only the next four-page window. The former implementation launched
                // up to 82 requests at once and silently lost many responses, then still
                // recorded the requested end page as loaded.
                var moviePages = [];
                var seriesPages = [];
                for (
                  var pageNumber = __AMIN_PAGE_START__;
                  pageNumber <= __AMIN_PAGE_LIMIT__ + 1;
                  pageNumber++
                ) {
                  var pair = await Promise.all([
                    page('MOVIE', pageNumber),
                    page('SERIES', pageNumber)
                  ]);
                  moviePages.push(pair[0]);
                  seriesPages.push(pair[1]);
                }
                var movieProbe = moviePages.pop() || [];
                var seriesProbe = seriesPages.pop() || [];
                var allMovies = unique([].concat.apply([], moviePages));
                var allSeries = unique([].concat.apply([], seriesPages));
                var movieResult = {
                  items: allMovies.slice(0, __AMIN_MAX_ITEMS__),
                  hasMore: movieProbe.length > 0
                };
                var seriesResult = {
                  items: allSeries.slice(0, __AMIN_MAX_ITEMS__),
                  hasMore: seriesProbe.length > 0
                };
                var movies = movieResult.items;
                var series = seriesResult.items;
                if (!movies.length && !series.length) {
                  AminCatalog.failed('mymoviz', 'Empty catalog');
                  return;
                }
                var all = [];
                for (var i = 0; i < Math.max(movies.length, series.length); i++) {
                  if (movies[i]) all.push(movies[i]);
                  if (series[i]) all.push(series[i]);
                }
                AminCatalog.section('mymoviz', JSON.stringify({
                  all: unique(all).slice(0, __AMIN_MAX_ITEMS__),
                  movies: movies,
                  series: series,
                  popularSeries: [],
                  featured: [],
                  hasMoreMovies: movieResult.hasMore,
                  hasMoreSeries: seriesResult.hasMore
                }));
              } catch (_) {
                AminCatalog.failed('mymoviz', 'Service unavailable');
              }
            })();
        """.trimIndent()
    }
}
