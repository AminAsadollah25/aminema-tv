# Aminema 0.17.1

## MyMoviz login entry

- Added MyMoviz beside the existing ParsiFlix and FilmRooz service banners at the bottom of Home.
- Clicking the MyMoviz banner opens the provider's normal home page in the existing BrowserActivity,
  so login and website inspection use exactly the same flow as the other services.
- Login, cookies, Local Storage and WebView sessions remain managed by the normal provider flow.
- No authentication bypass or protected media access was added.

## MyMoviz episode playback stabilization

- Season and episode selection now uses the provider's authoritative season and episode data.
- A stale previous-season panel can no longer route a selection to the wrong season.
- Verified on the emulator: Silo season 3 episode 3 opened as season 3 episode 3.

## Validation

- `testDebugUnitTest`: passed
- `lintDebug`: passed with no reported issues
- `assembleDebug`: passed
- APK installation used replace-install only; app data, cookies and signed-in sessions were preserved.
