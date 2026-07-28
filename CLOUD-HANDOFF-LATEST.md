# تحویل جاری Aminema برای Cloud / برنامه‌نویس بعدی

این فایل خلاصه عملیاتی همیشه‌به‌روز پروژه است. قبل از تغییر بعدی، همراه
`ENGINEERING-HANDOFF-FA.md`، `ROADMAP.md` و `TEST_REPORT.md` خوانده شود.

## قرارداد دائمی همکاری دو برنامه‌نویس

بعد از هر Feature/Fix/Release ثبت کن: نسخه و versionCode، تصمیم UX، فایل‌ها
و مسیرهای تغییرکرده، شواهد Probe واقعی، تست‌ها، مرز امنیتی، محدودیت‌ها، قدم
بعدی و وضعیت Commit/Tag/Release/Assets. فقط Release Notes کافی نیست.

---

## وضعیت جاری

- محصول: **Aminema**
- نسخه کد: **0.12.1**
- `versionCode`: **26**
- وضعیت: **Release Candidate محلی؛ هنوز Commit/Tag/Push/GitHub Release نشده**
- آخرین Release عمومی: `v0.12.0` در commit `5163c8f`
- شاخه: `main`؛ تغییرات 0.12.1 فعلاً Working Tree هستند.
- Package نصب واقعی: `com.amin.tvos.debug`
- Package پایه: `com.amin.tvos`
- Package، Debug signature و داده برنامه تغییر نکرده‌اند؛ `adb install -r`
  نشست‌های لاگین، Cookie، Poster و Library را حفظ کرد.

## مسئله‌های حل‌شده در 0.12.1

1. Continue گاهی فقط Detail page را باز می‌کرد؛ چون `click()` موفقیت حساب
   می‌شد حتی اگر React/SPA هنوز آماده نبود و کلیک را نپذیرفته بود.
2. هنگام Direct Play ابتدا Detail page برای چند ثانیه دیده می‌شد.
3. Continue تلویزیون و امولاتور فرق داشت؛ Sync فقط دستی بود و Repository
   موارد قدیمی محلی را کنار نتیجه حساب نگه می‌داشت.
4. ردیف‌های افقی برای موس دکمه صفحه‌به‌صفحه نداشتند.
5. زیرعنوان اضافی «پخش مستقیم • تمام‌صفحه» کنار Live لازم نبود.
6. کاربر خواست Loading شخصیت بامزه Aminema داشته باشد.

## تصمیم UX نهایی

- موفقیت Continue/Direct Play فقط وقتی تأیید می‌شود که URL مطابق
  `playbackUrlPatterns` باشد یا رویداد واقعی HTML5 Video/Fullscreen برسد؛
  نه صرفاً وقتی JavaScript تابع `click()` را صدا زده است.
- Detail bootstrap پشت یک Loading سینمایی پنهان می‌شود. متن Continue:
  «پاپ‌کورن یادت نره… 🍿» و «داریم فیلمت رو از همون‌جایی که جا گذاشتی
  میاریم؛ خوش بگذره!»
- اگر تا ۱۴ ثانیه Player تأیید نشود، Detail page برای انتخاب دستی آشکار
  می‌شود؛ صفحه سیاه دائمی نداریم. Back حین Loading مستقیم Activity را می‌بندد.
- Continue حساب در شروع سرد Process، حداکثر هر ۱۵ دقیقه، خودکار Sync می‌شود.
  دکمه Sync دستی همچنان باقی است.
- نتیجه موفق هر سرویس برای عضویت Continue authoritative است؛ Local فقط
  `playbackUrl/position/duration/poster` همان Content مشترک را غنی می‌کند.
- همه ردیف‌های افقی فلش ثابت چپ/راست در گوشه راست Header دارند؛ هر کلیک
  ۸۲٪ Viewport را با `animateScrollTo` جابه‌جا می‌کند.

## تغییرات فنی و فایل‌ها

### Playback reliability

- `browser/BrowserActivity.kt`
  - retryهای Direct: 250ms تا 12s؛ Continue: 250ms تا 10.8s
  - Cooldown برابر 850ms و guardهای `probeInFlight`
  - candidate scoring: Visible/Enabled، Exact text، تگ button/a، جلوگیری از
    Container/episode-season اشتباه
  - `observePlaybackNavigation()` و `confirmPlaybackReady()`
  - Click attempt دیگر فلگ success را Set نمی‌کند.
  - Login/Error automation را Cancel می‌کند تا Toast/Click دیرهنگام نرسد.
  - Timeout امن 14s و Back فوری در Loading
- `browser/PlaybackLoadingView.kt` (جدید)
  - View سبک Native، بدون Compose/WebView اضافه
  - تصویر، متن بامزه، Progress و Gradient سینمایی
- `drawable-nodpi/aminema_loading_popcorn.png` (جدید)
  - 640×640، 556KB؛ مسکات روی صندلی سینما با پاپ‌کورن و کنترل
  - مرجع هویت: `aminema_mascot.png`؛ تولید با built-in ImageGen
- `ui/home/HomeScreen.kt`
  - Detail URL مساوی Playback URL دیگر مقصد Dedicated تلقی نمی‌شود.
  - FilmRooz movie فاقد Player page ذخیره‌شده دوباره از Resolver معمول
    language/quality عبور می‌کند.
  - ParsiFlix Continue از دکمه دقیق account-aware خود سایت استفاده می‌کند.

### Cross-device Continue

- `browser/AccountSyncActivity.kt`
  - Process-cold auto-sync gate با فاصله 15 دقیقه
  - سقف Parse از 12 به 20 (هم‌اندازه rail Home)
  - Sync موفق حتی با لیست خالی authoritative است.
- `MainActivity.kt`
  - بعد از پایان/Skip Intro، Sync account خودکار را یک‌بار در Process Launch
    می‌کند؛ Task restore تلویزیون با savedInstanceState این Gate را خراب نمی‌کند.
- `data/LibraryRepository.kt`
  - `syncAccountSessions(serviceId, incoming)` کل همان Provider را reconcile
    می‌کند؛ سایر Providerها دست‌نخورده می‌مانند.
  - Local item فقط در صورت حضور همان Content در حساب حفظ و غنی می‌شود.

### Rail navigation

- `ui/components/TvComponents.kt`
  - `RailNavigationControls(ScrollState)` مشترک
  - `FocusableCard(enabled)` با حالت Dim/Disabled
  - `SectionRow` اکنون ScrollState را با Header controls شریک می‌کند.
- `CatalogSectionRow.kt`, `LiveTvSectionRow.kt`, `SearchActivity.kt`
  - استفاده از همان کنترل مشترک و ScrollState
- `LiveTvSectionRow.kt`
  - حذف کامل متن «پخش مستقیم • تمام‌صفحه»؛ badgeهای LIVE کارت‌ها باقی است.

### Version/docs

- `app/build.gradle.kts`: `0.12.1`, code `26`
- `RELEASE_NOTES_0.12.1.md`
- همین فایل + `ENGINEERING-HANDOFF-FA.md`, `ROADMAP.md`, `TEST_REPORT.md`

## شواهد واقعی حساب و تست

روی AVD `Television_1080p`، API 36، 1920×1080 و نشست واقعی لاگین‌شده:

- Sync account دستی 15 مورد ساخت: 7 FilmRooz و 8 ParsiFlix.
- شروع سرد 0.12.1 مقدار `last_attempt_at` را نوشت و `lastPlayed` همه نتایج
  حساب را تازه کرد؛ Auto Sync بدون پاک‌کردن login انجام شد.
- دو FilmRooz session محلی که داخل Account Recent هم بودند Player/Position
  خود را حفظ کردند؛ هیچ Local-only قدیمی خارج از Account باقی نماند.
- FilmRooz movie **Her Private Hell**: Loading → normal player page؛ Flash
  Detail دیده نشد. موردی که `playbackUrl == contentUrl` داشت با Resolver
  اصلاح‌شده باز هم به Player رسید.
- ParsiFlix movie **دو روز دیرتر**: Detail → دکمه Continue خود سایت → `/play`؛
  Retryها فقط بعد از Player route متوقف شدند.
- FilmRooz series فاقد مقصد Account-aware: بعد از Timeout به Detail page
  امن برگشت (fallback عمدی؛ exact episode از Account Recent ارائه نمی‌شود).
- Loading جدید با تصویر اختصاصی و متن بامزه در 1920×1080 درست Render شد.
- فلش‌ها در Services/Continue/Catalog/Live/More/Recent/Favorites/Search
  Headerها دیده می‌شوند؛ حالت ابتدا/انتها Dim می‌شود.
- زیرعنوان Live حذف و badge LIVE کارت‌ها حفظ شد.
- `assembleDebug` و `lintDebug`: موفق؛ Lint error صفر.
- نصب `adb install -r`: موفق؛ نسخه نصب‌شده 0.12.1 code 26.
- Continue هر دو Provider به Player واقعی رسید؛ fallback series بدون crash.

## محدودیت واقعی که باید حفظ شود

- ParsiFlix Account API واقعاً Continue می‌دهد، پس سریال/قسمت را خود سایت
  می‌تواند ادامه دهد.
- FilmRooz `/user/panel/` فقط «مشاهدات اخیر» و Content detail را می‌دهد؛
  برای فیلم‌ها Resolver عادی Player را پیدا می‌کند، اما برای Series
  Cross-device exact episode/quality/time همیشه موجود نیست. در این حالت
  Aminema پس از Timeout Detail را نشان می‌دهد و چیزی را حدس نمی‌زند.
- Loading زمان شبکه را حذف نمی‌کند؛ فقط Detail flash را به تجربه سینمایی
  تبدیل می‌کند.

## مرز امنیتی

- فقط بخش Continue/Recent حساب خود کاربر و Page URLهای عادی خوانده می‌شوند.
- هیچ Media URL، `video.src/currentSrc`، Stream request، DRM، Cookie، Token
  یا Auth header خوانده/ذخیره/Log/Export نمی‌شود.
- هیچ Auth bypass یا endpoint حدسی اضافه نشده است.
- Asset تصویری فقط Local resource است و در Runtime شبکه مصرف نمی‌کند.

## وضعیت انتشار و قدم بعدی

- Commit: هنوز نشده
- Tag `v0.12.1`: هنوز ساخته نشده
- GitHub Release: هنوز ساخته نشده
- RC محلی آماده:
  - `outputs/Aminema-v0.12.1-RC-debug.apk` (حدود 72MB)
  - `outputs/Aminema-v0.12.1-RC-debug.apk.sha256`
  - SHA-256:
    `94f4a7d988b2b5005800afe3da280e449f318211e12aacf9552d02ab110200b0`
- Asset عمومی هنوز Upload نشده. بعد از تأیید مالک: Commit/Tag/Push و GitHub
  Release Latest با نام نهایی بدون `RC` ایجاد شود.

**مرحله پیشنهادی بعد از انتشار 0.12.1:** نسخه 0.13.0، بازطراحی کامل کیبورد
به‌صورت State Machine ثابت برای Username → Password → Caps → Language →
Show/Hide → Done، سپس مهاجرت Home از `Column+verticalScroll` به Lazy layout
قبل از افزودن ردیف‌های ژانری بیشتر.
