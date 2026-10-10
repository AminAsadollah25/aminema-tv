# QA — Startup & Catalog، 2026-10-09

## نتیجهٔ تازه‌تر — مرحلهٔ سوم

گزارش دقیق `STARTUP-DEEP-DIVE-2026-10-09.md` است. ۶۵ تست موفق، Build
موفق، lint صفر خطا و ۱۳۱ هشدار، نصب درجا موفق. استفادهٔ مجدد از حافظه
هنگام Back و DPAD و نمایش پوسترهای خارجی بررسی شد. لاگ AndroidRuntime
فرایند تست نهایی خطای تازه نشان نداد؛ این بررسی محدود، تضمین نبود ANR
یا موفقیت پخش شبکه نیست. APK جدید `Aminema-0.18.8-startup-fixed.apk`
با SHA ثبت‌شده در گزارش مرحلهٔ سوم است؛ هش‌ها/نتایج پایین تاریخی هستند.

## وضعیت

### مرحلهٔ دوم — تنبل‌سازی Home

- تمام ردیف‌های عمودی Home اکنون آیتم‌های مستقل `LazyColumn` با کلید ثابت و
  `contentType` هستند؛ فقط قسمت قابل مشاهده و محدودهٔ پیش‌بارگیری ساخته می‌شود.
- ادغام Index عمومی کاتالوگ نیز از Main به `Dispatchers.Default` منتقل شد.
- Library تا پایان آماده‌سازی محلی درخواست صفحهٔ شبکه نمی‌دهد؛ viewport بدون
  آیتم دیگر محرک صفحه‌بندی نیست. کل آرشیو در View All همچنان در دسترس است.
- Gate نهایی Unit/Lint/assemble موفق؛ همان ۶۱ تست بدون Failure/Error/Skipped.
- نصب درجا موفق. DPAD از بالا به سریال ایرانی و سپس سریال خارجی رسید؛
  View All با موس باز شد (۱۰۲ سریال ایرانی) و Back همان موقعیت عمودی و
  موقعیت افقی ردیف را حفظ کرد. بازگشت خودِ فوکوس به همان کارت با موس تضمین/تست نشده.
- شواهد: `/private/tmp/aminema-lazy-settled.png`، `aminema-lazy-library-settled.png`،
  `aminema-lazy-back.png`، `aminema-lazy-lower.png`.
- `am start -W` قبل از نصب جدید، بعد از پایان Build: ۵۴۸۹ms.
  نسخهٔ جدید: ۲۳۵۵، ۵۷۲۰، ۶۴۹۶ms. اندازه‌گیری‌های هم‌زمان با Build
  (۲۸۰۶، ۵۶۴۰، ۱۴۹۹۴ms) برای مقایسه کنار گذاشته شدند.
  این نمونه‌ها کاهش قابل اتکای زمان اولین فریم را اثبات نمی‌کنند. Intro مستقل
  از اولین Activity frame است و زمان آماده شدن کامل Home با این فرمان سنجیده نمی‌شود.
- در پنجرهٔ لاگ نهایی ANR/FATAL دیده نشد؛ ۳۸ و ۷۸ فریم skipped ثبت شد.
  ادعای حذف کامل لگ یا موفقیت پخش/Continue/ورود زندهٔ Providerها نداریم.
- APK جدید: `/private/tmp/Aminema-0.18.8-performance-candidate.apk`.
  SHA-256: `d41e61c74edc90e99c8eab7960f85ff3503d5e8d19a23a757ad154400c512ef8`.
  هش بخش APK پایین مربوط به Candidate مرحلهٔ اول است، نه فایل جدید.

Candidate محلی `0.18.8 / code 50` روی پایهٔ `1ec6666`، همراه تغییرات
منتشرنشدهٔ قبلی Live TV. نسخهٔ تازه در این چرخه منتشر نشده است.

## بیلد و Regression

- پایهٔ پیش از اصلاح: Unit، Lint، assembleDebug موفق.
- Gate نهایی `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug`: موفق.
- ۶۱ تست، Failure/Error/Skipped صفر. ۶ تست جدید برای اصلاح جایگاه فیلم/سریال،
  route مبهم، عدم تأثیر query، محدودبودن Home و عدم برخورد kind/IMDb اضافه شد.
- `git diff --check`: موفق.
- هشدارهای موجود Gradle 10/deprecation و Debug minify باقی‌اند؛ Lint کامل
  اجرا شده ولی suppressهای تاریخی پروژه تغییر نکرده‌اند.

## امولاتور

- AVD موجود `Television_1080p`، Android TV API 36، 1920×1080.
- نصب‌های تمام چرخه فقط `adb install -r`؛ هیچ uninstall/pm clear/Wipe/Logout
  یا حذف Cookie/WebStorage/Library انجام نشد. ورود زندهٔ هر Provider در این
  چرخه دوباره بررسی نشده؛ صرف حفظ داده، ادعای صحت Login فعلی نیست.
- اجرای پایه واقعاً ANR ورودی در MainActivity داشت (15:10، زمان دستگاه).
- اجرای اصلاح‌شده Home و پوسترها، Hero و اسکرول ردیف‌ها را نشان داد؛ در
  پنجرهٔ لاگ اجرای نهایی ANR/FATAL تازه دیده نشد.
- زمان اولین Activity از `am start -W` متغیر بود: پایه ۱۰۱۰ms، Candidateهای
  میانی ۱۸۹۹/۴۱۵۱/۶۵۳۳ms، Candidate نهایی ۴۰۲۴ms. یک اجرای بلافاصله پس از
  نصب به‌دلیل لانچر `UNKNOWN` داد و به‌عنوان benchmark معتبر حساب نشده است.
  این اعداد **اثبات کاهش زمان اولین فریم نیستند**؛ ANR اولیه بازتولید نشد
  اما مقداری افت فریم هنوز دیده می‌شود. بنچمارک کنترل‌شده و Box واقعی باقی است.
- تصاویر شاهد در `/private/tmp`: `aminema-final-poster-qa.png`،
  `aminema-final-catalog-qa.png` و `aminema-foreign-series-qa.png`.
- View All سریال خارجی باز شد؛ ۵۷۵ عنوانِ کش کامل و Grid/فیلتر/پوستر با
  Badge «سریال» دیده شدند (`aminema-library-loaded-qa.png`). صفحه‌بندی خودکار
  موجود می‌تواند در این مسیر Refresh را فعال کند؛ نتیجهٔ تمام درخواست‌های
  شبکه و صفحات تازه در این تست مستقل ممیزی نشده است.
- حرکت D-pad و Back از Library به همان ناحیهٔ سریال خارجی در Home تأیید شد
  (`aminema-back-settled.png`).

## Probe عمومی منبع دوم

فرم فعلی Classic واقعاً `action="/"` است. مسیر قدیمی
`/_modern/classic?sort=latest&type=tv` فیلتر را از دست می‌دهد؛ مسیر صحیح
`/?sort=latest&type=tv` در بررسی واقعی ۱۰ کارت `/tvshows/` برگرداند.
ساختار `.mv-results-grid .mv-ritem` و `.mv-card__ep` در HTML واقعی تأیید شد.
آدرس مدیا، Manifest، Token یا Cookie دریافت/ثبت نشد. Sync کامل لاگین‌شدهٔ
تمام Providerها و پخش واقعی فیلم/قسمت در این چرخه تست نشده‌اند.

## APK

`/Users/aminasadollah/Library/Caches/AminemaBuild/app/outputs/apk/debug/app-debug.apk`

SHA-256: `b4be3f6e947d0d5ebc327d31b4d824af3571e411bfe89dd67602c681cfb9fe5a`

## باز

میزان بهبود روی Box واقعی، تأیید Player/Continue پس از این تغییر، ممیزی
Candidate قبلی Live TV و Metadata اشتباه Remakeها. قبل از GitHub Release
تأیید مالک لازم است.
