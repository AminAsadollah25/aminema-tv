package com.amin.tvos.browser

import android.app.Activity
import android.webkit.WebView

/**
 * Manages the state of playback sessions, especially for series episodes.
 * Bypasses intermediate browser history (like auto-next redirects) and 
 * handles complex provider state machines (like FilmRooz).
 */
class PlaybackSessionController(
    private val activity: Activity,
    private val webView: WebView
) {
    private var isPlayingEpisode = false

    fun markEpisodePlaybackStarted() {
        isPlayingEpisode = true
    }

    /**
     * @return true if the back press is consumed by this controller, false otherwise.
     */
    fun handleBackPress(isFullscreen: Boolean): Boolean {
        if (isPlayingEpisode) {
            // If in fullscreen, exit fullscreen first
            if (isFullscreen) {
                webView.evaluateJavascript(
                    "document.exitFullscreen ? document.exitFullscreen() : " +
                    "(document.webkitExitFullscreen ? document.webkitExitFullscreen() : null);", 
                    null
                )
            }
            // Bypass browser history and return directly to the Native Episode Navigator (Spotlight)
            activity.finish()
            return true
        }
        return false
    }

    fun executeFilmRoozStateMachine(seasonId: String, quality: String, episodeAction: String) {
        android.util.Log.d("PlaybackSession", "executeFilmRoozStateMachine: seasonId=$seasonId, episodeAction=$episodeAction")
        val script = """
            (function() {
                try {
                    console.log("StateMachine started: action=" + '$episodeAction');
                    // Try to handle episodeAction directly if it's a URL or Javascript
                    if ('$episodeAction'.indexOf('http') === 0 || '$episodeAction'.indexOf('/') === 0) {
                        console.log("StateMachine: Navigating directly to " + '$episodeAction');
                        window.location.href = '$episodeAction';
                        return;
                    }
                    if ('$episodeAction'.indexOf('javascript:') === 0) {
                        console.log("StateMachine: Evaluating JS " + '$episodeAction');
                        eval('$episodeAction'.replace('javascript:', ''));
                        return;
                    }
                    
                    // Parsiflix positional click handler
                    // Format: #parsiflix-s{seasonContainerIdx}-ep-{episodeIdx}
                    if ('$episodeAction'.indexOf('#parsiflix-s') === 0 || '$episodeAction'.indexOf('#parsiflix-epnum') === 0 || '$episodeAction'.match(/#parsiflix-s\d+-ep/)) {
                        console.log("StateMachine: Parsiflix positional click for " + '$episodeAction');
                        // Support both old (-ep-idx) and new (-epnum-num) formats for backward compatibility during testing
                        var parts = '$episodeAction'.match(/#parsiflix-s(\d+)-epnum-(\d+)/) || '$episodeAction'.match(/#parsiflix-s(\d+)-ep-(\d+)/);
                        if (parts) {
                            var sIdx = parseInt(parts[1], 10);
                            var isEpNum = '$episodeAction'.indexOf('-epnum-') !== -1;
                            var epTarget = parseInt(parts[2], 10);
                            
                            delete window._aminSeasonTabClicked;
                            function attemptParsiflix(retriesLeft) {
                                        function toEng(s) {
                                            var p = ['۰','۱','۲','۳','۴','۵','۶','۷','۸','۹'];
                                            var a = ['٠','١','٢','٣','٤','٥','٦','٧','٨','٩'];
                                            for(var i=0; i<10; i++) {
                                                s = s.replace(new RegExp(p[i], 'g'), i).replace(new RegExp(a[i], 'g'), i);
                                            }
                                            return s;
                                        }
                                        
                                        // 1. Click the correct season tab if it exists
                                        var targetSeasonText = "فصل " + (sIdx + 1);
                                        var seasonTabs = Array.from(document.querySelectorAll('*')).filter(function(el) {
                                            if (el.childElementCount > 1) return false;
                                            var t = toEng((el.textContent || '').trim());
                                            return t === targetSeasonText || t.indexOf(targetSeasonText) !== -1;
                                        });
                                        if (seasonTabs.length > 0) {
                                            var tab = seasonTabs[0];
                                            if (tab.tagName !== 'A' && tab.tagName !== 'BUTTON') {
                                                tab = tab.closest('a, button, [role="tab"], li') || tab;
                                            }
                                            if (tab.getAttribute('aria-selected') !== 'true' && tab.className.indexOf('active') === -1 && !window._aminSeasonTabClicked) {
                                                console.log("StateMachine: Clicking season tab for " + targetSeasonText);
                                                tab.dispatchEvent(new MouseEvent('click', { view: window, bubbles: true, cancelable: true }));
                                                tab.click();
                                                window._aminSeasonTabClicked = true;
                                                setTimeout(function() { attemptParsiflix(retriesLeft); }, 500);
                                                return; // wait for DOM update
                                            }
                                        }
                                        
                                        // 2. Find the containers
                                        var seasonContainers = Array.from(document.querySelectorAll(
                                          '[class*="season"], [class*="Season"], [data-season]'
                                        )).filter(function(el) {
                                          return el.querySelectorAll('a, button, [role="button"]').length > 0 ||
                                                 el.textContent.indexOf('قسمت') !== -1;
                                        });
                                
                                if (!seasonContainers.length) {
                                    var allEls = Array.from(document.querySelectorAll('*'));
                                    var headerEls = allEls.filter(function(el) {
                                        var t = el.childElementCount === 0 ? (el.textContent || '').trim() : '';
                                        return /^فصل\s*(\w+|\d+)$/.test(t) || /^Season\s*\d+$/i.test(t);
                                    });
                                    seasonContainers = headerEls.map(function(h) {
                                        return h.closest('section, div, article, li') || h.parentElement;
                                    }).filter(Boolean);
                                }
    
                                seasonContainers = seasonContainers.filter(function(c) {
                                  return !seasonContainers.some(function(o) { return o !== c && c.contains(o); });
                                }).filter(function(c, i, arr) { return arr.indexOf(c) === i; });
                                
                                var container = seasonContainers[sIdx] || seasonContainers[0];
                                if (!container && retriesLeft > 0) {
                                    console.log("StateMachine: Parsiflix DOM not ready, retrying...");
                                    setTimeout(function() { attemptParsiflix(retriesLeft - 1); }, 500);
                                    return;
                                }
                                
                                if (container) {
                                    // Prefer the provider's own episode-row element. Verified against the
                                    // live page: each row is
                                    //     div[class*=_episodeItem_]
                                    //       ├─ span[class*=_episodeNumber_]  "قسمت 11"
                                    //       └─ button                        "تماشا"
                                    // The number and the button are SIBLINGS, so a row has to stay whole:
                                    // read the number from one child and click the other.
                                    var epEls = Array.from(container.querySelectorAll('[class*="_episodeItem_"]'));
                                    if (!epEls.length) {
                                        epEls = Array.from(container.querySelectorAll(
                                          'a[href*="episode"], [class*="episode"], [class*="Episode"], [data-episode-id]'
                                        ));
                                        if (!epEls.length) {
                                            epEls = Array.from(container.querySelectorAll('a, button, [role="button"], [onClick]'))
                                              .filter(function(el) { return /قسمت|episode/i.test(el.textContent); });
                                        }
                                        if (!epEls.length) {
                                            epEls = Array.from(container.querySelectorAll('*')).filter(function(el) {
                                                return /قسمت\s*\d+/i.test((el.textContent || '').trim()) && el.childElementCount <= 2;
                                            });
                                        }
                                        // Only for the generic selectors above: drop wrappers that contain
                                        // another candidate. Episode rows are already one-per-episode, and
                                        // shrinking them to their leaf is exactly what broke clicking —
                                        // the leaf was the number label, which carries no handler.
                                        epEls = epEls.filter(function(el) {
                                            return !epEls.some(function(o) { return o !== el && el.contains(o); });
                                        });
                                    }

                                    var target = null;
                                    if (isEpNum) {
                                        // Find by episode number in text
                                        target = epEls.find(function(el) {
                                            // Read the number from the row's own number label when the
                                            // provider gives one. Matching against the row's whole text
                                            // would take the FIRST number it contains, which on a wrapper
                                            // is always episode 1 — the bug that made every request play
                                            // the same episode.
                                            var label = el.querySelector && el.querySelector('[class*="_episodeNumber_"]');
                                            if (label) {
                                                var lm = toEng((label.textContent || '').trim()).match(/(\d+)/);
                                                return !!lm && parseInt(lm[1], 10) === epTarget;
                                            }
                                            var t = toEng((el.textContent || '').trim());
                                            var match = t.match(/قسمت\s*(\d+)/);
                                            if (!match) {
                                                // If no "قسمت", require the text to be exactly the number or contain it as a standalone word
                                                if (t === epTarget.toString()) {
                                                    match = [t, t];
                                                } else {
                                                    var standaloneMatch = t.match(new RegExp('^' + epTarget + '$|\\\\b' + epTarget + '\\\\b'));
                                                    if (standaloneMatch) {
                                                        match = [epTarget.toString(), epTarget.toString()];
                                                    }
                                                }
                                            }
                                            var found = match && parseInt(match[1], 10) === epTarget;
                                            if (found) {
                                                console.log("StateMachine: Match found for epTarget " + epTarget + " -> text: '" + t + "' on tag: " + el.tagName);
                                            }
                                            return found;
                                        });
                                    }
                                    // Wait for episodes to render if not found
                                    if (!target && retriesLeft > 0) {
                                        console.log("StateMachine: Episode target " + epTarget + " not found yet, retrying...");
                                        setTimeout(function() { attemptParsiflix(retriesLeft - 1); }, 500);
                                        return;
                                    }
                                    
                                    // No index fallback. It assumed one element per episode in ascending
                                    // order, and when that was false it clicked a wrong-but-plausible row,
                                    // so the user silently got the wrong episode. Reporting nothing is
                                    // better: the detail page stays open and they can choose by hand.
                                    if (!target) {
                                        console.log("StateMachine: episode " + epTarget + " not found among " + epEls.length + " rows — giving up rather than guessing");
                                        window._aminEpisodeClickResult = 'NOT_FOUND:' + epTarget;
                                        return;
                                    }
                                    if (target) {
                                        console.log("StateMachine: Parsiflix clicking epTarget " + epTarget + " in season " + sIdx);
                                        // The handler lives on the row's own watch button, which is a
                                        // SIBLING of the number label — not an ancestor of it. Clicking the
                                        // label bubbled upward and never reached the button, so nothing
                                        // happened and the site's generic Continue action played instead.
                                        var clickable =
                                            target.querySelector('button, a, [role="button"]') ||
                                            (target.tagName === 'A' || target.tagName === 'BUTTON' ? target : null) ||
                                            target.closest('a') ||
                                            target.closest('[onclick]') ||
                                            target.closest('div[role="button"]') ||
                                            target;
                                        console.log("StateMachine: clicking <" + clickable.tagName + "> for episode " + epTarget);
                                        window._aminEpisodeClickResult = 'CLICKED:' + epTarget;

                                        // React/Vue SPAs often ignore simple .click() and require a full MouseEvent
                                        var event = new MouseEvent('click', {
                                            view: window,
                                            bubbles: true,
                                            cancelable: true
                                        });
                                        clickable.dispatchEvent(event);
                                        if (typeof clickable.click === 'function') clickable.click();
                                        return;
                                    }
                                }
                                console.log("StateMachine: Parsiflix - could not find container " + sIdx);
                            }
                            
                            attemptParsiflix(8); // Retry up to 8 times (4 seconds)
                            return; // Let the retry loop handle it
                        }
                    }
                    
                    // Custom internal fallback for FilmRooz DOM structure
                    if ('$episodeAction'.indexOf('#season-') === 0) {
                        console.log("StateMachine: Using DOM index fallback for " + '$episodeAction');
                        var parts = '$episodeAction'.split('-');
                        var seasonNum = parts[1];
                        var epIdx = parseInt(parts[3]);
                        
                        var seasonDiv = document.querySelector('#cseason_' + seasonNum);
                        if (!seasonDiv) {
                            var allSeasons = Array.from(document.querySelectorAll('.cseason'));
                            if (allSeasons.length >= seasonNum) seasonDiv = allSeasons[seasonNum - 1];
                        }
                        
                        if (seasonDiv) {
                            var streamBoxes = Array.from(seasonDiv.querySelectorAll('.eSbox'));
                            for (var i = 0; i < streamBoxes.length; i++) {
                                var epElements = Array.from(streamBoxes[i].children).filter(function(child) {
                                    var text = (child.textContent || '').replace(/\s+/g, ' ').trim();
                                    return text && /قسمت|[Ee]pisode|[Ee]\d+|[Ss]\d+/i.test(text);
                                });
                                if (epElements.length > epIdx && epElements[epIdx]) {
                                    var btn = epElements[epIdx];
                                    var target = btn.tagName === 'A' ? btn : (btn.querySelector('a') || btn);
                                    console.log("StateMachine: Clicked specific element for epIdx=" + epIdx);
                                    target.click();
                                    return;
                                }
                            }
                        }
                    }
                    
                    // Otherwise, try to find the button by data attributes or exact href
                    var epBtn = document.querySelector('a[href="' + '$episodeAction' + '"]') || 
                                document.querySelector('[data-post="' + '$episodeAction' + '"]') ||
                                document.querySelector('[data-url="' + '$episodeAction' + '"]');
                                
                    if (epBtn) {
                        console.log("StateMachine: Clicked epBtn found by exact attribute");
                        epBtn.click();
                        return;
                    }

                    // Fallback for SPA (e.g. Parsiflix) where href might contain the episode ID
                    var allLinks = document.querySelectorAll('a');
                    for (var j = 0; j < allLinks.length; j++) {
                        var href = allLinks[j].getAttribute('href') || '';
                        if (href.indexOf('$episodeAction') !== -1 && (href.indexOf('/play') !== -1 || href.indexOf('/stream') !== -1)) {
                            console.log("StateMachine: Clicked epBtn found by href heuristic");
                            allLinks[j].click();
                            return;
                        }
                    }
                    
                    // Final fallback: just try any href with the episode ID if it's long enough to be unique
                    if ('$episodeAction'.length >= 3) {
                        for (var k = 0; k < allLinks.length; k++) {
                            var href = allLinks[k].getAttribute('href') || '';
                            if (href.indexOf('/$episodeAction') !== -1 || href.indexOf('=$episodeAction') !== -1) {
                                console.log("StateMachine: Clicked epBtn found by loose href heuristic");
                                allLinks[k].click();
                                return;
                            }
                        }
                    }
                    
                    console.log("StateMachine: Fallback to old behavior");
                    // Fallback to old behavior if selector fails
                    // 1. Select Season
                    var seasonSelect = document.querySelector('select#cseason');
                    if (seasonSelect && seasonSelect.value !== '$seasonId') {
                        seasonSelect.value = '$seasonId';
                        seasonSelect.dispatchEvent(new Event('change', { bubbles: true }));
                    }

                    setTimeout(function() {
                        // 3. Activate online stream
                        var streamBtn = document.querySelector('.eSbox');
                        if (streamBtn) streamBtn.click();
                        
                        // 4. Exact episode selection
                        setTimeout(function() {
                            var epBtnFallback = document.querySelector('$episodeAction');
                            if (epBtnFallback) {
                                console.log("StateMachine: Clicked fallback selector");
                                if (epBtnFallback.href) { window.location.href = epBtnFallback.href; } else { epBtnFallback.click(); }
                            } else {
                                console.log("StateMachine: Could not find episode button");
                            }
                        }, 500);
                    }, 500);
                } catch(e) {
                    console.error("StateMachine failed:", e);
                }
            })();
        """.trimIndent()
        webView.evaluateJavascript(script, null)
    }
}
