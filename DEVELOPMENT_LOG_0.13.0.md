# Aminema 0.13.0 — development log (work in progress)

This file is the live engineering handoff for the 0.13.0 release cycle.
Both keyboard tracks passed emulator acceptance and the candidate is now
0.13.0 (`versionCode 27`). GitHub publication and physical-box acceptance are
tracked separately.

## Track A — native app Search Deck

### Product decision

The app-level search keyboard is a separate UX from the WebView login keyboard.
It remains a native Compose surface and never summons the Android system IME.
The redesign goal is visual hierarchy and frictionless TV input:

- one coherent dark cinematic deck instead of loose, variable-width cards;
- query preview, language badge and character count inside the deck;
- fixed TV-safe geometry with every row visible at 1920×1080;
- stronger shared mouse-hover/DPAD-focus state;
- an unmistakable red Search CTA, disabled until two characters exist;
- compact collapsed query bar after submit so poster results receive the screen;
- no empty result rails before the first search.

### QWERTY correction

The first redesign draft wrapped Persian letters into equal ten-key rows. It
looked regular but destroyed the familiar hardware-keyboard positions. After
user review this was rejected and replaced with staggered Persian QWERTY:

- `ض ص ث ق ف غ ع ه خ ح ج چ`
- `ش س ی ب ل ا ت ن م ک گ`
- `ظ ط ز ر ذ د پ و . ؟`

The second and third rows use physical-keyboard-style insets. `ژ` and `آ`
remain dedicated utility keys because search must not require Shift or a hidden
long-press menu. English now uses true staggered `QWERTY / ASDF / ZXCV` rows
instead of padding rows with punctuation.

### Changed files

- `app/src/main/java/com/amin/tvos/ui/search/SearchKeyboard.kt`
  - new Search Deck container and integrated query display;
  - equal key sizing within each physical row;
  - dedicated key focus/hover animation and visible focused fill/border;
  - staggered Persian and English QWERTY layouts;
  - dedicated Persian `ژ` and `آ`, wide Space, Backspace, Clear and Search;
  - compact 39dp keys and 58dp query display to fit the 1080p TV safe area.
- `app/src/main/java/com/amin/tvos/ui/search/SearchActivity.kt`
  - cleaner title/subtitle;
  - query is rendered by the Search Deck;
  - compact full-width edit bar after submit;
  - result groups stay hidden until a real search starts;
  - reduced vertical padding for complete 1080p visibility.

### Verification so far

- `clean :app:assembleDebug`: successful after the final QWERTY change.
- Earlier Search Deck draft also passed `assembleDebug` and `lintDebug`; final
  lint will be rerun together with Track B before release.
- Installed in place on AVD `emulator-5554`, Android TV, 1920×1080.
- Entire final Persian keyboard, including the action row, is visible without
  scrolling or clipping.
- Mouse taps entered `ام`; the counter changed to `2/60`, the preview border
  became red and Search changed from disabled to active red.
- Existing package data and service sessions were preserved by `adb install -r`.

### Build-environment note

Incremental resource builds occasionally found stale duplicated files such as
`banner 2.png` only under `app/build/intermediates`; no invalid filename exists
under `app/src`. `gradlew clean` removes them and a clean build succeeds. Do not
rename or delete source artwork to work around an intermediates-only duplicate.

## Track B — WebView login Input Deck

### Root cause and architecture

The previous implementation split DOM target, transition, Caps/language/show
state and native focus ownership between mutable overlay flags, BrowserActivity
flags and JavaScript globals. Language also deleted and rebuilt focused key
rows. The replacement uses one explicit browser state machine:

`CLOSED → EDITING → MOVING_NEXT → EDITING(password) → SUBMITTING → CLOSED`

- Every open field receives a DOM target token from a page-local `WeakMap`.
- Every native keyboard open receives a monotonically increasing `sessionId`.
- Every key, mode control, action and dismiss callback carries that session id.
- BrowserActivity rejects stale callbacks and non-explicit focus changes from a
  different target.
- A repeated `focusin` for the same target does not reopen/reset the deck; this
  is especially important because password values never cross from WebView.
- Next locks the new Password target and token before focus events can race.
- Transition callbacks have bounded retry/timeouts; the UI cannot remain stuck
  forever when a framework replaces the form.

### Input Deck UI

`MouseKeyboardOverlay` is now a fixed TV-safe cinematic deck:

- fixed-width field badge: `نام کاربری` / `رمز عبور` / `جستجو`;
- fixed preview region with start ellipsis, so a long password cannot move the
  Show/Close controls;
- `بعدی: رمز` hint on Username; Show/Hide only on Password;
- explicit always-visible `بستن`;
- red `بعدی` / `ورود` / `جستجو` action;
- true staggered English and Persian QWERTY;
- Persian `ژ` and `آ` remain directly accessible;
- all rows are constructed once; Caps and language only render state;
- shared mouse hover and DPAD focus styling;
- first letter receives focus when the deck opens, including after touch mode.

### Changed files

- `app/src/main/java/com/amin/tvos/browser/MouseKeyboardOverlay.kt`
  - immutable `KeyboardDeckState`, input/action enums and fixed view tree;
  - stable QWERTY rows, header, preview, hover/focus and session callbacks.
- `app/src/main/java/com/amin/tvos/browser/BrowserActivity.kt`
  - `BrowserKeyboardSession` and explicit phase machine;
  - DOM token bridge and stale-session rejection;
  - session-aware write, Next, Submit, release and reinforce paths;
  - old `keyboardTransitionInProgress/generation` patch set removed.

### Safe emulator acceptance

A temporary debug-only launcher opened a local synthetic login page inside the
real BrowserActivity/WebView. It contained no credentials or network request
and synchronously replaced Username on `change`, simulating a modern framework.
The launcher and its manifest entry were deleted before the final build.

Verified on Android TV AVD 1920×1080:

1. Mouse click opened Username with the correct badge and action.
2. Typed `amin`; the real HTML input received the value.
3. Next survived synchronous Username DOM replacement and opened Password.
4. Caps → `A`, language → Persian, `ض`, Show displayed `Aض`; Username remained
   `amin` and the target stayed Password.
5. `ورود` submitted the local form, dismissed the deck and displayed the
   synthetic success message.
6. Remote Back dismissed Password once and left the page/activity open.
7. System IME was not visible; `mInputShown=false` and served connection was null.
8. No `FATAL EXCEPTION` appeared.
9. Final APK without the harness installed with `adb install -r`; package
   resolution returned `No activity found` for the deleted harness.
10. Final `clean assembleDebug` and `lintDebug`: successful, zero lint errors.

### Post-release physical acceptance

- Do not log out the user's real service sessions merely to reproduce login.
- Physical Android Box check is still required when a real login is naturally
  available: USB mouse, DPAD, Caps, language, Show, Next, Done and Back.
- 0.13.0/code 27 is published as GitHub Latest. Physical-box feedback can be
  handled as a focused 0.13.x hotfix without logging out existing sessions.

## Publication

- Release commit: `fd3ac33`
- Tag: `v0.13.0`
- Release:
  `https://github.com/AminAsadollah25/aminema-tv/releases/tag/v0.13.0`
- APK: `Aminema-v0.13.0-debug.apk`, 75,958,740 bytes
- SHA-256:
  `acd6ed8d43df33f2c4813e603efe4e1f474622305129f05ea320c5da136e3b91`
- GitHub Latest API returned v0.13.0 and `versionCode: 27`; both assets are in
  `uploaded` state.
