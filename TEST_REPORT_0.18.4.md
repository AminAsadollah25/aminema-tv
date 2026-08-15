# Test Report — Aminema 0.18.4

## Automated verification

- `:app:testDebugUnitTest`: PASS — 41 tests, 0 failures, 0 errors.
- `:app:lintDebug`: PASS — 0 errors, 133 non-blocking warnings; warnings are
  existing localization/accessibility/KTX debt and do not block this release.
- `:app:assembleDebug`: PASS — APK produced with `versionCode 46` and
  `versionName 0.18.4`.
- `git diff --check`: PASS.

## Emulator verification

- AVD: `Television_1080p`, 1920×1080.
- Package: `com.amin.tvos.debug`.
- Installation: in-place `adb install -r`; no app data, Cookie, Login or WebStorage
  was cleared.
- Live TV opened without Crash/ANR.
- Initial Live TV state showed `287` raw channels, `282` unique channels after
  deduplication, and background progress without blocking the screen.
- After the first checks, the primary `فعال` tab showed confirmed active channels
  and the `همه کانال‌ها` tab remained available for the complete audit list.
- Cards visibly reported `فعال` and `فعلاً فعال نیست`; the tab counts and progress
  updated while the page remained usable.
- No fatal exception was found in the inspected emulator Logcat window.

## Remaining real-device verification

1. Let the complete background scan finish on the physical Android Box; 287 raw
   records can take time because each channel gets one deliberate attempt.
2. Open one active channel from the primary tab and verify normal provider playback,
   fullscreen and Back behavior.
3. Confirm the All tab is useful for temporarily unavailable channels after a
   provider-side outage.

## APK

- Candidate: `~/Library/Caches/AminemaBuild/app/outputs/apk/debug/app-debug.apk`
- Size: `25,331,897` bytes
- SHA-256: `4d169b01f059ed9d7c34ee5cf132669000165c864b6e262152c16bcfe6de09fd`

versionCode: 46
