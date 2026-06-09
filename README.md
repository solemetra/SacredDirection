# Sacred Direction

Android app for **Qibla direction**, **prayer times**, a **home-screen widget**, and **Dua** with audio.

Available on [Huawei AppGallery](https://developer.huawei.com/consumer/en/service/josp/agc/index.html) (package: `com.example.qiblaapp2`).

## Features

- **Map & Qibla** — GPS bearing, direction name, line to the Kaaba on the map (OSMDroid; no magnetometer compass)
- **Prayer times** — Fajr, Dhuhr, Asr, Maghrib, Isha; optional adhan alarms; compact header card with city, date, sunrise & sunset on one line
- **Hijri date** — Umm al-Qura calendar; tap date on Prayer tab to switch Gregorian / Hijri; Settings adjustment ±1 day to match local mosque
- **Widget** — prayer times on the home screen (updates after reboot)
- **Dua** — Bismillah, Arabic text, translation, and audio playback
- **Settings** — Asr method, fixed Fajr/Isha, map style (OSM or MapTiler), Hijri offset, About, Privacy Policy

## Map tiles

- **OSM Mapnik** (default) — house numbers, no API key
- **MapTiler Streets** (optional) — set `MAPTILER_API_KEY` in `local.properties` (not committed); falls back to OSM if the key is missing

## Tech stack

- Kotlin, XML layouts (no Compose)
- [OSMDroid](https://github.com/osmdroid/osmdroid) + OpenStreetMap / MapTiler
- [PrayTimes](http://www.praytimes.org) (via `PrayerTimesCalculator`)
- ICU `IslamicCalendar` (Umm al-Qura) for Hijri dates
- `TimezoneMapper` for local timezone from coordinates
- minSdk 24, targetSdk 34

## Project structure

```
app/src/main/java/com/example/qiblaapp2/
  MainActivity.kt              — map / Qibla
  PrayerTimesActivity.kt       — prayer times & alarms
  DuaActivity.kt               — Dua screen
  SettingsActivity.kt          — app settings
  HijriPrefs.kt                — Hijri date & offset
  MapStylePrefs.kt             — OSM / MapTiler preference
  MapTileSources.kt            — tile layer URLs
  TabUiHelper.kt               — bottom nav highlight
  NeumorphicSwitchView.kt      — custom switches in Settings
  PrayerTimesWidgetProvider.kt — home screen widget
tools/nav-icons-svg/           — build script for tab PNG icons
```

## Build

1. Open the project in **Android Studio**
2. Copy `local.properties` (SDK path; optional `MAPTILER_API_KEY=...`)
3. Sync Gradle
4. Run **debug** on a device, or **Generate Signed APK** for release

Release signing keystore is **not** included in this repository. Keep your `.jks` file local and never commit it.

Current version: **2.0** (`versionCode` 6)

## Privacy

Privacy policy (in-app and online): [telegra.ph/Privacy-Policy-09-18-69](https://telegra.ph/Privacy-Policy-09-18-69)

Contact: solemetra@gmail.com

## License

Private project. All rights reserved unless stated otherwise.
