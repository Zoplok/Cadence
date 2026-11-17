# Cadence Android Installer (`v1.0`)

- **Package ID:** `com.aistudio.cadence.rmqvxp`
- **Minimum Android Version:** Android 7.0 (API 24)
- **Target SDK:** Android 16 (API 36)
- **Languages:** English (`EN`) & Mongolian (`МН` / Cyrillic)

---

## Option 1 — Direct Phone Install (No PC Required)
1. Download [`Cadence-v1.0.apk`](./Cadence-v1.0.apk) onto your Android phone.
2. Tap the downloaded `.apk` file in your **Files** or **Downloads** app.
3. If prompted, enable **Install unknown apps** for your browser/file manager, then tap **Install**.

## Option 2 — 1-Command USB Install (ADB)
Connect your Android phone via USB with **USB Debugging** enabled, then run:

- **macOS / Linux:**
  ```bash
  chmod +x installer/install.sh
  ./installer/install.sh
  ```
- **Windows (PowerShell):**
  ```powershell
  .\installer\install.ps1
  ```

Verify checksum (optional):
```bash
sha256sum -c installer/Cadence-v1.0.apk.sha256
```

---

## Монгол заавар (Суулгах заавар)

### Арга 1 — Утсан дээрээ шууд суулгах
1. [`Cadence-v1.0.apk`](./Cadence-v1.0.apk) файлыг Android утсандаа татаж авна уу.
2. Татсан `.apk` файл дээр дарж нээнэ үү.
3. Хэрэв зөвшөөрөл асуувал **Unknown apps / Тодорхойгүй апп суулгах** зөвшөөрлийг идэвхжүүлээд **Install / Суулгах** товчийг дарна уу.

### Арга 2 — Компьютероос USB (ADB)-ээр суулгах
Утсандаа **USB Debugging** асаагаад кабелаар холбосны дараа:
- **macOS / Linux:** `./installer/install.sh`
- **Windows:** `.\installer\install.ps1`
