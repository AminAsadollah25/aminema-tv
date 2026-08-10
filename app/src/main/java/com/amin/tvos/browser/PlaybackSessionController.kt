package com.amin.tvos.browser

import android.app.Activity
import android.util.Log
import android.webkit.WebView
import org.json.JSONObject

/** Coordinates native episode selection with the provider's own page controls. */
class PlaybackSessionController(
    private val activity: Activity,
    private val webView: WebView
) {
    private var isPlayingEpisode = false

    fun markEpisodePlaybackStarted() {
        isPlayingEpisode = true
    }

    fun handleBackPress(isFullscreen: Boolean): Boolean {
        if (!isPlayingEpisode) return false
        if (isFullscreen) {
            webView.evaluateJavascript(
                "document.exitFullscreen ? document.exitFullscreen() : " +
                    "(document.webkitExitFullscreen ? document.webkitExitFullscreen() : null);",
                null
            )
        }
        // Skip the provider's auto-next history and return to the native navigator.
        activity.finish()
        return true
    }

    /**
     * Accepts only Aminema's semantic DOM actions. It never accepts, stores or logs a
     * download/media URL. A missing exact target leaves the normal provider page open.
     */
    fun executeFilmRoozStateMachine(
        seasonId: String,
        quality: String,
        episodeAction: String
    ) {
        val action = JSONObject.quote(episodeAction)
        val requestedSeason = JSONObject.quote(seasonId)
        val provider = when {
            episodeAction.startsWith("#parsiflix-") -> "parsiflix"
            episodeAction.startsWith("#filmrooz-") -> "filmrooz"
            episodeAction.startsWith("#mymoviz-") -> "mymoviz"
            else -> "invalid"
        }
        Log.d(
            "PlaybackSession",
            "Episode selection requested: provider=$provider season=$seasonId quality=${quality.ifBlank { "default" }}"
        )

        val script = """
            (function() {
              function clean(value) {
                return String(value || '').replace(/\s+/g, ' ').trim();
              }
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
              function episodeNumber(element) {
                if (!element) return 0;
                var label = element.querySelector &&
                  element.querySelector('[class*="_episodeNumber_"]');
                var text = toEnglishDigits(clean(label ? label.textContent : element.textContent));
                var match = text.match(/قسمت\s*(\d+)/i) ||
                  text.match(/[Ee]pisode\s*(\d+)/i) || text.match(/^(\d+)$/);
                return match ? parseInt(match[1], 10) : 0;
              }
              function visible(element) {
                if (!element) return false;
                var style = window.getComputedStyle(element);
                var rect = element.getBoundingClientRect();
                return style.display !== 'none' && style.visibility !== 'hidden' &&
                  rect.width > 0 && rect.height > 0;
              }
              function finishResult(value) {
                window._aminEpisodeClickResult = value;
                return value;
              }

              var action = $action;
              var requestedSeason = $requestedSeason;

              // ParsiFlix live DOM:
              // div[class*="_seasonItem_"] -> div[class*="_episodeItem_"]
              // episode number and its watch button are siblings inside the row.
              var parsi = action.match(/^#parsiflix-s(\d+)-epnum-(\d+)$/);
              if (parsi) {
                var parsiSeasonIndex = parseInt(parsi[1], 10);
                var parsiEpisode = parseInt(parsi[2], 10);
                function attemptParsi(retriesLeft) {
                  var seasons = Array.from(
                    document.querySelectorAll('[class*="_seasonItem_"]')
                  );
                  if (!seasons.length) {
                    seasons = Array.from(document.querySelectorAll(
                      '[class*="season"],[class*="Season"],[data-season]'
                    )).filter(function(element) {
                      return element.querySelectorAll('[class*="_episodeItem_"]').length > 0;
                    });
                  }
                  var season = seasons[parsiSeasonIndex];
                  if (!season) {
                    if (retriesLeft > 0) {
                      setTimeout(function() { attemptParsi(retriesLeft - 1); }, 400);
                    } else {
                      finishResult('NOT_FOUND:season');
                    }
                    return;
                  }
                  var rows = Array.from(
                    season.querySelectorAll('[class*="_episodeItem_"]')
                  );
                  var row = rows.find(function(candidate) {
                    return episodeNumber(candidate) === parsiEpisode;
                  });
                  if (!row) {
                    if (retriesLeft > 0) {
                      setTimeout(function() { attemptParsi(retriesLeft - 1); }, 400);
                    } else {
                      finishResult('NOT_FOUND:episode');
                    }
                    return;
                  }
                  var button = row.querySelector('button,a,[role="button"]');
                  if (!button) {
                    finishResult('NOT_FOUND:button');
                    return;
                  }
            // One user action must produce one click. Double-dispatching MouseEvent
                  // plus click() raced the provider's Continue behavior.
                  button.click();
                  finishResult('CLICKED:parsiflix');
                }
                attemptParsi(10);
                return 'scheduled:parsiflix';
              }

              // MyMoviz modern DOM:
              // .mv-tv__season[data-season] selects a season and
              // #mv-tv-panel details.mv-ep contains the episode. The provider's
              // visible play links live under .mv-epaudio--dubbed or
              // .mv-epaudio--original. We click that normal site control only;
              // no href, download URL or media value is read.
              var my = action.match(/^#mymoviz-s(\d+)-epnum-(\d+)$/);
              if (my) {
                var mySeason = my[1];
                var myEpisode = parseInt(my[2], 10);

                function qualityRank(text) {
                  var normalized = toEnglishDigits(clean(text));
                  if (/2160p/i.test(normalized)) return 0;
                  if (/1080p/i.test(normalized)) return 3;
                  if (/720p/i.test(normalized)) return 2;
                  if (/480p/i.test(normalized)) return 1;
                  return 0;
                }

                function bestPlayControl(container) {
                  if (!container) return null;
                  var candidates = Array.from(
                    container.querySelectorAll('.mv-eprow')
                  ).map(function(row) {
                    return {
                      rank: qualityRank(row.textContent),
                      button: row.querySelector('a.mv-eprow__btn--play')
                    };
                  }).filter(function(candidate) { return !!candidate.button; });
                  candidates.sort(function(a, b) { return b.rank - a.rank; });
                  return candidates.length ? candidates[0].button : null;
                }

                function attemptMyMoviz(retriesLeft) {
                  var seasonButton = document.querySelector(
                    '.mv-tv__season[data-season="' + mySeason + '"]'
                  );
                  if (!seasonButton) {
                    if (retriesLeft > 0) {
                      setTimeout(function() { attemptMyMoviz(retriesLeft - 1); }, 450);
                    } else {
                      finishResult('NOT_FOUND:season');
                    }
                    return;
                  }

                  if (!seasonButton.classList.contains('is-active')) {
                    seasonButton.click();
                    setTimeout(function() { attemptMyMoviz(retriesLeft); }, 500);
                    return;
                  }

                  // The tab's active class changes before the async panel is
                  // replaced. Do not read the old season's rows during that
                  // window; the provider exposes the authoritative season on
                  // each visible tracking button.
                  var panelRows = Array.from(
                    document.querySelectorAll('#mv-tv-panel details.mv-ep')
                  ).filter(function(candidate) {
                    var track = candidate.querySelector('.mv-eptrack[data-season]');
                    return track && track.getAttribute('data-season') === mySeason;
                  });
                  if (!panelRows.length) {
                    if (retriesLeft > 0) {
                      setTimeout(function() { attemptMyMoviz(retriesLeft - 1); }, 450);
                    } else {
                      finishResult('NOT_FOUND:season-panel');
                    }
                    return;
                  }

                  var rows = panelRows;
                  var row = rows.find(function(candidate) {
                    var track = candidate.querySelector(
                      '.mv-eptrack[data-season="' + mySeason + '"][data-track-ep="' +
                        myEpisode + '"]'
                    );
                    return !!track && episodeNumber(candidate) === myEpisode;
                  });
                  if (!row) {
                    if (retriesLeft > 0) {
                      setTimeout(function() { attemptMyMoviz(retriesLeft - 1); }, 450);
                    } else {
                      finishResult('NOT_FOUND:episode');
                    }
                    return;
                  }

                  row.open = true;
                  var dubbed = bestPlayControl(
                    row.querySelector('.mv-epaudio--dubbed')
                  );
                  var original = bestPlayControl(
                    row.querySelector('.mv-epaudio--original')
                  );
                  var target = dubbed || original ||
                    row.querySelector('a.mv-eprow__btn--play');
                  if (!target) {
                    if (retriesLeft > 0) {
                      setTimeout(function() { attemptMyMoviz(retriesLeft - 1); }, 450);
                    } else {
                      finishResult('NOT_FOUND:play-control');
                    }
                    return;
                  }

                  target.click();
                  finishResult('CLICKED:mymoviz');
                }

                attemptMyMoviz(15);
                return 'scheduled:mymoviz';
              }

              // FilmRooz live DOM:
              // #cseason_N -> quality block -> .eSbox -> DIV[onclick] "قسمت N".
              var film = action.match(/^#filmrooz-s(\d+)-box(\d+)-epnum-(\d+)$/);
              if (film) {
                var filmSeason = film[1];
                var qualityBlockIndex = parseInt(film[2], 10);
                var filmEpisode = parseInt(film[3], 10);
                var activationAttempted = false;

                function attemptFilm(retriesLeft) {
                  var select = document.querySelector('select#cseason');
                  if (select && select.value !== filmSeason) {
                    select.value = filmSeason;
                    select.dispatchEvent(new Event('change', {bubbles:true}));
                    setTimeout(function() { attemptFilm(retriesLeft); }, 450);
                    return;
                  }

                  var season = document.getElementById('cseason_' + filmSeason);
                  if (!season) {
                    if (retriesLeft > 0) {
                      setTimeout(function() { attemptFilm(retriesLeft - 1); }, 450);
                    } else {
                      finishResult('NOT_FOUND:season');
                    }
                    return;
                  }
                  var blocks = Array.from(season.children).filter(function(block) {
                    return !!block.querySelector('.eSbox');
                  });
                  var block = blocks[qualityBlockIndex];
                  var streamBox = block && block.querySelector('.eSbox');
                  if (!block || !streamBox) {
                    if (retriesLeft > 0) {
                      setTimeout(function() { attemptFilm(retriesLeft - 1); }, 450);
                    } else {
                      finishResult('NOT_FOUND:quality');
                    }
                    return;
                  }

                  var target = Array.from(streamBox.children).find(function(candidate) {
                    return episodeNumber(candidate) === filmEpisode;
                  });
                  if (!target) {
                    if (retriesLeft > 0) {
                      setTimeout(function() { attemptFilm(retriesLeft - 1); }, 450);
                    } else {
                      finishResult('NOT_FOUND:episode');
                    }
                    return;
                  }

                  // Some FilmRooz seasons keep .eSbox hidden until the normal
                  // "پخش آنلاین" control in the same quality block is activated.
                  if (!visible(target) && !activationAttempted) {
                    var activation = Array.from(block.querySelectorAll(
                      'button,a,[role="button"],[onclick]'
                    )).find(function(candidate) {
                      return clean(candidate.innerText || candidate.textContent) === 'پخش آنلاین';
                    });
                    if (activation) {
                      activationAttempted = true;
                      activation.click();
                      setTimeout(function() { attemptFilm(retriesLeft); }, 600);
                      return;
                    }
                  }

                  // The exact provider control owns the normal navigation to its player.
                  target.click();
                  finishResult('CLICKED:filmrooz');
                }
                attemptFilm(10);
                return 'scheduled:filmrooz';
              }

              return finishResult('REJECTED:invalid-action');
            })();
        """.trimIndent()
        webView.evaluateJavascript(script) { result ->
            // Result is a semantic state only (CLICKED/NOT_FOUND/REJECTED),
            // never a provider URL or media value.
            Log.d("PlaybackSession", "Episode state-machine result=$result")
        }
    }
}
