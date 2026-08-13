# Development Log — Aminema 0.18.3

## Goal

Stabilization and cinema polish without changing provider DOM automation, playback rules,
login/session identity or persisted user-library formats.

## Baseline before changes

- Git state was inspected before editing; existing untracked maintenance scripts were left
  untouched and are not part of this candidate.
- `testDebugUnitTest`, `lintDebug` and `assembleDebug` passed on the 0.18.2 baseline.
- 33 unit tests existed. Lint reported warnings but no blocking error.
- Signed-in emulator state was preserved; no uninstall, clear-data, cookie deletion or WebStorage
  deletion was used.

## Verified defects fixed

1. **False update banner**
   - GitHub release 0.18.2 had no `versionCode:` line.
   - The old fallback converted tag `0.18.2` to `1802` and compared it with Android code `44`,
     so the installed release was incorrectly presented as newer.
   - Fallback now compares semantic version with semantic version. An explicit published Android
     version code remains the preferred source of truth.
   - The updater also chooses the debug-signed APK deterministically when several APK assets are
     present, matches its exact `.sha256` companion and refuses an unverified APK.

2. **Hidden catalog WebView lifecycle leak**
   - Destroying Main while a deeper refresh was queued could launch another hidden WebView.
   - The coordinator now closes before releasing slots and cannot create work after destruction.

3. **Concurrent repository writes**
   - Catalog, library and service repositories had independent read/modify/write coroutines that
     could overwrite a newer state.
   - Small per-repository mutexes now serialize only app-private disk mutation.

4. **Live TV eager composition / image decode**
   - The screen composed all channel cards at once and requested logos without a decode bound.
   - Categories now use lazy vertical rows, four channels per row, and 320×180 bounded logo loads.

5. **Compose state mutation during composition**
   - Catalog genre reset ran directly while composing.
   - It now runs from `LaunchedEffect` when the available genre set changes.

6. **Settings safety and RTL defects**
   - Destructive remove-service, logout and clear-history actions were one click.
   - They now require explicit confirmation and explain what will be removed.
   - The screen is Persian/RTL while technical URLs remain correctly LTR.
   - Cookie-clear completion now fires only after WebView confirms deletion.
   - Persian service names now get a valid ID from their URL host instead of a blank ID.

7. **TV/mouse focus feedback**
   - Raw pointer enter/exit is a fallback for Android TV stacks that do not emit Compose hover
     interaction reliably.
   - Focus animation uses a softer one-pixel glow, bounded elevation and spring motion rather
     than a heavy white/red frame.

8. **Packaging hygiene**
   - A tracked obsolete `SpotlightScreen.kt.bak` was removed.
   - TV banner moved to `drawable-nodpi` to avoid density rescaling.
   - Android 13+ themed launcher icon receives a small TV/play monochrome mark.
   - A clean package build removed about 10 MB of stale incremental APK tail data; clean candidate
     size is about 25.25 MB.

9. **Persian metadata consistency**
   - Common English provider genres are translated only at presentation time across Hero,
     Spotlight, hover preview and catalog filters.
   - Stored provider metadata and matching keys remain unchanged, avoiding catalog regressions.

## Deliberately not changed

- Provider DOM selectors, episode selection, direct-play priorities, SSL exception behavior,
  cookies, WebStorage and stored playback/library schema.
- Large-file/module refactoring, dependency upgrades, target SDK upgrade, release signing channel
  migration and intro re-encoding. These require separate acceptance gates.

## Primary files

- `CatalogBackgroundSync.kt`
- `CatalogRepository.kt`, `LibraryRepository.kt`, `ServicesRepository.kt`
- `CatalogLibraryScreen.kt`, `LiveTvActivity.kt`, `TvComponents.kt`
- `SettingsScreen.kt`, `SettingsViewModel.kt`
- `UpdateRepository.kt`, `ReleaseVersionPolicy.kt`
- `AndroidManifest.xml` and launcher/banner resources

## Data safety

No application data was cleared. Emulator QA used only in-place `adb install -r`; all existing
provider cookies, logins, WebStorage, Continue, Favorites and local catalog/library files remained.

## Final provider playback triage

- General ParsiFlix playback remained functional; an experimental provider-wide autoplay change
  was fully reverted before release.
- The isolated ParsiFlix title «بی‌داد» correctly reached `/play` and its server returned
  `206 video/mp4`, but Android WebView never advanced beyond `readyState=0`.
- A sanitized probe recorded range-response churn without inspecting or storing the protected
  media URL, cookies or tokens. Because other titles work, no global playback rule was changed.
