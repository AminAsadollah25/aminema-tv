# Test Report — Aminema 0.18.6

## Build

- `:app:testDebugUnitTest`: PASS
- `:app:lintDebug`: PASS
- `:app:assembleDebug`: PASS
- APK با `adb install -r -d` و بدون حذف داده‌ها نصب شد.

## Emulator regression

- AVD: `Television_1080p`, 1920×1080
- Package: `com.amin.tvos.debug`
- Home → سریال «شاه‌گل» → انتخاب قسمت → قسمت ۱ → فریم واقعی پخش: PASS
- Search → «Shifting Gears» → فصل ۱ → قسمت ۲ → فریم واقعی پخش: PASS
- Search → «Masters of the Universe» → تماشا → فریم واقعی ویدئو: PASS
- Back از Browser به Spotlight/Live TV: PASS
- Live TV → تب‌های فعال/همه → GEM Series Plus → Back؛ Media Session پنهان پس
  از خروج باقی نماند: PASS
- در اجرای امولاتور ANR یا FATAL تازه مشاهده نشد.

## داده و نشست

`adb install -r -d` استفاده شد. App data، Login، Cookie، WebStorage، Continue
Watching، Favorites و Library پاک نشدند.

## محدودیت

تست Android Box فیزیکی و سنجش نهایی صدا/وقفهٔ Live TV روی WebView دستگاه واقعی
انجام نشده است.

## APK

- `Aminema-0.18.6-debug.apk`
- SHA-256: `26268c8cc6a82ca49ad16c71bcb786a8f887a4405155848329f49bae097d5326`

versionCode: 48
