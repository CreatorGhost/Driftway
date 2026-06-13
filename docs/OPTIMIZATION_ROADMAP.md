# AA Browser — Optimization & Redesign Roadmap

> **Purpose of this file:** the single source of truth for the optimization + redesign effort.
> It survives across sessions so context is never lost. Every work item lives here with a
> status. Update the status the moment an item moves. Work items are done **one at a time**.
>
> **North-star goal:** make AA Browser look and feel *professional, premium, and buttery-smooth*
> on Android Auto / Automotive head units, optimize video (Netflix / YouTube / Crunchyroll),
> and deliver the best achievable login UX — without destabilizing the working browser.

---

## How to use this file
- Status legend: `[ ]` todo · `[~]` in progress · `[x]` done · `[!]` blocked / needs decision · `[-]` dropped
- Each item: `ID | status | title | sev | effort | impact | evidence/notes`
- When an item is finished, flip its box to `[x]` and add a one-line note (commit/file).
- Keep the **Decisions log** at the bottom current — it records *why* we chose things.
- Full raw audit (8-agent deep-read, 49 findings) lives in the session task output; the
  distilled version is reproduced in the tables below.

---

## Current focus
- **Active sprint:** Quick Wins batch (low-effort / high-impact, design-agnostic)
- **Parallel:** UI design-direction research (workflow) → produce a "final list" of premium
  design languages for the user to choose from. **UI redesign is BLOCKED on that choice.**

---

## Sprint Q — Quick Wins (design-agnostic, ship first)
> All 9 implemented. ✅ `BUILD SUCCESSFUL` (debug APK built, 2026-06-13). Committed on
> `feat/ux-perf-video-audio` (PR #1). `local.properties` points at the local SDK (gitignored).

| ID | St | Item | Sev | Eff | Imp | Notes |
|----|----|------|-----|-----|-----|-------|
| Q1 | [x] | Gate `DynamicColors` behind a "Use system colors" toggle (default OFF) so the curated brand/AMOLED theme actually ships | high | small | high | `MainActivity.onCreate` gated; pref + appearance toggle added; recreate on change |
| Q2 | [x] | `CookieManager.flush()` in `onPause` — sessions persist across process kill | high | trivial | high | `MainActivity.onPause` |
| Q3 | [x] | Default desktop UA on car / large-landscape displays (better Netflix/YT/Crunchyroll players) | high | small | high | `BrowserPreferences.shouldUseDesktopMode` smart default via `isLikelyCarOrLargeDisplay` (sw≥480dp or CAR); explicit toggle still wins |
| Q4 | [x] | Fix `onCreateWindow` so OAuth/SSO popups ("Sign in with Google") open in a new tab | high | medium | high | `ConfiguredWebView` + `BrowserCallbacks.onCreateWindowRequest/onCloseWindowRequest` + `MainActivity.openPopupWindow` (WebViewTransport) |
| Q5 | [x] | Set status/nav bar icon appearance per theme (sunlight legibility) | high | small | med | `MainActivity.applySystemBarAppearance` called in onCreate/onResume/exitFullscreen |
| Q6 | [x] | QR generation: replace per-pixel `setPixel` loops with `setPixels()` + cache sponsor QR | med | small | med | `MainActivity.generateQrCode` + `SettingsViews.generateQrBitmap`; `cachedSponsorQr` field |
| Q7 | [x] | Populate `values-w600dp` dimens (full set) + raise menu cap for head units | high | small | high | new `values-w600dp/dimens.xml` (w600dp beats `land` precedence → fixes shrunk menu) |
| Q8 | [x] | Telemetry consent: opt-out toggle gating Umami; (privacy/Play policy) | high | small | high | `isAnalyticsEnabled` gates `trackEvent`; toggle in site-data card |
| Q9 | [x] | Pin `FreeDroidWarn` to an exact version (reproducible builds) | med | trivial | med | `V1.+` → `V1.13` in `app/build.gradle.kts` |

---

## Sprint 0 — Foundation (de-risks everything; do before deep refactors)

| ID | St | Item | Sev | Eff | Imp | Notes |
|----|----|------|-----|-----|-----|-------|
| F1 | [ ] | Introduce a `ViewModel` for tab list + UI state (survive config changes) | med | large | high | enables config-change correctness, kills god-class state |
| F2 | [ ] | `WebView.saveState`/`restoreState` per tab (scroll + history + form survive) | high | medium | high | session restore is URL-only today |
| F3 | [ ] | Lazy tab materialization + LRU destroy (memory / OOM on head units) | high | large | high | all tab WebViews retained simultaneously today |
| F4 | [ ] | Migrate hot prefs to DataStore + coroutines (kill main-thread disk I/O) | high | medium | high | sync SharedPreferences in attachBaseContext/onCreate/onResume |
| F5 | [ ] | Tab-session write off main thread (currently on every navigation) | high | medium | high | |
| F6 | [ ] | `onConfigurationChanged` handler (or drop configChanges) for day/night | high | medium | high | stale colors on theme switch today |

---

## Sprint A — Expressive Aurora (CHOSEN design direction — 2026-06-13)
> User chose **Expressive Aurora**. Accent color still to confirm (amber-coral vs teal). Build
> the shared foundation first; layer the signature flourishes after. Each step verified by build.

| ID | St | Item | Eff | Notes |
|----|----|------|-----|-------|
| A1 | [ ] | Bump `com.google.android.material` 1.13.0 → 1.14.x; switch base theme to a `Material3Expressive` variant | small | gating dep for Expressive APIs; verify build resolves 1.14.x |
| A2 | [ ] | Icon swap: replace `@android:drawable/ic_menu_*` Holo icons (activity_main.xml:379,1118,1452 + MainActivity.kt:1370) with Material Symbols vectors | small | **do first** — cheapest "looks default" fix; create `more_vert`/`add` vectors |
| A3 | [ ] | Fonts: `res/font` (one display + one humanist UI face) + `TextAppearance.App.*` scale on theme | medium | escape default Roboto |
| A4 | [!] | Color identity: neutral surface ladder + ONE accent (amber-coral OR teal); drop secondary/tertiary container fills; keep AMOLED night + error red | medium | **needs accent choice** |
| A5 | [ ] | Shape scale: theme `shapeAppearance{Small,Medium,Large}Component` squircle overlays; remove hardcoded 16/24/28dp | small | |
| A6 | [ ] | Elevation ladder: tonal surface steps + soft shadows instead of 0dp flat cards | medium | |
| A7 | [ ] | Motion: `dynamicanimation` SpringAnimation (press/sheet/tab) + MaterialContainerTransform/SharedAxis transitions | medium | overlaps U2 |
| A8 | [ ] | Signature shape-morph on active tab indicator + selected start-page tile (`androidx.graphics:graphics-shapes`) | medium | the one hand-built flourish |
| A9 | [ ] | Bento start page: squircle tiles, emphasized display headline, reuse gradient + dim overlay | large | overlaps U3/U4; home identity surface |
| A10 | [ ] | Floating/docked toolbar of large browser controls (replaces loose FAB + icon row) | medium | |

## Sprint 1 — UI/UX polish + anti-lag  *(general polish; visual identity handled in Sprint A)*

| ID | St | Item | Sev | Eff | Imp | Notes |
|----|----|------|-----|-----|-----|-------|
| U1 | [~] | Adopt chosen premium design language (theme/typography/shape/color) | high | large | high | **= Sprint A (Expressive Aurora)** |
| U2 | [ ] | Motion system: MaterialFadeThrough / ContainerTransform between panels | med | medium | high | no transitions today; #1 "premium feel" lever |
| U3 | [ ] | RecyclerView + DiffUtil for tabs/bookmarks/start-page lists | med | large | high | full teardown/rebuild on every callback today |
| U4 | [ ] | ViewStub lazy-inflate the 5-6 stacked secondary panels | high | large | high | `activity_main.xml` 1457 lines, all inflated cold |
| U5 | [ ] | Car touch targets (~76dp) + ≥16sp type + density-scaled swipe threshold | high | medium | high | 40-48dp today |
| U6 | [ ] | Typography scale + brand font; ShapeAppearance set | med | medium | med | depends on U1 |
| U7 | [ ] | Favicon load/decode off main thread + LruCache (or Coil) | med | medium | med | |
| U8 | [ ] | Start-page overdraw + wallpaper decode off main thread | med | small | med | |
| U9 | [ ] | Settings screen: reusable builders, in-place theme apply (no recreate flicker) | med | large | med | candidate Compose pilot |
| U10 | [ ] | One primary menu affordance; FAB position/auto-hide tuned for car | low | small | med | |
| U11 | [ ] | Extract hardcoded strings + contentDescription to resources | med | trivial | med | |

---

## Sprint 2 — Video (Netflix / YouTube / Crunchyroll)

| ID | St | Item | Sev | Eff | Imp | Notes |
|----|----|------|-----|-----|-----|-------|
| V1 | [ ] | Default/per-host desktop UA for streaming sites (covered partly by Q3) | high | small | high | |
| V2 | [x] | MediaSession + AudioFocus + foreground media service (background audio, controls) | high | medium | high | ✅ `media/MediaSessionController` + `media/MediaPlaybackService` + `web/MediaPlaybackBridge` (JS detects HTML5 media + navigator.mediaSession). build green. **This is the "keep YouTube AUDIO playing while driving" feature.** |
| V3 | [x] | Don't pause active tab's media on tab-switch/background | high | medium | high | ✅ `onStop` skips `webView.onPause()` when `isMediaPlaying`; `switchToTab` won't pause the media tab; `closeTab` tears down session |
| V4 | [x] | Fullscreen hardening: teardown moved `onPause`→`onStop`; BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE; opaque black bg; consistent inset target (root) | med | medium | med | ✅ build green. Skipped forced landscape lock (head unit is fixed-landscape; forcing orientation risks fighting the projection surface) |
| V5 | [ ] | App-level large in-player controls + visible exit affordance | low | medium | med | |
| V6 | [ ] | Picture-in-Picture (OEM-permitting) | med | medium | med | |
| V7 | [x] | `offscreenPreRaster` active tab only | med | small | med | ✅ default off in `configureWebView`; toggled on for active tab in `switchToTab`. build green |
| V8 | [ ] | Surface System WebView version + update prompt | low | small | med | |
| V9 | [x] | Keep-screen-on during inline playback (not just fullscreen) | low | trivial | low | ✅ tied to media playback state in `handleMediaState` (guards against clobbering fullscreen) |
| V0 | [!] | Widevine L1/HD — **HARD PLATFORM LIMIT** inside `android.webkit`; document, don't promise | med | large | med | Netflix ~480p, Crunchyroll ~720p |

---

## Sprint AD — Ad blocking (researched 2026-06-13; AWAITING USER SCOPE CHOICE)
> Research verdict: **No extensions** in `android.webkit` WebView (Brave/Helium ship their own
> Chromium/Gecko engine — swapping would break Widevine + be a multi-week rewrite). The right
> path is an **in-app layered blocker** in `ConfiguredWebView`. **General web + Facebook feed
> ads = achievable (Brave-class). YouTube video ads = NOT durably solvable** (first-party
> same-endpoint ads + anti-adblock locker + server-side SSAI). It's a perpetual maintenance
> commitment, not a one-time feature. Honest expectation must be set with the user.

| ID | St | Item | Eff | Notes |
|----|----|------|-----|-------|
| AD1 | [ ] | Settings toggle + per-site allowlist + global off (mirror cleartext-host pattern) | small | safety valve for breakage |
| AD2 | [ ] | Baseline hosts blocker via `shouldInterceptRequest` (StevenBlack ~85k domains in assets → HashSet) | small | thread-safe, allocation-free per subresource |
| AD3 | [ ] | ABP engine for EasyList/EasyPrivacy/AdGuard (vendor Edsuns/AdblockAndroid OR Brave adblock-rust via JNI) | large | adblock-rust = production/low-mem; Edsuns = fast but unmaintained → must fork |
| AD4 | [ ] | Cosmetic element-hiding CSS at `onPageStarted`/document-start (collapse blank gaps) | medium | |
| AD5 | [ ] | `WebViewCompat.addDocumentStartJavaScript` scriptlet runtime (gated on DOCUMENT_START_SCRIPT) | medium | runs before page scripts; needed for YT/FB |
| AD6 | [ ] | Facebook procedural cosmetic (MutationObserver reconstructs split "Sponsored") | medium | heuristic, fragile |
| AD7 | [!] | YouTube best-effort scriptlet (scrub InnerTube adPlacements; feature-flagged, isolated) | large | **fragile, intermittent, can't promise ad-free**; SSAI defeats it |
| AD8 | [ ] | SponsorBlock (skip creator sponsor segments) — separate, stable, labeled distinctly | small | not the same as ad removal |
| AD9 | [ ] | Background list updates (WorkManager/OkHttp) + pre-compiled blob in assets | medium | |
| AD10 | [ ] | Head-unit perf + correctness validation (latency, RAM, Widevine/OAuth/downloads still work) | medium | |

## Sprint 3 — Login UX  *(verdict: cross-browser import NOT possible; "log in once, stay logged in" IS)*

| ID | St | Item | Sev | Eff | Imp | Notes |
|----|----|------|-----|-----|-----|-------|
| L1 | [ ] | Cookie flush (= Q2) | high | trivial | high | prerequisite |
| L2 | [ ] | onCreateWindow OAuth popups (= Q4) | high | medium | high | prerequisite |
| L3 | [ ] | Credential Manager (`androidx.credentials`) — passwords + passkeys from Google Password Manager (same vault Chrome uses); inject via evaluateJavascript scoped to eTLD+1 | high | large | high | closest legit path to "reuse Chrome passwords" |
| L4 | [ ] | "Sign in with Google" (GetGoogleIdOption) for one-tap app identity | high | medium | high | |
| L5 | [-] | Import cookies/passwords from other browsers | — | — | — | **DROPPED**: impossible (sandbox + Keystore) |

---

## Sprint X — Foundation / Security / Build hardening

| ID | St | Item | Sev | Eff | Imp | Notes |
|----|----|------|-----|-----|-----|-------|
| X1 | [ ] | Telemetry consent (= Q8) | high | small | high | |
| X2 | [ ] | Reconsider global cleartext + `MIXED_CONTENT_ALWAYS_ALLOW` | high | medium | med | |
| X3 | [ ] | Third-party cookies default-on review | med | small | med | needed for SSO; keep but document |
| X4 | [ ] | Remove per-tab `JavascriptInterface` in `closeTab`; tighten WebMessage origins | low | small | low | |
| X5 | [ ] | Pin FreeDroidWarn (= Q9); decide on dead Compose deps | med | trivial | med | |
| X6 | [ ] | Manifest: revisit `appCategory=game`, NAVIGATION/APP_MAPS categories | med | small | med | automotive policy risk |
| X7 | [ ] | Split `MainActivity` god-class; `BrowserPreferences` god-object | med | large | med | |

---

## Design-direction research (workflow) — DONE, final list below
> Key finding: stock M3 here isn't "outdated" — it's **unconfigured AND on the wrong version**.
> The dep is `com.google.android.material:1.13.0`, which ships *without* the Expressive APIs
> (those land in 1.14, stable May 2026). Plus default Roboto, the literal Theme-Builder blue
> seed, hardcoded corners, and Holo-era `@android:drawable/ic_menu_*` icons. Fixable in Views.
>
> **The 4 directions (full details in chat / pick one to unblock U1):**
> 1. **Expressive Aurora** — M3 Expressive done right (bump to MDC 1.14, custom shape/type/color + shape-morph). *medium effort. ← recommended.*
> 2. **Cockpit Noir** — instrument-grade dark HMI, build-from-black, one accent. *medium, works on 1.13 today.*
> 3. **Liquid Cockpit** — premium "liquid glass" mesh-gradient + frosted chrome. *large, highest risk.*
> 4. **Bento Brutalist** — oversized bento tiles + huge editorial type. *small, cheapest/safest.*

| St | Item |
|----|------|
| [x] | Research Material 3 **Expressive** (Google 2025) vs the dated-looking baseline M3 |
| [x] | Research premium automotive HMI design languages (Tesla / Rivian / Polestar / AAOS) |
| [x] | Research 2025-26 premium app UI trends (glass/spatial, bold type, motion, dark-first) |
| [x] | Map each candidate direction onto AA Browser (View-based constraint, effort, mockups) |
| [!] | **Present final list → AWAITING USER CHOICE of direction (unblocks U1)** |

### Shared design foundations (do regardless of which direction wins)
- Bump `com.google.android.material` 1.13.0 → 1.14.x (unlocks Expressive themes/type/components)
- Replace ALL `@android:drawable/ic_menu_*` Holo icons with one Material Symbols family (cheapest, highest-impact "looks default" fix — do first)
- Add real fonts (`res/font`: one display + one humanist UI face) + a `TextAppearance.App.*` scale
- Automotive type scale on 4dp grid (≥32dp primary / ≥24dp secondary, medium weight; ≥4.5:1 contrast day+night)
- Collapse blue+purple rainbow → neutral surface ladder + exactly ONE brand accent (active/focus/primary/progress)
- Centralize corners into theme `shapeAppearance*Component` overlays (remove hardcoded 16/24/28dp)
- Real elevation ladder (tonal surface steps + soft shadows) instead of 0dp flat cards
- One quiet motion vocabulary (`dynamicanimation` SpringAnimation + MaterialContainerTransform, 150-250ms)
- Bento-tile start page (reuse existing gradient + dim-overlay assets)
- Keep AMOLED true-black night; enforce 76dp targets + ≥23dp spacing; gate text entry to parked state

---

## Quality gate — review of quick-wins + video sprint (run `wig3eqh4t`, 22 agents, 0 false positives)
All confirmed findings fixed on `feat/quick-wins-batch`:

| # | St | Finding | Fix |
|---|----|---------|-----|
| MF1 | [x] | FGS background-start can crash (`ForegroundServiceStartNotAllowedException`) | `runCatching` around `startForegroundService` |
| MF2 | [x] | FGS could miss `startForeground` deadline on null/stale-token path | `startForeground` unconditional + first, 3-arg typed overload; `MediaControllerCompat` in try/catch |
| MF3 | [x] | AudioFocus LOSS never abandoned focus; GAIN auto-resumed unconditionally | abandon+reset on LOSS; `wasTransientlyPaused` flag gates GAIN resume |
| MF4 | [x] | Notification Stop leaked audio focus (DOM round-trip only paused) | bridge `stop()` now reports `'stopped'` → `onPlaybackStopped` abandons focus |
| MF5 | [x] | Background tab's 5s heartbeat stole transport ownership (ping-pong) | ownership guard in `handleMediaState` "playing" |
| MF6 | [x] | `onCreateWindow` accepted programmatic popups (focus-steal/phishing) | `if (!isUserGesture) return false` |
| SF1 | [x] | WebView/fullscreen kept running on transient `onPause` (battery) | `onPause` pauses when `!hasActiveMediaSession` |
| SF2 | [x] | Resume-from-notification could hit a paused WebView | `hasActiveMediaSession` flag (playing+paused) gates pause; `evalOnMediaTab` resumes WebView/timers |
| SF3 | [x] | Album-art plumbing was a page-controlled SSRF sink | removed `artworkUrl` from `MediaState` + JS entirely (re-add later w/ allowlist) |
| SF4 | [x] | Per-5s-tick metadata alloc + double notification rebuild | metadata pushed only on track change (cache in MainActivity + controller early-return) |
| SF5 | [x] | QR encode ARGB_8888 | switched to `RGB_565` (half memory); + sponsor cache. (async off-thread = follow-up) |
| SF7 | [x] | Analytics default-on (opt-out) w/ persistent UUID | flipped to **opt-in** (default off) |
| SF6 | [ ] | Desktop-UA on ≥480dp + autoplay distraction risk | **DEFERRED**: kept desktop-UA default (serves user's video goal); follow-up = CarUxRestrictions video gating while driving |
| NTH | [x] | Null `mediaController` before `release()` | done. (other nice-to-haves deferred) |

## Decisions log
- **2026-06-13** — Architecture: **stay View-based + targeted polish** for Phase 1; do NOT
  big-bang migrate to Compose. Compose only as an incremental Settings-first pilot. Reason:
  the M3 layer is already solid; the "cheap" feel is from specific defects, not the toolkit.
- **2026-06-13** — Login: cross-browser credential import is **impossible** on stock Android
  (sandbox + Keystore). Deliver "log in once, stay logged in" + Credential Manager instead.
- **2026-06-13** — Video: Widevine L1/HD is a **hard platform limit** in `android.webkit`.
  Document the caps; do not promise HD Netflix.
- **2026-06-13** — User feedback: stock Material 3 feels "outdated." → ran design research.
  Finding: it's unconfigured AND on MDC 1.13 (Expressive APIs need 1.14). Not a Compose problem.
- **2026-06-13** — **Design direction CHOSEN: Expressive Aurora** (M3 Expressive done right,
  View-based, MDC 1.14 bump, one bold accent, custom shape/type, shape-morph, spring motion).
  Fallback was Cockpit Noir. → Sprint A is the implementation plan. **Open: accent color
  (amber-coral #D47C45-ish vs teal #1B5A66-ish) — confirm with user before A4.**
- **2026-06-13** — Quick Wins batch (Q1-Q9) implemented on `feat/quick-wins-batch`; build green.
- **2026-06-13** — Video Chunk 1 (V4, V7) + Chunk 2 (V2, V3, V9 = audio continuity) done, build green.
  Added `androidx.media:media:1.7.0`, foreground `mediaPlayback` service + perms.
- **2026-06-13** — Driving-audio research (`w86usyybp`): the video-in-motion stop is the car host
  stopping/hiding the Activity → WebView lifecycle kills media. Legit fix = MediaSession +
  foreground service decoupled from Activity (built). Driver VIDEO in motion = hard NHTSA/UX
  safety lock (`UX_RESTRICTIONS_NO_VIDEO`), NOT bypassed. Follow-up: AAOS
  `FEATURE_BACKGROUND_AUDIO_WHILE_DRIVING` opt-in beta worth investigating.
- **2026-06-13** — Ad-block research (`w0qxe5ouj`): extensions impossible on android.webkit
  (would need GeckoView/Chromium fork → breaks Widevine). In-app layered blocker is the path.
  General web + FB feed achievable; YouTube ads NOT durably (SSAI). → Sprint AD; **awaiting user
  scope choice** (how far to go, given YouTube maintenance burden).
- **2026-06-13** — Build env: created `local.properties` (sdk.dir, gitignored). gradlew chmod +x.

## Artifacts
- Deep audit (49 findings, 8 agents): session task `wsm42ntc4` output.
- Branch for quick wins: `feat/quick-wins-batch`.
