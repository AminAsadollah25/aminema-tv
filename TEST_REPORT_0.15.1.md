# Aminema 0.15.1 — test report

Test target: Android TV 1920×1080 emulator with the existing debug application
data preserved through `adb install -r`.

## Build gates

- Candidate: `0.15.1`, `versionCode 32`, `com.amin.tvos.debug`.
- `clean testDebugUnitTest lintDebug assembleDebug`: successful.
- Unit tests: 10 passed, 0 failed, 0 errors.
- Android Lint: completed with 0 blocking errors.
- Minified/shrunk APK: `22,544,296` bytes.
- SHA-256:
  `dacc292ba98cef9d0202f0edab1c485f025b80d6a25b5a8e00907f77c5b7a305`.

## Emulator QA

- `adb install -r` succeeded and retained the existing app data.
- Offline Aminema intro completed normally.
- Home rendered with compact brand bar, one-line smart greeting, complete
  cinematic Hero and the beginning of the next rail visible at 1920×1080.
- Real cached FilmRooz content populated the Hero and authenticated poster
  requests rendered.
- Provider doorways appeared as compact cards near the end of Home.
- No FATAL EXCEPTION was observed.

## Regression boundaries

- Hero opens the existing Spotlight payload.
- Browser, login, Continue, direct play, Live TV and service URLs were not
  changed.
- Existing rail arrows remain available; fixed two-card service dock suppresses
  unnecessary arrows.
- Cold-empty Home still exposes provider entry near the top.

## Physical Android Box acceptance still required

- Confirm overscan and typography.
- Confirm DPAD focus growth and pause-on-Hero interaction.
- Confirm real USB mouse hover and click.
- Confirm both provider login sessions survive the release APK update.
