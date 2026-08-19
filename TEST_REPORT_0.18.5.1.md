# Test Report — Aminema 0.18.5.1

## Automated verification

- `:app:testDebugUnitTest`: PASS.
- `:app:lintDebug`: PASS; خطای Lint وجود ندارد. هشدارهای non-blocking موجود
  (از جمله هشدار SSL و localization) همچنان جدا از خطای انتشار گزارش می‌شوند.
- `:app:assembleDebug`: PASS.
- `:app:assembleRelease`: PASS؛ خروجی `app-release-unsigned.apk` است و برای
  نصب روی کانال فعلی استفاده نشد.
- `git diff --check`: باید قبل از Commit نهایی اجرا شود.

## Emulator verification

- AVD: `Television_1080p`, 1920×1080.
- Package: `com.amin.tvos.debug`.
- نصب: `adb install -r -d`؛ هیچ App data، Cookie، Login یا WebStorage پاک نشد.
- نسخهٔ نصب‌شده: `versionCode=47`, `versionName=0.18.5.1`.
- Intro جدید روی صفحهٔ واقعی امولاتور دیده شد و برنامه پس از اجرا Crash نکرد.
- Fatal Exception در Logcat پس از اجرای نسخه صفر گزارش شد.

## محدودیت باقی‌مانده

این چرخه روی امولاتور تأیید شد؛ تست تازه روی Android Box فیزیکی انجام نشده است.
رفتار Providerها و سرعت شبکهٔ واقعی باید در اولین فرصت روی دستگاه مالک بررسی شود.

## APK

- `Aminema-0.18.5.1-debug.apk`
- SHA-256: `043aea2dbe0b23dab1099c31c8dc775dd9c5a0014113fc42a4eef71d41ded2a4`

versionCode: 47
