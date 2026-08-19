package com.amin.tvos.ui.spotlight

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
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import com.amin.tvos.AminTvApp
import com.amin.tvos.data.model.Episode
import com.amin.tvos.data.model.Season
import com.amin.tvos.data.model.SeriesEdition
import com.amin.tvos.data.model.SpotlightItem
import com.amin.tvos.data.model.UserAgentMode
import org.json.JSONObject

/**
 * Loads the season/episode structure of a series from its provider title page.
 * Uses the same invisible-WebView + JS-bridge pattern as SpotlightMetadataLoader.
 * Supports FilmRooz, Parsiflix and MyMoviz season/episode DOM structures.
 */
class EpisodeLoader(
    private val activity: ComponentActivity,
    private val app: AminTvApp,
    private val onLoaded: (List<SeriesEdition>) -> Unit,
    private val onFailed: () -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private var webView: WebView? = null
    private var delivered = false
    private var extractionScheduled = false
    private var extractionAttempt = 0
    private val timeout = Runnable {
        Log.d("EpisodeLoader", "Timed out after ${TIMEOUT_MS}ms; attempts=$extractionAttempt")
        finish(null)
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun load(item: SpotlightItem) {
        destroy()
        Log.d("EpisodeLoader", "load() called for: ${item.contentUrl}")
        delivered = false
        extractionScheduled = false
        extractionAttempt = 0
        val service = app.servicesRepository.findById(item.serviceId) ?: run {
            Log.d("EpisodeLoader", "Service not found: ${item.serviceId}")
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
        view.addJavascriptInterface(EpisodeBridge(), BRIDGE_NAME)
        view.webViewClient = object : WebViewClient() {
            override fun onPageFinished(source: WebView, url: String) {
                CookieManager.getInstance().flush()
                Log.d("EpisodeLoader", "onPageFinished: $url delivered=$delivered")
                if (delivered || source !== webView) return
                // onPageFinished can arrive late on lower-powered TV boxes. Queue extraction
                // relative to the actual page completion instead of racing a short global
                // timeout. A null bridge result is retried because SPA episode controls may
                // hydrate after the document itself has finished loading.
                val delay = when (item.serviceId) {
                    "parsiflix" -> 2_000L
                    "mymoviz" -> 1_500L
                    else -> 1_200L
                }
                queueExtraction(source, delay)
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                if (request.isForMainFrame) {
                    Log.d("EpisodeLoader", "Main-frame load failed: ${error.errorCode}")
                    finish(null)
                }
            }
        }
        activity.addContentView(
            view,
            FrameLayout.LayoutParams(2, 2, Gravity.BOTTOM or Gravity.END)
        )
        handler.postDelayed(timeout, TIMEOUT_MS)
        view.loadUrl(item.contentUrl)
    }

    private inner class EpisodeBridge {
        @JavascriptInterface
        fun result(payload: String?) {
            Log.d("EpisodeLoader", "Bridge result received: ${payload?.take(200)}")
            handler.post {
                if (delivered) return@post
                val editions = parse(payload.orEmpty())
                if (!editions.isNullOrEmpty()) {
                    finish(editions)
                } else {
                    webView?.let { queueExtraction(it, RETRY_DELAY_MS) }
                }
            }
        }

    }

    private fun queueExtraction(source: WebView, delayMs: Long) {
        if (delivered || source !== webView || extractionScheduled) return
        extractionScheduled = true
        handler.postDelayed({
            extractionScheduled = false
            if (delivered || source !== webView) return@postDelayed
            extractionAttempt += 1
            Log.d("EpisodeLoader", "Executing EXTRACT_SCRIPT attempt=$extractionAttempt")
            source.evaluateJavascript(EXTRACT_SCRIPT, null)
        }, delayMs)
    }

    private fun parse(payload: String): List<SeriesEdition>? {
        if (payload.isBlank() || payload == "null") return null
        return try {
            val root = JSONObject(payload)
            val editionsArr = root.optJSONArray("editions") ?: return null
            buildList {
                for (i in 0 until editionsArr.length()) {
                    val ed = editionsArr.optJSONObject(i) ?: continue
                    val seasonsArr = ed.optJSONArray("seasons") ?: continue
                    val seasons = buildList {
                        for (j in 0 until seasonsArr.length()) {
                            val s = seasonsArr.optJSONObject(j) ?: continue
                            val epsArr = s.optJSONArray("episodes") ?: continue
                            val episodes = buildList {
                                for (k in 0 until epsArr.length()) {
                                    val ep = epsArr.optJSONObject(k) ?: continue
                                    add(Episode(
                                        id = ep.optString("id"),
                                        title = ep.optString("title"),
                                        actionPayload = ep.optString("actionPayload"),
                                        isAvailableOnline = ep.optBoolean("isAvailableOnline", true),
                                        isWatched = ep.optBoolean("isWatched", false)
                                    ))
                                }
                            }
                            if (episodes.isNotEmpty()) {
                                add(Season(
                                    id = s.optString("id"),
                                    name = s.optString("name"),
                                    episodes = episodes
                                ))
                            }
                        }
                    }
                    if (seasons.isNotEmpty()) {
                        add(SeriesEdition(
                            id = ed.optString("id", "default"),
                            label = ed.optString("label", "پیش‌فرض"),
                            language = ed.optString("language", ""),
                            resolution = ed.optString("resolution", ""),
                            isDefault = ed.optBoolean("isDefault", i == 0),
                            seasons = seasons
                        ))
                    }
                }
            }.takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            null
        }
    }

    private fun finish(editions: List<SeriesEdition>?) {
        if (delivered) return
        delivered = true
        extractionScheduled = false
        handler.removeCallbacks(timeout)
        Log.d("EpisodeLoader", "finish() editions=${editions?.size ?: "null"}")
        if (editions.isNullOrEmpty()) onFailed() else onLoaded(editions)
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
        const val BRIDGE_NAME = "AminEpisode"
        const val TIMEOUT_MS = 25_000L
        const val RETRY_DELAY_MS = 1_500L

        /**
         * Extracts season/episode structure.
         * Supports FilmRooz (#cseason select), Parsiflix (API-driven) and
         * MyMoviz (visible season tabs + visible episode play controls).
         */
        val EXTRACT_SCRIPT = """
            (async function() {
              function clean(v) { return String(v || '').replace(/\s+/g, ' ').trim(); }
              function toEnglishDigits(value) {
                var fa = ['۰','۱','۲','۳','۴','۵','۶','۷','۸','۹'];
                var ar = ['٠','١','٢','٣','٤','٥','٦','٧','٨','٩'];
                var result = String(value || '');
                for (var i = 0; i < 10; i++) {
                  result = result.replace(new RegExp(fa[i], 'g'), String(i));
                  result = result.replace(new RegExp(ar[i], 'g'), String(i));
                }
                return result;
              }

              function wait(ms) {
                return new Promise(function(resolve) { setTimeout(resolve, ms); });
              }

              function episodeNumberFromText(value) {
                var text = toEnglishDigits(clean(value));
                var match = text.match(/قسمت\s*(\d+)/i) ||
                            text.match(/[Ee]pisode\s*(\d+)/i) ||
                            text.match(/^(\d+)$/);
                return match ? parseInt(match[1], 10) : 0;
              }

              function numericAttribute(node) {
                if (!node || !node.getAttribute) return 0;
                var names = [
                  'data-track-ep', 'data-episode', 'data-episode-number',
                  'data-ep', 'data-number'
                ];
                for (var i = 0; i < names.length; i++) {
                  var raw = toEnglishDigits(clean(node.getAttribute(names[i])));
                  var numeric = raw.match(/^\d+$/);
                  if (numeric) return parseInt(numeric[0], 10);
                }
                return 0;
              }

              // ── MyMoviz modern pattern ───────────────────────────────────
              // Season tabs are rendered immediately. The episode panel is
              // hydrated after a season click, so each season is read only
              // after its own visible episode details exist. The payload is
              // semantic (season + episode); it never contains a media URL.
              async function myMovizEditions() {
                if (!/mymoviz/i.test(location.hostname)) return null;
                var seasonButtons = Array.from(
                  document.querySelectorAll('.mv-tv__season[data-season]')
                );
                var panel = document.querySelector('#mv-tv-panel');
                if (!seasonButtons.length || !panel) return null;

                var seasons = [];
                for (var i = 0; i < seasonButtons.length; i++) {
                  var seasonButton = seasonButtons[i];
                  var seasonNum = toEnglishDigits(
                    seasonButton.getAttribute('data-season') || String(i + 1)
                  );
                  seasonButton.click();

                  var episodeElements = [];
                  for (var attempt = 0; attempt < 12; attempt++) {
                    await wait(attempt === 0 ? 650 : 300);
                    episodeElements = Array.from(
                      panel.querySelectorAll('details.mv-ep')
                    ).filter(function(details) {
                      var track = details.querySelector('.mv-eptrack');
                      var trackSeason = track && track.getAttribute('data-season');
                      return !trackSeason || trackSeason === seasonNum;
                    });
                    if (episodeElements.length) break;
                  }

                  var episodes = [];
                  episodeElements.forEach(function(details) {
                    var numberLabel = details.querySelector('.mv-ep__no');
                    var track = details.querySelector('.mv-eptrack');
                    var epNum = numericAttribute(track) || numericAttribute(details) ||
                      episodeNumberFromText(
                        numberLabel ? numberLabel.textContent : details.textContent
                      );
                    if (!epNum || episodes.some(function(ep) { return ep._order === epNum; })) return;
                    var playControls = Array.from(
                      details.querySelectorAll('a.mv-eprow__btn--play')
                    );
                    var trackButton = details.querySelector('.mv-eptrack');
                    episodes.push({
                      id: seasonNum + '-ep-' + epNum,
                      title: 'قسمت ' + epNum,
                      actionPayload: '#mymoviz-s' + seasonNum + '-epnum-' + epNum,
                      isAvailableOnline: playControls.length > 0,
                      isWatched: !!trackButton &&
                        trackButton.getAttribute('aria-pressed') === 'true',
                      _order: epNum
                    });
                  });

                  episodes.sort(function(a, b) { return a._order - b._order; });
                  if (episodes.length) {
                    var firstNode = seasonButton.childNodes[0];
                    var seasonText = clean(firstNode ? firstNode.textContent : '');
                    seasons.push({
                      id: seasonNum,
                      name: seasonText || ('فصل ' + seasonNum),
                      episodes: episodes
                    });
                  }
                }

                if (!seasons.length) return null;
                return [{
                  id: 'mymoviz-online',
                  label: 'پخش آنلاین',
                  language: 'دوبله فارسی / زبان اصلی',
                  resolution: '1080p / 720p',
                  isDefault: true,
                  seasons: seasons
                }];
              }

              // ── FilmRooz pattern ──────────────────────────────────────────
              // DOM structure (from actual inspection):
              //   <select id="cseason" class="select2-hidden-accessible">
              //     <option value="1">فصل اول</option>
              //     <option value="2">فصل دوم</option>
              //   </select>
              //   <div id="cseason_1" class="cseason"> ... season 1 content ... </div>
              //   <div id="cseason_2" class="cseason"> ... season 2 content ... </div>
              //   Inside each cseason div:
              //     <div class="eSbox"> ... stream episodes (قسمت ۰۱, قسمت ۰۲) ... </div>
              //     <div class="eDbox"> ... download episodes ... </div>
              function filmRoozEditions() {
                var cseasonDivs = Array.from(document.querySelectorAll('div.cseason[id^="cseason_"]'));
                var seasonSelect = document.querySelector('select#cseason');
                if (!cseasonDivs.length && !seasonSelect) return null;

                var seasonNames = {};
                if (seasonSelect) {
                  Array.from(seasonSelect.options).forEach(function(opt) {
                    seasonNames[opt.value] = clean(opt.textContent);
                  });
                }
                if (!cseasonDivs.length && seasonSelect) {
                  Array.from(seasonSelect.options).forEach(function(opt) {
                    var div = document.getElementById('cseason_' + opt.value);
                    if (div) cseasonDivs.push(div);
                  });
                }
                if (!cseasonDivs.length) return null;

                var seasons = [];
                cseasonDivs.forEach(function(div, idx) {
                  var idMatch = div.id.match(/cseason_(\d+)/);
                  var seasonNum = idMatch ? idMatch[1] : String(idx + 1);
                  var seasonName = seasonNames[seasonNum] || ('فصل ' + (idx + 1));

                  // Verified FilmRooz structure: each quality block owns one .eSbox.
                  // Its children are the site's real online-play controls; .eDbox links
                  // are downloads and must never become an Aminema action payload.
                  var qualityBlocks = Array.from(div.children).filter(function(block) {
                    return !!block.querySelector('.eSbox');
                  });
                  var candidates = qualityBlocks.map(function(block, blockIndex) {
                    var header = block.querySelector('.dlbox-color') || block.firstElementChild;
                    var label = clean(header ? header.textContent : '');
                    var normalized = toEnglishDigits(label);
                    var resolutionMatch = normalized.match(/(?:^|\s)(2160|1080|720|480)p(?:\s|$)/i);
                    var resolution = resolutionMatch ? parseInt(resolutionMatch[1], 10) : 0;
                    var languageRank = /دو\s*زبانه|دوبله|صوت\s*فارسی|فارسی/i.test(label) ? 2 : 1;
                    var qualityRank = resolution === 1080 ? 3 :
                                      resolution === 720 ? 2 :
                                      resolution === 480 ? 1 : 0;
                    var streamBox = block.querySelector('.eSbox');
                    var episodeControls = streamBox ? Array.from(streamBox.children) : [];
                    return {
                      blockIndex: blockIndex,
                      languageRank: languageRank,
                      qualityRank: qualityRank,
                      episodeControls: episodeControls
                    };
                  }).filter(function(candidate) {
                    // 2160p is intentionally not an automatic choice on TV boxes.
                    return candidate.qualityRank > 0 && candidate.episodeControls.length > 0;
                  });

                  candidates.sort(function(a, b) {
                    return (b.languageRank - a.languageRank) ||
                           (b.qualityRank - a.qualityRank) ||
                           (a.blockIndex - b.blockIndex);
                  });
                  var best = candidates[0];
                  if (!best) return;

                  var episodesMap = {};
                  best.episodeControls.forEach(function(control) {
                    var text = toEnglishDigits(clean(control.textContent));
                    var match = text.match(/قسمت\s*(\d+)/i) ||
                                text.match(/[Ee]pisode\s*(\d+)/i);
                    if (!match) return;
                    var epNum = parseInt(match[1], 10);
                    if (!epNum || episodesMap[epNum]) return;
                    episodesMap[epNum] = {
                      id: seasonNum + '-ep-' + epNum,
                      title: 'قسمت ' + epNum,
                      actionPayload: '#filmrooz-s' + seasonNum +
                        '-box' + best.blockIndex + '-epnum-' + epNum,
                      isAvailableOnline: true,
                      isWatched: !!control.querySelector(
                        '.fa-check,.fa-check-circle,[class*="check"],[class*="Check"]'
                      ),
                      _order: epNum
                    };
                  });
                  var episodes = Object.keys(episodesMap).map(function(key) {
                    return episodesMap[key];
                  }).sort(function(a, b) { return a._order - b._order; });

                  if (episodes.length > 0) {
                    seasons.push({
                      id: seasonNum,
                      name: seasonName,
                      episodes: episodes
                    });
                  }
                });

                if (!seasons.length) return null;
                return [{
                  id: 'default',
                  label: 'پخش پیشنهادی',
                  language: '',
                  resolution: '',
                  isDefault: true,
                  seasons: seasons
                }];
              }

              // ── Parsiflix DOM pattern ─────────────────────────────────────
              // Parsiflix renders a Next.js SPA. Season/episode data is injected
              // into the DOM as a list of season containers, each with episode cards.
              // Structure discovered from DOM dump:
              //   Season tabs: elements with class containing 'season' or h2/h3 with season name
              //   Episode cards: each card has ep number in text and data-id or similar
              function parsiflixDomEditions() {
                if (!/parsiflix/i.test(location.hostname)) return null;
                var mediaMatch = location.pathname.match(/\/medias\/series\/(\d+)/i);
                if (!mediaMatch) return null;

                var seasons = [];

                var allEls = Array.from(document.querySelectorAll('*'));
                // Verified live structure. The suffix is hashed, `_seasonItem_` is stable.
                var seasonContainers = Array.from(
                  document.querySelectorAll('[class*="_seasonItem_"]')
                );

                if (!seasonContainers.length) {
                  seasonContainers = Array.from(document.querySelectorAll(
                    '[class*="season"], [class*="Season"], [data-season]'
                  )).filter(function(el) {
                    return el.querySelectorAll('a, button, [role="button"]').length > 0 ||
                           el.textContent.indexOf('قسمت') !== -1;
                  });
                }

                // Remove duplicate/ancestor containers - keep only leaf containers
                seasonContainers = seasonContainers.filter(function(c) {
                  return !seasonContainers.some(function(other) {
                    return other !== c && c.contains(other);
                  });
                });
                // Deduplicate by DOM node identity
                seasonContainers = seasonContainers.filter(function(c, i, arr) {
                  return arr.indexOf(c) === i;
                });

                // Process each season container
                seasonContainers.forEach(function(container, sIdx) {
                  // Find season name
                  var headerEl = container.querySelector('h1,h2,h3,h4,h5') ||
                                  Array.from(container.querySelectorAll('*')).find(function(el) {
                                    return el.childElementCount === 0 && /فصل/i.test(el.textContent);
                                  });
                  var seasonName = headerEl ? clean(headerEl.textContent) : ('فصل ' + (sIdx + 1));
                  if (!seasonName) seasonName = 'فصل ' + (sIdx + 1);

                  // Find episode elements within this season container
                  // Prefer the provider's verified row. The hashed suffix changes,
                  // but `_episodeItem_` is the stable semantic part of the class.
                  var epEls = Array.from(container.querySelectorAll('[class*="_episodeItem_"]'));
                  if (!epEls.length) {
                    epEls = Array.from(container.querySelectorAll(
                      'a[href*="episode"], a[href*="ep"], [class*="episode"], [class*="Episode"], [data-episode-id]'
                    ));
                    // Generic selectors can return a wrapper, row and number label
                    // for the same episode. Keep one leaf candidate only in fallback.
                    epEls = epEls.filter(function(el) {
                      return !epEls.some(function(other) {
                        return other !== el && el.contains(other);
                      });
                    });
                  }

                  // Fallback: any clickable/link element with قسمت in text
                  if (!epEls.length) {
                    epEls = Array.from(container.querySelectorAll('a, button, [role="button"], [onClick]'))
                      .filter(function(el) { return /قسمت|episode/i.test(el.textContent); });
                  }
                  // Fallback 2: any element whose sole text content is قسمت N
                  if (!epEls.length) {
                    epEls = Array.from(container.querySelectorAll('*')).filter(function(el) {
                      return /قسمت\s*\d+/i.test((el.textContent || '').trim()) && el.childElementCount <= 2;
                    });
                  }

                  var episodes = [];
                  epEls.forEach(function(el, epIdx) {
                    var numberEl = el.querySelector && el.querySelector('[class*="_episodeNumber_"]');
                    var epText = clean(numberEl ? numberEl.textContent : (el.textContent || ''));
                    var epNumMatch = epText.match(/قسمت\s*(\d+)/);
                    if (!epNumMatch) {
                        var standalone = epText.match(/^(\d+)$/);
                        if (standalone) epNumMatch = standalone;
                    }
                    var epNum = epNumMatch ? parseInt(epNumMatch[1], 10) : (epIdx + 1);
                    var epId = el.getAttribute('data-id') || el.getAttribute('data-episode-id') ||
                               el.getAttribute('data-ep') || String(epNum);
                    var href = el.getAttribute('href') || el.getAttribute('data-href');
                    // For Parsiflix: use positional payload since href is null
                    // Format: #parsiflix-s{seasonIndex}-epnum-{epNum} for StateMachine click simulation
                    var actionPayload = href || ('#parsiflix-s' + sIdx + '-epnum-' + epNum);
                    // Only add if not duplicate
                    if (!episodes.find(function(e) { return e.id === String(epId); })) {
                      episodes.push({
                        id: String(epId),
                        title: 'قسمت ' + epNum,
                        actionPayload: actionPayload,
                        isAvailableOnline: true,
                        isWatched: false
                      });
                    }
                  });
                  episodes.sort(function(a, b) {
                    return parseInt(a.title.replace(/[^0-9]/g,''),10) - parseInt(b.title.replace(/[^0-9]/g,''),10);
                  });

                  if (episodes.length > 0) {
                    // Deduplicate by season name
                    if (!seasons.find(function(s) { return s.name === seasonName; })) {
                      seasons.push({ id: String(seasons.length + 1), name: seasonName, episodes: episodes });
                    }
                  }
                });

                // ── Fallback: direct DOM scan for episode links anywhere on page ──
                if (!seasons.length) {
                  var allEpEls = Array.from(document.querySelectorAll('*')).filter(function(el) {
                    return el.childElementCount <= 2 && /قسمت\s*\d+/i.test((el.textContent||'').trim());
                  });
                  var epMap = {};
                  allEpEls.forEach(function(el) {
                    var epText = clean(el.textContent || '');
                    var epNumMatch = epText.match(/قسمت\s*(\d+)/);
                    if (!epNumMatch) {
                        var standalone = epText.match(/^(\d+)$/);
                        if (standalone) epNumMatch = standalone;
                    }
                    if (!epNumMatch) return;
                    var epNum = parseInt(epNumMatch[1], 10);
                    var linkEl = el.closest('a') || el.querySelector('a') || el;
                    var epId = linkEl.getAttribute('data-id') || linkEl.getAttribute('data-episode-id') || String(epNum);
                    if (!epMap[epNum]) {
                      epMap[epNum] = {
                        id: epId,
                        title: 'قسمت ' + epNum,
                        actionPayload: '#parsiflix-s0-epnum-' + epNum,
                        isAvailableOnline: true,
                        isWatched: false
                      };
                    }
                  });
                  var flatEps = Object.keys(epMap).sort(function(a,b){return a-b;}).map(function(k){return epMap[k];});
                  if (flatEps.length) {
                    seasons.push({ id: '1', name: 'فصل ۱', episodes: flatEps });
                  }
                }

                if (!seasons.length) return null;
                return [{
                  id: 'default',
                  label: 'پیش‌فرض',
                  language: 'فارسی',
                  resolution: '',
                  isDefault: true,
                  seasons: seasons
                }];
              }

              try {
                var editions = await myMovizEditions() ||
                               filmRoozEditions() ||
                               parsiflixDomEditions();
                AminEpisode.result(editions ? JSON.stringify({ editions: editions }) : null);
              } catch(e) {
                AminEpisode.result(null);
              }
            })();
        """.trimIndent()
    }
}
