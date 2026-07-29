# Aminema 0.15.0 — test report

Test target: Android TV 1920×1080 emulator with the existing authenticated
provider WebView state preserved through `adb install -r`.

## Build gates

- Candidate: `0.15.0`, `versionCode 31`, `com.amin.tvos.debug`.
- `testDebugUnitTest lintDebug assembleDebug`: successful.
- Unit tests: 10 passed, 0 failed, 0 errors.
- Android Lint: 0 errors; 108 non-blocking advisory issues.
- Minified/shrunk APK: `22,462,380` bytes.
- SHA-256:
  `0cc3742aa1b3de12e0681ed61a61a781f0f9c49a6e647c660a50102dec2ae6ee`.
- Signing certificate SHA-256:
  `ba6ac8c4c2e1828462e7a6b122ad60856a054a18389af36bc001a1ee38ba13d3`
  (same update identity as 0.14.6).

## Real provider metadata probe

FilmRooz title:
`https://sean.robert-redford.net/post/film/33337/spider-man-homecoming/`

The fresh on-demand pass produced:

- year `2017`;
- genres `اکشن، ماجراجویی، علمی-تخیلی`;
- rating `۷.۴`;
- runtime `۱۳۳ دقیقه`;
- country `آمریکا`;
- languages `انگلیسی، اسپانیایی`;
- normal provider synopsis;
- director `Jon Watts`;
- cast `Tom Holland`, `Michael Keaton`, `Robert Downey Jr.`.

The final loaded screen kept poster, title metadata, two-line synopsis, credits
and both actions visible in the 1080p safe area. No placeholder “details later”
sentence remained.

FilmRooz's real Spider-Man: No Way Home page was separately inspected and the
same title-local rules returned its normal synopsis and `دوبله فارسی=true`.
Global category navigation was excluded.

## Navigation and regression checks

- Home/Search/catalog/Continue/Recent/Favorites route to Spotlight.
- Live TV still routes directly to BrowserActivity.
- Watch/Continue payload serialization preserves exact playback strategy.
- Back returns to the prior Activity/rail state.
- My List uses the existing library repository.
- No FATAL EXCEPTION appeared in the final real-title probe.
- The async metadata focus crash was reproduced, fixed and not observed again.

## Security verification

Only ordinary same-host title metadata and person profile links crossed the JS
bridge. No protected media URL, stream request, token, password, cookie,
authentication header or DRM value was read or logged.

## Remaining physical-device acceptance

- Confirm typography and overscan on the user's Android Box/TV.
- Confirm DPAD focus, USB mouse click and Back restoration.
- Confirm dub/subtitle badges on several known dubbed/subtitled titles.
- Confirm both provider logins remain valid after installing with `-r`.
