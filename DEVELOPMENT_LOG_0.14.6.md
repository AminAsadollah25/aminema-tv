# Aminema 0.14.6 — development log

## Objective

Finish the interaction and playback polish before beginning the larger
`0.15.0 — Aminema Spotlight` redesign.

## Product decisions

- ParsiFlix autoplay was already correct and must not change.
- FilmRooz needs one more step after normal direct-play navigation: start the
  provider's own player.
- Selection must not look like a red debug outline. DPAD focus and physical
  mouse hover use the same gentle scale/lift language.
- Quick Glance remains spoiler-safe and opens only after a short dwell.
- Movie click behaviour stays unchanged in 0.14.6. Native detail pages belong
  to 0.15.0.

## Implementation

### Pointer and focus

`FocusableCard` now combines:

- DPAD focus state;
- Compose `HoverInteraction`;
- explicit pointer Enter/Exit tracking as a fallback for Android TV mouse
  stacks that do not emit the interaction source consistently;
- pressed-state feedback.

The red border was removed. Selection animates scale, container brightness,
elevation and z-order. Poster cards use a 1.06 scale, while larger service cards
use a quieter 1.035 scale. Lazy rails gained 16dp vertical content padding to
avoid clipping.

### FilmRooz player start

`DirectPlayConfig` gained the backward-compatible
`autoPlayOnPlaybackPage=false` field. Only FilmRooz enables it in
`services.json`.

After a user-requested Direct Play or Continue action reaches a configured
`/stream/...` route, `BrowserActivity` retries a small page-local action:

1. enable controls and call `play()` on the page's own HTML5 video; or
2. click a visible JW Player, Video.js or Plyr play control.

The preparation overlay remains until a real HTML5 `play` report, native custom
view, or confirmed already-playing state. Existing timeout/manual fallback is
preserved.

## Files changed

- `app/build.gradle.kts`
- `app/src/main/assets/services.json`
- `data/model/Models.kt`
- `browser/BrowserActivity.kt`
- `ui/components/TvComponents.kt`
- `ui/components/CatalogCard.kt`
- `ui/home/CatalogSectionRow.kt`
- `app/src/test/.../DirectPlayConfigTest.kt`

## Security boundary

The implementation never reads `video.src/currentSrc`, network requests,
protected stream URLs, cookies, tokens, passwords, authentication headers or
DRM values. It operates only on the normal signed-in page and its visible player
control.

## Next milestones

1. `0.15.0 — Aminema Spotlight`: native cinematic movie/series details.
2. `0.15.1 — Episode Navigator`: season/episode choice and honest progress.
3. `0.15.2 — Cinema Library`: complete grids and filters.
