# Aminema 0.14.6 — test report

Test target: Android TV 1920×1080 emulator with the existing authenticated
ParsiFlix and FilmRooz WebView data preserved through `adb install -r`.

## Build gates

- Installed candidate: `0.14.6`, `versionCode 30`,
  `com.amin.tvos.debug`.
- `clean testDebugUnitTest lintDebug assembleDebug`: successful.
- Unit tests: 6 passed, 0 failed, 0 errors.
- Android Lint: 0 errors; 99 non-blocking advisory warnings.
- Minified/shrunk APK: `22,232,968` bytes.
- SHA-256:
  `08889d3e65ac170a35c1806668bdcdec5b0154237749f49415ef968301fe011e`.
- Signing certificate matches 0.14.5:
  `ba6ac8c4c2e1828462e7a6b122ad60856a054a18389af36bc001a1ee38ba13d3`.

## Physical mouse interaction

- Injected a real mouse-source Move over a Home poster.
- The card lifted and brightened without a red border.
- After the dwell, Quick Glance opened with the expected cached metadata.
- Moving the mouse away closed Quick Glance.
- Click behaviour remained separate from hover.

Evidence:

- `work/qa-0.14.6/mouse-hover-leviticus.png`
- `work/qa-0.14.6/mouse-hover-exit.png`

## FilmRooz autoplay

Opened the real signed-in Leviticus movie card from Home with a mouse click.
Aminema selected the normal 1080p/original provider option and reached the
provider's top-level `/stream/...` page.

After 25 seconds:

- the loading overlay was gone;
- the video was visibly playing;
- the normal Playback bridge reported position `20,438 ms`;
- duration was `5,288,872 ms`;
- the saved playback page was a normal top-level FilmRooz page;
- no FATAL EXCEPTION was present.

This is stronger than a click-only test: the increasing HTML5 playback position
proves that the player actually started.

Evidence:

- `work/qa-0.14.6/filmrooz-autoplay-click-25s.png`

## Regression boundaries

- ParsiFlix does not enable `autoPlayOnPlaybackPage`; its existing working path
  is unchanged.
- Old service JSON without the new field decodes with autoplay disabled.
- If FilmRooz cannot mount or start its player, retries stop at the existing
  preparation timeout and the normal manual page remains usable.

## Security verification

No media source URL, stream request, cookie, token, password, authentication
header or DRM value was inspected or logged. The test observed only the normal
top-level page URL and playback position/duration already used by Continue
Watching.
