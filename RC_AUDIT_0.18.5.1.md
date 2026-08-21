# Aminema — RC Audit نهایی‌شده در نسخهٔ 0.18.6

این گزارش مبنای Release `0.18.6 / versionCode 48` شد. فایل با نام تاریخی خود
نگه داشته شده تا سابقهٔ چرخهٔ RC از بین نرود. نسخهٔ قبلی `0.18.5.1 / versionCode
47` بود.

## تغییرهای اعمال‌شده

- سریال‌های Providerهای شناخته‌شده اگر با اقدام قدیمی `WATCH` وارد Spotlight شوند،
  به `SELECT_EPISODE` می‌روند؛ کلیک اول دیگر نباید کاربر را به صفحهٔ جزئیات خام سایت
  پرت کند.
- Probe پخش زنده در timeout و cancellation نیز continuation را پاک و WebView را
  mute، pause و `about:blank` می‌کند. اسکن هنگام خروج از Live TV قطع می‌شود تا صدای
  Probe پنهان با Browser یا Home قاطی نشود.
- MyMoviz دکمهٔ `تماشا و دانلود` را هم به الگوی اقدام اضافه می‌کند و صفحهٔ عادی
  `/watch/` را برای autoplay آماده می‌کند؛ این تغییر فقط روی کنترل قابل‌مشاهدهٔ سایت
  اعمال می‌شود و لینک مدیا، Token، DRM یا Cookie را نمی‌خواند.

## تصمیم عملکردی

Refresh خودکار کاتالوگ در cold-start عمداً فعال نیست. در این process، WebViewهای
مخفی می‌توانند UI همان Android Box را متوقف کنند؛ در اندازه‌گیری قبلی بیش از ۲۰۰۰
فریم از دست رفت. Home از کش فوراً قابل استفاده است و Refresh دستی/View All مسیرهای
قابل مشاهده‌اند. فعال‌کردن دوبارهٔ Refresh خودکار فقط بعد از طراحی process جدا با
بررسی دقیق Cookie/WebStorage مجاز است.

## شواهد تست امولاتور

- دستور بیلد: `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug`
- نتیجه: `BUILD SUCCESSFUL`؛ Unit Test، Lint و APK موفق.
- نصب: `adb install -r -d` روی `Television_1080p`؛ App data، Cookie، Login،
  WebStorage و Library پاک نشدند.
- Home → Spotlight سریال «شاه‌گل» → «انتخاب قسمت» → قسمت ۱؛ BrowserActivity
  باز شد و پس از آماده‌سازی فریم واقعی پخش دیده شد.
- Back از Browser به Spotlight موفق بود.
- Live TV با تب‌های «فعال/همه کانال‌ها» باز شد و هنگام خروج Activity اسکن مخفی
  لغو شد؛ Crash/ANR تازه در این مسیر دیده نشد.
- Search → «Shifting Gears» → سریال → فصل اول → قسمت ۲ به Browser رسید و فریم
  واقعی پخش با زیرنویس دیده شد؛ Logcat نیز `season=1` و مسیر `filmrooz` را ثبت کرد.
- Search → «Masters of the Universe» → فیلم → `تماشا`، پس از آماده‌سازی، فریم
  واقعی ویدئو در Browser دیده شد و صفحهٔ جزئیات خام باقی نماند. این عنوان کاندید
  یکتای MyMoviz است؛ چون مسیر فیلم شناسهٔ Provider را Log نمی‌کند، این سند آن را
  «تأیید عملی مسیر مستقیم کاندید MyMoviz» می‌نامد، نه ادعای استخراج منبع.
- Live TV در یک اجرای تازه تب‌های «فعال/همه کانال‌ها» (۱۱۰/۲۸۲) و اسکن را نشان
  داد. GEM Series Plus باز شد و با Back به Live TV برگشت؛ بعد از خروج، برای UID
  اپ هیچ Media Session فعالی باقی نماند و ANR/FATAL تازه دیده نشد. سنجش صوت روی
  Android Box واقعی هنوز لازم است.

## موارد باز قبل از Release Candidate

1. Probe واقعی MyMoviz روی همان عنوانی که حساب مالک اجازهٔ پخش دارد؛ تأیید route،
   autoplay و Back بدون استخراج هیچ URL محافظت‌شده.
2. تست Android Box فیزیکی با حساب‌های موجود؛ امولاتور جای دستگاه واقعی را نمی‌گیرد.
3. اندازه‌گیری مستقل صدای Audio در Live TV روی WebView provider دستگاه واقعی.
4. طراحی معماری Refresh کاتالوگ در process جدا، بدون از دست‌دادن Cookie/WebStorage.

## مرز امنیتی

این چرخه Authentication را دور نمی‌زند، Cookie را خارج نمی‌کند، لینک مدیا/توکن/DRM
را ذخیره یا لاگ نمی‌کند و فقط کنترل‌های عادی قابل مشاهدهٔ Provider را فعال می‌کند.

versionCode: 47
