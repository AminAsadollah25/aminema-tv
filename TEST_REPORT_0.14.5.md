# Aminema 0.14.5 — test report

Tested on the Android TV 1920×1080 emulator with the user's existing authenticated ParsiFlix and FilmRooz WebView sessions. Installed with `adb install -r`; package data and login state were preserved.

## Build quality

- Installed version: `0.14.5`, `versionCode 29`, package `com.amin.tvos.debug`.
- `clean testDebugUnitTest lintDebug assembleDebug`: successful.
- Unit tests: 4 passed, 0 failed, 0 errors.
- Android Lint: 0 errors. Remaining warnings are non-blocking dependency/SDK advisory warnings.
- `git diff --check`: clean.
- Minified/shrunk APK: approximately 22,216,560 bytes.
- No `FATAL EXCEPTION` observed after install, cold launch, background provider sync, rail navigation or BrowserActivity launch.

## Recently Opened regression

- Reproduced the old corrupt ParsiFlix record: a real `/medias/movies/357` URL paired with the generic service homepage title.
- The signed-in catalog contained the same URL as `دو روز دیرتر` with its correct poster.
- On 0.14.5, the stored legacy item was automatically repaired to that title/poster.
- New metadata capture rejects a result when the requested URL, returned DOM URL and current WebView route differ.
- A recognized content URL carrying a generic provider shell title is not recorded.

## Hover / focus metadata

- ParsiFlix sync returned 10 combined latest, 24 movie and 24 series items with title summaries and available year/genre metadata.
- FilmRooz sync returned 24 combined items, 8 movies and 16 series in the observed account session.
- A real FilmRooz card produced title, spoiler-safe synopsis, year, genre, rating (`۶.۷`) and runtime (`۸۸ دقیقه`).
- Quick Glance appeared after DPAD focus dwell without moving the rail. Mouse hover uses the same focus path.
- Missing fields degrade to a short neutral prompt instead of an empty or broken panel.

## Performance / interaction

- Home, catalog and Live rails use lazy keyed lists.
- Rail arrows disable at their boundaries and page by the number of visible cards.
- Services, Continue, My Series, latest, popular, Live, More, Recent and Favorites remain navigable with DPAD.
- The existing physical-mouse hover/click interaction path remains attached to `FocusableCard`.
- APK size is about 71% smaller than v0.14.0 while retaining the offline intro and all browser features.

## Security verification

The implementation and test used only normal page URLs and visible/provider-supplied title metadata. No media URL, stream request, password, cookie, token, authentication header or DRM value was inspected, persisted or logged.

## Physical Android-box acceptance

1. Update over 0.14.0 and confirm both website logins remain active.
2. Hover a fresh Iranian and foreign card with the USB mouse; verify Quick Glance appears after roughly half a second and disappears on leave.
3. Open several Iranian titles and verify only actual titles—not the service shell—appear in `اخیراً بازشده`.
4. Page through long rails with both shared arrows and DPAD.
5. Test Continue and fullscreen playback once per provider to catch device-specific WebView behavior.
