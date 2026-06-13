# Driftway — overnight relaunch plan (autonomous)

> Authorized 2026-06-14 by the owner: "complete the revamp and relaunch this new app… by the
> morning most of the things will be completely revamped." The owner is asleep; decisions below
> use safe defaults and are reversible. Standing rule still enforced: **no PR is merged before its
> CodeRabbit review completes and its findings are fixed.**

## Decisions locked for this run (defaults — change any in the morning)

| # | Decision | Choice | Why / reversible? |
|---|----------|--------|-------------------|
| D1 | App **name** | **Driftway** (label, menu title, all UI strings) | Locked earlier. |
| D2 | **applicationId / package** (`com.kododake.aabrowser`) | **KEEP for now** (invisible to users); flip to `com.driftway.browser` later on your one-word go | Changing it orphans your current install + is the riskiest, fully-invisible change; not worth gambling the build the night before you test. The product a tester *sees* is 100% Driftway regardless. Fully reversible — it's a staged, separate step. |
| D3 | **Launcher icon** | New Driftway mark (electric-blue), replace the car+magnifier | Blocker for a believable rebrand. |
| D4 | **Update checker** repo | Point at `CreatorGhost/Driftway` (was wrongly `kododake/AABrowser` upstream — a real bug) | Blocker fix; GitHub redirects old links. |
| D5 | **Analytics** (Umami → original author's server) | **Remove** the network send (was opt-in/off-by-default) | Privacy + don't leak to the original creator's server. Reversible. |
| D6 | **Donations/sponsor** (kododake Bitcoin + Sponsors + FUNDING.yml) | **Remove** from UI/links (donate card already gone); **keep** GPLv3 attribution to Kododake | GPLv3 requires keeping attribution; donations to the original owner are removed. |
| D7 | **Repo** | Rename `CreatorGhost/AABrowser` → `CreatorGhost/Driftway` as the final step | Locked earlier; GitHub redirects old URLs + open PRs survive. |
| D8 | **Version** | `versionCode 12`, `versionName "2.2-beta1"`, release tag `v2.2-beta1` | Clean relaunch marker. |

## Execution order (each stage build-verified; PRs wait for CodeRabbit)

```
Stage 0  Home polish + blocker fixes on PR #7 (front-not-ugly + NPE guards)   [in progress]
Stage 1  WAIT for CodeRabbit on #6, #7, #5 → fix findings → merge in order:
            #6 (theme) → #7 (home, stacked) → #5 (popup)  →  main
Stage 2  REBRAND (visible): app_name/menu_title/settings strings/app_credit,
            new launcher icon, update-checker → CreatorGhost/Driftway,
            remove analytics + donations/sponsor + FUNDING.yml, keep GPLv3 attribution
Stage 3  Control bar (Step 2): replace the 3-dots FAB with a hidden + swipe-up
            bottom bar (Back · Home · Tabs · Menu), fixed always-visible handle
Stage 4  Pre-launch audit fixes (P0/P1): defer notification permission until first
            playback; sparse-shelf balance; hide empty shelf labels; long-name/continue
            caps; menu legibility (car-HMI); fix genuinely-broken light-theme surfaces
            (keep the home hero intentionally cinematic-dark)
Stage 5  Build SIGNED release APK (Driftway 2.2-beta1) + GitHub Release
Stage 6  Rename repo → CreatorGhost/Driftway; verify update-checker resolves
--- stretch (if time) ---
Stage 7  Motion layer (press springs + panel transitions)
Stage 8  Read-Aloud TTS flagship
Stage 9  Settings polish
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
