# Aminema 0.15.0 — Aminema Spotlight

`versionCode: 31`

## What changed

- Movies and series from Home, Search, Continue, My Series, Recent and
  Favorites now open a native cinematic title page on the first click.
- The red primary action preserves the exact existing Watch/Continue request;
  playback behaviour is not reimplemented inside Spotlight.
- My List can be changed directly on the title page.
- Back returns to the exact previous Home/Search rail and scroll position.
- Live TV remains direct one-click and does not open Spotlight.
- No Download or Trailer action was added.

## Rich title information

Latest catalog items pass their normal cached metadata directly to Spotlight.
Old Recent/Continue/Search titles are completed on demand from the provider's
ordinary signed-in title page and cached locally for 14 days:

- year beside the title;
- rating, runtime and primary genre;
- country and language;
- spoiler-safe provider synopsis;
- director and principal cast;
- latest published episode label for series when already available;
- green `دوبله فارسی` and blue `زیرنویس فارسی` availability badges.

Audio/subtitle detection excludes headers, navigation and global category
links, so a site's generic “dubbed movies” menu cannot mark every title as
dubbed.

## TV layout and reliability

- The poster and information column were rebalanced for 720p and 1080p safe
  areas.
- Optional information takes no space when genuinely absent; Aminema no longer
  shows a misleading “more details later” sentence.
- Synopsis is limited to two lines and credits remain visible above the
  primary actions.
- Tiny website beacons below one minute are not shown as meaningful progress.
- The primary focus requester survives asynchronous metadata recomposition,
  fixing a crash found during emulator QA.

## Privacy and security

The metadata loader stays on the configured provider host and reads only normal
visible title information. It never reads or stores a media URL, stream request,
cookie, token, password, authentication header or DRM value.

## Install

Install this APK over the existing `com.amin.tvos.debug` app. Package ID and
debug signing identity are unchanged, so WebView logins, cookies, settings,
library, progress and poster cache remain intact.

The public GitHub release remains 0.14.6 until the explicit 0.15.0 publish step.
