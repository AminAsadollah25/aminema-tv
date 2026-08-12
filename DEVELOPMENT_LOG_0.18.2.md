# Development Log — Aminema 0.18.2

## Problem

The full foreign-series library showed a loading control after 208 titles,
then returned to «نمایش بیشتر» without adding new items.

## Evidence

The provider pages after page 20 were real and distinct. The application was
requesting many pages concurrently, losing responses, and storing a page limit
that did not represent the rows actually received. The library route also held
the compact Home snapshot instead of selecting from the current catalog state.

## Implementation

- `MainActivity.kt`: live catalog selector for the library route.
- `HomeScreen.kt`: provider-aware selector preserving canonical merge rules.
- `CatalogLibraryScreen.kt`: next load starts after the stored page limit.
- `CatalogBackgroundSync.kt`: incremental four-page MyMoviz fetches, explicit
  page start, loaded-window metadata and bounded page ceiling.
- `CatalogRepository.kt`: safe merge of old and incoming windows and legacy
  cache migration.
- `CatalogModels.kt`: `loadedPageLimit` persisted in `CatalogSection`.

## Safety boundary

No login, cookie, WebStorage, Continue, Favorite or media URL data was cleared
or changed by this fix.
