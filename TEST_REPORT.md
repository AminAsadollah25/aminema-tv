# Amin TV OS v0.6.0 — test report

Tested on the Android TV 1080p emulator (Android TV API 36) using the debug
build and the user's existing authenticated ParsiFlix and FilmRooz sessions.

## Root-cause diagnosis

### FilmRooz poster

- FilmRooz detail pages do not publish an `og:image`.
- The visible poster is a same-origin image that returns HTML without the
  authenticated WebView session but returns a valid image inside that session.
- A normal image loader therefore could not display it reliably.
- Fix: discover only the current detail page's visible poster, fetch it with
  `credentials: include` inside WebView, validate type/size/host, and save it
  to app-private poster storage. Cookies and image bytes are not exported.

### ParsiFlix resume

- ParsiFlix uses a stable `/medias/.../<id>` detail route.
- Its `/play` route is transient and redirects to the service root after a
  reload, so saving `/play` cannot provide reliable Continue Watching.
- Fix: save the stable detail route and, on Continue, activate the website's
  own visible `ادامه تماشا` button. The website remains responsible for its
  episode and playback state.

### Recent-page pollution

- An older generic content pattern treated `/play` as content.
- An overly broad ParsiFlix exclusion pattern also matched valid detail pages.
- Fix: migrate away from both legacy patterns and use explicit content,
  playback, root, login, category, and excluded-route rules.

## Verified

- Gradle `clean assembleDebug`: successful.
- APK install/replace and launch: successful.
- Existing authenticated sessions survive APK replacement.
- FilmRooz detail poster is cached privately and renders on Home.
- FilmRooz Continue opens the stable normal `/stream/...` page and restored the
  HTML5 position (tested around 425 seconds).
- ParsiFlix detail is recorded with the visible title and poster.
- A real ParsiFlix HTML5 playback event creates a Continue session with:
  stable detail page as content, `/play` as browser playback page, and
  `CLICK_SITE_CONTINUE` strategy.
- ParsiFlix Continue reopens its detail page, activates the site's own Continue
  control, reaches `/play`, and restored playback near the saved position.
- Continue Watching is populated only by real video playback events.
- Recently Opened contains only explicit detail routes.
- Service roots, login/profile/category pages, `/play`, and FilmRooz `/stream`
  pages are absent from Recently Opened.
- Continue progress bars and both authenticated posters render correctly.
- No protected media URL is read, stored, logged, or displayed.
- No app crash was found in final emulator logcat.

## Physical Android Box acceptance checks

1. Play one title on each service, return Home, and reopen both Continue cards.
2. Confirm physical USB mouse hover/click/wheel and remote DPAD/OK/Back.
3. Interrupt a long playback, reopen the app, and verify the saved position.
4. Reboot the box and confirm both login sessions and private posters persist.
5. Verify fullscreen and Back behavior with the physical remote.
