# 📱 DawaCheck (दवा चेक)

Medicine expiry scanner, cabinet & reminders — Flutter app, Black & White theme, Hindi + English.

## ✅ Features (Phase 1 — Scanner + Cabinet)
- 📷 Take a photo or pick from gallery
- 🤖 On-device OCR (Google ML Kit) reads the expiry date off the label
- ✍️ Confirm / edit detected details before saving
- 🏠 Medicine cabinet — see all saved medicines, sorted by expiry
- ⚠️ Status badges: Expired / Expiring soon (≤30 days) / Valid
- 🔔 Local notifications: 7 days before expiry + on expiry day
- 🌐 Hindi + English toggle (top-right button on Home)
- ⚫⚪ Minimal Black & White design

> Reminder system exists (auto-scheduled per medicine) but pharmacy-map is planned for a later phase.

---

## 🚀 How to build

### Option A — GitHub Actions (recommended, no setup needed)
1. Push this repo to GitHub.
2. Go to the **Actions** tab → the `Build DawaCheck APK` workflow runs automatically on push to `main`
   (or trigger manually via "Run workflow").
3. When it finishes, download the `dawa-check-apk` artifact — that's your installable `.apk`.
4. Transfer it to your Android phone and install (enable "Install from unknown sources" if asked).

### Option B — Build locally in Termux
```bash
pkg update && pkg upgrade
pkg install git openjdk-17 -y

# Install Flutter SDK (one-time)
git clone https://github.com/flutter/flutter.git -b stable ~/flutter
export PATH="$PATH:$HOME/flutter/bin"
flutter doctor

# Clone your repo
git clone <YOUR_REPO_URL> dawa_check
cd dawa_check

# Generate the android/ folder + set permissions (same steps CI runs)
flutter create --platforms=android --org com.dawacheck --project-name dawa_check .
flutter pub get
flutter build apk --release

# APK will be at:
# build/app/outputs/flutter-apk/app-release.apk
```
Termux on-device builds can be slow/memory-heavy — Option A is easier for most phones.

---

## 🗂️ Project structure
```
lib/
├── main.dart                  # App entry
├── theme/app_theme.dart       # Black & White theme
├── l10n/app_strings.dart      # Hindi + English strings
├── models/medicine.dart       # Medicine data model
├── services/
│   ├── storage_service.dart      # Hive local storage (medicine cabinet)
│   ├── ocr_service.dart          # ML Kit text recognition + date parsing
│   └── notification_service.dart # Expiry reminders
├── widgets/medicine_card.dart
└── screens/
    ├── home_screen.dart       # Cabinet list
    ├── scan_screen.dart       # Camera/gallery capture + OCR
    └── add_edit_screen.dart   # Confirm/edit/add medicine
```

## 🔜 Next phases
- Nearest pharmacy map (Google Maps / geolocation)
- Barcode scan for known-brand lookup
- Export cabinet list / share with family

## 📝 Notes
- OCR reads common formats: `EXP 05/2027`, `EXP: 05/06/2027`, `MAY 2027`, etc.
  If it can't find a date, you can always type it in manually on the confirm screen.
- All data stays on-device (Hive local database) — no server, no login needed.
