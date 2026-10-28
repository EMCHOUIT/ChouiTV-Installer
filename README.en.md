<p align="center"><a href="README.md">🇲🇦 العربية</a> · <a href="README.fr.md">🇫🇷 Français</a> · <b>🇬🇧 English</b></p>

<h1 align="center">ChouiTV Installer</h1>

<p align="center"><b>Install ChouiTV on your TV from your phone, no computer needed.</b></p>

<p align="center">
<a href="https://github.com/EMCHOUIT/ChouiTV-Installer/releases/latest"><img src="https://img.shields.io/github/v/release/EMCHOUIT/ChouiTV-Installer?label=installer&color=D6AF6B&labelColor=0A1D14" alt="Installer version"></a>
<img src="https://img.shields.io/badge/Android-8%2B-3F7A5C?labelColor=0A1D14" alt="Android 8+">
<a href="LICENSE"><img src="https://img.shields.io/badge/licence-MIT-D6AF6B?labelColor=0A1D14" alt="MIT"></a>
</p>

## ✨ What is it?

An Android phone app that finds your TV on the Wi-Fi, downloads the **latest version** of [ChouiTV](https://github.com/EMCHOUIT/ChouiTV), installs it and sets it up, in about two minutes, with no computer and no commands.

## 🖼️ Screenshots

<table><tr><td align="center"><img src="docs/media/language.png" width="170"></td><td align="center"><img src="docs/media/prepare.png" width="170"></td><td align="center"><img src="docs/media/install.png" width="170"></td><td align="center"><img src="docs/media/customise.png" width="170"></td><td align="center"><img src="docs/media/done.png" width="170"></td><td align="center"><img src="docs/media/report.png" width="170"></td></tr></table>

## ⬇️ Download

Download [**ChouiTV-Installer.apk**](https://github.com/EMCHOUIT/ChouiTV-Installer/releases/latest/download/ChouiTV-Installer.apk) on an Android phone (8 or newer) and open it. The phone may ask you to allow installs from this source.

## 🚀 How it works

1. **Language**: Arabic, French or English.
2. **Prepare the TV**: turn on "USB debugging" and "Network debugging" (explained in the app).
3. **Connect**: the app finds the TV on the Wi-Fi; you press "Allow" on the TV screen.
4. **Install**: a progress bar with a percentage and 8 steps, from the check to the final verification.
5. **Make it yours**: city, time zone (Africa/Casablanca), prayer calculation method, or import a settings file.
6. **Done**: a thank-you, a request for a GitHub star, and the optional sharing of your TV's result.

## 📦 What it installs

The **home screen**, the **screensaver and settings**, and the **Channels app** (311 channels), then makes ChouiTV the default home. It reads the version from [`latest.json`](https://raw.githubusercontent.com/EMCHOUIT/ChouiTV/main/latest.json) and checks the SHA-256 of every file.

## 🛡️ Safety

- **Safety net**: before turning the TV's original home off, the app arms a script on the TV that puts it back by itself after 5 minutes if ChouiTV does not open.
- If a TV refuses to turn its home off (like the Nokia Streaming Box 8000), it keeps it and the user is told.
- No accounts, no ads, no tracking. The key that identifies your phone stays in the phone.
- Turn "Network debugging" off on the TV when you are done.

## 📮 Share your TV's result

At the end you can share your TV's result. **If you ran into a problem**, your report helps us find a fix. **If everything went well**, it helps us suggest new features for your TV in the next versions. You see **everything that will be sent before it goes** (brand and model, Android, versions, the result of the steps); **never** the IP address, the Wi-Fi name, the serial number or your city. You press "Submit" yourself. The list is in [Tested TVs](https://github.com/EMCHOUIT/ChouiTV/blob/main/docs/TESTED-TVS.en.md).

## 🛠️ Build

JDK 17 and the Android SDK (platform 34):

```bash
git clone https://github.com/EMCHOUIT/ChouiTV-Installer
cd ChouiTV-Installer
./gradlew :app:assembleRelease
```

## 💚 Credits and licence

**MIT** licence (EMChouit). Fonts and their licences: [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). The apps it installs are those of [ChouiTV](https://github.com/EMCHOUIT/ChouiTV).
