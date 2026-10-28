<p align="center"><a href="https://github.com/EMCHOUIT/ChouiTV-Installer/releases/latest/download/ChouiTV-Installer.apk"><img src="docs/media/hero-en.png" alt="ChouiTV Installer: install ChouiTV on your TV from your phone" width="100%"></a></p>

<p align="center"><a href="README.md">🇲🇦 العربية</a> &nbsp;·&nbsp; <a href="README.fr.md">🇫🇷 Français</a> &nbsp;·&nbsp; <b>🇬🇧 English</b></p>

<p align="center">
<a href="https://github.com/EMCHOUIT/ChouiTV-Installer/releases/latest"><img src="https://img.shields.io/github/v/release/EMCHOUIT/ChouiTV-Installer?label=installer&color=D6AF6B&labelColor=0A1D14" alt="version"></a>
<img src="https://img.shields.io/badge/Android-8%2B-3F7A5C?labelColor=0A1D14" alt="Android 8+">
<a href="LICENSE"><img src="https://img.shields.io/badge/licence-MIT-D6AF6B?labelColor=0A1D14" alt="MIT"></a>
<a href="https://github.com/EMCHOUIT/ChouiTV-Installer/stargazers"><img src="https://img.shields.io/github/stars/EMCHOUIT/ChouiTV-Installer?style=flat&color=D6AF6B&labelColor=0A1D14" alt="stars"></a>
</p>

<p align="center"><a href="#why">✨ Why</a> · <a href="#journey">🚀 The journey</a> · <a href="#installs">📦 Installed</a> · <a href="#safety">🛡️ Safety</a> · <a href="#cleanup">🧹 Cleanup</a> · <a href="#faq">❓ FAQ</a> · <a href="#build">🛠️ Build</a></p>

A small app on your phone that finds your TV, downloads the **latest version** of [ChouiTV](https://github.com/EMCHOUIT/ChouiTV), installs it and sets it up. **No computer, no commands, in about two minutes.**

<p align="center"><a href="https://github.com/EMCHOUIT/ChouiTV-Installer/releases/latest/download/ChouiTV-Installer.apk"><img src="https://img.shields.io/badge/%E2%AC%87%20Download%20ChouiTV%20Installer-D6AF6B?style=for-the-badge&labelColor=0A1D14" alt="download"></a><br><sub>For Android 8+ phones · about 650 KB</sub></p>

<a id="why"></a>

## ✨ Why this app?

<table>
<tr><td width="50%" valign="top">💻<br><b>No computer</b><br><sub>Everything happens on the phone: connect, download, install, set up.</sub></td><td width="50%" valign="top">📊<br><b>You see everything</b><br><sub>A global percentage and eight steps moving forward one by one in front of you.</sub></td></tr>
<tr><td width="50%" valign="top">🛡️<br><b>With a safety net</b><br><sub>If the new home does not open, the TV puts its own back by itself.</sub></td><td width="50%" valign="top">🌍<br><b>In your language</b><br><sub>Arabic, French and English, with the same care.</sub></td></tr>
</table>

<a id="journey"></a>

## 🚀 The journey in five steps

<img src="docs/media/journey-en.png" alt="🚀 The journey in five steps" width="100%">

1. **Language**: Arabic, French or English.
2. **Prepare the TV**: turn on "USB debugging" and "Network debugging"; it is explained in the app.
3. **Connect**: the app finds the TV on the Wi-Fi, and you press "Allow" on its screen.
4. **Install**: a bar with a percentage and eight steps, from the check to the final verification.
5. **Make it yours, clean up (optional), then done**: city, time zone, prayer method; an optional cleanup of the preinstalled apps; a thank-you.

<a id="installs"></a>

## 📦 What it installs

| App | Its role |
|---|---|
| **Home screen** | the TV's first screen, with prayer, listening and dhikr |
| **Screensaver and settings** | dhikr, guided tasbih and wisdom from history |
| **Channels app** | 311 live Arabic channels |

The app reads the version from [`latest.json`](https://raw.githubusercontent.com/EMCHOUIT/ChouiTV/main/latest.json): it always installs the **latest version** without being rebuilt, and checks the SHA-256 of every file before sending it.

<a id="safety"></a>

## 🛡️ Safety

- **Safety net**: before turning the TV's original home off, the app arms a script on the TV that puts it back by itself after 5 minutes if ChouiTV does not open.
- **Respect for your TV**: if it refuses to turn its home off (like the Nokia Streaming Box 8000), it keeps it and you are told.
- **Cleanup without risk**: it is optional, turns apps off without deleting them, checks the TV after each group and puts them back if it gets unwell.
- **Privacy**: no accounts, no ads, no tracking; the key that identifies your phone stays in it.

> [!TIP]
> **When you are done**: turn "Network debugging" off on the TV.


<a id="cleanup"></a>

## 🧹 Clean your TV, safely

<img src="docs/media/cleanup-en.png" alt="🧹 Clean your TV, safely" width="100%">

Some TVs (Xiaomi, TCL and others) ship with preinstalled apps you do not need: tracking, analytics, ads, services that slow the device down. After the install the app offers an **optional cleanup**.

- **Turned off, never deleted**: every app comes back in one tap from "Restore" on the first screen.
- **You see the plan first**: apps are grouped (tracking, ads, preinstalled, optional) and you choose.
- **Careful lists**: ChouiTV's list for TCL, community lists for your TV's model, and when the model is unknown only what several lists agree on.
- **Always protected**: ChouiTV, Google services, the settings and any home screen.
- **The TV is checked after each group**: if it gets unwell the group is put back and the cleanup stops; if the connection is lost the TV puts everything back by itself within 5 minutes.

The restore point is kept on the phone and on the TV.

<a id="faq"></a>

## ❓ FAQ

<details><summary><b>I can't find my TV in the list</b></summary>

Check that the phone and the TV are on the **same Wi-Fi** and that "Network debugging" is on. You can also type the TV's address by hand.
</details>

<details><summary><b>The "Allow" window does not show on the TV</b></summary>

The app waits two minutes. If the window does not appear, restart the TV and try again: an earlier request may be stuck.
</details>

<details><summary><b>The Home button does not open ChouiTV</b></summary>

Some TVs force their own home. The app tells you at the end, and you find ChouiTV in the TV's apps. In any case, the TV puts its original home back by itself if the new one does not work.
</details>

<details><summary><b>How do I go back?</b></summary>

Turn the TV's home back on in its settings, or use `optimize/reset.sh` from [ChouiTV](https://github.com/EMCHOUIT/ChouiTV).
</details>

<details><summary><b>Is the cleanup safe?</b></summary>

It is optional, turns apps off without deleting them, shows you the plan first and checks the TV after each group. But an app that is off no longer updates, and a system update can turn some back on. Some TVs would lose their saved Wi-Fi if the setup wizard were turned off, so it is protected too.
</details>

<details><summary><b>How do I undo a cleanup?</b></summary>

From the first screen choose "Restore after a cleanup", then put one app or all of them back. A plain-text copy is also on the TV: `/data/local/tmp/chouitv-restore.txt`.
</details>

<a id="build"></a>

## 🛠️ Build

JDK 17 and the Android SDK (platform 34):

```bash
git clone https://github.com/EMCHOUIT/ChouiTV-Installer
cd ChouiTV-Installer
./gradlew :app:assembleRelease
```

## 💚 Credits and licence

**MIT** licence (EMChouit). Fonts and their licences: [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). The apps it installs are those of [ChouiTV](https://github.com/EMCHOUIT/ChouiTV). **Like the project? Give it a star ⭐, it keeps us going.**
