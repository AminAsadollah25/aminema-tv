# Aminema 0.18.6 — Playback & Live TV Stabilization

## تغییرات اصلی

- مسیر Home، Search و Continue به Spotlight Native و سپس پخش مستقیم متصل شد.
- سریال‌های Providerهای شناخته‌شده ابتدا وارد انتخاب فصل و قسمت می‌شوند؛ شمارهٔ
  فصل/قسمت دقیق حفظ می‌شود و fallback اشتباه ایندکسی استفاده نمی‌شود.
- پخش واقعی سریال «Shifting Gears» از فصل ۱، قسمت ۲ در امولاتور تأیید شد.
- مسیر فیلم به‌صورت مستقیم به Browser/Player می‌رود؛ کاندید MyMoviz با عنوان
  «Masters of the Universe» تا فریم واقعی ویدئو تست شد.
- MyMoviz برای دکمهٔ `تماشا و دانلود` و autoplay صفحهٔ `/watch/` مقاوم‌تر شد.
- Live TV هنگام خروج از صفحه، Probeهای مخفی را cancel، mute، pause و blank می‌کند
  تا صدای پنهان پس‌زمینه وارد Home یا Player نشود.
- تب‌های «فعال» و «همهٔ کانال‌ها» باقی ماندند و lifecycle اسکن با Activity هماهنگ شد.
- لودینگ سینمایی، Focus، Back و شروع برنامه بدون تغییر در نشست کاربر حفظ شدند.

## عملکرد و محدودیت آگاهانه

Refresh خودکار کاتالوگ با WebView مخفی در cold-start عمداً فعال نیست؛ اندازه‌گیری
قبلی در همین process بیش از ۲۰۰۰ فریم افت نشان داد. Home از کش فوری باز می‌شود و
Refresh دستی/View All فعال است. Refresh هم‌زمان در process جدا، با حفظ Cookie و
WebStorage، برای نسخهٔ آینده باقی مانده است.

## امنیت و دادهٔ کاربر

- `applicationId` و کانال Debug برای نصب درجا حفظ شدند.
- Login، Cookie، WebStorage، Continue Watching، Favorites و Library پاک نشدند.
- هیچ Authentication Bypass، استخراج/ذخیرهٔ لینک مدیا، Token یا DRM انجام نشد.

## تست

- `:app:testDebugUnitTest`: موفق
- `:app:lintDebug`: موفق
- `:app:assembleDebug`: موفق
- نصب امولاتور با `adb install -r -d`: موفق، بدون پاک‌سازی داده
- AVD: `Television_1080p`، وضوح 1920×1080
- تست فیزیکی Android Box و سنجش نهایی صدای Live TV هنوز انجام نشده است.

## Asset انتشار

- `Aminema-0.18.6-debug.apk`
- SHA-256: `26268c8cc6a82ca49ad16c71bcb786a8f887a4405155848329f49bae097d5326`

versionCode: 48
