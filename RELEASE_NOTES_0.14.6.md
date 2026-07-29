# Aminema 0.14.6 — Pointer & Playback Polish

`versionCode: 30`

## What changed

- FilmRooz movies now start playing automatically after Aminema reaches the
  provider's normal top-level player page.
- The existing ParsiFlix playback path is intentionally unchanged.
- Real USB/Bluetooth mouse hover and DPAD focus now share one cinematic
  interaction language.
- Removed the red selection border from cards.
- Selected cards lift gently with a short scale, brightness and shadow
  transition; pressing gives a subtle tactile response.
- Mouse dwell now opens the same spoiler-safe Quick Glance that already worked
  with the TV remote, and leaving the card closes it.
- Added extra vertical breathing room to lazy rails so focused posters can grow
  without clipping or moving neighbouring content.

## Playback safety

Autoplay is opt-in per provider. On FilmRooz, Aminema only calls `play()` on the
page's own HTML5 video or clicks a visible control belonging to the embedded
player. It never reads a media source URL, stream request, cookie, token, DRM
value or authentication header. If the player does not become ready, the normal
manual website page remains available after the existing timeout.

## Install

Install this APK over the existing `com.amin.tvos.debug` app. Package ID and
debug signing identity are unchanged, so cookies, logins, settings, history,
playback progress and poster cache remain intact.
