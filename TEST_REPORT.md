# Amin TV OS v0.7.0 — test report

Tested on the Android TV 1080p emulator (Android TV API 36) using the debug
build and the user's existing authenticated ParsiFlix and FilmRooz sessions.

## v0.7 fixes

### Account history from other devices

- ParsiFlix stores signed-in account history on its own API. The app now reads
  only the account's Continue section and builds stable `/medias/.../<id>`
  detail links.
- FilmRooz exposes signed-in account Recent Watching in `/user/panel/`. The app
  now imports those stable `/post/...` detail links.
- Existing local playback sessions retain their playback page and exact
  same-device HTML5 position when account metadata is merged.
- Cross-device FilmRooz history supplies the content page but not an exact
  episode/quality/time. The website remains responsible for that selection.

### FilmRooz lazy poster

- Off-screen Recent cards use a grey SVG in `src/currentSrc`; their real poster
  is in `data-src`.
- Sync now prefers and resolves `data-src`, fetches authenticated images inside
  WebView, validates host/type/size, and caches them in app-private storage.

### Login keyboard deadlock

- **Next** on username/email updates the field and moves focus to the next
  visible password input.
- The overlay reopens in password mode with masking, Show/Hide, and **Done**.
- Cancel, remote Back, and mouse Back dismiss the keyboard and release the
  focused website input, so the login page can always be used again.

## Verified

- Gradle `clean assembleDebug`: successful.
- APK install/replace and launch: successful.
- Existing authenticated sessions survive APK replacement.
- User-triggered account sync completed with 8 ParsiFlix and 11 FilmRooz rows.
- 19 merged Continue sessions rendered; 17 were account-synced and the two
  existing local playback sessions retained their saved progress.
- ParsiFlix sync used only stable detail URLs and account metadata.
- FilmRooz sync used only stable detail URLs and account Recent metadata.
- Lazy FilmRooz posters resolved correctly and rendered on Home; authenticated
  images were cached privately when available.
- Synthetic two-field login test: username **Next** focused the password input,
  retained the username value, and reopened the masked password keyboard.
- Remote Back closed the keyboard, cleared its tracked input, focused WebView,
  and did not leave an unresponsive overlay.
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
- Account tokens remained inside WebView during sync.
- No protected media URL is read, stored, logged, or displayed.
- No app crash was found in final emulator logcat.

## Physical Android Box acceptance checks

1. Play one title on each service, return Home, and reopen both Continue cards.
2. Confirm physical USB mouse hover/click/wheel and remote DPAD/OK/Back.
3. Interrupt a long playback, reopen the app, and verify the saved position.
4. Reboot the box and confirm both login sessions and private posters persist.
5. Verify fullscreen and Back behavior with the physical remote.
