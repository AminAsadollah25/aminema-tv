# Development Log — Aminema 0.16.5

## مسئله واقعی

پس از نصب 0.16.4 روی Android TV، بازکردن Silo پیام
«اطلاعات قسمت‌ها یافت نشد» نشان می‌داد؛ در حالی که عنوان و قسمت‌ها در حساب
FilmRooz موجود بودند.

## Evidence و ریشه باگ

همان Silo از مسیر عادی Home → Search → Spotlight روی AVD لاگین‌شده باز شد:

- شروع Load: `12:43:49.465`
- `onPageFinished`: `12:43:55.708`
- پایان زودهنگام با `editions=null`: `12:43:57.847`

EpisodeLoader Timeout سراسری هشت‌ثانیه‌ای داشت. استخراج FilmRooz پس از
`onPageFinished` با تأخیر ۱.۸ ثانیه و سپس ۵۰۰ میلی‌ثانیه دیگر زمان‌بندی می‌شد؛
در نتیجه اجرای Script حدود `12:43:58.008` بود، اما WebView کمی قبل از آن Destroy
می‌شد. پس مشکل DOM، فصل‌های Silo یا Login نبود؛ Script اصلاً اجرا نشده بود.

## اصلاح کوچک و برگشت‌پذیر

1. Deadline محدود Loader از ۸ به ۲۵ ثانیه افزایش یافت.
2. اولین Extraction نسبت به پایان واقعی صفحه Queue می‌شود: FilmRooz بعد از
   ۱.۲ ثانیه و ParsiFlix بعد از ۲ ثانیه.
3. نتیجه خالی Bridge در SPA فوراً Failure نیست؛ تا پایان Deadline هر ۱.۵ ثانیه
   Retry می‌شود.
4. فقط یک Extraction هم‌زمان Queue می‌شود و `finish/destroy` تمام Callbackها
   و WebView را آزاد می‌کند.
5. اسکن و Log غیرضروری همه تصاویر DOM حذف شد.
6. خطای Native دکمه `تلاش دوباره` دارد و همان Loader عنوان را دوباره اجرا
   می‌کند.

## Evidence پس از اصلاح

- شروع Load: `12:51:00.287`
- `onPageFinished`: `12:51:03.973`
- اجرای Extraction Attempt 1: `12:51:05.422`
- دریافت payload فصل/قسمت: `12:51:05.434`
- پایان موفق با یک Edition: `12:51:05.547`
- سه فصل Silo و Episode cardهای فصل اول در 1920×1080 بصری تأیید شدند.

## فایل‌های تغییرکرده

- `app/src/main/java/com/amin/tvos/ui/spotlight/EpisodeLoader.kt`
- `app/src/main/java/com/amin/tvos/ui/spotlight/EpisodeNavigator.kt`
- `app/src/main/java/com/amin/tvos/ui/spotlight/SpotlightActivity.kt`
- `app/build.gradle.kts`
- مستندات Release، Roadmap و Handoff

## مرز امنیت و داده

- هیچ Cookie، WebStorage، Login، Library، Continue، Favorite یا Cache پاک نشد.
- نصب QA فقط با `adb install -r` انجام شد.
- هیچ Media/Stream URL، Token، Password، Auth header یا DRM خوانده، ذخیره یا
  Log نشد.

## محدودیت صادقانه

موفقیت امولاتور دلیل قوی برای رفع Race است، اما سرعت WebView روی Android Box
واقعی متفاوت است و پذیرش نهایی پس از نصب 0.16.5 روی تلویزیون مالک انجام می‌شود.

## Artifact کاندید نهایی

- فایل: `Aminema-0.16.5-debug.apk`
- اندازه: `22,921,132` بایت
- SHA-256: `54bc877a92e551c991caf0c622303047ce302060ea484df11f10a1ffed272211`
- Package: `com.amin.tvos.debug`، versionCode `37`، versionName `0.16.5`
- Digest امضا با 0.16.4 یکسان است:
  `ba6ac8c4c2e1828462e7a6b122ad60856a054a18389af36bc001a1ee38ba13d3`
