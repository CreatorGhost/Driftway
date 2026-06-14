# Driftway — overnight relaunch plan (autonomous)

> Authorized 2026-06-14 by the owner: "complete the revamp and relaunch this new app… by the
> morning most of the things will be completely revamped." The owner is asleep; decisions below
> use safe defaults and are reversible. Standing rule still enforced: **no PR is merged before its
> CodeRabbit review completes and its findings are fixed.**

## ☀️ MORNING STATUS (2026-06-14)

**Driftway 2.2-beta1 is LIVE and installable:** https://github.com/CreatorGhost/Driftway/releases/tag/v2.2-beta1

| Stage | Status |
|---|---|
| 0 Home polish + blockers | ✅ done (NPE guard, IP-host title, Continue cap, Photo button off front) |
| 1 Merge PRs to main | ⚠️ partial — #5 CR-reviewed + 3 findings fixed; **#6/#7 rate-limited by CR all night → NOT merged** (rule honored). Release built from integrated `release/driftway` branch instead. |
| 2 Rebrand | ✅ done — package flip `com.driftway.browser`, name/icon/strings, update-checker → CreatorGhost/Driftway, analytics + donations de-leaked |
| 3 Control bar (swipe-up) | ⛔ not done (next priority) |
| 4 Pre-launch fixes | ⚠️ partial — home fixes done; **notification-perm deferral + menu ≥16sp legibility + full photo-feature code removal still pending** |
| 5 Signed release build | ✅ done — `Driftway-2.2-beta1.apk`, verified signed |
| 6 Thorough emulator test | ✅ functional pass — launches/responsive, branded home both orientations, browsing (example.com + YouTube), no crashes. **Deep press-play audio playback = unchanged media code from the verified audio-focus fix; confirm on your phone.** |
| 7 Rename repo + publish | ✅ done — repo = CreatorGhost/Driftway; release published; updater `/releases/latest` resolves it |

**Verified:** published signed APK installs + launches; in-app updater will detect v2.2-beta1.
**Your follow-ups (told me you'd provide):** your own analytics endpoint + sponsor/Bitcoin (TODO seams left in code).
**Top remaining build work:** Stage 3 control bar → Stage 4 (defer notification prompt, menu legibility, finish photo-feature removal) → then CR-merge #5/#6/#7 to main and reconcile the rebrand onto main.

## Decisions locked for this run (defaults — change any in the morning)

| # | Decision | Choice (FINAL, owner-confirmed 2026-06-14) | Notes |
|---|----------|--------|-------------------|
| D1 | App **name** | **Driftway** (label, menu title, all UI strings) | Locked. |
| D2 | **applicationId / package** | **FLIP to `com.driftway.browser`** (full clean rebrand) | Installs as a separate new app; owner will reinstall fresh. Done AFTER the open PRs merge (package move conflicts otherwise). |
| D3 | **Launcher icon** | New Driftway mark (electric-blue), replace the car+magnifier | Vector foreground; keep #1565FF adaptive background. |
| D4 | **Update checker** repo | Point at `CreatorGhost/Driftway` (was wrongly `kododake/AABrowser` upstream — a real bug) | Blocker fix; GitHub redirects old links. |
| D5 | **Analytics** (Umami → original author's server) | **Remove now**; repoint to OWNER's endpoint when provided | Owner chose "repoint to me" — he'll give the endpoint in the morning. Until then the network send is removed (no leak to the original creator). Leave a clear TODO + config seam. |
| D6 | **Donations/sponsor** (kododake Bitcoin + Sponsors + FUNDING.yml) | **Remove now**; repoint to OWNER when provided | Owner will give his Bitcoin/sponsor details in the morning. Keep GPLv3 attribution to Kododake (legally required). |
| D7 | **Repo + release** | **Rename** `CreatorGhost/AABrowser` → `CreatorGhost/Driftway` AND **publish** signed Release `v2.2-beta1` | Owner wants an installable build by morning. Old URLs redirect; open PRs survive. |
| D8 | **Version** | `versionCode 12`, `versionName "2.2-beta1"`, release tag `v2.2-beta1` | Clean relaunch marker. |
| D9 | **Photo / ambient mode** | **Remove the feature entirely** (button + photo-only mode + custom-background + its settings/prefs) | Owner found it confusing; home is always the cinematic Driftway hero. Button hidden in Stage 0; full removal in Stage 2. |

## Execution order (each stage build-verified; PRs wait for CodeRabbit)

```
Stage 0  Home polish + blocker fixes on PR #7 (front-not-ugly + NPE guards)   [DONE ✅ 5634c9d]
Stage 1  WAIT for CodeRabbit on #6, #7, #5 → fix findings → merge in order:
            #6 (theme) → #7 (home, stacked) → #5 (popup)  →  main
Stage 2  REBRAND: PACKAGE FLIP com.kododake.aabrowser → com.driftway.browser (dir move +
            all refs + intent actions + tests + gradle ns/applicationId); app_name/menu_title/
            settings strings/app_credit → Driftway; new launcher icon; update-checker →
            CreatorGhost/Driftway; REMOVE analytics + donations/sponsor + FUNDING.yml
            (leave owner-config TODO seams); REMOVE photo/ambient feature entirely;
            keep GPLv3 attribution; versionCode 12 / versionName 2.2-beta1
Stage 3  Control bar (Step 2): replace the 3-dots FAB with a hidden + swipe-up
            bottom bar (Back · Home · Tabs · Menu), fixed always-visible handle
Stage 4  Pre-launch audit fixes: defer notification permission until first playback;
            sparse-shelf balance; menu legibility (car-HMI ≥16sp); fix genuinely-broken
            light-theme surfaces (keep the home hero intentionally cinematic-dark)
Stage 5  Adversarial review workflow → fix → build SIGNED release APK (Driftway 2.2-beta1)
Stage 6  ★ THOROUGH EMULATOR TEST (MANDATORY before release is announced) ★
            - Video playback: open YouTube, press play, confirm it KEEPS playing (the
              play-then-stop regression) for ≥30s; check audio continues in background.
            - Pop-up / ad handling: visit an ad-heavy / sketchy site, confirm pop-ups &
              pop-unders are blocked and no dialog spam; SponsorBlock if enabled.
            - Navigation: address bar, search, back/forward, tabs (open/close/switch),
              bookmarks, settings open + toggles, QR.
            - Rebrand: launcher label = Driftway, new icon, no "AABrowser" in any UI,
              update-check points at the new repo.
            - Both orientations (portrait phone + landscape head unit).
            Capture screenshots + a logcat scan for crashes/ANRs. Fix anything found,
            rebuild, re-test until clean.
Stage 7  Rename repo → CreatorGhost/Driftway; publish GitHub Release; verify update-checker
            resolves the new release.
--- stretch (if time) ---
Stage 8  Motion layer · Stage 9  Read-Aloud TTS · Stage 10  Settings polish
```

## Workflows / automation used (in order)
1. `rebrand-surface-audit` ✅ done — full inventory of every old-identity reference (70 pkg, 20 name, 38 author/link, 16 icon, 29 runtime).
2. `prelaunch-ux-audit` ✅ done — 5-lens UI/UX + readiness audit (home empty-states, visual consistency, car-HMI, crash/correctness, rebrand-gate).
3. CR-watcher (background poll) — re-invokes on CodeRabbit completion to fix findings + merge (Stage 1).
4. Post-implementation **adversarial review** workflow before the release build (Stage 5 gate).

## Audit blockers being fixed (from prelaunch-ux-audit)
- BLOCKER update-checker points at upstream `kododake/AABrowser`, not the fork → update detection broken (Stage 2/D4).
- BLOCKER `applyDynamicStartPageGradientBackground` missing `binding` init guard → NPE on rotation (Stage 0).
- First-run **notification-permission dialog** greets the empty front (and shows old name) → defer to first playback (Stage 4).
- HIGH Continue chip shows raw "10"/"192" for IP hosts (displayTitleForUrl) (Stage 0).
- Dead donate code still wired though hidden → remove (Stage 2).

## State / continuity
- Branch state: `redesign/home-cinematic` (PR #7, stacked on #6); `feat/popup-annoyance-blocker` (#5).
- The home left/right two-pane + cinematic blue-glow hero is committed + pushed (verified on emulator, both orientations).
- Tracker of record: this file + `docs/V3_REDESIGN.md` + `docs/OPTIMIZATION_ROADMAP.md`.
