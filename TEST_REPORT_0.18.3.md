# Test Report — Aminema 0.18.3

## Automated verification

- `testDebugUnitTest`: PASS — 38 tests, 0 failures/errors
- `lintDebug`: PASS — 0 blocking errors; 125 non-blocking warnings audited separately
- `assembleDebug`: PASS
- `git diff --check`: PASS
- New update-version regression tests: 4 PASS

## Emulator verification

- AVD: `Television_1080p`, 1920×1080
- Package: `com.amin.tvos.debug`
- Installation method: in-place `adb install -r`
- Home and cinematic Hero: rendered successfully
- Current 0.18.2 release after cold/manual update check: no false update banner
- Settings: Persian RTL, scrollable, URLs LTR, update action available
- Live TV: category rows and logos rendered; repeated vertical scrolling showed no crash/ANR
- Live TV memory after scrolling stayed near its initial screen value rather than growing with the
  whole catalog (observed PSS approximately 258 MB → 251 MB in this emulator run)
- Fatal exception / ANR in final inspected log: none
- ParsiFlix general movie flow: detail-page Watch action reached `/play`; other
  tested titles remained functional after rolling back an unnecessary autoplay experiment.
- Provider-specific limitation: ParsiFlix title «بی‌داد» reached `/play` and received
  valid `206 video/mp4` responses, but Android WebView stayed at `readyState=0` with no
  duration or first frame. A sanitized 12-second probe observed 15 canceled range requests
  after about 1.3 MB. The app-wide playback path and ad blocker were not changed for this
  isolated source-file/server behavior.

## APK

- Clean candidate path:
  `~/Library/Caches/AminemaBuild/app/outputs/apk/debug/app-debug.apk`
- Final clean candidate size: 25,248,509 bytes
- SHA-256: `23b4c947b4cfed7c72d523b04e6e3b63bf8a12482ad655b373315713bc468189`

## Lint debt that is not a verified regression

Lint still reports non-blocking warnings, dominated by KTX suggestions and dependency/toolchain
age. Two artwork files flagged as unused are referenced dynamically from `services.json` and must
not be deleted.

## Physical TV follow-up (not a release blocker for this owner-approved personal build)

1. Recheck one title from ParsiFlix, FilmRooz and MyMoviz without logging in again.
2. Verify one movie direct-play and one multi-season episode selection per provider.
3. Verify ParsaTV server fallback/fullscreen on several channels.
4. Verify USB mouse hover/click/wheel and remote DPAD/OK/Back.
5. Reopen Settings and confirm destructive actions show a dialog; cancel them without deleting
   any state.
