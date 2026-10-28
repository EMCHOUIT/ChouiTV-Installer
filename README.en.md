<p align="center"><img src="docs/media/logo.png" alt="ChouiTV Installer logo (Ch8TV)" width="132"></p>

<p align="center"><a href="https://github.com/EMCHOUIT/ChouiTV-Installer/releases/latest/download/ChouiTV-Installer.apk"><img src="docs/media/hero-en.png" alt="ChouiTV Installer: install ChouiTV on your TV from your phone" width="100%"></a></p>

<p align="center"><a href="README.md">🇲🇦 العربية</a> &nbsp;·&nbsp; <a href="README.fr.md">🇫🇷 Français</a> &nbsp;·&nbsp; <b>🇬🇧 English</b></p>

<p align="center">
<a href="https://github.com/EMCHOUIT/ChouiTV-Installer/releases/latest"><img src="https://img.shields.io/github/v/release/EMCHOUIT/ChouiTV-Installer?label=installer&color=D6AF6B&labelColor=0A1D14" alt="version"></a>
<img src="https://img.shields.io/badge/Android-8%2B-3F7A5C?labelColor=0A1D14" alt="Android 8+">
<a href="LICENSE"><img src="https://img.shields.io/badge/licence-MIT-D6AF6B?labelColor=0A1D14" alt="MIT"></a>
<a href="https://github.com/EMCHOUIT/ChouiTV-Installer/stargazers"><img src="https://img.shields.io/github/stars/EMCHOUIT/ChouiTV-Installer?style=flat&color=D6AF6B&labelColor=0A1D14" alt="stars"></a>
</p>

<p align="center"><a href="#why">✨ Why</a> · <a href="#journey">🚀 The journey</a> · <a href="#installs">📦 Installed</a> · <a href="#safety">🛡️ Safety</a> · <a href="#cleanup">🧹 Cleanup</a> · <a href="#settings">🔁 Settings</a> · <a href="#faq">❓ FAQ</a> · <a href="#build">🛠️ Build</a> · <a href="#download">⬇️ Download</a></p>

A small app on your phone that finds your TV, cleans it if you want, downloads the **latest version** of [ChouiTV](https://github.com/EMCHOUIT/ChouiTV), installs and sets it up, then copies your settings to another TV. **It guides you step by step, or you choose yourself. No computer, no commands.**

<p align="center"><a href="https://github.com/EMCHOUIT/ChouiTV-Installer/releases/latest/download/ChouiTV-Installer.apk"><img src="https://img.shields.io/badge/%E2%AC%87%20Download%20ChouiTV%20Installer-D6AF6B?style=for-the-badge&labelColor=0A1D14" alt="download"></a><br><sub>For Android 8+ phones · about 1 MB</sub></p>

<a id="why"></a>

## ✨ Why this app?

<table>
<tr><td width="50%" valign="top">🧭<br><b>Guided or free</b><br><sub>A step-by-step assistant, or five spaces to open directly.</sub></td><td width="50%" valign="top">🧹<br><b>A cleanup you control</b><br><sub>One box per app, with readable names, and nothing without your say.</sub></td></tr>
<tr><td width="50%" valign="top">🔁<br><b>Your settings travel</b><br><sub>Save one TV's settings under a name, send them to another, now or later.</sub></td><td width="50%" valign="top">🛡️<br><b>Everything can be undone</b><br><sub>Apps are turned off, not deleted, with a safety net on the TV.</sub></td></tr>
</table>

<a id="journey"></a>

## 🚀 The journey in five screens

<img src="docs/media/journey-en.png" alt="🚀 The journey in five screens" width="100%">

1. **Language**: Arabic, French or English.
2. **The mode**: "Guide me" (a step-by-step assistant, with resume if you leave) or "I choose myself".
3. **The path or the spaces**: "Set up my TV", "Copy from one TV to another", "Send saved settings", "Put an app back", or the five spaces directly.
4. **The cleanup**: you see the plan before any change and tick what you do not use.
5. **Install and settings**: a bar with a percentage, then city, time zone and prayer method.

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
- **Cleanup without risk**: it is optional, turns apps off without deleting them, checks the TV after each group and puts them back if it gets unwell; the "Turn apps back on" space puts an app back whenever you want.
- **Privacy**: no accounts, no ads, no tracking; the key that identifies your phone stays in it.

> [!TIP]
> **When you are done**: turn "Network debugging" off on the TV.


<a id="cleanup"></a>

## 🧹 Clean your TV, safely

<img src="docs/media/cleanup-en.png" alt="🧹 Clean your TV, safely" width="100%">

Some TVs (Xiaomi, TCL and others) ship with preinstalled apps you do not need: tracking, analytics, ads, services that slow the device down. You may also have apps that start with the TV without you using them. **The cleanup is optional, and you decide.**

- **One box per app**: "Preinstalled apps" (ticked for you), "Streaming and entertainment" (Netflix, Prime Video… unticked) and "Your installed apps", with an advice for the ones that start with the TV and slow it down.
- **Readable names**: the app name, its package below.
- **A confirmation before it starts**: you look over the list and leave unticked the ones you use, they stay untouched.
- **Turned off, never deleted**: the TV is checked after each group, put back if it gets unwell, and if the connection is lost it puts everything back by itself within 5 minutes.
- **Always protected**: ChouiTV, Google services, the settings and any home screen.
- **Put an app back any time**: the "Turn apps back on" space reads what is off on the TV itself.

<a id="settings"></a>

## 🔁 Copy your settings to another TV

<img src="docs/media/settings-en.png" alt="🔁 Copy your settings to another TV" width="100%">

Use the **Get the settings** and **Send settings** spaces (or the guided "Copy from one TV to another" path).

- **Save under a name**: the settings of the connected TV (city, prayer, adhan, panels, teams, home shortcuts…) are kept on your phone, nowhere else.
- **Send now or later**: if both TVs are together, send right away; otherwise take the settings with you and send them from "Send saved settings".
- **Preview and undo**: you see the changes before applying them, and the send keeps a copy of the previous settings.
- **Never saved**: the IP address or Wi-Fi name, the serial number, your photos and your tasks.

ChouiTV 3.173 or newer is needed on both TVs.

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

Open the "Turn apps back on" space and tick the ones you want back: it reads what is off on the TV itself, so any phone can do it. A plain-text copy is also on the TV: `/data/local/tmp/chouitv-restore.txt`.
</details>

<details><summary><b>What is the difference between "Guide me" and "I choose myself"?</b></summary>

The first is an assistant that tells you what to do step by step and lets you resume if you leave. The second opens the five spaces directly. You can change the mode at any time.
</details>

<details><summary><b>How do I copy my settings to another TV?</b></summary>

With "Copy from one TV to another": you save the first TV's settings under a name, then send them right away if both TVs are together, or later from "Send saved settings". ChouiTV 3.173 or newer is needed on both TVs; the preview shows before applying and the send can be undone.
</details>

<a id="build"></a>

## 🛠️ Build

JDK 17 and the Android SDK (platform 34):

```bash
git clone https://github.com/EMCHOUIT/ChouiTV-Installer
cd ChouiTV-Installer
./gradlew :app:assembleRelease
```

<a id="download"></a>

<h2 align="center">⬇️ Direct download</h2>

<p align="center"><img src="docs/media/qr-download.png" alt="QR code" width="230"><br><sub>Scan it from your computer to open the link on your phone.</sub></p>

<p align="center"><a href="https://github.com/EMCHOUIT/ChouiTV-Installer/releases/latest/download/ChouiTV-Installer.apk"><img src="https://img.shields.io/badge/%E2%AC%87%20Download%20the%20APK-D6AF6B?style=for-the-badge&labelColor=0A1D14" alt="⬇ Download the APK"></a><br><sub><code>ChouiTV-Installer.apk</code> · about 1 MB</sub></p>

<p align="center"><a href="https://github.com/EMCHOUIT/ChouiTV-Installer/releases/latest"><img src="https://img.shields.io/github/v/release/EMCHOUIT/ChouiTV-Installer?label=version&color=D6AF6B&labelColor=0A1D14" alt="version"></a> <a href="https://github.com/EMCHOUIT/ChouiTV-Installer/releases"><img src="https://img.shields.io/github/downloads/EMCHOUIT/ChouiTV-Installer/total?label=downloads&color=3F7A5C&labelColor=0A1D14" alt="downloads"></a></p>

<p align="center">📱 Android 8 or newer &nbsp;·&nbsp; 🔒 Signed, no account, no ads &nbsp;·&nbsp; 📦 <a href="https://github.com/EMCHOUIT/ChouiTV-Installer/releases">All versions</a></p>

<h3 align="center">Install in three steps</h3>

<table align="center">
<tr><td width="33%" valign="top" align="center">1️⃣<br><br><b>Download</b> the <code>ChouiTV-Installer.apk</code> file on your phone (the button above, or the QR code from another screen).</td><td width="33%" valign="top" align="center">2️⃣<br><br><b>Open it.</b> If Android asks for permission, allow your browser or file manager to install apps once. An "unknown app" warning may appear: the code is open and you can build it yourself (<a href="#build">Build</a>).</td><td width="33%" valign="top" align="center">3️⃣<br><br><b>Open ChouiTV Installer</b> and follow the steps.</td></tr>
</table>

<div align="center">

<details><summary><b>🔍 Check the file</b></summary>

The [release page](https://github.com/EMCHOUIT/ChouiTV-Installer/releases/latest) shows the SHA-256 next to the file. Compare it with the result of:

```bash
sha256sum ChouiTV-Installer.apk                       # Linux, macOS
certutil -hashfile ChouiTV-Installer.apk SHA256       # Windows
```

The signing certificate fingerprint (SHA-256) is the same for every version:

```text
f29f993b7456170455dcc574a2bc5f1f74b97f7995a564f8a839175d068a79c1
```

To check it:

```bash
apksigner verify --print-certs ChouiTV-Installer.apk
```
</details>

</div>

## 💚 Credits and licence

**MIT** licence (EMChouit). Fonts and their licences: [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). The apps it installs are those of [ChouiTV](https://github.com/EMCHOUIT/ChouiTV). **Like the project? Give it a star ⭐, it keeps us going.**
