# Development Log — Aminema 0.18.6

## هدف

نهایی‌کردن چرخهٔ تثبیت RC برای پخش مستقیم، انتخاب فصل/قسمت و جلوگیری از صدای
پس‌زمینهٔ Live TV، بدون تغییر هویت اپ یا داده‌های نشست کاربر.

## نقاط فنی

- `SpotlightActivity` برای سریال‌های شناخته‌شده اقدام `WATCH` را به
  `SELECT_EPISODE` تبدیل می‌کند.
- `PlaybackSessionController` شمارهٔ فصل/قسمت انتخاب‌شده را به مسیر Provider
  می‌دهد و fallback ایندکسی ندارد.
- `BrowserActivity` کنترل‌های قابل مشاهدهٔ MyMoviz را برای `تماشا و دانلود` و
  مسیر `/watch/` آماده می‌کند؛ URL مدیا یا Token وارد مدل نمی‌شود.
- `LiveChannelHealthProbe` در پایان، timeout و cancellation پاک‌سازی می‌شود.
- `LiveTvActivity` با `onPause` اسکن و Probeهای پنهان را متوقف می‌کند.

## تصمیم عملکردی

به‌دلیل افت شدید فریم هنگام اجرای WebViewهای مخفی در همان process، Refresh خودکار
کاتالوگ در cold-start فعال نشده است. این تصمیم از روانی Home و شروع پخش محافظت
می‌کند؛ طراحی process جدا باید با تست حفظ Cookie/WebStorage انجام شود.

## حفاظت

هیچ Logout، حذف Cookie، پاک‌سازی WebStorage، حذف Library یا تغییر package/signing
در این انتشار انجام نشد.

versionCode: 48
