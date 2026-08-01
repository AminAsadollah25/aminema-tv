# تحویل جاری Aminema برای Cloud / برنامه‌نویس بعدی

این فایل خلاصه عملیاتی همیشه‌به‌روز پروژه است. برای جزئیات Candidate جدید،
فایل‌های `DEVELOPMENT_LOG_0.16.2.md`، `TEST_REPORT_0.16.2.md` و
`RELEASE_NOTES_0.16.2.md` خوانده شوند. معماری پایدار در
`ENGINEERING-HANDOFF-FA.md` و صف محصول در `ROADMAP.md` است.

## قرارداد دائمی دو برنامه‌نویس

بعد از هر Feature/Fix/Release ثبت شود: نسخه و versionCode، تصمیم UX، علت باگ،
فایل‌های تغییرکرده، Probe واقعی، تست‌ها، مرز امنیتی، محدودیت‌ها، قدم بعدی و
وضعیت Commit/Tag/GitHub Release/Assets. README عمومی نیز باید هم‌زمان Current
release و Next queue واقعی را نشان دهد.

برای هر ایده قبل از اجرا فقط به دستور خام اکتفا نشود. چهار خروجی ارائه شود:
مسئله واقعی، راه سریع و مقاوم، راه کامل و بلندمدت، و یک پیشنهاد مکمل UI/UX
برای رساندن کاربر به تماشا با کلیک کمتر. اگر راه دقیق قابل‌اعتماد نبود،
Fallback صادقانه طراحی شود؛ داده حدس زده نشود و نبود API بهانه توقف ایده
نباشد. این قرارداد دائمی Creative Co-Creator پروژه است.

## وضعیت فعلی

- محصول: **Aminema**
- نسخه کد: **0.16.2 / versionCode 34 — Cinematic Hero, Complete Metadata & Search Polish**
- وضعیت در این لحظه: **Candidate نهایی؛ Build، Unit، Lint و Emulator QA موفق**
- Commit کد انتشار: هنوز ساخته نشده
- Tag هدف: `v0.16.2`
- GitHub Latest فعلی: **v0.16.1؛ بدون Asset دانلود**
- Release هدف: `https://github.com/AminAsadollah25/aminema-tv/releases/tag/v0.16.2`
- شاخه: `main`
- مخزن: `https://github.com/AminAsadollah25/aminema-tv`
- Package نصب: `com.amin.tvos.debug`
- Package پایه: `com.amin.tvos`
- Package و Debug signing identity تغییر نکرده‌اند؛ نصب `adb install -r`
  Cookie، login، تنظیمات، Library و Poster cache را حفظ کرد.

## 0.16.2 چه چیزی را حل می‌کند

### 1. Hero پوسترمحور، RTL و بنرهای سبک‌تر

- Featured title با پوستر واقعی عمودی در foreground دیده می‌شود؛ Wide art
  فقط backdrop سینمایی است و هرگز داخل قاب پوستر Stretch نمی‌شود.
- فقط اسلاید فعال Compose می‌شود؛ Background حداکثر 1280×720 و پوستر حداکثر
  420×630 Decode می‌شوند و Motion بی‌نهایت حذف شده است.
- Hero صریحاً RTL است: متن راست‌چین، اکشن اصلی سمت راست، «بعدی» سمت چپ و جهت
  فلش درست.
- بعد از تعامل کاربر با Railها، Auto advance متوقف می‌شود.
- Hero دیگر برای هر اسلاید WebView مخفی Metadata نمی‌سازد؛ Enrichment هنگام
  ورود واقعی به Spotlight انجام می‌شود.
- بنرهای Featured بزرگ‌تر و خواناتر شدند و Focus treatment ظریف دارند.

### 2. Metadata کامل‌تر و غیرمخرب

- Featuredهای ParsiFlix/FilmRooz در Sync با Detail page به پوستر واقعی و
  خلاصه Hydrate می‌شوند؛ banner در `backdropUrl` باقی می‌ماند.
- variantهای تکراری all/series/popular/featured بر اساس URL canonical Merge
  می‌شوند؛ رکورد ناقص خلاصه و عوامل رکورد کامل را پاک نمی‌کند.
- `SpotlightMetadataLoader` برای SPA Retry دارد و Summary selectorهای schema,
  meta و DOM را با فیلتر متن منو/دانلود ترکیب می‌کند.
- `PublicTitleMetadataEnricher` فقط فیلدهای خالی Provider را از Wikipedia/
  Wikidata تکمیل می‌کند. Persian-first، English fallback، تطبیق IMDb ID عمومی
  یا عنوان+سال+نوع و نام فارسی عوامل.
- IMDb Scrape نمی‌شود. Cookie، Token، Password، DRM و media URL به منبع عمومی
  ارسال نمی‌شود. نتیجه نامطمئن ساخته نمی‌شود و miss به‌مدت ۳۰ روز Cache است.

### 3. Back جستجو

- دکمه واضح «بازگشت» با پشتیبانی DPAD و Mouse به Header جستجو اضافه شد.
- Back ریموت و دکمه روی صفحه هر دو به `returnToHome()` می‌روند.
- در حالت Task-root restore، مسیر `CLEAR_TOP | SINGLE_TOP` Home را باز می‌کند
  و از برگشت اشتباه به Launcher جلوگیری می‌کند.

### 4. تست و خروجی

- `testDebugUnitTest lintDebug assembleDebug`: موفق
- Lint blocking error: صفر
- نصب درجا روی `Television_1080p`: موفق؛ نشست‌ها حفظ شد
- QA واقعی: `Tuner` و `Michael` در Hero، `House of the Dragon` و `The Hawk`
  خارجی، `کوری` ایرانی؛ پوستر/خلاصه/RTL و Metadata صحیح.
- دکمه Back تصویری و Back ریموت از Search: هر دو به MainActivity برگشتند.
- FATAL EXCEPTION: صفر
- APK تمیز نهایی: `22,691,756` بایت
- SHA-256:
  `0724de50be9af4a65b1098a88154a95db45fd91cb2abd511ce4e9ac3355443aa`
- تغییرات هنوز Commit/Push/Tag/Release نشده‌اند و قبل از انتشار باید از مالک
  تأیید گرفته شود.

## قدم بعد از پذیرش 0.16.2

1. `0.16.3 — Episode Navigator`
2. `0.16.4 — Canonical Library, Dedupe & Smart Search`
3. `0.16.5 — My Series`
4. `0.16.6 — Cinematic Promo Feed`
5. سپس MyMoviz/Best Source/People طبق `ROADMAP.md`

## 0.15.1 چه چیزی را حل می‌کند

### 1. Home محتوامحور و یک Moment سینمایی

- Service cardهای بزرگ دیگر تصمیم اول کاربر نیستند.
- `CinematicHero` حداکثر پنج عنوان Canonical از Continue، My Series،
  تازه‌های ایرانی/خارجی و سریال برگزیده می‌سازد.
- Hero هر ۱۱ ثانیه با Fade/Depth نرم عوض می‌شود و هنگام Focus/Hover روی
  دکمه‌ها متوقف می‌ماند.
- Primary action همان `SpotlightItem` قبلی را باز می‌کند؛ هیچ Browser،
  Login، Resume یا Direct-play path بازنویسی نشده است.
- عنوان، سال، نوع، آخرین Episode label، Rating و دوبله/زیرنویس فقط در صورت
  وجود واقعی نمایش داده می‌شوند؛ Chipها برای 720p حداکثر چهار عددند.

### 2. سلسله‌مراتب جدید Home

- Brand bar کوچک: Mascot، Search، Sync و Settings.
- Greeting از Header دوخطی بزرگ به Moment یک‌خطی تبدیل شد.
- Hero کامل داخل First viewport 1080p می‌ماند و ابتدای Rail بعدی را نیز نشان
  می‌دهد.
- Continue، My Series، تازه‌های ایرانی، تازه‌های خارجی، برگزیده، Live،
  Recent و Favorites همان Data/Action قبلی را حفظ کرده‌اند.
- دو کارت بامزه فیلم ایرانی/خارجی حذف نشدند: در انتهای Home به‌عنوان
  `ورود مستقیم به سینماها` هستند. فقط اگر Home واقعاً هیچ Hero data نداشته
  باشد نزدیک بالا با عنوان `از اینجا شروع کن` می‌آیند.

### 3. Motion و Performance

- Header و Hero با Fade/Slide کوتاه وارد می‌شوند.
- Focus ریموت و Hover موس روی Cardها همان Scale، Lift، Brightness و Z-order
  ۱۸۰–۱۹۰ms را دارند؛ Border قرمز وجود ندارد.
- Focus هر کارت Poster/Title، Backdrop همان عنوان را درخواست می‌کند.
- Dwell برابر ۲۲۰ms مانع Decode هنگام DPAD sweep سریع است؛ Crossfade
  پس‌زمینه ۷۰۰ms است.
- Backdrop همچنان نسخه کوچک 96×144 می‌گیرد؛ Railها LazyRow باقی مانده‌اند.
- Hero Promoهای واقعی Provider هنوز خوانده نمی‌شوند؛ 0.15.5 می‌تواند همان
  Shell را با Adapter عادی Carousel تغذیه کند.

### 4. فایل‌ها و تست فعلی Candidate

- فایل جدید: `ui/home/CinematicHero.kt`
- فایل‌های اصلی تغییرکرده: `HomeScreen.kt`، `SmartGreetingHeader.kt`،
  `CatalogSectionRow.kt`، `CatalogCard.kt` و `TvComponents.kt`
- `clean testDebugUnitTest lintDebug assembleDebug`: موفق
- Unit test: ۱۰ pass، failure/error صفر؛ Lint blocking error صفر
- نصب `adb install -r` روی Android TV 1920×1080: موفق
- QA بصری: Header/Greeting/Hero کامل، Rail بعدی قابل‌تشخیص و FATAL
  EXCEPTION صفر
- APK Candidate: `22,544,296` بایت
- SHA-256 Candidate:
  `dacc292ba98cef9d0202f0edab1c485f025b80d6a25b5a8e00907f77c5b7a305`
- تغییرات 0.15.1 هنوز Commit/Push/Tag/Release نشده‌اند؛ دستور ادامه دقیق در
  `CLOUD-NEXT-PROMPT.md` است.

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

### 4. گیت‌های فنی و انتشار

- `testDebugUnitTest lintDebug assembleDebug`: موفق
- Unit test: 10 pass، failure/error صفر
- Lint error: صفر؛ 108 advisory
- APK: `22,462,380` بایت
- SHA-256:
  `0cc3742aa1b3de12e0681ed61a61a781f0f9c49a6e647c660a50102dec2ae6ee`
- Signing certificate همان 0.14.6:
  `ba6ac8c4c2e1828462e7a6b122ad60856a054a18389af36bc001a1ee38ba13d3`
- GitHub Assetها:
  - `Aminema-v0.15.0-debug.apk` — state `uploaded`
  - `Aminema-v0.15.0-debug.apk.sha256` — state `uploaded`
- `/releases/latest` برابر `v0.15.0`، `draft=false` و `prerelease=false` است.
- APK دوباره از GitHub دانلود شد و SHA آن دقیقاً با مقدار بالا برابر بود.

### 5. مرز امنیتی 0.15.0

- فقط Normal same-host title page و Person profile URL عادی پذیرفته می‌شود.
- هیچ Media/Stream URL، Network request، Cookie، Token، Password، Auth header
  یا DRM value خوانده، ذخیره یا Log نمی‌شود.

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

تحلیل کامل MyMoviz، Dedupe، Series Progress، Promo Banner و Geek Mode در
`MYMOVIZ_PRODUCT_ANALYSIS.md` و نسخه اولویت‌بندی‌شده در `ROADMAP.md` ثبت شد.

1. **0.16.3 — Episode Navigator:** فصل/قسمت، `ادامه قسمت بعد`،
   `آخرین قسمت منتشرشده` و Progress صادقانه.
2. **0.16.4 — Canonical Library, Dedupe & Smart Search:** یک عنوان/یک کارت/
   چند SourceVariant؛ پذیرش اولیه با `لیسانسه`؛ Query variant برای
   `spiderman`، `spider man` و `spider-man`.
3. **0.16.5 — My Series:** Follow، Baseline دستی، قسمت/فصل جدید و سپس
   Account progress در Providerهایی که شاهد قابل‌اعتماد دارند.
4. **0.16.6 — Cinematic Promo Feed:** تغذیه Hero موجود، توقف روی Focus و
   Title→Spotlight / Live→Direct.
5. **Coverage Lab:** گزارش حداقل 100 عنوان پیش از ادعای درصد هم‌پوشانی.
6. **0.17.0 — MyMoviz Provider:** فقط Coverage gap یا نسخه دوبله بهتر.
7. **0.17.1 — Best Source Resolver:** Dub-first/original-first/ask.
8. **0.17.2 — People & Alerts**
9. **0.18.0 — Cinema Library & Personal Home**
10. **0.18.1 — Reliability, Keyboard & Provider Health**
11. **0.19.0 — Geek Mode**

یافته عملی MyMoviz: صفحه عادی سریال فصل‌ها، تعداد قسمت، آخرین انتشار،
Progress، `قسمت بعدی شما` و علامت‌گذاری قسمت/فصل را دارد؛ صفحه `من` نیز
Continue، قسمت بعدی و تقویم پخش دارد. Search واقعی `spiderman` صفر،
`spider man` شانزده و `spider-man` سیزده نتیجه داد. صفحه عنوان IMDb ID
صریح دارد، پس برای Canonical matching بسیار مناسب است.

قانون Progress همچنان «صادقانه» است: Baseline دستی `تا این قسمت دیدم` و
Release delta؛ واژه `دیده‌نشده` فقط با شاهد دقیق. برای Back در FilmRooz نیز
ابتدا KeyCode واقعی Android Box ثبت می‌شود، سپس Short Back = history و Long
Back/Menu = Home طراحی خواهد شد.

## وضعیت GitHub پیش از انتشار Candidate 0.16.2

- HEAD/Tag فعلی: `7c3ef90 / v0.16.1`
- `/releases/latest` برابر `v0.16.1` است، اما این Release هیچ Asset دانلودی
  ندارد.
- آخرین Release دارای APK تأییدشده: `v0.16.0`.
- اقدام لازم پس از تأیید مالک: Commit تغییرات، Tag `v0.16.2`، Push و Release
  همراه هر دو Asset یعنی APK و فایل SHA-256؛ سپس دانلود مجدد Asset و تطبیق
  Digest.

## آخرین تکمیل محلی 0.16.2 — Codex، ۱ اوت ۲۰۲۶

- Hero: سال بدون «شمسی/میلادی»، حذف Country chip، کنترل قبلی/بعدی در پایینِ
  چپ و بدون هم‌پوشانی متن.
- Spotlight: حذف سال تکراری از عنوان، Synopsis چهارخطی، کشور/زبان/ژانر/مدت/
  دوبله/زیرنویس و Credits کامل‌تر.
- IMDb خارجی: چهار سطح رنگی `<5`، `5–7`، `7–9` و `9+` همراه برچسب کیفیت.
- fallback تازه `SheydaMetadataLoader.kt`: WebView عمومی بدون Login، تطبیق
  دقیق Title+Kind و استخراج فقط Metadata قابل‌مشاهده شیدا برای آثار ایرانی.
- `PublicTitleMetadataEnricher.LOOKUP_VERSION = 5` تا Negative cache قدیمی
  یک‌بار Retry شود.
- پذیرش واقعی «بامداد خمار» موفق: ۱۴۰۳، نرگس آبیار، علی مصفا، رضا کیانیان،
  لاله اسکندری، گلاره عباسی، ژانرها و Backdrop رسمی.
- گیت نهایی `clean + testDebugUnitTest + assembleDebug` موفق و
  `adb install -r` موفق بود. APK نهایی `22,708,140` بایت با SHA-256
  `7e434aea4f5aa5b81d238eac33e63adf107ced420149a58b1b5d5f552e81580f`
  است. `SpotlightActivity` در APK نهایی
  `android:exported="false"` است.
- هنوز Commit/Push/Tag/Release انجام نشده؛ فقط پس از تأیید مالک منتشر شود.
