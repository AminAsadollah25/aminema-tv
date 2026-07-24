# Amin TV OS v0.5.0 — MVP test report

Tested on the Android TV 1080p emulator (Android TV API 36) using the debug build.

## Verified

- Gradle `assembleDebug`: successful.
- App installation and launch: successful.
- Existing ParsiFlix session persisted after APK replacement.
- Home → Continue Watching with an Android `mouse` input source: successful.
- ParsiFlix detail → Continue playback with a `mouse` input source: successful.
- Player page renders at 100% with no Amin TV zoom overlay.
- Website fullscreen control enters native fullscreen through `WebChromeClient`.
- Android Back exits native fullscreen.
- Remote `MENU` shortcut requests fullscreen successfully.
- Home-screen headings and card content use the correct white-on-dark colors.
- Both cinematic service cards fit side-by-side at 1920×1080.
- Existing service data is enriched with artwork while preserving saved URLs.
- Native TV pointer mode is enabled with `android.software.leanback.supports_touch`.
- FilmRooz login fields open the Amin TV OS mouse keyboard.
- Keyboard letter buttons accept emulator touchscreen/mouse-equivalent clicks.
- Password input is masked by default in the native overlay.
- Show/Hide reveals and masks the currently typed password without submitting it.
- Caps Lock changes the English layout and typed characters to uppercase, with a visible active indicator.
- Rebuilding the keyboard for Caps Lock or language changes no longer closes the browser.
- Existing authenticated ParsiFlix and FilmRooz sessions persist after the v0.5 APK replacement.
- MENU opens the hidden Quick Menu without covering the website during normal browsing.
- Quick Menu fits at 1920×1080 and is clickable with a mouse.
- Quick Menu receives DPAD focus even after mouse input; DPAD Down + OK activates actions.
- ParsiFlix native search is opened from Quick Menu and automatically launches the Amin TV mouse keyboard.
- The current FilmRooz movie page can be added to Favorites and reports the new state when the menu is reopened.
- Returning Home displays the saved movie in Continue Watching with the favorite indicator.
- Service home, login, profile, and direct image/asset URLs are excluded from new library entries.
- Player/search/content/excluded-route adapter rules migrate into existing service configuration without replacing saved URLs.
- HTML5 position/duration capture and best-effort resume-seek code compile and run without WebView crashes.

## Real box acceptance checks

Run these on the target Android Box before marking v0.5 stable:

1. Pair/login with the ParsiFlix QR page at 85% scale.
2. Verify USB mouse click, hover, wheel, and Back side button.
3. Play at least 20 minutes and test pause/resume after app interruption.
4. Test native fullscreen and Back with the physical remote.
5. Reboot the box and confirm the ParsiFlix login session persists.
6. Start an HTML5 title, watch for two minutes, return Home, and reopen its
   Continue Watching card to verify whether that site's player permits seeking.
