$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ApkPath = Join-Path $ScriptDir "Cadence-v1.0.apk"
$AppId = "com.aistudio.cadence.rmqvxp"
$MainActivity = "com.example.MainActivity"

Write-Host "=== Cadence Android Installer ===" -ForegroundColor Cyan

if (-not (Test-Path $ApkPath)) {
    Write-Error "APK not found at $ApkPath. Build with 'gradle :app:assembleDebug' first."
    exit 1
}

if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
    Write-Host "Error: 'adb' (Android Platform Tools) is not in PATH." -ForegroundColor Red
    Write-Host "Install via winget: winget install Google.PlatformTools"
    Write-Host "Or copy $ApkPath directly to your Android phone and tap it to install."
    exit 1
}

$devices = adb devices | Select-String -Pattern "\tdevice$"
if (-not $devices) {
    Write-Host "No authorized Android device detected." -ForegroundColor Yellow
    Write-Host "1. Enable Developer Options & USB Debugging on your phone."
    Write-Host "2. Connect via USB and accept the debugging prompt."
    Write-Host "3. Re-run .\installer\install.ps1"
    exit 1
}

Write-Host "Installing Cadence ($AppId)..." -ForegroundColor Green
adb install -r -d "$ApkPath"

Write-Host "Launching Cadence..." -ForegroundColor Green
adb shell am start -n "$AppId/$MainActivity" | Out-Null

Write-Host "Done! Cadence is installed and running on your Android device." -ForegroundColor Cyan
