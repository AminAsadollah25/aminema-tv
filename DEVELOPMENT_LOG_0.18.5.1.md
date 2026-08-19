# Development Log — Aminema 0.18.5.1

## هدف

این انتشار یک مرحلهٔ محافظه‌کارانه برای سبک‌تر و سینمایی‌تر شدن تجربهٔ TV است؛
بدون تغییر دادن هویت بسته یا مسیرهای Login/Playback.

## تصمیم‌های اجرایی

- Intro همچنان همیشه نمایش داده می‌شود. به‌جای حذف آن، پنجرهٔ زمانی Intro برای
  شروع Refresh اولیهٔ Providerها استفاده شد؛ سرویس‌ها یکی‌یکی اجرا می‌شوند تا
  فشار هم‌زمان روی Android Box ایجاد نشود.
- برای حس Liquid Glass از شفافیت، خط نور و رنگ لایه‌ای استفاده شد؛ Blur تمام‌صفحه
  فقط برای backdrop باقی ماند و Blur جداگانه به کارت‌ها اضافه نشد.
- Font داخلی Vazirmatn جای fallback وابسته به دستگاه را گرفت تا RTL فارسی و
  عنوان‌های انگلیسی در تلویزیون‌های مختلف یکدست‌تر باشند.
- Health Coordinator از Activity جدا شد تا با خروج کاربر از Live TV، بررسی پس‌زمینه
  قطع نشود. دادهٔ health فقط status و timestamp است.

## فایل‌های اصلی

- `app/build.gradle.kts`
- `MainActivity.kt`, `AminTvApp.kt`
- `TvComponents.kt`, `CinematicBackground.kt`, `Theme.kt`
- `SearchKeyboard.kt`, `SpotlightScreen.kt`, `LiveTvActivity.kt`
- `PlaybackSessionController.kt`, `EpisodeLoader.kt`
- `LiveChannelHealthCoordinator.kt`
- فونت‌های `app/src/main/res/font/vazirmatn_*.ttf`

## مرزهای حفظ‌شده

هیچ Logout، حذف Cookie، پاک‌سازی WebStorage، حذف Library یا تغییر package/signing
در این چرخه انجام نشد. هیچ URL مدیا یا Token وارد مدل محلی نشد.

## وضعیت انتشار

APK Debug-signed برای کانال نصب فعلی ساخته شده است. Release build به‌دلیل نبودن
keystore پروژه unsigned است و عمداً منتشر نمی‌شود.

versionCode: 47
