# Driftway

[![Android](https://img.shields.io/badge/Android-15%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://www.android.com/)
[![License](https://img.shields.io/badge/License-GPLv3-blue.svg?style=for-the-badge)](https://www.gnu.org/licenses/gpl-3.0)
[![Release](https://img.shields.io/github/v/release/CreatorGhost/Driftway?style=for-the-badge&color=1565FF)](https://github.com/CreatorGhost/Driftway/releases/latest)

**A premium, car‑native browser for parked head units.**
Driftway turns a parked car's screen into a cinematic media hub — YouTube, Netflix, Crunchyroll, your AI assistants — with a true‑black AMOLED interface built for glancing, not squinting.

<p align="center">
  <img src="docs/screenshots/home.png" alt="Driftway Media Hub — the cinematic, car-native home screen with Entertainment and AI shelves" width="100%">
</p>

> [!WARNING]
> **For parked use.** Driftway is designed for when the car is stationary or for passengers. Video playback requires explicit passenger confirmation. **If you are driving, keep your eyes on the road** — use audio and voice only.

---

## ✨ Features

- 🎬 **Cinematic Media Hub home** — a landscape, left/right layout with an **Entertainment** shelf (YouTube · Netflix · Crunchyroll · Animetsu · Twitch) and an **AI** shelf (ChatGPT · Claude · Gemini · Perplexity · Copilot), real site logos, and a "Continue" row.
- 🎛️ **Swipe‑up control bar** — a fixed, always‑visible handle reveals **Back · Home · Tabs · Menu**. No hunting for a floating button.
- 🌌 **AMOLED‑dark design** — one electric‑blue accent on true black, a soft squircle shape language, and a glanceable type scale. Smooth, lightweight motion that stays fluid on any head unit.
- 🛡️ **Ad, tracker & pop‑up blocking** — blocks ad/tracker requests, hides leftover ad slots, and stops pop‑up / pop‑under / dialog spam on sketchy sites. *(YouTube's own video ads can't be reliably blocked — see Limitations.)* Includes optional **SponsorBlock** for YouTube.
- 🔊 **Background audio** — audio keeps playing when the app is backgrounded or the screen is off, with full media‑notification controls.
- 🔒 **Passenger video consent gate** — video only starts after a passenger/non‑driver confirms; muted previews never interrupt browsing.
- 🗣️ **Read‑Aloud (TTS)** — have the current page's text read to you. Listen to articles while driving.
- 🎙️ **Voice search** — tap the mic and speak a search or a URL.
- 🗂️ **Tabs, bookmarks & more** — real multi‑tab browsing with a tab manager, dashboard bookmarks, QR share, smart desktop mode, and a global display‑scale control.

---

## 📸 Screenshots

> Captured on a landscape head‑unit display — the layout Driftway is built for.

<table>
  <tr>
    <td width="50%" valign="top">
      <img src="docs/screenshots/menu.png" alt="Driftway menu — a clean vertical list of actions">
      <br><sub><b>Sleek menu</b> — every control one tap away, no browser clutter.</sub>
    </td>
    <td width="50%" valign="top">
      <img src="docs/screenshots/control-bar.png" alt="Driftway swipe-up control bar">
      <br><sub><b>Swipe‑up control bar</b> — Back · Home · Tabs · Menu, always within reach.</sub>
    </td>
  </tr>
  <tr>
    <td width="50%" valign="top">
      <img src="docs/screenshots/browsing.png" alt="Browsing YouTube inside Driftway">
      <br><sub><b>The real web</b> — full sites and real video, not a stripped‑down shell.</sub>
    </td>
    <td width="50%" valign="top">
      <img src="docs/screenshots/settings.png" alt="Driftway settings and preferences">
      <br><sub><b>Settings</b> — legible AMOLED‑dark preferences and built‑in updates.</sub>
    </td>
  </tr>
</table>

---

## 📦 Install

> **Requires Android 15+.** Sideload only.

1. Download the latest **`Driftway-x.y.apk`** from **[GitHub Releases](https://github.com/CreatorGhost/Driftway/releases/latest)**.
2. Install it. To **update**, install the new APK *over* the existing one (don't uninstall) so your logins and data are kept.

<details>
<summary><b>Enabling sideloaded apps on Android Auto</b></summary>

1. Open your phone's **Android Auto** settings.
2. Tap the **Version** entry **10 times** to unlock Developer settings.
3. Open the **⋮ menu → Developer settings**.
4. Enable **Unknown sources**.
</details>

---

## ⚠️ Limitations (honest)

- 📺 **Video in motion is blocked by the platform** — this is an Android Auto / AAOS rule, not a Driftway choice. Driftway is for parked use.
- 🔐 **Widevine L3 only** — DRM video (Netflix etc.) is limited to standard definition by the head unit's DRM level.
- 🚫 **YouTube video ads aren't blocked** — they're served inside the video stream and can't be reliably removed in a WebView. General web ads, trackers, and pop‑ups *are* blocked.
- 🚘 **Host behavior varies** — exact projection behavior differs by car/OEM and region.

---

## 🔒 Privacy

- **No analytics, no trackers.** Driftway does not collect usage data or phone home.
- **No URL/keystroke tracking** — your browsing stays on your device.

---

## 🙏 Credits

Driftway is a complete redesign built on **[AABrowser](https://github.com/kododake/AABrowser) by Kododake**, the original Android‑Auto WebView browser. Huge thanks to Kododake and the original contributors — Driftway exists because of that groundwork. Licensed under **GPLv3**; original copyright and attribution are preserved.

## 🤝 Contributing

Bug reports, ideas, and PRs are welcome — open an issue or a pull request.

## 📄 License

Released under the **[GNU General Public License v3.0](https://www.gnu.org/licenses/gpl-3.0)**. If you reuse this code, you must keep it open‑source under GPLv3 and preserve attribution.

---

**Stay parked, keep your eyes on the road, and enjoy the ride. 🚗💙**
