# Aminema 0.14.5 — Cinema Polish

`versionCode: 29`

## What changed

- Fixed the ParsiFlix SPA race that could save the service homepage title under a real movie URL in **اخیراً بازشده**.
- Existing malformed Recent entries are repaired from the signed-in catalog when possible; otherwise generic shell entries stay hidden.
- Added a spoiler-safe cinematic **Quick Glance** after a short mouse-hover or DPAD-focus dwell.
- Quick Glance shows the provider's normal title metadata when available: synopsis, year, genres, rating, runtime and latest published episode label.
- FilmRooz metadata extraction now follows its real signed-in archive-card DOM; ParsiFlix uses the metadata already returned by its catalog response.
- All Home horizontal rails now use lazy rendering, preserving the shared left/right paging controls while composing and decoding only visible cards.
- Applied R8/code shrinking and resource shrinking to the installed debug update channel, kept every WebView JavaScript bridge explicitly, and packaged only Persian/English locales.
- Compressed the two cinema artwork cards without changing their dimensions.
- Localized the remaining Home labels and strengthened the visual Quick Glance panel.

## Performance

- APK reduced from **75,975,092 bytes** in v0.14.0 to approximately **22.2 MB** (about **71% smaller**) without removing the local intro video or browser features.
- Lazy rails reduce startup composition work, poster decoding and memory pressure on low-RAM Android boxes.

## Safety

Aminema still reads only ordinary signed-in catalog/detail-page metadata and opens normal website pages. It does not read, extract or store media URLs, stream requests, DRM values, authentication tokens or passwords.

## Install

Install this APK over the existing `com.amin.tvos.debug` app. The package ID and debug signing identity are unchanged, so cookies, logins, settings, history and cached posters remain intact.
