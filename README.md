# Aminema

A premium personal Android TV streaming hub — a Netflix-style dashboard that opens your own subscribed streaming websites in an optimized embedded browser. It hosts, scrapes, and redistributes nothing; it only renders websites with standard browser technology.

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
of its own catalog, read inside the signed-in WebView by `CatalogSyncActivity`:
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

## Next update queue (not implemented in v0.8.1)

- **Keyboard redesign (0.9.1):** explicit
  `Username → Password → Caps → Language → Show/Hide → Done` state machine.
- **Unified search (0.9.0):** one Home search bar with `ایرانی | خارجی` result groups,
  reusing the 0.8.0 catalog adapters.
- Neither will inspect or store protected media/stream URLs.

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

The bundled FilmRooz entry currently points to the user-provided subscribed site.
You can replace any service URL through Settings or JSON without changing code.

## Architecture

```
com.amin.tvos
├── AminTvApp.kt            Application + lightweight service locator
├── MainActivity.kt         Compose NavHost (home / settings)
├── data/
│   ├── model/Models.kt     StreamingService, MovieItem, PlaybackSession, ResumeStrategy
│   ├── ServicesRepository  JSON-file-backed service list (StateFlow)
│   ├── LibraryRepository   Smart Resume / recents / favorites store
│   └── SettingsRepository  DataStore preferences (User-Agent)
├── ui/
│   ├── theme/              Dark cinematic Material 3 theme
│   ├── components/         FocusableCard, PosterCard, ServiceCard, SectionRow, ErrorScreen
│   ├── home/               HomeScreen + HomeViewModel
│   └── settings/           SettingsScreen + SettingsViewModel
└── browser/
    ├── BrowserActivity     Playback bridge, poster cache, resume, native fullscreen
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
