# Amin TV OS

A premium personal Android TV streaming hub — a Netflix-style dashboard that opens your own subscribed streaming websites in an optimized embedded browser. It hosts, scrapes, and redistributes nothing; it only renders websites with standard browser technology.

## Features

Netflix-style dark home screen with Continue Watching, My Services, Recently Opened, and Favorites rows. JSON-configurable services (no hardcoded websites). Persistent login sessions (cookies, DOM storage, local storage). Smart Resume that remembers the exact page, title, and poster (og:image) per service. Native HTML5 fullscreen through `WebChromeClient` with hidden system UI and hardware acceleration. Full DPAD, remote OK/Back, air-mouse, and USB-mouse support with real mouse hover, click, wheel scrolling, and side-button navigation. Switchable User-Agent (Android TV / Desktop Chrome / Mobile Chrome), remembered across launches. TV-friendly error screens with retry. Settings for managing services, QR/login scale, clearing cookies (logout), cache, and history.

Version 0.3 adds a service-first home layout with two large cinematic artwork cards. Existing installs automatically receive new bundled presentation metadata without replacing user-defined service URLs.

Version 0.3.1 declares `android.software.leanback.supports_touch=true` so
Android TV uses native pointer/cursor mode for USB/Bluetooth mice and pointer
remotes. Hover, click, wheel scrolling, and browser mouse side buttons can be
tested directly in the TV emulator or on a physical box.

Version 0.4 adds Amin TV OS's own mouse-clickable web keyboard. It replaces
DPAD-only Android TV IMEs for website login and search fields, supports English,
Persian, numbers, email punctuation, password masking, Backspace, Space, and
Done/Search, and never reads an existing password from a website.

Version 0.4.1 adds a visible Caps Lock state and a password Show/Hide button.
Passwords remain masked by default and are revealed only while the user chooses
`Show`.

Version 0.5 turns the browser into a personal TV hub. A hidden Quick Menu opens
with MENU/INFO or mouse right-click and provides fullscreen, favorite, native
website search, reload, browser back, and Amin TV Home actions. Favorites can be
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
    ├── ServiceAdapter      JSON-driven page/player/search rules per service
    ├── QuickMenuOverlay    Hidden MENU/INFO/right-click browser controls
    ├── MouseKeyboardOverlay Mouse keyboard with Caps and password Show/Hide
    └── TvErrorView         TV-friendly error overlay with retry
```

Design decisions: MVVM with repositories exposed as StateFlow; JSON-file persistence instead of Room for fast startup and low RAM on Android boxes (swappable later without UI changes); `ServiceType.LIVE_TV` and the poster/`MovieItem` model prepare the architecture for future Live TV, TMDB metadata, and manual catalogs.

## Navigation model

Back inside the browser: exits fullscreen video first, then steps back through WebView history, then returns to the Amin TV home screen. DPAD focus and USB-mouse hover both scale cards with a red glow.

### Browser rules in v0.2

- No floating zoom or fullscreen controls cover the website.
- Catalog and player pages always render at 100%.
- Only login/QR routes use the configured reduced scale (85% by default).
- QR dialogs and late-mounted SPA players are detected with a lightweight `MutationObserver`.
- Clicking the website player's fullscreen button uses Android's native `WebChromeClient` custom view.
- `MENU` or `INFO` opens the hidden Quick Menu; mouse right-click does the same.
- Red color key, `F11`, or long-press OK requests player fullscreen directly.
- A USB mouse is the primary pointer: left click and wheel go directly to WebView; Back/Forward side buttons navigate browser history.
- Quick Menu search focuses the website's own search field and opens the Amin TV
  mouse keyboard; no catalog scraping or cross-service search is performed.

## Security

The app never bypasses authentication, extracts protected streams, or scrapes sites. SSL errors are surfaced, never silently ignored. File and content access are disabled in the WebView.
