# AA Browser — Relaunch Research & Roadmap (2026-06-13)

> Source: 6-agent web research (car-native apps incl. CarTube, competitor browsers, platform
> capabilities, stability practices, differentiators/naming) + product synthesis. This is the
> blueprint for the rename + relaunch. Honest about Android Auto platform limits.

## Positioning
A **car-native media-hub browser** for Android Auto / Automotive: a polished, stable, AMOLED
launcher of the media services people actually want in the car (YouTube, Netflix, Twitch, web
video, news) — big glanceable tiles, voice-driven, with a real tabbed browser underneath. Its
defining promise sits on the platform's own line between parked and driving: **park to watch
full-screen video; drive and keep listening** — media audio and a flagship **Read-Aloud** mode
continue safely through the speakers. For people who today sideload CarStream/Fermata or admire
iOS CarTube but want one stable, no-root, open-source Android app. It does **not** pretend to be a
Play-Store AA app: it's a sideloaded, parked-aware, audio-while-driving media browser that turns
"stationary use only" into **"watch when parked, listen when you drive."**

## ⏰ Timing (why now)
Google announced **official browser + video app categories for Android Auto, rolling out with
Android 16** (Vivaldi + Chrome as launch partners). Once a browser is one tap away from Play, "a
browser exists for AA" is no longer the pitch. We must win on **Read-Aloud, Media Hub, voice,
AMOLED polish, SponsorBlock/ad-block, and stability**, and time the relaunch to that window.

## 🚩 The 3 flagship features to lead with
1. **Read-Aloud / Listen-to-Articles** (on-device TTS through our existing MediaSession) — the one
   premium feature *legally usable while driving* (audio, not video). No rival (Vivaldi / CarStream
   / Fermata / CarTube) advertises it. Flips "park to use" into "drive and listen." *Reuses the
   onPageFinished JS-injection hook + MediaSessionController we already shipped.*
2. **Media Hub start page** — curated big-tile launcher (YouTube/Netflix/Twitch/web/news + a
   "Continue" row). The core "this is OURS" identity; evolves the existing 6-card start page. Apply
   the neon **Aurora** styling here as the hero surface.
3. **Voice-first navigation** — surface the already-built `SpeechRecognitionBridge` as a large
   primary mic button ("open YouTube", "search …", "go to …") so users dodge the laggy head-unit
   keyboard.

## Feature roadmap (feasibility · driving-safety)
**Flagship**
- Read-Aloud TTS — moderate · audio-while-driving
- Media Hub start page — easy · parked-only
- Voice-first nav — easy · parked-only

**Core**
- MediaBrowserService + `onPlayFromSearch` (browsable Saved/Bookmarks/History in the car media UI; the only sanctioned voice hook for a sideloaded app) — moderate · audio-while-driving
- Reader Mode (Readability.js; also the clean text source for Read-Aloud — build together) — moderate · parked-only
- Quick-resume "Continue watching/listening" row (resume last page/tab/video + TTS position) — easy · parked-only

**Nice-to-have**
- On-device translation (ML Kit, offline, privacy-safe) — moderate · parked-only
- Find-in-page (`findAllAsync`) — easy · parked-only
- Reading List → "read these to me" TTS queue (local only) — moderate · audio-while-driving
- One-tap fullscreen + oversized gesture controls + clean parked→driving handoff — easy · parked-only
- Per-site settings (desktop-UA/zoom/dark by host) + download manager — moderate · parked-only
- Optional cosmetic "Pro" pack (extra TTS voices/themes; never gates safety features; no ads) — easy
- (Defer) Tab groups + Credential Manager passkeys — hard · low car value

## ✅ Pre-relaunch STABILITY checklist (owner's #1 ask: "everything stable")
- **`onRenderProcessGone()` in ConfiguredWebView — MISSING, #1 fix.** On renderer crash/OOM: remove dead WebView, destroy, recreate fresh + reload last URL, return true so the app process survives. No tight reload loop.
- `setRendererPriorityPolicy(RENDERER_PRIORITY_BOUND, waivedWhenNotVisible=true)` so hidden tabs are reclaimed first.
- `WebView.saveState`/`restoreState` per tab + `onSaveInstanceState` — persist heavy state to disk; keep the Bundle tiny (avoid TransactionTooLargeException). *(roadmap F2)*
- Lazy tab materialization + LRU eviction; destroy background renderers; target <250 MB steady. *(F3)*
- Audit MainActivity tab lifecycle for destroy()/leak discipline (LeakCanary + Profiler).
- **CarUxRestrictions / CarConnection listener — MISSING.** When driving, hide/block browsing UI and degrade to audio-only (MediaSession already keeps audio alive). Makes the parked/driving promise honest.
- StrictMode (debug) to catch main-thread I/O; move prefs/icon cache off-thread; DataStore for hot prefs. *(F4/F5)*
- Keep `startForeground(...MEDIA_PLAYBACK)` synchronous on every start path (already done — preserve in refactors).
- **Opt-in** crash + ANR reporting (self-hosted ACRA/Sentry) behind the existing analytics toggle (default OFF); strip PII/URLs.
- Stand up **CI** (`.github/workflows` is empty): build signed release + lint + tests → publish to GitHub Releases (Obtainium). Real semantic version for relaunch.
- Tests beyond stubs: AAOS-emulator Espresso (tab manager no-dead-ends), a driving-state test (UI blocked / audio-only when Moving), AMOLED golden tests at 800×480; DHU manual checklist.
- Validate car-quality numbers: launch ≤10s, content ≤10s, button ≤2s; touch ≥64dp, ≥24dp apart, fonts ≥24sp; window insets; no heads-up notifications while driving.
- Pinned deps; CHANGELOG per tag; staged rollout (beta tag → latest); **do NOT rotate the release keystore** (Obtainium pins the cert).
- QA matrix: Android 13/14/16 + recent AA updates; multi-tab + desktop-UA video + Read-Aloud surviving parked→driving end-to-end.

## Name ideas (verify trademark/USPTO classes 9/42 + Play + domain before committing)
- **Aurova** — ties to the neon "Expressive Aurora" identity; short, brandable, no Tube/Car/Google echo.
- **Cabana** — in-cabin "your media cabin"; warm, memorable.
- **Nocturne** — true-black AMOLED / night-drive aesthetic; premium.
- **Driftway** — road + flow; reads as a real product, easy to say aloud (matters for voice).
- **Lumicar** — light/luminance + car; descriptive yet ownable.
- **Halo Drive** — cockpit "halo" glow + driving.
- **Idlecast** — wink at the parked ("idle") + casting reality; self-explanatory.
- **Wayfare** — journey + "fare" (content); trademark-light fallback.

## ⚠️ Honest constraints (don't market against these)
- **Video-in-motion is banned** (driver-distraction rules) AND sensor-gated; the platform pauses WebView while driving. Durable value = parked-video + audio-while-driving. Never imply in-motion video.
- **WebView DRM = Widevine L3 (SD only)** — Netflix ~480p, Crunchyroll ~720p. No HD/4K. Document it.
- **Not Play-Store distributable** ("video, games and browsers are not allowed" for AA). Runs only because the manifest declares NAVIGATION + ACCESS_SURFACE + MAP_TEMPLATES and draws the WebView on the nav Surface — a sideload-only workaround. Distribution stays sideload (APK / Obtainium / GitHub Releases) + AAAD whitelist.
- **Distribution is fragile**: AAAD free tier throttles to 1 app / 30 days; Google's 2025-2026 unverified-developer sideload block + stricter AA verification can break install/launch anytime. Ship our own clear sideload+whitelist guide + a "why can't AA see me?" self-diagnostic.
- **Official "background audio while driving"** is an invite-only Play-partner beta — NOT for sideloaded apps. We achieve the effect with our OWN MediaSession + foreground service (market that).
- **No arbitrary apps / no generic browser template** while driving; Car App Library templates are category-gated + Play-reviewed. Only voice hook open to us: `MediaSession.onPlayFromSearch`. PiP / passenger displays are OEM/AAOS-controlled, not addressable by a sideload.
- **Cross-browser/device login import impossible** (sandbox + Keystore). Offer "log in once, stay logged in" + quick site switching; don't market profiles/sync.
- **YouTube video ads not durably blockable** (first-party SSAI). Don't promise YouTube ad-free.

## Suggested relaunch sequence
1. **Stability hardening first** (onRenderProcessGone, saveState, lazy tabs, CarUxRestrictions, crash reporting, CI) — the "everything stable" foundation.
2. **Flagship build**: Read-Aloud + Reader Mode (together) → Media Hub start page (with neon Aurora) → Voice-first mic.
3. **Identity**: pick name → icon/splash/theme → MediaBrowserService browsable tree.
4. **Relaunch** with this doc + `RELAUNCH_FEATURES.md` as the landing/changelog copy, timed near the Android-16 AA-browser-category window.
