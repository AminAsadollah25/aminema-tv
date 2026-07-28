# Aminema 0.14.0 — Candidate Test Report

## Environment

- Android TV emulator: 1920×1080
- Installed package: `com.amin.tvos.debug`
- Installed version: `0.14.0` / code `28`
- Both provider sessions remained signed in after `adb install -r`

## Build gates

- `assembleDebug`: PASS
- `lintDebug`: PASS
- `git diff --check`: PASS
- Fatal Android crash scan: PASS (zero app crashes)
- RC SHA-256:
  `37a699b70c28bc21669e4d0d4f26254cd7cd336ebfda75af6efbd46ebcf24681`
- Signing certificate matches 0.13.0:
  `ba6ac8c4c2e1828462e7a6b122ad60856a054a18389af36bc001a1ee38ba13d3`

## Functional acceptance

### Cold start and background work

- MainActivity remained top/resumed throughout automatic refresh: PASS
- No automatic full-screen AccountSyncActivity interruption: PASS
- Iranian provider completed before international provider began: PASS
- Existing cached rows remained visible and interactive: PASS
- Manual Iranian refresh showed only its row spinner: PASS
- Horizontal interaction remained available during refresh: PASS

### Catalog output

- ParsiFlix: 24 movies, 24 series: PASS
- FilmRooz: 8 new movies, 16 release-ordered series: PASS
- FilmRooz popular/curated series: 9: PASS
- Separate curated rail populated: PASS
- Per-provider error isolation and stale-cache path retained: PASS

### Episode labels

Verified examples from the signed-in FilmRooz archive:

- Silo — `قسمت ۰۴ فصل سوم`
- Spiral — `قسمت ۱۰ فصل هشتم`
- Better Late Than Single — `قسمت ۱۰ فصل دوم`
- Knot — `قسمت ۰۵ فصل اول`
- All American — `قسمت ۰۴ فصل هشتم`
- House of the Dragon — `قسمت ۰۶ فصل سوم`

Status tails such as “در حال پخش” and “پایان فصل” were correctly omitted.

### My Series and account convergence

The non-modal provider pass imported title-level series membership from both
signed-in accounts. Verified FilmRooz examples:

- Silo
- The Handmaid’s Tale
- Better Late Than Single
- President Curtis

Verified ParsiFlix examples:

- بامداد خمار
- بدنام
- جیمی جام
- گل سنگ

The rail does not claim exact unwatched counts when the account does not expose
reliable episode history.

## Watched-tick investigation

Cross-device test result: FilmRooz episode checkmarks did not converge
reliably between laptop and emulator. They are not accepted as authoritative
account-wide watched state. This is a provider limitation, not an Aminema
rendering bug.

## Known candidate limitations

- ParsiFlix list data contains no season/episode field, so its series cards do
  not show a guessed label.
- `سریال‌های من` is title-level on cross-device FilmRooz data.
- Exact “unwatched episode” badges require a future local progress baseline or
  explicit user confirmation.
- Manual `Sync accounts` still uses the existing visible diagnostic activity;
  only automatic startup reconciliation is fully background.
