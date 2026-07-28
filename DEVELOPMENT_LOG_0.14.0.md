# Aminema 0.14.0 — Development Log

## Status

- Candidate version: `0.14.0`
- `versionCode`: `28`
- Package installed for acceptance: `com.amin.tvos.debug`
- Release status: **not published yet**
- Previous public release remains `v0.13.0`
- Local RC asset: `Aminema-v0.14.0-RC-debug.apk`
- SHA-256:
  `37a699b70c28bc21669e4d0d4f26254cd7cd336ebfda75af6efbd46ebcf24681`

## Product result

0.14.0 turns the static series catalog into a lightweight **Series Pulse**:

1. FilmRooz series are ordered by newly released episode, using
   `/archive/series/` rather than the unrelated “new TV shows” category.
2. Every FilmRooz series card whose source exposes release metadata shows only
   the spoiler-safe part, for example `قسمت ۰۴ فصل سوم`.
3. Provider-curated popular series are a separate
   `سریال‌های برگزیده جهان` rail; they are never mixed with “latest”.
4. A new `سریال‌های من` rail is built from signed-in account Recent/Continue
   membership plus local Aminema sessions.
5. Iranian and international refreshes are independent, automatic on cold
   start, non-modal, and manually repeatable per provider.
6. Cached cards remain usable during refresh. Only the relevant row button
   changes to a small red spinner.
7. Account Continue/Recent reconciliation now rides in the same invisible
   provider pass. The old automatic full-screen Account Sync interruption is
   gone; the explicit manual Sync action remains available.

## Important truth about watched ticks

The FilmRooz watched checkmarks were tested with the same signed-in account on
the laptop and Android emulator:

- Silo season 3 showed episodes 1–3 checked in the supplied screenshot.
- After episode 4 was watched on the laptop, the emulator still showed no
  corresponding authoritative update.
- The Handmaid’s Tale season 6 was also incomplete across devices: episodes
  4–5 were checked, while episodes 1–3 had been watched elsewhere.

Conclusion: the checkmarks are browser/device-local hints, not reliable
account-wide watched history. Aminema must therefore distinguish:

- `قسمت ۰۴ فصل سوم` = latest **published** episode (safe and exact)
- `۲ قسمت تازه` = catalog delta since a known local baseline (future)
- `۲ قسمت دیده‌نشده` = allowed only with exact watched evidence (not currently
  available cross-device)

0.14.0 intentionally does not invent “unwatched” counts.

## Architecture

### `browser/CatalogBackgroundSync.kt`

- One tiny, non-focusable WebView per active provider job.
- Cold-start jobs are serialized: Iranian finishes or times out before
  international starts.
- Manual provider jobs remain independent.
- Home is never replaced and no mouse/DPAD focus is taken.
- 35-second provider-local timeout and stale-cache fallback.
- Same signed-in Cookie/DOM/local-storage session as BrowserActivity.
- Strict same-host normal content-page validation at the JS bridge.
- ParsiFlix pass:
  - account home `جدیدترین‌ها`
  - typed movie/series catalog calls
  - account Continue membership
- FilmRooz pass:
  - `/archive/category/new-films/`
  - `/archive/series/`
  - `/archive/playlist/show/most-popular-tv-shows/`
  - account Recent membership from `/user/panel/`

The obsolete modal `CatalogSyncActivity` and its manifest registration were
removed.

### Data/UI changes

- `CatalogItem.episodeLabel`
- `CatalogSection.popularSeries`
- `CatalogRepository.refreshingServices`
- `HomeViewModel.mySeries`
- `CatalogSectionRow` supports:
  - per-provider spinner
  - cached-content continuity
  - optional fixed rail data without filters
- `CatalogCard` reserves one stable metadata line below the title.
- `HomeScreen` adds `سریال‌های من` and
  `سریال‌های برگزیده جهان`.

## Provider limitations

FilmRooz exposes season/episode status directly in its archive card and is the
authoritative source for the new label.

ParsiFlix’s catalog-list response exposes title, type, description, genres and
artwork but no season/episode value. 0.14.0 leaves that line empty rather than
making extra detail requests that could touch unrelated playback data or
guessing a number.

## Security boundary

- Only title, kind, poster, normal same-host detail URL, and release label are
  stored in `catalog.json`.
- Account sync stores only ordinary content-page membership and optional local
  resume time already exposed by the website session.
- No cookies, tokens, auth headers, DRM data, `video.src`, media URLs or stream
  links cross the WebView bridge, enter logs, or persist in Aminema.
- No authentication bypass or scraping outside the user’s normal signed-in
  browser session.

## Deferred follow-up

### 0.15.0 — Cinematic Hover Preview

For international movie/series cards, after roughly 600 ms of mouse hover or
DPAD focus, show a spoiler-safe cinematic information panel:

- short synopsis
- year, genre, rating and runtime where available
- latest published episode for series
- blurred backdrop
- lightweight local metadata cache; no request on every hover

This is intentionally separate from 0.14.0 so the current release does not add
network work or memory pressure to focus navigation.

### Honest episode progress

Add a local `SeriesProgress` baseline later:

- exact Aminema playback can advance watched season/episode
- a manual “تا این قسمت دیدم” action can repair progress from other devices
- catalog deltas may say `قسمت تازه` or `فصل جدید`
- provider ticks remain optional hints, never the source of truth
