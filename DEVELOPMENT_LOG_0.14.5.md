# Development log — Aminema 0.14.5

## Goal

A stability/performance/polish release after Series Pulse: fix the false ParsiFlix Recent card, add useful hover metadata, and make the existing TV dashboard significantly lighter without changing its update identity or security boundary.

## Root cause: false ParsiFlix Recent item

The device library contained a concrete example:

- URL: `https://app.parsiflix.com/medias/movies/357`
- saved title: the generic `ParsiFlix - Watch Persian...` homepage title

The URL classifier was already correct. The failure was an SPA timing race: `location.href` changed to a detail route before the React detail DOM had hydrated, and an asynchronous metadata callback saved the old shell title under the new content URL.

### Fix layers

1. `BrowserActivity` now returns `location.href` with each metadata result and accepts it only when requested URL, DOM URL and current WebView URL still identify the same top-level page.
2. Generic provider-shell titles are rejected on recognized content routes.
3. `ContentMetadataPolicy` centralizes canonical URL identity and title-quality checks.
4. `LibraryRepository.repairMetadata()` replaces legacy generic titles/posters from a matching catalog item.
5. `HomeViewModel` also enriches display data and hides an unrepairable generic shell entry.
6. Four JVM tests cover route identity and generic-title rejection.

The real bad `/movies/357` entry was repaired on-device to `دو روز دیرتر` with its correct poster.

## Cinematic Quick Glance

`CatalogItem` gained optional, backward-compatible fields:

- `summary`
- `year`
- `genres`
- `rating`
- `runtime`

`CatalogBackgroundSync` sanitizes and bounds all page output before persistence. ParsiFlix metadata comes from its existing catalog JSON. FilmRooz was inspected through the authenticated emulator WebView; its actual `.postMeta` cards expose runtime, rating, genres separated by `<spl>`, and a spoiler-safe title synopsis. The parser now follows those real nodes rather than guessing from a whole-row text blob.

After a 520 ms mouse-hover or DPAD-focus dwell, `CinematicHoverPreview` appears as a fixed no-layout-shift panel with poster art, title, compact badges, synopsis and the label `معرفی بدون اسپویل`. It never autoplays video and never uses episode plot text.

## Performance and size

- Every Home rail (`SectionRow`, catalog rows and Live TV) migrated from eager `Row + horizontalScroll` to keyed `LazyRow`.
- Shared rail arrows received a `LazyListState` implementation and page by the visible-card count.
- Debug update builds now run R8 plus resource shrinking. `@JavascriptInterface` methods and containing bridge classes are explicitly kept.
- Only `fa` and `en` locale resources are packaged.
- Two 1280×720 cinema artwork PNGs were converted to quality-86 JPEGs, reducing them from about 2.3 MB combined to about 390 KB combined.
- Deprecated WebSettings database toggles and lifecycle imports were removed; arrow icons now use auto-mirrored variants.
- APK: 75,975,092 bytes (0.14.0) → about 22,216,560 bytes (0.14.5), roughly 71% smaller. The 10.6 MB offline intro remains bundled.

## Visual polish

- Home labels `Continue Watching`, `Recently Opened`, `Favorites` and `Sync accounts` were localized.
- The Continue badge now says `ادامه`.
- Quick Glance is opaque, bordered, shadowed and does not resize or push a rail.
- Existing focus scale/red outline, mouse hover, DPAD navigation and arrow controls remain consistent.

## Files

- `app/build.gradle.kts`, `app/proguard-rules.pro`
- `browser/BrowserActivity.kt`, `browser/CatalogBackgroundSync.kt`
- `data/ContentMetadataPolicy.kt`, `data/LibraryRepository.kt`
- `data/model/CatalogModels.kt`
- `ui/components/CatalogCard.kt`, `ui/components/TvComponents.kt`
- `ui/home/CinematicHoverPreview.kt`, `HomeScreen.kt`, `HomeViewModel.kt`
- `ui/home/CatalogSectionRow.kt`, `LiveTvSectionRow.kt`
- `app/src/test/.../ContentMetadataPolicyTest.kt`
- two service artwork resources converted from PNG to JPEG

## Security boundary

Only normal top-level content URLs and ordinary catalog/card metadata are accepted. No `video.src/currentSrc`, media request, protected stream URL, cookie, token, authentication header or DRM value crosses the bridge or enters the cache.
