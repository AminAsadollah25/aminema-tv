# Aminema 0.14.0 — Series Pulse

## What’s new

- **Updated series that actually mean updated:** international series now come
  from the provider’s episode-release feed, so an older show returns to the top
  when a new episode arrives.
- **Season and episode under the card:** concise labels such as
  `قسمت ۰۴ فصل سوم`, without noisy “airing/finale” text.
- **My Series:** a native rail built from your signed-in account history and
  Aminema’s local viewing history.
- **Curated world series:** popular recommendations are shown separately from
  fresh releases.
- **Silent startup refresh:** Iranian and international sources update one at
  a time behind Home.
- **No blocked screen:** cached content stays clickable and a small spinner
  appears only beside the source currently updating.
- **Continue also converges in the background:** startup no longer opens the
  old full-screen account-sync page.

## Accuracy promise

Aminema labels the provider’s latest published episode, but does not call an
episode “unwatched” unless exact evidence exists. FilmRooz checkmarks were
proven to be incomplete across devices, so 0.14.0 chooses honesty over a
confident but wrong badge.

## Security

Aminema still stores only ordinary detail-page metadata. It does not bypass
login, inspect protected streams, read DRM data, or export cookies/tokens.

