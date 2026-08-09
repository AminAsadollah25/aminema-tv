# گزارش تست Aminema 0.16.5.1

تاریخ: 2026-08-09  
محیط هدف: AVD `Television_1080p`، نصب درجا روی `com.amin.tvos.debug`

## بررسی Asset

- مدت خروجی: `8.20s`
- Video: H.264 (`avc1`)
- Audio: AAC Stereo، 44.1kHz
- کادر Preview: 16:9/1080p، بدون Crop یا نوار
- Fast Start: فعال
- SHA-256 Asset:
  `54c2ed65e42a2d546a059f1b8cf94344c75352b50dea775ed0d70bff39e4369e`

## Verification نهایی

- `clean :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`: موفق
- نصب با `adb install -r`: موفق
- Package نصب‌شده: `com.amin.tvos.debug` با `versionCode 38`
- پخش Full-screen ویدئو بدون Crop یا نوار: موفق
- پایان طبیعی/Watchdog و ورود خودکار به Home: موفق
- Skip با `DPAD_CENTER`: موفق
- Skip با Click موس روی خود `VideoView`: موفق
- `FATAL EXCEPTION` در Logcat: صفر
- نصب درجا انجام شد؛ Package، Cookie، Login و App data پاک نشدند.
- `git diff --check`: موفق

## Artifact نهایی

- فایل: `Aminema-0.16.5.1-debug.apk`
- اندازه: `24,749,024` بایت
- SHA-256:
  `1bb45c567aefe2d5a83af2fce47ffc4907058c1c2d6d76d99569bc842485429a`
- Digest امضای Debug:
  `ba6ac8c4c2e1828462e7a6b122ad60856a054a18389af36bc001a1ee38ba13d3`

## وضعیت انتشار

تأیید فنی و تأیید مالک انجام شد؛ APK و SHA-256 با Tag `v0.16.5.1` در GitHub
Release منتشر شدند.
