# تحویل جاری Aminema برای Cloud / برنامه‌نویس بعدی

این فایل خلاصه عملیاتی همیشه‌به‌روز پروژه است. قبل از تغییر بعدی، همراه
`ENGINEERING-HANDOFF-FA.md`، `ROADMAP.md` و `TEST_REPORT.md` خوانده شود.

## قرارداد دائمی همکاری دو برنامه‌نویس

بعد از هر Feature/Fix/Release ثبت کن: نسخه و versionCode، تصمیم UX، فایل‌ها
و مسیرهای تغییرکرده، شواهد Probe واقعی، تست‌ها، مرز امنیتی، محدودیت‌ها، قدم
بعدی و وضعیت Commit/Tag/Release/Assets. فقط Release Notes کافی نیست.

**الزام تازه از 0.13.0:** در هر Release، بخش‌های `Current release` و
`Next update queue` در `README.md` هم باید همان لحظه به نسخه واقعی به‌روز
شوند. README عمومی GitHub نباید از Handoff یا Latest Release عقب بماند.

---

## وضعیت جاری

- محصول: **Aminema**
- نسخه کد: **0.13.0**
- `versionCode`: **27**
- وضعیت: **منتشرشده و GitHub Latest**
- Release commit و Tag: `fd3ac33` / `v0.13.0`
- GitHub Release:
  `https://github.com/AminAsadollah25/aminema-tv/releases/tag/v0.13.0`
- شاخه: `main`
- Package نصب واقعی: `com.amin.tvos.debug`
- Package پایه: `com.amin.tvos`
- Package، Debug signature و داده برنامه تغییر نکرده‌اند؛ `adb install -r`
  نشست‌های لاگین، Cookie، Poster و Library را حفظ کرد.

## کار در حال انجام برای 0.13.0

- مرجع جزئیات زنده: `DEVELOPMENT_LOG_0.13.0.md`
- گزارش پذیرش Candidate: `TEST_REPORT_0.13.0.md`
- Track A، بازطراحی Search Deck داخل خود اپ: پیاده‌سازی و Build شده، هنوز
  Release نشده است.
- چیدمان فارسی پس از بازخورد کاربر از ردیف‌های ده‌تایی اشتباه به QWERTY
  استاندارد و پلکانی اصلاح شد؛ `ژ` و `آ` کلید مستقل دارند.
- روی AVD واقعی 1920×1080 کل پنل بدون Scroll/Clipping دیده شد و ورود دو حرف
  با Mouse، Counter و فعال‌شدن CTA جستجو تأیید شد.
- Track B، State Machine و Input Deck کیبورد Login داخل WebView: پیاده‌سازی
  و روی صفحه Login محلی داخل BrowserActivity تست شد.
- `Username → Next → Password` حتی با تعویض هم‌زمان DOM موفق بود؛
  `Caps → A → فارسی → ض → Show` مقدار `Aض` را فقط در Password نگه داشت.
- Submit و Back موفق، System IME بسته، Crash صفر؛ Harness موقت قبل از Build
  نهایی حذف شد. جزئیات و فایل‌ها در `DEVELOPMENT_LOG_0.13.0.md`.
- تنها Acceptance باقی‌مانده، تست همین مسیر روی Android Box واقعی در اولین
  فرصت Login طبیعی است؛ برای تست حساب‌های فعلی عمداً Logout نشوند.
- هر دو Track تکمیل شده‌اند و Candidate به 0.13.0/code 27 ارتقا یافته است؛
  تا تأیید GitHub Release وضعیت Published اعلام نشود.

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

- Release commit: `fd3ac33`
- Tag: `v0.13.0`
- GitHub Release: Published، Latest، Draft=false، Prerelease=false
- Assetهای عمومی:
  - `Aminema-v0.13.0-debug.apk` — `75,958,740` بایت، state=`uploaded`
  - `Aminema-v0.13.0-debug.apk.sha256` — `92` بایت، state=`uploaded`
- SHA-256 APK:
  `acd6ed8d43df33f2c4813e603efe4e1f474622305129f05ea320c5da136e3b91`
- امضای APK با 0.12.1 یکسان است:
  `ba6ac8c4c2e1828462e7a6b122ad60856a054a18389af36bc001a1ee38ba13d3`
- GitHub Latest API تأیید شد: `tag_name=v0.13.0`، متن Release شامل
  `versionCode: 27` و هر دو Download URL است؛ بنابراین updater خود Aminema
  روی code 26 و پایین‌تر نسخه جدید را تشخیص می‌دهد.
- README عمومی هم‌زمان به `Current release 0.13.0` و صف واقعی بعدی به‌روز شد.

**مرحله بعد از انتشار 0.13.0:** ابتدا Acceptance کوتاه روی Android Box؛ سپس
0.14.0 با عنوان **Smart Series Continuity**. قبل از افزودن ردیف سنگین جدید،
Home به Lazy layout مهاجرت کند. Season/Episode فقط وقتی نمایش یا Next Episode
ارائه شود که از عنوان/صفحه عادی و Stable page link به‌صورت بدون ابهام قابل
تشخیص باشد؛ در غیر این صورت Detail page برای انتخاب دستی باز شود.
