# Sacred Direction

Android app for **Qibla direction**, **prayer times**, a **home-screen widget**, and **Dua** with audio.

Available on [Huawei AppGallery](https://developer.huawei.com/consumer/en/service/josp/agc/index.html) (package: `com.example.qiblaapp2`).

## Features

- **Map & Qibla** — direction and distance to the Kaaba, line on OpenStreetMap (OSMDroid)
- **Prayer times** — Fajr, Dhuhr, Asr, Maghrib, Isha with optional adhan alarms
- **Widget** — prayer times on the home screen (updates after reboot)
- **Dua** — Arabic text, translation, and audio playback
- **Settings** — Asr calculation method, fixed Fajr/Isha angles, About, Privacy Policy

## Tech stack

- Kotlin, XML layouts (no Compose)
- [OSMDroid](https://github.com/osmdroid/osmdroid) + OpenStreetMap Mapnik tiles
- [PrayTimes](http://www.praytimes.org) (via `PrayerTimesCalculator`)
- `TimezoneMapper` for local timezone from coordinates
- minSdk 24, targetSdk 34

## Project structure

```
app/src/main/java/com/example/qiblaapp2/
  MainActivity.kt           — map / Qibla
  PrayerTimesActivity.kt    — prayer times & alarms
  DuaActivity.kt            — Dua screen
  SettingsActivity.kt       — app settings
  PrayerTimesWidgetProvider.kt — home screen widget
```

## Build

1. Open the project in **Android Studio**
2. Sync Gradle
3. Run **debug** on a device, or **Generate Signed APK** for release

Release signing keystore is **not** included in this repository. Keep your `.jks` file local and never commit it.

Current version: **1.1** (`versionCode` 3)

## Privacy

Privacy policy (in-app and online): [telegra.ph/Privacy-Policy-09-18-69](https://telegra.ph/Privacy-Policy-09-18-69)

Contact: solemetra@gmail.com

## License

Private project. All rights reserved unless stated otherwise.
