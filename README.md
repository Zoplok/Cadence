# Cadence — Local-First Android Life Tracker

Cadence is a Kotlin + Jetpack Compose Android app built on a **Room SQLite Single Source of Truth** paired with **Firebase Firestore** delta sync (`WorkManager`), featuring:

- **Daily Habits & Dynamic CTE Streaks**: Boolean, numeric, and duration habits with interactive progress rings and a Jetpack Glance home screen widget.
- **Tasks & Time-Blocking**: Priority-tagged tasks, subtasks, and daily timeline scheduling.
- **Doze-Safe Focus Timer**: Pomodoro (25m), Deep Work (50m), and Stopwatch sessions persisted in SQLite.
- **Goals & Milestones**: Macro targets linked to daily habits, tasks, and numeric milestones.
- **Calorie (`kcal`) Calculator**: Mifflin-St Jeor BMR/TDEE, macro breakdown, MET exercise burn estimator, and 1-tap conversion into a daily `kcal` habit.
- **Reflections & Journal**: 28-day habit consistency heatmap, mood/energy tracking, wins, and blockers.
- **Bilingual (`EN` / `МН`)**: Full English and Cyrillic Mongolian localization with an instant in-app language switcher.

## Install on Android
Pre-built APK and USB installer scripts are included in [`installer/`](./installer/README.md):
- Direct APK: [`installer/Cadence-v1.0.apk`](./installer/Cadence-v1.0.apk)
- macOS / Linux ADB installer: `./installer/install.sh`
- Windows PowerShell ADB installer: `.\installer\install.ps1`

## Build & Test from Source
Requires Android SDK (API 36), JDK 17+, and Gradle 9.3.1:
```bash
base64 -d debug.keystore.base64 > debug.keystore
gradle :app:testDebugUnitTest
gradle :app:assembleDebug
```
