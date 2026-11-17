#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APK_PATH="${SCRIPT_DIR}/Cadence-v1.0.apk"
APP_ID="com.aistudio.cadence.rmqvxp"
MAIN_ACTIVITY="com.example.MainActivity"

echo "=== Cadence Android Installer ==="

if [[ ! -f "${APK_PATH}" ]]; then
  echo "Error: APK not found at ${APK_PATH}" >&2
  echo "Run 'gradle :app:assembleDebug' from the repository root and copy the APK to installer/Cadence-v1.0.apk." >&2
  exit 1
fi

if ! command -v adb >/dev/null 2>&1; then
  echo "Error: 'adb' (Android Platform Tools) is not installed or not in PATH." >&2
  echo "Install Android Platform Tools:" >&2
  echo "  macOS:  brew install --cask android-platform-tools" >&2
  echo "  Ubuntu: sudo apt-get install android-tools-adb" >&2
  echo "Or copy ${APK_PATH} directly onto your Android phone and open it to install." >&2
  exit 1
fi

echo "Checking connected Android devices..."
DEVICE_COUNT="$(adb devices | awk 'NR>1 && $2=="device" {count++} END {print count+0}')"
if [[ "${DEVICE_COUNT}" -eq 0 ]]; then
  echo "No authorized Android device detected." >&2
  echo "1. Enable Developer Options & USB Debugging on your phone." >&2
  echo "2. Connect via USB and tap 'Allow USB debugging'." >&2
  echo "3. Re-run ./installer/install.sh" >&2
  exit 1
fi

echo "Installing Cadence (${APP_ID})..."
adb install -r -d "${APK_PATH}"

echo "Launching Cadence..."
adb shell am start -n "${APP_ID}/${MAIN_ACTIVITY}" >/dev/null

echo "Done! Cadence is installed and running on your Android device."
