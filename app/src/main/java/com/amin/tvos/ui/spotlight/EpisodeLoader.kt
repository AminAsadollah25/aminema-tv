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
 * Supports FilmRooz and Parsiflix season/episode DOM structures.
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
    private val timeout = Runnable { finish(null) }

    @SuppressLint("SetJavaScriptEnabled")
    fun load(item: SpotlightItem) {
        destroy()
        Log.d("EpisodeLoader", "load() called for: ${item.contentUrl}")
        delivered = false
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
                // Wait for SPA hydration then extract
                val delay = if (item.serviceId == "parsiflix") 2_500L else 1_800L
                source.postDelayed({
                    if (source === webView && !delivered) {
                        // No pre-dump needed; EXTRACT_SCRIPT handles everything
                        // Then run the actual extraction after a small delay
                        source.postDelayed({
                            if (source === webView && !delivered) {
                                source.evaluateJavascript("""
                                    (function() {
                                        var imgs = Array.from(document.querySelectorAll('img')).map(i => i.src).filter(s => s && !s.startsWith('data:'));
                                        var bgs = Array.from(document.querySelectorAll('*')).map(e => window.getComputedStyle(e).backgroundImage).filter(b => b && b !== 'none');
                                        return JSON.stringify({imgs: imgs, bgs: bgs});
                                    })();
                                """.trimIndent()) { res ->
                                    Log.d("EpisodeLoader", "Found Images: ${res?.take(2000)}")
                                }
                                Log.d("EpisodeLoader", "Executing EXTRACT_SCRIPT")
                                source.evaluateJavascript(EXTRACT_SCRIPT, null)
                            }
                        }, 500)
                    }
                }, delay)
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                if (request.isForMainFrame) finish(null)
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
                finish(editions)
            }
        }
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
        const val TIMEOUT_MS = 8_000L  // Fail fast if page doesn't load

        /**
         * Extracts season/episode structure.
         * Supports FilmRooz (#cseason select) and Parsiflix (API-driven) DOM patterns.
         */
        val EXTRACT_SCRIPT = """
            (async function() {
              function clean(v) { return String(v || '').replace(/\s+/g, ' ').trim(); }

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
                // Method 1: Read cseason divs directly (works even without select)
                var cseasonDivs = Array.from(document.querySelectorAll('div.cseason[id^="cseason_"]'));
                
                // Method 2: Fall back to select#cseason
                var seasonSelect = document.querySelector('select#cseason');
                
                if (!cseasonDivs.length && !seasonSelect) return null;
                
                // If we have the select, use its options for season names
                var seasonNames = {};
                if (seasonSelect) {
                  Array.from(seasonSelect.options).forEach(function(opt) {
                    seasonNames[opt.value] = clean(opt.textContent);
                  });
                }
                
                // If no cseason divs, try to determine them from select
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
                  
                  var episodesMap = {};
                  var links = Array.from(div.querySelectorAll('a'));
                  
                  links.forEach(function(link) {
                    var href = link.getAttribute('href') || link.getAttribute('data-url') || '';
                    if (!href || href === '#' || href.indexOf('javascript') === 0) return;
                    
                    var parent = link.closest('.eSbox, .dl-box, .item, .eDbox') || link.parentElement;
                    var titleEl = parent ? (parent.querySelector('.eTitle') || parent.querySelector('span')) : null;
                    var fullText = clean(link.textContent + ' ' + (titleEl ? titleEl.textContent : '') + ' ' + (parent ? parent.textContent : ''));
                    
                    var epMatch = fullText.match(/قسمت\s*(\d+)/i) || 
                                  fullText.match(/[Ee]pisode\s*(\d+)/i) || 
                                  fullText.match(/[Ee]p\s*(\d+)/i) ||
                                  fullText.match(/[Ss]\d+[Ee](\d+)/i) ||
                                  href.match(/[Ss]\d+[Ee](\d+)/i) ||
                                  href.match(/episode-(\d+)/i);
                                  
                    if (epMatch) {
                      var epNum = parseInt(epMatch[1], 10);
                      if (!episodesMap[epNum]) {
                         episodesMap[epNum] = {
                           id: seasonNum + '-ep-' + epNum,
                           title: 'قسمت ' + epNum,
                           actionPayload: href,
                           isAvailableOnline: true,
                           isWatched: false,
                           _order: epNum
                         };
                      }
                    }
                  });
                  
                  var episodes = Object.keys(episodesMap).map(function(k) { return episodesMap[k]; });
                  episodes.sort(function(a, b) { return a._order - b._order; });
                  
                  if (episodes.length === 0) {
                     var streamBoxes = Array.from(div.querySelectorAll('.eSbox'));
                     if (streamBoxes.length > 0) {
                        streamBoxes.forEach(function(sbox, i) {
                           var link = sbox.querySelector('a');
                           episodes.push({
                              id: seasonNum + '-ep-' + (i+1),
                              title: 'قسمت ' + (i+1),
                              actionPayload: link ? link.getAttribute('href') : '',
                              isAvailableOnline: true,
                              isWatched: false
                           });
                        });
                     } else {
                        var dBoxes = Array.from(div.querySelectorAll('.eDbox, .dl-box'));
                        dBoxes.forEach(function(dbox, i) {
                           episodes.push({
                              id: seasonNum + '-ep-' + (i+1),
                              title: 'قسمت ' + (i+1),
                              actionPayload: '',
                              isAvailableOnline: false,
                              isWatched: false
                           });
                        });
                     }
                  }
                  
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
                  label: 'پیش‌فرض',
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

                // Try various selectors to find season containers
                // From DOM dump: seasonItems with title (فصل اول) and eps array
                var seasons = [];

                // Strategy 1: look for season header elements (h2,h3,div with فصل text)
                var allEls = Array.from(document.querySelectorAll('*'));
                
                // Find containers that group episodes by season
                // Parsiflix uses accordion or tab pattern for seasons
                var seasonContainers = Array.from(document.querySelectorAll(
                  '[class*="season"], [class*="Season"], [data-season]'
                )).filter(function(el) {
                  return el.querySelectorAll('a, button, [role="button"]').length > 0 ||
                         el.textContent.indexOf('قسمت') !== -1;
                });

                // If no explicit season containers, look for sections with episode lists
                if (!seasonContainers.length) {
                  // Find all elements whose text is just a season name
                  var headerEls = allEls.filter(function(el) {
                    var t = el.childElementCount === 0 ? (el.textContent || '').trim() : '';
                    return /^فصل\s*(\w+|\d+)$/.test(t) || /^Season\s*\d+$/i.test(t);
                  });
                  // Use parent containers of those headers
                  seasonContainers = headerEls.map(function(h) {
                    return h.closest('section, div, article, li') || h.parentElement;
                  }).filter(Boolean);
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
                  var epEls = Array.from(container.querySelectorAll(
                    'a[href*="episode"], a[href*="ep"], [class*="episode"], [class*="Episode"], [data-episode-id]'
                  ));
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
                    var epText = clean(el.textContent || '');
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
                        actionPayload: linkEl.getAttribute('href') || epId,
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
                var editions = filmRoozEditions() ||
                               parsiflixDomEditions();
                AminEpisode.result(editions ? JSON.stringify({ editions: editions }) : null);
              } catch(e) {
                AminEpisode.result(null);
              }
            })();
        """.trimIndent()
    }
}
