# Aminema 0.15.1 — development log

## Objective

Finalize a premium content-first Home that feels like a personal cinema while
keeping Aminema's playful mascot and every proven provider/browser path.

## Product decisions

- Content is the first decision; provider entry is an escape hatch.
- The first viewport contains compact brand controls, one smart greeting and
  one cinematic moment.
- Hero candidates are deduplicated by canonical content URL.
- Provider promo carousels are not scraped in this release. A future adapter
  can feed the same Hero model without another redesign.
- Live TV remains direct and unchanged.

## Architecture

- `HomeHeroSlide` carries display copy plus the existing `SpotlightItem`.
- `CinematicHero` owns rotation, pause-on-interaction, transition and progress.
- Home chooses at most five candidates from Continue, My Series and cached
  catalogue sections.
- Focused rail cards request a backdrop through a 220 ms dwell gate.
- `PosterCard`, `CatalogCard` and `CatalogSectionRow` expose focus-preview
  callbacks without changing their click behavior.
- `SectionRow` can suppress arrows for the fixed two-card provider dock.

## Motion budget

- Hero auto-rotation: 11 seconds.
- Hero fade/depth transition: 300–620 ms.
- Entry transition: 420–680 ms.
- Card focus response: 180–190 ms.
- Backdrop focus dwell: 220 ms.
- Backdrop crossfade: 700 ms.

The backdrop still requests a tiny image before upscale/blur, and only visible
rail cards are composed, preserving low-memory Android Box behavior.

## Files added

- `ui/home/CinematicHero.kt`

## Main files changed

- `ui/home/HomeScreen.kt`
- `ui/home/SmartGreetingHeader.kt`
- `ui/home/CatalogSectionRow.kt`
- `ui/components/CatalogCard.kt`
- `ui/components/TvComponents.kt`
- `app/build.gradle.kts`
- `README.md`, `ROADMAP.md`, `CLOUD-HANDOFF-LATEST.md`

## Security boundary

The release only rearranges already cached title metadata and ordinary poster
requests. It does not inspect media URLs, stream requests, cookies, tokens,
passwords, authentication headers or DRM data.

## Next milestones

1. `0.15.2 — Episode Navigator`
2. `0.15.3 — Canonical Library, Dedupe & Smart Search`
3. `0.15.4 — My Series`
4. `0.15.5 — Cinematic Promo Feed` using the existing Hero shell
