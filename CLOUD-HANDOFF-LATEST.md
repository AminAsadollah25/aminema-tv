# تحویل جاری Aminema برای Cloud / برنامه‌نویس بعدی

این فایل خلاصه عملیاتی همیشه‌به‌روز پروژه است. برای جزئیات 0.14.5، فایل‌های
`DEVELOPMENT_LOG_0.14.5.md`، `TEST_REPORT_0.14.5.md` و
`RELEASE_NOTES_0.14.5.md` خوانده شوند. معماری پایدار در
`ENGINEERING-HANDOFF-FA.md` و صف محصول در `ROADMAP.md` است.

## قرارداد دائمی دو برنامه‌نویس

بعد از هر Feature/Fix/Release ثبت شود: نسخه و versionCode، تصمیم UX، علت باگ،
فایل‌های تغییرکرده، Probe واقعی، تست‌ها، مرز امنیتی، محدودیت‌ها، قدم بعدی و
وضعیت Commit/Tag/GitHub Release/Assets. README عمومی نیز باید هم‌زمان Current
release و Next queue واقعی را نشان دهد.

## وضعیت فعلی

- محصول: **Aminema**
- نسخه کد و APK: **0.14.5 / versionCode 29 — Cinema Polish**
- وضعیت در این لحظه: Candidate پذیرفته‌شده، آماده Commit/Tag/انتشار GitHub
- نسخه عمومی قبلی: **0.14.0 / code 28**
- شاخه: `main`
- مخزن: `https://github.com/AminAsadollah25/aminema-tv`
- Package نصب: `com.amin.tvos.debug`
- Package پایه: `com.amin.tvos`
- Package و Debug signing identity تغییر نکرده‌اند؛ نصب `adb install -r`
  Cookie، login، تنظیمات، Library و Poster cache را حفظ کرد.

## 0.14.5 چه چیزی را حل می‌کند

### 1. باگ Recently Opened پارسی‌فلیکس

رکورد واقعی خراب روی امولاتور این بود:

- URL: `https://app.parsiflix.com/medias/movies/357`
- Title ذخیره‌شده: عنوان عمومی Home یعنی `ParsiFlix - Watch Persian...`

URL classifier صحیح بود. علت، Race در SPA بود: Router URL فیلم را پیش از
Hydrateشدن Detail DOM تغییر می‌داد و callback دیررس عنوان Shell را زیر URL
فیلم ذخیره می‌کرد.

اصلاح چندلایه:

1. `BrowserActivity.captureResumeMetadata()` حالا `location.href` را همراه DOM
   metadata برمی‌گرداند و Requested/DOM/Current WebView URL باید یکسان باشند.
2. عنوان عمومی Provider روی Content route رد می‌شود.
3. `ContentMetadataPolicy.kt` Canonical URL و Generic-title policy مشترک است.
4. `LibraryRepository.repairMetadata()` داده قدیمی را از Catalog cache ترمیم
   می‌کند.
5. Home دفاع دوم دارد؛ Generic shell بدون Catalog match نمایش داده نمی‌شود.
6. چهار Unit test این Policy را پوشش می‌دهند.

شاهد پذیرش: همان رکورد `/movies/357` روی دستگاه به **دو روز دیرتر** و Poster
صحیح آن ترمیم شد.

### 2. Cinematic Quick Glance

- بعد از 520ms Hover موس یا Focus ریموت/DPAD ظاهر می‌شود.
- Panel ثابت است و Rail را هل نمی‌دهد یا Focus را نمی‌دزدد.
- Title، Synopsis بدون اسپویل، Year، Genre، Rating، Runtime و Latest published
  episode را در صورت وجود نشان می‌دهد.
- Episode plot و Video autoplay عمداً وجود ندارند.
- ParsiFlix از JSON عادی کاتالوگ خودش استفاده می‌کند.
- DOM واقعی FilmRooz در WebView لاگین‌شده Probe شد: `.postMeta`، فیلدهای
  Runtime/Rating/Genre با separator `<spl>` و Synopsis معمولی کارت.
- `CatalogItem` پنج Field اختیاری و backward-compatible گرفت:
  `summary/year/genres/rating/runtime`.
- JS output قبل از Cache محدود و پاک‌سازی می‌شود؛ Focus move Request جدید
  شبکه نمی‌زند.

### 3. Performance، حجم و Cleanup

- تمام Railهای افقی Home به keyed `LazyRow` منتقل شدند.
- Arrowها Overload مبتنی بر `LazyListState` دارند و به اندازه تعداد کارت‌های
  قابل مشاهده Page می‌کنند.
- R8 و Resource shrink برای همان Debug-signed update channel فعال شد.
- کلاس‌ها و متدهای `@JavascriptInterface` با ProGuard rule صریح حفظ شدند.
- فقط Localeهای `fa/en` Package می‌شوند.
- دو Artwork سینما از PNG حدود 2.3MB مجموع به JPEG حدود 390KB مجموع رسیدند؛
  ابعاد 1280×720 حفظ شد.
- WebSettings deprecated و Lifecycle importهای قدیمی حذف شدند؛ Arrowها
  AutoMirrored هستند.
- Labelهای باقیمانده Home فارسی شدند.
- APK قبلی 0.14.0: `75,975,092` بایت؛ APK Candidate 0.14.5 حدود
  `22,216,560` بایت؛ حدود **71٪ کوچک‌تر**، با حفظ ویدئوی Intro 10.6MB.

## فایل‌های اصلی تغییرکرده

- `app/build.gradle.kts`, `app/proguard-rules.pro`
- `browser/BrowserActivity.kt`, `browser/CatalogBackgroundSync.kt`
- `data/ContentMetadataPolicy.kt`, `data/LibraryRepository.kt`
- `data/model/CatalogModels.kt`
- `ui/components/CatalogCard.kt`, `ui/components/TvComponents.kt`
- `ui/home/CinematicHoverPreview.kt`, `HomeScreen.kt`, `HomeViewModel.kt`
- `ui/home/CatalogSectionRow.kt`, `LiveTvSectionRow.kt`
- `app/src/test/java/com/amin/tvos/data/ContentMetadataPolicyTest.kt`
- `service_iranian_cinema.jpg`, `service_international_cinema.jpg`

## شواهد تست 0.14.5

- `clean testDebugUnitTest lintDebug assembleDebug`: موفق
- Unit test: 4 passed، failure/error صفر
- Android Lint: error صفر
- `git diff --check`: پاک
- نصب: `adb install -r` موفق
- نسخه نصب‌شده: 0.14.5 code 29
- Process بعد از Cold launch فعال؛ FATAL EXCEPTION صفر
- Background catalog واقعی:
  - ParsiFlix: 10 All، 24 Movie، 24 Series
  - FilmRooz: 24 All، 8 Movie، 16 Series در نشست مشاهده‌شده
- نمونه واقعی FilmRooz: Leviticus؛ Synopsis، Year 2026، Genre، Rating ۶.۷ و
  Runtime ۸۸ دقیقه استخراج شد.
- R8 build با Catalog/Keyboard/Playback/Poster bridge rule حفظ‌شده اجرا شد.

## مرز امنیتی که نباید شکسته شود

- فقط Normal top-level page URL و Catalog/Card metadata عادی خوانده می‌شود.
- هیچ `video.src/currentSrc`، Media/Stream request، Protected URL، Cookie،
  Token، Auth header، Password یا DRM value خوانده/ذخیره/Log/Export نمی‌شود.
- هیچ Authentication bypass یا Download/Redistribution وجود ندارد.

## محدودیت‌های صادقانه

- FilmRooz watched checkmark بین Browserها device-local مشاهده شده؛ پس مبنای
  `قسمت ندیده` cross-device نیست.
- ParsiFlix list API همیشه Episode/Season نمی‌دهد؛ Aminema چیزی حدس نمی‌زند.
- Home عمودی هنوز `Column + verticalScroll` است؛ Railها Lazy شده‌اند. اگر
  تعداد بخش‌های عمودی زیاد شود، مرحله بعدی Performance مهاجرت به LazyColumn است.
- Physical Android Box هنوز باید Mouse-hover و WebView device-specific را در
  Acceptance کوتاه تأیید کند.

## گزینه‌های مرحله بعد

1. **Cinema Library:** View All grid برای Latest/Continue/Recent/Favorites با
   Filter سرویس، نوع، ژانر و سال؛ همراه حفظ Focus هنگام برگشت.
2. **Cinematic Home Hero:** یک Hero بزرگ و بدون اسپویل برای بهترین Continue،
   با یک کلیک پخش و Backdrop نرم.
3. **Honest Episode Progress:** Baseline دستی `تا این قسمت دیدم` و Release
   delta؛ واژه `دیده‌نشده` فقط با شاهد دقیق.
4. **Back UX برای FilmRooz:** Short Back = history، Long Back/Menu = Home؛
   ابتدا KeyCode واقعی Android Box Log شود.

## وضعیت Release

پس از انتشار باید همین بخش با Commit، Tag، URL، Asset size، SHA-256 و تأیید
GitHub Latest API جایگزین شود. Release body حتماً `versionCode: 29` داشته باشد
تا Updater داخلی نسخه را تشخیص دهد.
