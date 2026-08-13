# Stability Audit — Aminema 0.18.3

This audit separates verified defects from uncertain or high-regression-risk work. The latter was
deliberately not changed in this stabilization candidate.

## Baseline

- The 0.18.2 baseline already passed unit tests, Lint and APK assembly.
- No cookie, login, WebStorage, app data, library, catalog or emulator profile was cleared.
- Existing untracked maintenance scripts and backups were excluded from this candidate.

## Verified defects fixed

### High

1. **False update availability** — semantic release tags were compared to unrelated Android
   version-code numbers.
2. **Unverified update installation** — a missing or malformed SHA file previously allowed the
   downloaded APK to continue; updates now fail closed.
3. **Concurrent local-data overwrite risk** — catalog, library and service read/modify/write
   operations could race and lose a newer write.
4. **Hidden WebView lifecycle leak** — leaving Home during a queued catalog refresh could start
   another invisible WebView after destruction.

### Medium

5. **Live TV eager composition** — whole categories and unbounded logo images could be retained
   while scrolling.
6. **Compose state mutation during composition** — the catalog genre reset changed state while UI
   was rendering.
7. **One-click destructive settings actions** — logout, history deletion and service removal had
   no confirmation.
8. **Persian service ID failure** — a Persian-only service name could produce a blank identifier.
9. **Cookie-clear completion race** — the UI reported completion before WebView's asynchronous
   cookie deletion callback.
10. **Mouse hover inconsistency** — some Android TV pointer stacks did not emit Compose hover
    interactions; raw enter/exit is now a non-consuming fallback.

### Low / polish

11. Settings and browser status text mixed English/LTR content into Persian screens.
12. Common English genres leaked into Persian Hero, Spotlight, hover and filter surfaces.
13. TV banner density scaling and Android 13 monochrome launcher support were incomplete.
14. A tracked obsolete Kotlin backup file remained in production source control.

## Investigate separately — not changed

### High risk / requires physical-TV acceptance

1. **Provider playback automation:** the large WebView/DOM state machines are fragile by nature.
   They were not refactored without characterization tests. Verify movie and multi-season playback
   on ParsiFlix, FilmRooz and MyMoviz while already signed in.
2. **Live TV SSL exception and cleartext allowance:** current ParsaTV playback depends on provider
   behavior. Tightening these settings without channel-by-channel testing could break streams.
3. **No automated signed-in end-to-end tests:** unit tests cannot prove provider login, cookies,
   episode choice, fullscreen, remote Back or server fallback on a physical box.

### Medium

4. **Debug signing/minify configuration:** Gradle warns that debug is both debuggable and minified,
   so code optimization/obfuscation is disabled. Changing the installation channel could break
   in-place updates and must be planned separately.
5. **Dependency/toolchain age:** Compose, Android Gradle and related libraries have newer versions;
   upgrading all at once is not a safe stabilization change.
6. **Gradle 10 compatibility:** deprecated Gradle behavior remains and needs a focused toolchain
   audit before a future Gradle upgrade.
7. **Large source files:** BrowserActivity, catalog sync and Home UI need incremental extraction
   backed by regression tests, not a broad rewrite.
8. **Home vertical composition:** the growing Home surface may eventually benefit from a lazy
   vertical container, but TV focus restoration must be characterized first.

### Low / optional optimization

9. **Intro video size:** the intro is the largest APK asset. Re-encoding could materially reduce
   package size but needs visual/audio comparison on TV.
10. **125 Lint warnings:** 89 are KTX style suggestions; others include dependency notices,
    custom-lint compatibility and known dynamic-resource false positives. They are not proof of a
    runtime defect and should be handled by category.
11. **Dynamic artwork warnings:** two images reported as unused are referenced from JSON and must
    not be removed.

## Candidate evidence

- Clean `testDebugUnitTest`: 38 tests, 0 failures/errors.
- Clean `lintDebug`: 0 errors, 125 warnings.
- Clean `assembleDebug`: passed.
- `git diff --check`: passed.
- APK: 25,248,509 bytes.
- SHA-256: `23b4c947b4cfed7c72d523b04e6e3b63bf8a12482ad655b373315713bc468189`.

This document does not claim physical-TV provider playback is verified; those acceptance checks
remain mandatory before publishing 0.18.3.
