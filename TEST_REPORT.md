# Aminema v0.8.0 — test report

Tested on the Android TV 1080p emulator (Android TV API 36) using the debug
build and the user's existing authenticated ParsiFlix and FilmRooz sessions.

## v0.8.0 latest-catalog rows

Build: `clean assembleDebug` with JDK 17 — successful. Version `0.8.0`,
`versionCode 15`, package and debug signing certificate unchanged.

### Sources confirmed against the live signed-in accounts

Nothing here was guessed. Each endpoint/path was observed first with a
throwaway debug probe against the signed-in sites, then implemented:

- ParsiFlix combined latest = the account home's own `جدیدترین‌ها` section.
- ParsiFlix per type = `GET /medias?type=MOVIE|SERIES&page=1&size=20`, which
  is the exact call the website itself issues when its own `/medias/movies`
  and `/medias/series` pages are opened (captured by hooking the site's
  fetch/XHR and clicking its own navigation).
- FilmRooz movies = `فیلم های جدید` → `/archive/category/new-films/`.
- FilmRooz series = `سریال های جدید` → `/archive/category/new-tv-show/`.
  Read from the signed-in menu, which resolves the open question about the
  international series path.

### Verified

- Both adapters synced: ParsiFlix 10 combined / 20 movies / 20 series;
  FilmRooz 8 movies / 15 series / 20 combined. The FilmRooz movie count is
  the site's own page-1 count — the other 8 `/post/` links on that page are
  its sidebar series block and are correctly excluded by type.
- Titles, posters and types render correctly on the TV layout, including
  FilmRooz posters, which only load with the signed-in cookie.
- Every stored link is a normal detail page (`/medias/{movies,series}/<id>`
  and `/post/{film,series}/<id>/<slug>/`). No media URL, stream URL, DRM
  value or token appears anywhere in `catalog.json`.
- Opening a card opened exactly that title's normal detail page in
  `BrowserActivity` while signed in; Back returned to the same Home state.
- `همه | فیلم | سریال` switches the row live, is written to settings, and
  survives a full app restart.
- The last-sync label renders and updates ("هم‌اکنون", "۳ دقیقه پیش").
- Empty state before the first sync shows the refresh prompt rather than a
  blank row.
- Adapter isolation: with the ParsiFlix endpoint deliberately pointed at an
  unreachable host, that row recorded `Service unavailable`, **kept its
  previously cached 10/20/20 items**, and the FilmRooz row refreshed normally.
  A later successful sync cleared the error.
- Logcat contains no `FATAL EXCEPTION` across sync, filtering, card opening
  and the induced failure.
- The debug-only probe used for discovery is not part of the shipped build:
  the final APK's manifest and dex contain no `CatalogProbeActivity`.

### Known limitations

- FilmRooz answers an unknown category path with HTTP 200 and a generic
  page instead of a 404. If the site ever renames those two categories, the
  adapter would return plausible-looking but wrong items rather than an
  error. Title extraction was hardened for this case (it now requires the
  title link to point at the same item and rejects episode-status text), but
  a category rename still needs to be caught by eye.
- An offline test via emulator airplane mode was inconclusive because the
  emulator keeps its host NAT connection; the failure path was therefore
  verified with a forced unreachable endpoint instead.
- Auto-refresh is intentionally not implemented: only the manual refresh
  button goes to the network.

## v0.7.4 cold-start intro

Build: `clean assembleDebug` with JDK 17 (Android Studio JBR) — successful, no
errors. Package `com.amin.tvos.debug`, version `0.7.4`, `versionCode 14`.
The debug signing certificate SHA-256 is byte-identical to the installed
v0.7.3 APK (`ba6ac8c4…38ba13d3`), so this build updates the TV install in
place. `res/raw/aminema_intro.mp4` ships inside the APK at 10,635,285 bytes,
stored uncompressed.

Verified on the emulator:

- Cold start plays the intro full-screen at 1920×1080, 16:9, once. Both closing
  texts (`تقدیم به فارسی‌زبانان کهکشان…` and `AMINEMA`) render inside the TV
  safe area.
- The video does not loop; Home opens by itself when playback completes
  (~7 s).
- `DPAD_CENTER`/OK skips instantly and lands on Home. The same key press does
  **not** leak through to Home — no service card is opened.
- `Back` skips instantly and does not exit the app.
- A mouse click anywhere skips instantly.
- Opening a service card and returning with Back shows Home immediately — no
  replay. Leaving to the TV launcher and reopening the app also does not
  replay it (same process).
- Turning **Settings ➜ Startup Intro ➜ Play intro** off makes the next cold
  start go straight to Home; the flag persists in `intro_prefs.xml`.
- Error fallback: a deliberately corrupted `aminema_intro.mp4` (40 KB of random
  bytes) was packaged and installed. Home appeared in about one second, no
  crash, and no system "can't play this video" dialog. Only the expected
  `MediaPlayer Error (1,-2147483648)` line appears in logcat.
- Logcat contains no `FATAL EXCEPTION` for the app across every intro run.
- Home, Settings, service cards, browser launch and Back are unchanged.

Not verifiable in the emulator, left for the physical box:

- Real remote/mouse key codes for skip on the Android Box.
- Audio level against the device's own media volume.
- Decoder behaviour on the weaker TV hardware.

## v0.7.3 brand update

- App label changed to **Aminema**.
- Launcher icon, Android TV banner, Home logo, and About name now use the new
  mustachioed Aminema mascot.
- Two new 16:9 Home artworks present the configured destinations as
  **فیلم ایرانی** and **فیلم خارجی**; provider names are no longer shown on
  the main cards.
- Existing `services.json` files migrate only the known legacy names,
  subtitles, and bundled artwork. Service IDs, URLs, order, login/session data,
  and user-added services remain unchanged.
- Existing library items resolve their visible service label from the current
  JSON configuration without rewriting saved content URLs.
- Package remains `com.amin.tvos.debug`; version is `0.7.3` (`versionCode 13`),
  allowing installation over v0.7.2.

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
- Root cause hardened for physical boxes: the native Android TV IME could keep
  its own WebView input connection, consume Back/Next, and re-focus the same
  HTML field behind the Amin keyboard. Browser WebView now exposes no native
  IME connection; all mouse-keyboard input stays in the explicit page bridge.
- The terminal Done/close path no longer requests WebView focus immediately
  after dismissal, which previously could reopen the same website input.

### Caps Lock returned typing to Username

- Root cause: Caps rebuilt and removed the entire native key row, including the
  clicked/focused Caps button. Some Android TV boxes then restored WebView's
  first HTML input (`username`).
- Caps now changes existing English key labels in place; no focused key row is
  removed.
- The selected password DOM element is locked as the keyboard's write target.
  A programmatic/native fallback to Username is rejected unless the user
  explicitly clicks another website input.
- Caps, language rebuild, and Show/Hide reinforce Password immediately and
  again after the native focus transition settles.

## Verified

- Gradle `clean assembleDebug`: successful.
- APK install/replace and launch: successful. Emulator package data showed the
  original `firstInstallTime` and a newer `lastUpdateTime`, confirming an
  in-place update rather than a fresh install.
- Package metadata reports `com.amin.tvos.debug`, version `0.7.3`, code `13`,
  minimum Android 9, and application label `Aminema`.
- APK signing certificate SHA-256 is identical to v0.7.2, so Android accepts it
  as an authenticated update to the installed debug build.
- New Home header, both mascot banners, Persian labels, Settings service names,
  and dynamic About version rendered correctly at 1920×1080.
- Clicking the Iranian-film card opened `BrowserActivity`; Back returned to the
  same Aminema Home state.
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
- The test used the same plain HTML field structure as FilmRooz's public login
  form (`username`, `password`, submit) without real credentials.
- No system IME package appeared while the Amin keyboard was open.
- Remote Back closed the keyboard, cleared its tracked input, kept the page
  open, and remained closed after the focus-suppression window.
- Clicking Password again with the mouse reopened the correct masked keyboard.
- Cancel also closed the keyboard, cleared the tracked input, and did not
  reopen automatically.
- A form-replacement stress case is retried once so Next can target newly
  mounted username/password nodes.
- The real logged-out FilmRooz login page was tested with dummy values:
  `Username → Next → Password → Caps → A → فا → ض`.
- A simulated old-box focus fallback to Username was injected during both Caps
  and language changes. Visible focus and the locked target returned to
  Password; Username stayed `a` and Password became `Aض`.
- Back then cleared the target, focused the page body, and kept the keyboard
  closed. No crash appeared in final logcat.
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
