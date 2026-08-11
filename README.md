# Aminema

A premium personal Android TV streaming hub — a Netflix-style dashboard that opens your own subscribed streaming websites in an optimized embedded browser. It hosts, scrapes, and redistributes nothing; it only renders websites with standard browser technology.

## Current release

**Aminema 0.18.0 — Live TV Playback Polish** (`versionCode 42`)

- Adds a dedicated ParsaTV Live TV destination with Persian, national, sports
  and other channel categories.
- Opens ParsaTV's first-party embed player for faster startup and keeps the
  provider page as a safe fallback when the embed is unavailable.
- Starts live playback automatically and expands HTML5, JWPlayer and
  same-origin iframe players to the TV viewport without a manual fullscreen click.
- Confirms real playback before hiding the native connection cover; a visible
  but empty player is no longer treated as success.
- Preserves provider cookies, sessions, local data and the existing WebView
  security boundary. No media URLs or tokens are extracted or stored.
- Unit tests, Lint, final APK build, mouse playback and D-pad/OK playback pass
  on the signed-in 1920×1080 TV emulator.
- Published as GitHub release `v0.18.0` with the tested APK and SHA-256 file.

## Previous release

**Aminema 0.17.1 — MyMoviz Home Entry** (`versionCode 41`)

- Adds MyMoviz beside ParsiFlix and FilmRooz in the direct service row.
- Preserves the normal browser Login path and provider sessions.

## Earlier release

**Aminema 0.16.5 — Episode Loading Hotfix** (`versionCode 37`)

- Episode loading now waits for the signed-in provider page to finish before it
  starts its hydration window, instead of racing an eight-second global timeout.
- A temporarily empty SPA result is retried at a bounded interval; the first null
  result no longer becomes an immediate false “episodes not found” error.
- The error state now offers a TV-friendly **Retry** action without leaving the
  title page.
- Silo was reproduced failing on the old timing and then verified with all three
  seasons and episode cards on the signed-in 1920×1080 emulator.
- Unit tests, Lint and the final APK build pass.
- The package id, debug signing channel, cookies, provider sessions and library data
  remain unchanged; upgrades continue to install in place.

The release status and next milestone below are updated as part of every
version's release checklist, together with `CLOUD-HANDOFF-LATEST.md`.

## Features

Netflix-style dark home screen with Continue Watching, My Services, Recently Opened, and Favorites rows. JSON-configurable services (no hardcoded websites). Persistent login sessions (cookies, DOM storage, local storage). Smart Resume that remembers the exact page, title, and poster (og:image) per service. Native HTML5 fullscreen through `WebChromeClient` with hidden system UI and hardware acceleration. Full DPAD, remote OK/Back, air-mouse, and USB-mouse support with real mouse hover, click, wheel scrolling, and side-button navigation. Switchable User-Agent (Android TV / Desktop Chrome / Mobile Chrome), remembered across launches. TV-friendly error screens with retry. Settings for managing services, QR/login scale, clearing cookies (logout), cache, and history.

Version 0.3 adds a service-first home layout with two large cinematic artwork cards. Existing installs automatically receive new bundled presentation metadata without replacing user-defined service URLs.

Version 0.3.1 declares `android.software.leanback.supports_touch=true` so
Android TV uses native pointer/cursor mode for USB/Bluetooth mice and pointer
remotes. Hover, click, wheel scrolling, and browser mouse side buttons can be
tested directly in the TV emulator or on a physical box.

Version 0.4 adds Aminema's own mouse-clickable web keyboard. It replaces
DPAD-only Android TV IMEs for website login and search fields, supports English,
Persian, numbers, email punctuation, password masking, Backspace, Space, and
Done/Search, and never reads an existing password from a website.

Version 0.4.1 adds a visible Caps Lock state and a password Show/Hide button.
Passwords remain masked by default and are revealed only while the user chooses
`Show`.

Version 0.5 turns the browser into a personal TV hub. A hidden Quick Menu opens
with MENU/INFO or mouse right-click and provides fullscreen, favorite, native
website search, reload, browser back, and Aminema Home actions. Favorites can be
saved from the current page, Continue Watching only includes detected content
pages, HTML5 playback time is saved every 15 seconds, and reopening a card
attempts a best-effort seek to the saved position. Service-specific content,
player, search, fullscreen, and excluded-route rules now live in JSON adapters.

Version 0.6 separates **Recently Opened** from **Continue Watching**. Recent
contains only recognized content-detail pages; service roots, login/category
pages, and player pages are excluded. Continue is created only after a real
HTML5 video playback event and stores the top-level browser page plus progress
(never a media/stream URL). FilmRooz resumes its stable normal player page.
ParsiFlix reopens the stable detail page and activates the site's own
`ادامه تماشا` control because its `/play` route is not reload-stable.
Authenticated FilmRooz posters are fetched inside the signed-in WebView and
saved to app-private storage, fixing missing cards without exporting cookies.

Version 0.7 adds a visible **Sync accounts** action. On demand, it imports only
the signed-in account's own Continue/Recent metadata: ParsiFlix Continue
Watching and FilmRooz Recent Watching. Local real playback sessions always win,
so their exact normal browser page and saved HTML5 position are preserved.
FilmRooz lazy-loaded authenticated posters are resolved from `data-src`, fetched
inside the signed-in WebView, and cached privately. Authentication tokens never
leave WebView and media/stream URLs are never inspected during account sync.

The mouse keyboard login flow is also fixed: a text/email field now shows
**Next**, which moves focus to the visible password field and reopens the
keyboard in masked password mode. **Done** submits password/search fields;
**Cancel**, remote Back, and mouse Back always dismiss the keyboard, clear the
website input focus, and return control to the page.

Version 0.7.1 hardens that keyboard fix for physical Android boxes. The browser
no longer creates a second native TV-IME input connection behind Aminema's
keyboard. Closing the overlay suppresses immediate focus-based reopening,
clears the active website field, and leaves focus on the app rather than
refocusing WebView. Next also re-finds a password field if a website rebuilds
its form during an input/change event and retries that transition once.

Version 0.7.2 fixes a physical-box-only Caps/language focus regression on the
FilmRooz login form. Caps now updates existing letter labels in place instead
of deleting and rebuilding the focused native key row. The browser also locks
the keyboard session to the selected password element until the user explicitly
chooses another website input. Caps, language, and Show/Hide reassert that
target after native focus settles, so a TV box restoring the first HTML field
cannot redirect password typing back into Username.

Version 0.7.3 introduces the **Aminema** identity while keeping the same
application ID and debug signing identity, so it installs as an update and
preserves cookies, sessions, settings, and the personal library. The new
mustachioed cinema mascot now appears in the launcher icon, Android TV banner,
Home header, and two dedicated cinematic cards. The Home cards are presented
as **فیلم ایرانی** and **فیلم خارجی** without showing provider brand names.
Existing installs automatically migrate the old card labels and artwork while
keeping service IDs, URLs, login data, and custom services unchanged.

Version 0.7.4 adds the **cold-start intro**: a local, offline branded video in
`app/src/main/res/raw/aminema_intro.mp4` that plays full-screen once per
process launch, above a Home screen that is already being composed underneath.
It never loops and never touches the network. OK/Enter/DPAD_CENTER/Back or a
mouse click skips it instantly, and while it is on screen it is modal, so a
skip key cannot also activate a Home card. Returning from the browser, the
account sync, or Settings does not replay it — `IntroGate` is a one-shot,
process-scoped gate and a recreated activity (configuration change or process
death) is treated as "already played". Any decoder error, missing file, or
stall falls through to Home, guarded by a prepare timeout and a hard playback
cap. `Settings ➜ Startup Intro` offers `Play intro` and `Mute intro`; these two
flags live in SharedPreferences rather than the DataStore settings, because the
decision has to be made synchronously before the first frame.

Version 0.8.0 adds the two **latest rows** to Home — «تازه‌های ایرانی» and
«تازه‌های خارجی» — each with a remembered `همه | فیلم | سریال` selector, a
refresh button and a last-sync label. Every source is the service's own view
of its own catalog, read inside the signed-in WebView adapter:
the Iranian row uses the account home's `جدیدترین‌ها` section plus the
`/medias?type=…` call the website itself makes for its movie and series pages,
and the international row uses the signed-in menu's own `فیلم های جدید` and
`سریال های جدید` pages. Each service has its own adapter and its own cache
entry in `catalog.json`, so a failing source records an error and keeps its
previously cached items instead of emptying the row or affecting the other one.
Only title, poster and the stable detail-page link are stored; provider brand
names never appear on Home.

Version 0.8.1 makes Home feel awake. The static "امشب چی می‌بینیم؟" heading is now a
live greeting computed from the local clock and the calendar: time of day, the Dutch week
(the weekend starts Friday evening and runs through Sunday, with Sunday night treated as a
school night), and occasions — Christmas Eve, Christmas, New Year's Eve, New Year, plus
Nowruz, Sizdah Bedar and Yalda via an in-app Gregorian→Jalali conversion. Each greeting
carries one action chip that actually does something (resume the last unfinished title,
filter both latest rows to movies or series, or open a random cached title), and the chip
is hidden whenever the data cannot serve it. Home also gained a cinematic backdrop built
from the last watched title's artwork: the image is fetched at a deliberately tiny size and
scaled up, which blurs it on every device including Android 9, where `Modifier.blur` is a
no-op.

Version 0.8.2 takes a film card one step further. Instead of stopping on the detail page,
the browser follows the website's own **Play online** control to its normal `/stream/...`
page, choosing a Persian dub when one exists and the height ladder 1080 → 720 → 480 (2160p
is never auto-selected). The choice reads only the numeric `h=` and `lang=` values the page
already exposes, so release labels like WEBRip or DVDRip cannot affect it, and no media
URL, signed link, DRM value or token is ever read or stored. Services that instead show a
single watch button — as the Iranian service does — get that button clicked. Titles with no
online version simply stay on their detail page. The behaviour is configured per service by
a `directPlay` block in services.json, which existing installs adopt automatically.

Version 0.12.0 replaces the old Live shortcut with a native cinematic
**پخش زنده** rail. Twenty verified channels render as TV-friendly cards with
their real logos; OK or a mouse click opens that channel's ordinary signed-in
page and sizes its visible HTML5 video to the full Android TV viewport without
an intermediate channel grid or second fullscreen click. D-pad navigation
auto-scrolls the row, and Back returns to Home with the same card focused.
Channel `id`, `name`, normal page `path`, and `logoUrl` live in the optional
`liveTv.channels` block of `services.json`; no media URL, token or DRM value is
read or stored.

Version 0.12.1 makes Continue Watching reliable and consistent across Aminema
devices. A cold process launch periodically reconciles each signed-in
website's own Continue/Recent account row; the account decides membership,
while a matching local session can still retain its exact normal player page
and HTML5 position. Continue and Direct Play no longer treat a JavaScript
`click()` as success: retries stop only after a configured player route, a
real HTML5 playback event, or native fullscreen is observed. The unavoidable
detail-page bootstrap is covered by a lightweight cinematic popcorn loading
screen and falls back safely to manual selection after 14 seconds. Every
horizontal Home/Search rail also has shared mouse- and DPAD-friendly page
arrows, and the redundant Live subtitle was removed.

Version 0.13.0 replaces both keyboard experiences. The native app Search Deck
now keeps the query, language and character count in one cinematic panel, uses
real staggered Persian/English QWERTY, provides direct `ژ` and `آ`, and folds
into a compact query bar when results appear. The WebView login Input Deck now
shows an explicit field badge, fixed-width preview, `بعدی: رمز`, password
Show/Hide and a permanent Close action. A DOM-token/native-session state
machine owns Username → Password → Submit, so stale focus or a framework form
rebuild cannot redirect password typing back into Username. Caps and language
no longer delete focused key rows, Android's system IME stays out of the flow,
and mouse hover plus DPAD focus share the same clear visual state.

Version 0.14.0, **Series Pulse**, replaces the old modal catalog sync
with independent background provider jobs. Home stays interactive, cached
cards remain visible, and only the active provider's refresh button shows a
small spinner. FilmRooz series now use `/archive/series/`, which is ordered by
new episode releases, and cards show concise provider metadata such as
`قسمت ۰۴ فصل سوم`. A native `سریال‌های من` rail reconciles title-level
account Recent/Continue membership with local Aminema sessions, while a
separate `سریال‌های برگزیده جهان` rail keeps curated shows out of “latest”.
Startup account reconciliation is bundled into the same invisible passes, so
the full-screen Sync activity no longer interrupts cold start.

FilmRooz watched checkmarks were tested across a laptop and the Android
emulator and proved browser/device-local rather than authoritative account
history. Aminema therefore shows the latest **published** episode but never
invents an “unwatched” count. ParsiFlix's catalog-list response contains no
season/episode field, so those cards stay honest and leave the line blank.

Version 0.14.5, **Cinema Polish**, fixes the SPA metadata race that could pair
a real ParsiFlix movie URL with the generic service-home title. Metadata results
now carry their DOM URL and are accepted only while the requested, current and
DOM routes still match; generic shell titles are rejected, and old malformed
items are repaired from the signed-in catalog. A 520 ms mouse-hover/DPAD-focus
dwell now opens a no-layout-shift, spoiler-safe Quick Glance with provider
synopsis, year, genres, rating, runtime and latest published episode when those
fields exist. FilmRooz extraction follows its real authenticated `.postMeta`
card structure; ParsiFlix reuses its existing catalog response. Every Home rail
now uses keyed lazy rendering. R8/resource shrinking, Persian/English locale
filtering and compressed cinema artwork reduce the update APK from about 76 MB
to about 22.2 MB without removing the offline intro or WebView features.

Version 0.14.6, **Pointer & Playback Polish**, gives USB/Bluetooth mouse hover
and DPAD focus the same borderless scale, brightness and elevation transition.
Mouse dwell now reliably drives Quick Glance. FilmRooz opts into a page-local
autoplay step after Aminema reaches the provider's normal player route; it asks
only the site's own HTML5 video or visible player control to start, while
ParsiFlix remains unchanged and the manual timeout fallback stays available.

Version 0.15.0, **Aminema Spotlight**, replaces hover-dependent decision making
with a native, TV-safe title page. It preserves the exact existing browser
request for Watch/Continue, restores the Home/Search position on Back and
enriches old titles from ordinary visible provider metadata without reading
media URLs or authentication values.

Version 0.15.1, **Cinematic Home**, turns Home into a content-first personal
cinema rather than a pair of provider doorways. A single rotating Hero chooses
from Continue, My Series and fresh Iranian/international titles, pauses while
the user interacts, crossfades the ambient artwork and opens the existing
Spotlight action. The playful provider cards are now compact direct-entry
shortcuts near the end of Home (or near the top only on a truly empty first
run). Greeting, header, mouse hover and DPAD focus use one restrained cinematic
motion language, while 220 ms backdrop dwell avoids image churn on fast remote
navigation.

Version 0.16.0 adds correct portrait/backdrop image separation, a redesigned
Hero and two provider-featured wide banner rails. Version 0.16.1 refines the
visual shell and Spotlight metadata fallback. Version 0.16.2 removes the
remaining hidden Hero work: background images are decoded at TV-appropriate
sizes, only the active slide is composed, rotation stops after rail interaction
and no hidden metadata WebView is started during carousel motion. A selected
title now uses a real portrait poster over its separate cinematic backdrop.
Duplicate catalog variants are merged instead of losing summaries. Spotlight
keeps provider data authoritative and fills only missing synopsis/year/credits
from public Wikipedia/Wikidata with identity validation and Persian labels when
available. It also adds a visible Search Back action whose remote and pointer
paths always return to Aminema Home, even if Android restored Search as the
task root.

## Next update queue — after v0.16.5.1

1. **0.16.6 — Canonical Library & Dedupe:** one card per title across
   ParsiFlix/FilmRooz, source variants, and normalized searches such as
   `spiderman` / `spider man` / `spider-man`. The local candidate now merges
   verified Search duplicates and exposes provider choices in Spotlight; the
   non-destructive Continue/Favorite/Recent bridge remains before release.
2. **0.16.7 — My Series:** followed shows, manual watched baseline, new episode
   and new season indicators, then provider-account progress where reliable.
3. **0.16.8 — Cinematic Promo Feed:** provider carousels join the new Hero;
   title promos open Spotlight and Live TV promos stay direct.
4. **0.17.0 — MyMoviz Provider:** add only archive gaps or a better Persian-dub
   variant after a 100-title overlap/coverage report.
5. **0.17.1 — Best Source Resolver:** dub-first/original-first/ask preferences
   while keeping one canonical card and never switching sources mid-play.
6. **0.17.2+ — People, Personal Home, Reliability and Geek Mode:** merged
   filmographies, person alerts, pinnable rails, provider health, and curated
   MCU/Star Wars/LOTR/Harry Potter collections.

The complete prioritized roadmap and the signed-in MyMoviz product analysis are
kept in `ROADMAP.md` and `MYMOVIZ_PRODUCT_ANALYSIS.md`.

## Requirements

Android Studio (Koala or newer), JDK 17, Android SDK 35. Target device: Android 9+ TV / box, landscape.

## Build

1. Open the `AminTVOS` folder in Android Studio and let Gradle sync (first sync downloads dependencies and generates the Gradle wrapper).
2. Debug APK: `Build > Build Bundle(s) / APK(s) > Build APK(s)`, or from a terminal: `./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`.
3. Release APK: create a keystore (`Build > Generate Signed Bundle / APK`), wire it into `signingConfigs` in `app/build.gradle.kts`, then `./gradlew assembleRelease`. Minify + resource shrinking are already configured.
4. Install on your box: `adb connect <BOX_IP>` then `adb install app-debug.apk`.

## Configuring services

Services are defined in JSON only. The bundled defaults live at `app/src/main/assets/services.json`; on first launch they're copied to the app's private storage and become editable:

- In-app: Settings → Manage Services → Add / Remove.
- Bundled defaults: edit `assets/services.json` before building. Core fields: `id`, `name`, `url`, `icon`, `color` (hex), `type` (`STREAMING`, or `LIVE_TV` for future use).
- Optional presentation fields: `subtitle` and `artwork` (drawable resource name or remote image URL).
- Optional compatibility fields: `loginZoomPercent`, `userAgent` (`TV`, `DESKTOP`, `MOBILE`), and `fullscreenSelectors`.
- Adapter fields: `playerSelectors`, `searchSelectors`, `contentUrlPatterns`,
  `playbackUrlPatterns`, `excludedUrlPatterns`, `resumeStrategy`, and
  `resumeButtonTextPatterns`. These isolate site-specific behavior while
  keeping the browser and library shared.
- Optional Live TV rail: `liveTv.channels[]` with `id`, `name`, `path`, and
  `logoUrl`. `path` must be a normal page route owned by that service, never a
  media/stream URL.

The bundled FilmRooz entry currently points to the user-provided subscribed site.
You can replace any service URL through Settings or JSON without changing code.

## Architecture

```
com.amin.tvos
├── AminTvApp.kt            Application + lightweight service locator
├── MainActivity.kt         Compose NavHost (home / settings)
├── data/
│   ├── model/Models.kt     StreamingService, MovieItem, PlaybackSession, ResumeStrategy
│   ├── model/CatalogModels CatalogItem (including episode label) + provider sections
│   ├── ServicesRepository  JSON-file-backed service list (StateFlow)
│   ├── LibraryRepository   Smart Resume / recents / favorites store
│   ├── CatalogRepository   Per-provider cached rails + background refresh state
│   └── SettingsRepository  DataStore preferences (User-Agent)
├── ui/
│   ├── theme/              Dark cinematic Material 3 theme
│   ├── components/         FocusableCard, PosterCard, ServiceCard, SectionRow, ErrorScreen
│   ├── home/               HomeScreen + HomeViewModel
│   └── settings/           SettingsScreen + SettingsViewModel
└── browser/
    ├── BrowserActivity     Playback bridge, poster cache, resume, native fullscreen
    ├── CatalogBackgroundSync Non-modal, independent catalog/account provider jobs
    ├── AccountSyncActivity Visible, user-triggered signed-in account history sync
    ├── ServiceAdapter      JSON-driven page/player/search rules per service
    ├── QuickMenuOverlay    Hidden MENU/INFO/right-click browser controls
    ├── MouseKeyboardOverlay Mouse keyboard with Caps and password Show/Hide
    └── TvErrorView         TV-friendly error overlay with retry
```

Design decisions: MVVM with repositories exposed as StateFlow; JSON-file persistence instead of Room for fast startup and low RAM on Android boxes (swappable later without UI changes); `ServiceType.LIVE_TV` and the poster/`MovieItem` model prepare the architecture for future Live TV, TMDB metadata, and manual catalogs.

## Navigation model

Back inside the browser: exits fullscreen video first, then steps back through WebView history, then returns to the Aminema home screen. DPAD focus and USB-mouse hover both scale cards with a red glow.

### Browser rules in v0.2

- No floating zoom or fullscreen controls cover the website.
- Catalog and player pages always render at 100%.
- Only login/QR routes use the configured reduced scale (85% by default).
- QR dialogs and late-mounted SPA players are detected with a lightweight `MutationObserver`.
- Clicking the website player's fullscreen button uses Android's native `WebChromeClient` custom view.
- `MENU` or `INFO` opens the hidden Quick Menu; mouse right-click does the same.
- Red color key, `F11`, or long-press OK requests player fullscreen directly.
- A USB mouse is the primary pointer: left click and wheel go directly to WebView; Back/Forward side buttons navigate browser history.
- Quick Menu search focuses the website's own search field and opens the Aminema
  mouse keyboard; no catalog scraping or cross-service search is performed.

## Security

The app never bypasses authentication, extracts protected streams, or scrapes sites. SSL errors are surfaced, never silently ignored. File and content access are disabled in the WebView.
