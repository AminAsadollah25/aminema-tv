# Aminema — Legacy / Existing Project Rules

This is an established Android TV product with real users, persistent signed-in
sessions, released APKs, provider-specific integrations, and substantial prior
work. Treat it as a legacy/existing system, not a greenfield starter project.

These rules apply to Codex, Claude, IDE agents, and human contributors.

## 1. Prime directive

Understand and preserve the current system before changing it.

- Do not rewrite, restructure, migrate, rename, delete, or broadly refactor
  existing code merely to make it match a preferred pattern.
- Do not change working behavior unless the requested task requires it.
- Never claim behavior is correct, safe, or tested without evidence.
- Make the smallest safe, reversible change that solves the verified problem.
- Apply better standards prospectively to new code. Improve existing code only
  when it is touched for a real task, presents a verified risk, or the owner has
  explicitly approved cleanup.

## 2. Required reading and discovery

Before significant work, read at least:

1. `AGENT-ONBOARDING.md`
2. `ENGINEERING-HANDOFF-FA.md`
3. `CLOUD-HANDOFF-LATEST.md`
4. `ROADMAP.md`
5. The latest relevant `DEVELOPMENT_LOG_*`, `TEST_REPORT_*`, and
   `RELEASE_NOTES_*` files
6. Every source, model, adapter, configuration, and test file directly involved
   in the requested flow

Document the system as it exists. Explicitly separate:

- verified facts;
- assumptions;
- unknowns that still require a real probe, device test, or owner decision.

Do not guess provider endpoints, DOM structures, selectors, authentication
behavior, playback behavior, or data fields.

## 3. Baseline before implementation

Before a significant code change, capture the current repository state and run
the existing verification commands without modifying code first:

```bash
git status --short --branch
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Keep pre-existing failures and dirty files separate from failures or changes
introduced by the current task. Never overwrite, revert, stash, discard, or
silently include another contributor's uncommitted work.

For a small isolated documentation or copy change, a targeted inspection is
enough; do not manufacture unnecessary refactors or test work.

## 4. Risk-ranked baseline

For significant work, report verified risks before implementation:

- **Critical:** authentication/session loss, security, privacy, credentials,
  protected playback data, signing/update continuity, or user-data loss.
- **High:** crashes, failed upgrades, broken Home/Search/Browser/Playback,
  provider-wide breakage, or unusable TV navigation.
- **Medium:** missing regression coverage, fragile provider DOM assumptions,
  performance/memory risks, maintainability, or accessibility gaps.
- **Low:** naming, cleanup, dead code, minor visual inconsistency.

Propose stabilization in small reversible steps. Obtain owner approval before
broad refactors, migrations, dependency replacements, architecture changes,
data migrations, signing changes, or deletion of existing code.

## 5. Aminema invariants — do not break

### Installation, identity, and user data

- Preserve `applicationId`, package identity, debug signing continuity, and
  update compatibility with the APK already installed on the owner's TV.
- **Development-session preservation is a hard rule:** throughout coding,
  building, installing, debugging, emulator testing, and release QA, preserve
  every existing provider login, cookie, WebView session, and local app record.
- Never use `adb uninstall`, `adb shell pm clear`, Android's **Clear storage**,
  emulator **Wipe Data**, AVD recreation, or deletion of the app's WebView,
  SharedPreferences, DataStore, database, cache, or files directories unless
  the owner explicitly approves that exact destructive action.
- Never call `CookieManager.removeAllCookies`, `removeSessionCookies`, or an
  equivalent logout/cleanup path as part of development setup, automated tests,
  debugging, migration, or release validation.
- If a logged-out or first-run state must be tested, use a separate temporary
  AVD/profile with a different data directory. Never sacrifice the owner's
  signed-in `Television_1080p` environment for that test.
- Install development and candidate builds only as in-place updates with
  `adb install -r`. Do not change package/signing identity to work around an
  installation problem; diagnose the mismatch instead.
- Preserve cookies, WebView sessions, DOM/local storage, settings, catalog,
  Continue Watching, Favorites, poster cache, and all owner-created data.
- Never log out a provider as part of debugging or cleanup without approval.

### Provider and browser behavior

- Provider adapters are isolated contracts. A fix for one service must not
  change another service's selectors, routes, cache, login, or playback path.
- Inspect the real signed-in page before changing a provider adapter. Prefer a
  characterization/regression test or captured fixture before refactoring a
  working parser.
- Preserve normal-browser authentication. Do not bypass login, DRM, geographic
  controls, subscriptions, or access restrictions.
- Never extract, store, export, or log protected media URLs, signed links,
  authentication tokens, passwords, DRM data, or stream manifests.
- Only stable ordinary provider page URLs and permitted metadata may enter
  Aminema's library/history models.
- Do not send provider cookies, tokens, or private URLs to public metadata
  services.

### Playback and navigation

- Preserve the established browser Back contract, full-screen HTML5 playback,
  system-UI handling, Quick Menu, resume behavior, and safe manual fallback.
- Direct-play success must be observed through a verified route, playback event,
  or native full-screen event; a JavaScript click alone is not proof.
- Current automatic quality preference remains provider-defined and verified;
  do not change it from assumptions or filenames.
- Series playback behavior must not be generalized from movies. Episode and
  season handling requires verified provider data and explicit fallback.

### TV-first UX

- Every changed interactive flow must consider 1080p landscape, Persian RTL,
  D-pad focus/OK/Back, USB/Bluetooth mouse hover/click/wheel/back, and air mouse.
- Focus must remain visible without hiding text or changing layout unexpectedly.
- Preserve focus and rail/scroll position when returning from Spotlight,
  Search, Browser, or Settings where the current flow does so.
- Keep Home usable during background refresh. One provider's failure must retain
  cached data and must not block or empty other rails.
- Prefer fewer TV clicks, truthful empty/error states, and spoiler-safe metadata.

### Performance

- Protect startup time, low-RAM Android Box behavior, lazy rail rendering,
  bounded image decoding, background refresh isolation, and cache limits.
- Do not add perpetual animation, hidden WebViews per card/slide, unbounded image
  loads, synchronous network work on the UI thread, or catalog-wide recomposition.
- Do not move the Gradle build directory back into the iCloud-synced project.

## 6. Implementation and verification

- Prefer characterization/regression tests around working behavior before
  refactoring it.
- New code must have a clear owner, bounded responsibility, honest fallback,
  and no invented data.
- After changes, rerun the relevant unit tests, Lint, and APK build. For a
  user-visible flow, install with `adb install -r`, launch it, inspect the actual
  1920x1080 result, exercise the relevant D-pad/mouse/Back path, and check for
  fatal exceptions.
- Emulator success does not prove physical TV-box behavior. State clearly which
  device/environment was tested and what still needs owner verification.
- Report separately:
  1. pre-existing failures;
  2. failures introduced by this task;
  3. tests passed;
  4. tests not run and why.

## 7. Release and handoff discipline

For every release-worthy Feature/Fix:

- update `versionCode` and `versionName` without breaking update continuity;
- write accurate Persian release notes and include the required final
  `versionCode: N` line;
- update the current roadmap/handoff/development/test documentation;
- record the root cause, UX decision, files changed, real probes, security
  boundary, limitations, and next step;
- attach both APK and SHA-256 to the GitHub Release;
- obtain owner approval before commit/push/tag/GitHub Release unless the owner
  explicitly requested those exact publishing actions in the current task.

Never mark a candidate released until Git, tag, GitHub Release, downloadable
assets, and update-path verification reflect reality.

## 8. Product collaboration

The owner is a non-technical Persian speaker and expects a creative product
partner, not blind implementation.

Before implementing a substantial idea, explain concisely:

1. the actual user problem;
2. the smallest reliable solution;
3. the stronger long-term solution;
4. one complementary cinematic TV UX improvement;
5. verified constraints and honest unknowns.

Do not use lack of an API as an excuse to stop thinking, but never replace
missing evidence with fabricated behavior. Optimize for a premium, cinematic,
low-friction and spoiler-safe TV experience.
