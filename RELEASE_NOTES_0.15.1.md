# Aminema 0.15.1 — Cinematic Home

`versionCode: 32`

## Home is now about the movie, not the provider

- A single cinematic Hero is assembled from Continue Watching, My Series,
  fresh Iranian titles, fresh international titles and a featured series.
- The Hero rotates every 11 seconds, pauses during mouse/DPAD interaction and
  opens the existing native Spotlight page.
- Watch, Continue, account login and browser playback requests are unchanged.
- The two mascot cinema cards remain available as compact direct-entry
  shortcuts near the end of Home. On a completely empty first run they move
  near the top so login is still obvious.

## Cinematic motion without heavy TV effects

- Header and Hero enter with short fade/slide transitions.
- Hero changes use a restrained fade and depth transition.
- The ambient background follows the currently featured/focused artwork with
  a crossfade.
- A 220 ms focus dwell prevents rapid DPAD movement from starting unnecessary
  image decodes.
- Mouse hover and remote focus share the same scale, brightness and elevation
  response; no red selection border is used.
- Greeting is now one compact line, leaving the complete Hero visible in the
  first 1080p viewport.

## Performance and compatibility

- Existing LazyRow rails, authenticated poster loading and cached provider
  catalog remain intact.
- Hero metadata is capped to four compact badges to avoid 720p overflow.
- Empty-state service access and all existing rail navigation arrows remain
  TV-safe.

## Install

Install this APK over the existing `com.amin.tvos.debug` package. The package
ID and debug signing identity are unchanged, so WebView cookies, provider
logins, settings and local library data are preserved.
