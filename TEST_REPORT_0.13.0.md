# Aminema 0.13.0 — test report

Candidate tested on Android TV AVD at 1920×1080 without logging out the user's
existing ParsiFlix or FilmRooz sessions.

## Build and package

- Version: `0.13.0`
- Version code: `27`
- Package: `com.amin.tvos.debug`
- Minimum Android: 9 / API 28
- `clean assembleDebug`: successful
- `lintDebug`: successful, zero errors
- APK size: `75,958,740` bytes
- APK SHA-256:
  `acd6ed8d43df33f2c4813e603efe4e1f474622305129f05ea320c5da136e3b91`
- Signing certificate SHA-256:
  `ba6ac8c4c2e1828462e7a6b122ad60856a054a18389af36bc001a1ee38ba13d3`
- Certificate is identical to 0.12.1.

## In-place update

Installed 0.13.0 with `adb install -r` over installed 0.12.1:

- before: version `0.12.1`, code `26`
- after: version `0.13.0`, code `27`
- `firstInstallTime` stayed `2026-07-26 22:21:38`
- `lastUpdateTime` advanced
- launch succeeded and no `FATAL EXCEPTION` appeared
- existing package data was not cleared

## Native app Search Deck

- Full Persian keyboard and action row fit inside 1920×1080 without scrolling.
- Persian and English use staggered QWERTY rows.
- Persian `ژ` and `آ` are directly reachable.
- Mouse entry of `ام` updated the integrated preview and `2/60` counter.
- Search CTA remained disabled below two characters and became active red at two.
- Submit collapses the deck into a compact editable query bar.
- Empty result rails are not rendered before the first real search.
- Key hover/focus states and result-rail navigation remain available.

## WebView Input Deck

A temporary debug-only launcher opened a fully local synthetic login form inside
the production BrowserActivity/WebView path. It contained no real credentials
and no network request. The form synchronously replaced its Username element on
`change` to reproduce SPA/framework form replacement.

Verified sequence:

1. Mouse click opened the deck in `نام کاربری`.
2. Entered `amin`.
3. `بعدی` survived Username node replacement and opened `رمز عبور`.
4. Caps + `a` entered `A`.
5. Language switch + Persian `ض` produced `Aض` in Password only.
6. Show displayed `Aض`; Hide/masking remained available.
7. `ورود` submitted the local form and dismissed the deck.
8. The page displayed its synthetic success message.
9. Remote Back dismissed the deck once and kept BrowserActivity open.
10. Android system IME was not shown; served input connection was null.

The temporary launcher and debug manifest entry were deleted before the final
APK. Package resolution confirmed `No activity found` for the removed harness.

## Architecture assertions

- Every editable DOM node receives a page-local WeakMap token.
- Every native deck open receives a monotonic session id.
- Value, mode, action and dismiss callbacks carry the session id.
- Stale sessions and non-explicit target changes are rejected.
- Same-target `focusin` cannot reset an open Password buffer.
- Caps and language do not remove/rebuild focused key rows.
- Next locks Password before focus events can race.
- Next/Submit transitions have bounded timeout and retry behavior.

## Privacy and security

- Password values entered during the test were synthetic.
- Existing real service accounts were not logged out.
- Existing website passwords are never read from WebView into native UI.
- No cookie, token, authentication header, media URL, stream request or DRM
  value was read, stored, logged or exported.

## Physical Android Box acceptance

After GitHub updater installation:

1. Open app Search and verify Persian/English QWERTY with USB mouse and DPAD.
2. If a real login is naturally available, check Username → Next → Password.
3. Toggle Caps, language and Show without returning to Username.
4. Confirm Back and `بستن` each close the deck with one action.
5. Confirm existing ParsiFlix/FilmRooz sessions remain signed in.

