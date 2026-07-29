# Aminema 0.15.0 — development log

## Objective

Replace hover-dependent movie decisions with a premium native title page that
works equally well with DPAD and mouse, while preserving every proven provider
playback path.

## Product decisions

- First click opens Spotlight; second click performs Watch/Continue.
- Iranian and international movies/series share one native layout.
- Live TV remains direct one-click.
- No Download and no Trailer action.
- Series episode selection is intentionally deferred until provider controls
  and honest watched evidence are fully mapped.
- Provider pages remain the source of truth for normal metadata.
- Missing optional data is omitted instead of replaced with an excuse.

## Architecture

### Activity boundary

`SpotlightActivity` is separate from Home/Search navigation. This makes Back
restore the exact previous Activity state, including rail position and focus.
`SpotlightItem` carries both display metadata and the exact already-tested
`BrowserActivity` request:

- content and browser start URLs;
- resume position/duration;
- auto-resume and direct-play flags;
- resume strategy and provider action patterns.

Spotlight therefore cannot accidentally change playback semantics.

### Title metadata enrichment

`SpotlightMetadataLoader` mounts a 2×2 non-focusable WebView only while a title
page is being completed. It reuses normal provider cookies and user agent,
accepts only the configured same-host content URL and destroys itself after a
result or timeout.

The loader reads visible/schema.org metadata:

- synopsis, year, genre, rating and runtime;
- country and language;
- director and principal cast with same-host person profile URLs;
- title-local Persian dub/subtitle indicators.

`CatalogRepository` stores up to 150 canonical `TitleMetadata` entries in
`title_metadata.json`. Entries are normalized and reused for 14 days. This
solves old Recent/Continue cards that predate the richer catalog cache without
adding old titles to Latest rows.

### Provider catalog enrichment

The normal background catalog adapters now also accept country, language,
Persian dub/subtitle flags and person credits when exposed by the provider.
FilmRooz uses its real `.postMeta` fields. ParsiFlix maps optional ordinary API
fields conservatively.

### TV UI

- cinematic backdrop and portrait poster;
- title with year in the same title block;
- compact metadata chips;
- green dubbed and blue Persian-subtitle states;
- two-line synopsis;
- director and up to four principal cast names;
- Watch/Continue and My List actions;
- fixed 720p/1080p safe-area spacing.

The empty synopsis message was removed. During a real fetch only a small
“در حال تکمیل اطلاعات…” state appears; after completion genuinely missing
sections disappear.

## Important bug fixes found during QA

1. A non-remembered `FocusRequester` could target a detached tree when metadata
   recomposed the page. It is now remembered across recompositions.
2. The first FilmRooz synopsis selector was too narrow. The real detail element
   is `.col-12.mt-2.p-2.text-justify.rounded`; the loader now reads it directly.
3. Old titles did not have a year to place beside the title. `TitleMetadata`
   now includes year, rating, runtime and genres, not only synopsis/credits.
4. Multi-language fields now join linked values with Persian commas instead of
   producing text such as `انگلیسیاسپانیایی`.
5. Progress below one minute is treated as a website beacon and hidden from the
   visual progress section.

## Files added

- `data/model/SpotlightModels.kt`
- `ui/spotlight/SpotlightActivity.kt`
- `ui/spotlight/SpotlightScreen.kt`
- `ui/spotlight/SpotlightViewModel.kt`
- `ui/spotlight/SpotlightMetadataLoader.kt`
- `app/src/test/.../SpotlightItemTest.kt`

## Main files changed

- `AndroidManifest.xml`
- `app/build.gradle.kts`
- `data/model/CatalogModels.kt`
- `data/CatalogRepository.kt`
- `browser/CatalogBackgroundSync.kt`
- `ui/home/HomeScreen.kt`
- `ui/search/SearchActivity.kt`
- `ui/components/CatalogCard.kt`
- `ui/home/CatalogSectionRow.kt`
- `README.md`, `ROADMAP.md`, `CLOUD-HANDOFF-LATEST.md`

## Security boundary

No media/stream URL, network request, cookie, token, password, authentication
header or DRM value is inspected, logged or persisted. Person links and title
URLs must be ordinary same-host provider pages.

## Next milestones

1. `0.15.1 — Spotlight Series & People`: season/episode actions, clickable
   cast/director filmography and local following.
2. `0.15.2 — Cinema Library & Alerts`: View All grids plus restrained local
   alerts for new catalog works from followed people.
3. Short post-release acceptance on the user's physical Android Box.
