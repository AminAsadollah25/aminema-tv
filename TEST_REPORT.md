# Amin TV OS v0.2 — MVP test report

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

## Real box acceptance checks

Run these on the target Android Box before marking v0.2 stable:

1. Pair/login with the ParsiFlix QR page at 85% scale.
2. Verify USB mouse click, hover, wheel, and Back side button.
3. Play at least 20 minutes and test pause/resume after app interruption.
4. Test native fullscreen and Back with the physical remote.
5. Reboot the box and confirm the ParsiFlix login session persists.
