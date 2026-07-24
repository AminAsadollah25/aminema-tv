# Amin TV OS — next update queue

The following catalog rows are intentionally **not implemented in v0.7.1**.
They are queued until the physical-TV FilmRooz keyboard fix is confirmed.

## Latest Iranian

- Source: the signed-in ParsiFlix website's own latest section.
- Home presentation: poster, title, service name, and normal content-page link.
- No protected stream extraction and no authentication bypass.

## Latest International

- Source: the signed-in FilmRooz page:
  `https://sean.robert-redford.net/archive/category/featured-films/`
- Home presentation: poster, title, service name, and normal content-page link.
- Authenticated posters may be cached app-privately using the existing WebView
  poster architecture; cookies remain inside WebView.
- No protected stream extraction and no authentication bypass.

## Release gate

Implementation starts only after Next, Done, Cancel, remote Back, and mouse
reopening are confirmed on the user's physical Android TV box.
