# Aminema 0.18.0 — Live TV implementation status

## Scope

This work-in-progress release introduces a dedicated Live TV destination backed by normal
provider WebViews. It does not extract, store, log, download, or redistribute media URLs,
authentication tokens, or DRM data.

## Implemented in the working tree

- Dedicated `LiveTvActivity` opened from Home.
- ParsaTV configured as a `LIVE_TV` provider in `services.json`.
- Existing Parsiflix live catalogue preserved.
- Separate translated category sections: `شبکه‌های فارسی`,
  `شبکه‌های سراسری و صداوسیما`, `شبکه‌های ورزشی`, and `سایر شبکه‌ها`.
- Compact four-card rows; categories no longer require horizontal scrolling.
- Channel click opens ParsaTV's own first-party `embed.php` player with the original channel
  page as Referer. This avoids the full directory page, its menus, ads, analytics, and unrelated
  scripts. If the public embed page rejects the request or has no player, the app falls back to
  the original provider page.
- Playback begins automatically. The native cover is now a connection state rather than a
  mandatory second Play click. A manual `تلاش دوباره` action appears only if real playback is
  not confirmed.
- Live playback starts as soon as the player DOM becomes visible (`onPageCommitVisible`), without
  waiting for trailing page resources to finish.
- A provider-scoped coordinator supports top-level HTML5 video, JWPlayer, and same-origin nested
  iframe players. It expands the actual player surface to the full TV viewport and reapplies the
  layout when the provider replaces the player.
- Playback success now requires a real playing signal: HTML5 playing/time progress/decoded frames
  or JWPlayer's `playing` state. A merely visible iframe or empty player is not treated as success.
- Fallback discovers only visible controls explicitly labelled Server/سرور/Source/Mirror/Backup,
  including controls inside reachable same-origin frames.
- The configured JW7/JW8/HLS/HTML5/Clappr entries are no longer treated as channel mirrors. On the
  public site they belong to a custom-URL player-engine form and caused false, slow fallback loops.
- ParsaTV ad filtering remains provider-scoped and targets identifiable MGID containers only.
- SSL handling remains strict. The sole existing narrow exception is the exact
  `ssl.p.jwpcdn.com` ParsaTV player subresource host; provider pages and all other hosts remain
  strict.

## Verification completed on 2026-08-11

- Full checks passed with Android Studio's bundled JDK:
  `:app:testDebugUnitTest`, `:app:lintDebug`, and `:app:assembleDebug`.
- `git diff --check` passed.
- Installed with `adb install -r` on `Television_1080p`; no app data, cookies, or login sessions
  were cleared.
- Verified normal Home → Live TV navigation with a USB/mouse-style tap.
- Verified D-pad focus plus Remote OK opening a channel.
- Verified real advancing playback and automatic full-viewport presentation on:
  - Pars TV (JWPlayer)
  - Iran National Revolution TV (JWPlayer)
  - Iran International TV (same-origin iframe wrapper)
  - DEJ TV (opened with D-pad/Remote OK)
- Two Iran National screenshots one second apart had different frame hashes, and WebView requested
  media audio focus; this is stronger evidence than a mounted player poster.
- Measured Pars TV audio-focus start at about 4.78 seconds in the final emulator pass, improved from
  about 6.4 seconds before early DOM startup. Network/provider response time can still vary.
- All tested channels entered the TV viewport automatically; no manual fullscreen click was needed.

## Known limitations / next QA

- The provider catalogue contains many channels and not every channel has been exercised. Do not
  claim universal channel availability from the four verified samples.
- ParsaTV currently serves `/player/jw7/jquery-2.1.4.min.js` as HTML, producing an
  `Unexpected token '<'` console message. JWPlayer still played in the verified samples, but this is
  a provider response and should not be hidden by weakening security.
- Some provider streams may be offline even when the page itself loads. If the app cannot confirm
  playback and no real alternate server control exists, it exposes the expanded provider controls
  through the manual retry path rather than looping over unrelated engine names.
- Capture the exact names of any remaining failing channels and test them individually; their
  provider pages may use a third player structure or an actually offline upstream.
- The duplicate-title report in merged latest rows is intentionally deferred. Capture exact
  duplicates and compare title, year, IMDb id, media kind, and provider priority before changing
  `CanonicalLibrary` matching.

## Release state

- Release approval was given after emulator QA.
- Release commit: `85ae6501c1cdc232b860db1fbee803c1a878c791`
- Tag: `v0.18.0`
- GitHub Release: `https://github.com/AminAsadollah25/aminema-tv/releases/tag/v0.18.0`
- APK asset: `Aminema-0.18.0-debug.apk`
- SHA-256 asset: `Aminema-0.18.0-debug.apk.sha256`
- APK SHA-256: `91a0a663c54086b4e45cd2895c5dfacc4311cea7642839b47ab9509f0b64eaa8`
