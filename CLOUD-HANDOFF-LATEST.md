# تحویل جاری Aminema برای Cloud / برنامه‌نویس بعدی

این فایل خلاصه عملیاتی همیشه‌به‌روز پروژه است. برای جزئیات Candidate جدید،
فایل‌های `DEVELOPMENT_LOG_0.15.0.md`، `TEST_REPORT_0.15.0.md` و
`RELEASE_NOTES_0.15.0.md` خوانده شوند. معماری پایدار در
`ENGINEERING-HANDOFF-FA.md` و صف محصول در `ROADMAP.md` است.

## قرارداد دائمی دو برنامه‌نویس

بعد از هر Feature/Fix/Release ثبت شود: نسخه و versionCode، تصمیم UX، علت باگ،
فایل‌های تغییرکرده، Probe واقعی، تست‌ها، مرز امنیتی، محدودیت‌ها، قدم بعدی و
وضعیت Commit/Tag/GitHub Release/Assets. README عمومی نیز باید هم‌زمان Current
release و Next queue واقعی را نشان دهد.

## وضعیت فعلی

- محصول: **Aminema**
- نسخه کد: **0.15.0 / versionCode 31 — Aminema Spotlight**
- وضعیت در این لحظه: **Candidate محلی آمادهٔ تست تلویزیون؛ هنوز منتشرنشده**
- Commit/Tag نسخه 0.15.0: **هنوز ساخته نشده**
- نسخه عمومی فعلی: **0.14.6 / code 30**
- Release فعلی: `https://github.com/AminAsadollah25/aminema-tv/releases/tag/v0.14.6`
- شاخه: `main`
- مخزن: `https://github.com/AminAsadollah25/aminema-tv`
- Package نصب: `com.amin.tvos.debug`
- Package پایه: `com.amin.tvos`
- Package و Debug signing identity تغییر نکرده‌اند؛ نصب `adb install -r`
  Cookie، login، تنظیمات، Library و Poster cache را حفظ کرد.

## 0.15.0 چه چیزی را حل می‌کند

### 1. Spotlight Native به‌جای وابستگی به Hover

- کلیک اول روی فیلم/سریال ایرانی یا خارجی، صفحه Native سینمایی باز می‌کند.
- کلیک دوم روی Watch/Continue همان Browser request آزمایش‌شده قبلی را اجرا
  می‌کند؛ `browserStartUrl`، direct play، resume strategy و action patternها
  داخل `SpotlightItem` حفظ می‌شوند.
- Home، Search، Continue، My Series، Recent و Favorites متصل‌اند.
- Live TV عمداً مستقیم و یک‌کلیکی باقی مانده است.
- Back به Activity قبلی برمی‌گردد و موقعیت Rail/Scroll حفظ می‌شود.
- Download و Trailer وجود ندارد.

### 2. تکمیل اطلاعات واقعی برای عنوان‌های قدیمی

- مشکل گزارش‌شده: Recentهای قدیمی فقط Title/Poster داشتند و Spotlight به‌اشتباه
  می‌گفت معرفی دریافت نشده است، درحالی‌که اطلاعات روی سایت وجود داشت.
- راه‌حل: `SpotlightMetadataLoader` هنگام بازشدن همان Normal signed-in detail
  page را در WebView مخفی 2×2 باز می‌کند و فقط Visible/schema metadata را
  می‌خواند.
- داده‌ها: Synopsis، Year، Genre، Rating، Runtime، Country، Language،
  Director، Cast و وضعیت title-local دوبله/زیرنویس فارسی.
- نمونه واقعی Spider-Man Homecoming:
  `2017`، `۷.۴`، `۱۳۳ دقیقه`، `اکشن/ماجراجویی/علمی-تخیلی`، `آمریکا`،
  `انگلیسی/اسپانیایی`، Jon Watts و سه بازیگر اصلی.
- FilmRooz Synopsis واقعی در
  `.col-12.mt-2.p-2.text-justify.rounded` است؛ Selector قبلی بیش از حد محدود
  بود و اصلاح شد.
- `title_metadata.json` تا 150 عنوان Canonical را نگه می‌دارد و 14 روز تازه
  محسوب می‌کند.
- وضعیت سبز `دوبله فارسی` و آبی `زیرنویس فارسی` فقط از محتوای محلی همان
  Title/Post/Download خوانده می‌شود؛ Header/Nav و Category link عمومی حذف
  می‌شوند تا False positive نسازند.

### 3. بازطراحی Safe Area

- جمله ساختگی «جزئیات بیشتر هنگام ورود...» کاملاً حذف شد.
- اگر Synopsis واقعاً خالی باشد، Section خالی هیچ فضا نمی‌گیرد.
- عنوان و سال در یک Title block هستند؛ Font متعادل شده تا عنوان‌هایی مانند
  Homecoming با سال کنار نام جا شوند و عنوان خیلی بلند حداکثر دو خط باشد.
- Synopsis دو خط، Credits بالای CTAها و Poster کوتاه‌تر است؛ همه اطلاعات و
  دکمه‌ها در 720p/1080p داخل Safe Area می‌مانند.
- Progress کمتر از یک دقیقه به‌عنوان Beacon سایت نمایش داده نمی‌شود.
- FocusRequester اکنون `remember` می‌شود؛ Crash ناشی از Recomposition
  async metadata در QA پیدا و بسته شد.

### 4. گیت‌های فنی Candidate

- `testDebugUnitTest lintDebug assembleDebug`: موفق
- Unit test: 10 pass، failure/error صفر
- Lint error: صفر؛ 108 advisory
- APK: `22,462,380` بایت
- SHA-256:
  `0cc3742aa1b3de12e0681ed61a61a781f0f9c49a6e647c660a50102dec2ae6ee`
- Signing certificate همان 0.14.6:
  `ba6ac8c4c2e1828462e7a6b122ad60856a054a18389af36bc001a1ee38ba13d3`

### 5. مرز امنیتی 0.15.0

- فقط Normal same-host title page و Person profile URL عادی پذیرفته می‌شود.
- هیچ Media/Stream URL، Network request، Cookie، Token، Password، Auth header
  یا DRM value خوانده، ذخیره یا Log نمی‌شود.

## صف بعد از پذیرش 0.15.0

1. `0.15.1 — Spotlight Series & People`: انتخاب فصل/قسمت، Latest published،
   Continue صادقانه، کلیک روی کارگردان/بازیگر و Provider filmography، Follow
   محلی افراد.
2. `0.15.2 — Cinema Library & Alerts`: View All grid و Alert کم‌مزاحمت برای
   اثر جدید فرد دنبال‌شده پس از Sync معمول کاتالوگ.
3. سپس `0.16.0 — Cinematic Home` و Reliability track.

## 0.14.6 چه چیزی را حل می‌کند

### 1. Hover و Focus مشترک و سینمایی

- `FocusableCard` اکنون DPAD Focus، Compose HoverInteraction و Pointer
  Enter/Exit واقعی را با هم ادغام می‌کند.
- Border قرمز حذف شد؛ Scale، Brightness، Elevation و Z-order با Transition
  کوتاه جای آن را گرفت.
- Posterها 1.06 و Service Cardها 1.035 بزرگ می‌شوند.
- Railها Vertical padding کافی دارند؛ Scale کارت‌ها را Clip یا جابه‌جا
  نمی‌کند.
- Probe واقعی Mouse-source روی امولاتور Quick Glance را باز کرد و خروج موس
  آن را بست.

### 2. Autoplay فقط برای FilmRooz

- `DirectPlayConfig.autoPlayOnPlaybackPage` اختیاری و پیش‌فرض `false` است.
- فقط FilmRooz آن را فعال می‌کند؛ ParsiFlix دست‌نخورده باقی مانده است.
- پس از رسیدن به Normal top-level `/stream/...`، Aminema روی HTML5 video همان
  صفحه `play()` می‌زند یا کنترل Visible خود JW/Video.js/Plyr را Click می‌کند.
- Test واقعی Leviticus پس از Mouse click بدون Click دوم به Playback position
  `20,438ms` و duration `5,288,872ms` رسید؛ FATAL EXCEPTION صفر.
- Loading تا Play واقعی می‌ماند؛ اگر Player آماده نشود Timeout قبلی صفحه
  Manual را در دسترس می‌گذارد.
- گیت نهایی `clean testDebugUnitTest lintDebug assembleDebug` موفق شد؛ ۶ تست
  Pass، Lint error صفر، APK برابر `22,232,968` بایت و SHA-256 برابر
  `08889d3e65ac170a35c1806668bdcdec5b0154237749f49415ef968301fe011e`
  است. Signing certificate با 0.14.5 یکسان است.

### 3. مرز امنیتی

- هیچ `video.src/currentSrc`، Network request، Protected media URL، Cookie،
  Token، Password، Auth header یا DRM value خوانده یا Log نمی‌شود.
- فقط صفحه عادی Provider و کنترل Play قابل‌مشاهده همان صفحه استفاده می‌شود.

## صف قطعی بعد از انتشار 0.14.6

1. `0.15.0 — Aminema Spotlight`: Hero و Detail Native، بدون Download/Trailer.
2. `0.15.1 — Episode Navigator`: فصل/قسمت، Latest published و Continue صادقانه.
3. `0.15.2 — Cinema Library`: View All grid و Filter.
4. `0.16.0 — Cinematic Home`.
5. `0.16.1 — Reliability & Provider Health`.

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

## صف قطعی نسخه‌های بعد

1. **0.15.0 — Aminema Spotlight:** Hero/Detail سینمایی Native برای فیلم و
   سریال، خلاصه بدون اسپویل، Watch/Continue، My List و More؛ بدون Download و
   بدون Trailer در مسیر اصلی.
2. **0.15.1 — Episode Navigator:** انتخاب فصل/قسمت پس از Provider Probe،
   «آخرین قسمت منتشرشده» مستقل و «ادامه قسمت بعد» فقط با شاهد معتبر.
3. **0.15.2 — Cinema Library:** View All grid برای Latest/Continue/Recent/
   Favorites با Filter سرویس، نوع، ژانر و سال و حفظ Focus هنگام برگشت.
4. **0.16.0 — Cinematic Home:** Hero پویا، Continue اولویت‌دار، Backdrop نرم
   و ردیف‌های کمتر و هدفمندتر.
5. **0.16.1 — Reliability & Provider Health:** وضعیت Login سرویس‌ها، تفکیک
   Internet/DNS/Login/Unavailable، Cache آفلاین و Back بهتر FilmRooz.

قانون Progress همچنان «صادقانه» است: Baseline دستی `تا این قسمت دیدم` و
Release delta؛ واژه `دیده‌نشده` فقط با شاهد دقیق. برای Back در FilmRooz نیز
ابتدا KeyCode واقعی Android Box ثبت می‌شود، سپس Short Back = history و Long
Back/Menu = Home طراحی خواهد شد.

## وضعیت Release

- Commit: `b08f7c1`
- Tag و Latest API: `v0.14.6`
- Release عمومی: `https://github.com/AminAsadollah25/aminema-tv/releases/tag/v0.14.6`
- APK: `Aminema-v0.14.6-debug.apk` — `22,232,968` بایت — state `uploaded`
- SHA asset: `Aminema-v0.14.6-debug.apk.sha256` — state `uploaded`
- SHA-256:
  `08889d3e65ac170a35c1806668bdcdec5b0154237749f49415ef968301fe011e`
- Release عمومی، `draft=false` و `prerelease=false` است و
  `/releases/latest` همین Tag و هر دو Asset را برگرداند.
- Release body شامل `versionCode: 30` است تا Updater داخلی نسخه را تشخیص دهد.
