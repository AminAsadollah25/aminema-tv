# سند فنی مداوم Aminema

این سند برای این نوشته شده که **هر مدل هوش مصنوعی دیگری** (یا خود مالک پروژه
بعد از مدتی) بتواند بدون بازخوانی کل تاریخچه چت، هم تصویر کلی پروژه و هم
جزئیات فنی دقیق (Endpoint واقعی، الگوریتم، فرمول، تصمیم و چرایی‌اش) را
بفهمد و مستقیم ادامه بدهد. **این سند باید بعد از هر تغییر مهم بروزرسانی
شود** — یک خط جدید در بخش مربوطه، نه بازنویسی کامل.

چهار سند دیگر هم هست و نقش جدا دارند:
`CLOUD-HANDOFF-LATEST.md` (خلاصه عملیاتی و وضعیت آخر برای تحویل مستقیم بین
دو برنامه‌نویس/مدل)، `README.md` (توضیح عمومی قابلیت‌ها),
`ROADMAP.md` (لیست کارهای آینده و تصمیم‌های ثبت‌شده به ترتیب زمانی گفتگو),
`TEST_REPORT.md` (نتیجه تست هر نسخه). نقش این سند: **چگونگی فنی**.

**قاعدهٔ دائمی همکاری:** بعد از هر Feature/Fix/Release فقط کد کافی نیست؛
نسخه، تصمیم UX، فایل‌های تغییرکرده، کشف‌های Probe واقعی، تست‌ها، مرز امنیتی،
کارهای باز و وضعیت Release باید در `CLOUD-HANDOFF-LATEST.md` خلاصه و در سند
تخصصی مربوطه با جزئیات ثبت شوند؛ مثل تحویل کار بین دو برنامه‌نویس.

---

## ۱. پروژه در یک نگاه

Aminema یک Hub شخصی Android TV است — نه سرویس استریم. فقط وب‌سایت‌هایی را
که مالک پروژه شخصاً در آن‌ها حساب و اشتراک قانونی دارد، در یک WebView
حرفه‌ای برای تلویزیون باز می‌کند. **خط قرمزهای مطلق** که در تمام تصمیم‌های
فنی رعایت شده‌اند:

- هیچ Authentication Bypass
- هیچ استخراج یا ذخیره لینک فایل مدیا/استریم/DRM/Token
- هیچ Download یا بازنشر محتوا
- هیچ خروج Cookie از WebView
- هر چیزی که پیاده‌سازی می‌شود باید معادل همان کاری باشد که کاربر با
  کلیک‌کردن روی خود سایت انجام می‌داد — نه بیشتر

**شناسه فنی:** `com.amin.tvos` (Release) / `com.amin.tvos.debug` (نصب‌شده
روی تلویزیون واقعی). **این هرگز عوض نمی‌شود** — تمام Buildها با Debug
Keystore محیط اصلی امضا می‌شوند تا هر نسخه جدید روی نصب قبلی Update شود، نه
نصب تازه.

**زبان/UI:** Kotlin، Jetpack Compose، Material 3، حداقل Android 9
(minSdk 28)، compileSdk/targetSdk 35.

**سرویس‌های پیکربندی‌شده (`app/src/main/assets/services.json`):**
| Id داخلی | نام نمایشی | دامنه |
|---|---|---|
| `parsiflix` | فیلم ایرانی | `app.parsiflix.com` |
| `filmrooz` | فیلم خارجی | `sean.robert-redford.net` |
| `mymoviz` | مای‌موویز | `mymoviz.co` |
| `parsatv` | پخش زنده | `parsatv.com` |

این Idها شناسه داخلی‌اند و نباید به‌عنوان نام فنی در UI نمایش داده شوند.

---

## ۲. نقشه فایل‌ها (کجا دنبال چی بگردیم)

```
app/src/main/java/com/amin/tvos/
├── MainActivity.kt                 Cold-start + Intro gate + Nav host
├── AminTvApp.kt                    Service locator (تمام Repositoryها اینجا lazy می‌شوند)
├── intro/                          اینترو Cold-start (0.7.4)
│   ├── IntroGate.kt                یک‌بارمصرف در سطح Process
│   ├── IntroOverlay.kt             VideoView + Fallbackها
│   └── IntroPreferences.kt         SharedPreferences (نه DataStore — باید Sync باشد)
├── browser/
│   ├── BrowserActivity.kt          مرورگر اصلی؛ Resume/DirectPlay/Keyboard/QuickMenu همه اینجا
│   ├── ServiceAdapter.kt           منطق تشخیص صفحه بر اساس services.json (per-service, no hardcode)
│   ├── CatalogBackgroundSync.kt    Sync مستقل کاتالوگ+حساب پشت Home (0.14.0)
│   ├── AccountSyncActivity.kt      Sync حساب برای Continue Watching (0.7)
│   ├── SiteSearchEngine.kt         جستجوی یکپارچه (0.9.0)
│   ├── KeyboardSafeWebView.kt      WebView بدون IME سیستمی
│   └── MouseKeyboardOverlay.kt     کیبورد اختصاصی Aminema
├── ui/home/
│   ├── HomeScreen.kt               صفحه اصلی؛ Scroll عمودی + Railهای Lazy (0.14.5)
│   ├── HomeViewModel.kt            State تمام ردیف‌های Home + Update state
│   ├── SmartGreeting.kt            سلام هوشمند + تبدیل جلالی (0.8.1)
│   ├── CinematicBackground.kt      پس‌زمینه محو 960×540 با Dwell و Crossfade
│   ├── CinematicHero.kt            Hero ثابت Copy/Art + Motion و Auto-advance کنترل‌شده
│   ├── CinematicHoverPreview.kt    Quick Glance بدون اسپویل (0.14.5)
│   ├── CatalogSectionRow.kt        ردیف «تازه‌ها» با فیلتر همه/فیلم/سریال
│   ├── FeaturedBannerCard.kt       بنر عریض برگزیده Providerها
│   ├── LiveTvSectionRow.kt         ردیف بومی شبکه‌ها و کارت‌های LIVE (0.12.0)
│   └── UpdateBanner.kt             بنر بروزرسانی خودکار (0.10.0)
├── ui/search/SearchActivity.kt     صفحه جستجو + کیبورد فارسی/انگلیسی روی صفحه
├── update/
│   ├── UpdateModels.kt             ReleaseInfo, UpdateState (sealed class)
│   └── UpdateRepository.kt         چک GitHub API، دانلود، sha256، Install Intent
└── data/
    ├── ServicesRepository.kt       بارگذاری/ادغام services.json (نگاه کن به بخش ۷.۱ برای باگ مهاجرت)
    ├── CatalogRepository.kt        کش JSON «تازه‌ها»
    ├── LibraryRepository.kt        Continue/Recents/Favorites (MovieItem, PlaybackSession)
    ├── ContentMetadataPolicy.kt    Canonical URL + رد Shell metadata (0.14.5)
    ├── SettingsRepository.kt       DataStore: Zoom, UA, فیلتر کاتالوگ، Skipped update version
    └── model/
        ├── Models.kt               StreamingService (شامل DirectPlayConfig), MovieItem, PlaybackSession
        └── CatalogModels.kt        CatalogItem/Section/Kind + catalogKindFromUrl()
```

---

## ۳. تاریخچه نسخه‌ها (فشرده)

| نسخه | چی اضافه شد |
|---|---|
| 0.7.3 | Rebrand به Aminema |
| **0.7.4** | اینترو Cold-start (VideoView، IntroGate یک‌بارمصرف) |
| **0.8.0** | ردیف‌های «تازه‌ها»ی ایرانی/خارجی، CatalogRepository |
| **0.8.1** | سلام هوشمند (ساعت+تقویم)، پس‌زمینه سینمایی |
| **0.8.2** | Direct Play فیلم (پخش مستقیم به‌جای توقف روی جزئیات) |
| **0.9.0** | جستجوی یکپارچه با کیبورد روی‌صفحه |
| 0.9.1–0.9.3 | باگ‌فیکس‌های Direct Play (پخش زنده اشتباهی، Continue، Recently/Favorites) |
| **0.10.0** | بروزرسانی خودکار درون‌برنامه‌ای (GitHub Releases) |
| 0.10.1 | اصلاح ظاهری بنر بروزرسانی |
| **0.11.0** | فیکس race شرطی Direct Play در SPA، سقف Sync از ۱۲ به ۳۰، `quickLinks[]` برای یوتیوب فارسی؛ Live آن نسخه فقط لینک واسط و اشتباه بود |
| **0.12.0** | ردیف بومی ۲۰ شبکه با لوگوی واقعی، پخش مستقیم CSS تمام‌صفحه، D-pad/موس و Back به همان کارت |
| **0.12.1** | Continue/Direct Play تأییدمحور، Sync حساب خودکار و یکسان بین دستگاه‌ها، Loading سینمایی و فلش مشترک ردیف‌ها |
| **0.13.0** | Search Deck و Login Input Deck با QWERTY فارسی/انگلیسی و State Machine پایدار |
| **0.14.0** | Series Pulse: قسمت/فصل، سریال‌های من، برگزیده‌ها، Sync مستقل و پس‌زمینه‌ای |
| **0.14.5** | Cinema Polish: اصلاح Recent، Quick Glance، Lazy rail، APK حدود 71٪ کوچک‌تر |
| **0.14.6** | Pointer & Playback Polish: Hover واقعی موس، Focus بدون Border قرمز، Autoplay صفحه پلیر FilmRooz |
| **0.15.0/0.15.1** | Spotlight و Cinematic Home |
| **0.16.0** | رفع باگ «تصویر عریض در کارت عمودی» (۴ جا)، بازطراحی Hero با رنگ استخراج‌شده از پوستر و اسکلت ثابت، رفع بریدگی دکمه‌ها در Hero و Spotlight، فیلد `backdropUrl`، دو ردیف «برگزیده‌ها» با بنر عریض خود سایت‌ها، انتقال خروجی بیلد به بیرون از iCloud |
| **0.16.1** | Visual polish و اصلاح fallback خلاصه Spotlight؛ Tag/Release ساخته شد اما APK داخلی روی 0.16.0/code33 ماند و GitHub Asset ندارد |
| **0.16.2** | Hero کم‌حافظه با پوستر واقعی و backdrop جدا، RTL کامل، Merge غیرمخرب Metadata، خلاصه سریال ایرانی/خارجی، fallback عمومی Wikipedia/Wikidata و Back مطمئن Search |
| **0.18.3 Candidate** | Stability & Cinema Polish: Update صحیح، Repository mutex، Lazy Live TV، lifecycle cleanup، Settings فارسی/ایمن، Focus/Hover نرم و Packaging تمیز؛ هنوز منتشر نشده |

**قرارداد Versioning:** بعد از 0.9 → 0.10 → 0.11 … نه 1.0. باگ‌فیکس هم
نسخه جدا می‌گیرد (0.9.1، 0.9.2، …)، نه Patch روی نسخه قبلی.

### ۳.۱ تصمیم‌های Performance و Navigation نسخه 0.16.2

- Blur کردن یک تصویر به رزولوشن اصلی نیازی ندارد. `CinematicBackground`
  تصویر را در 960×540 می‌گیرد و Hero تصویر عریض را حداکثر 1280×720؛ این
  کیفیت لازم برای صفحه 1080p را حفظ می‌کند و از Decode فایل 2K/4K جلوگیری
  می‌کند.
- فقط اسلاید فعال Compose می‌شود. `backdropUrl` حداکثر 1280×720 و پوستر واقعی
  foreground حداکثر 420×630 درخواست می‌شود؛ بنر هیچ‌وقت داخل قاب پوستر کشیده
  نمی‌شود.
- Hero فقط یک حرکت کوتاه scale دارد، نه InfiniteTransition. پس از Focus/Hover
  روی Railها `allowAutoAdvance=false` می‌شود تا کار خارج Viewport ادامه پیدا
  نکند.
- تکمیل Metadata با `SpotlightMetadataLoader` فقط هنگام بازشدن Spotlight
  انجام می‌شود؛ چرخش Home نباید WebView مخفی بسازد.
- رکوردهای یک URL canonical در ردیف‌های مختلف با `mergeCatalogVariants`
  ادغام می‌شوند؛ first-wins نباید خلاصه کامل یک variant را پاک کند.
- `PublicTitleMetadataEnricher` فقط پس از Provider و فقط برای فیلدهای خالی
  اجرا می‌شود. جستجوی فارسی Wikipedia اول است، English fallback دوم؛ تطبیق
  با IMDb ID عمومی یا عنوان+سال+نوع محتوا اعتبارسنجی می‌شود. هیچ Cookie/Token/
  media URL ارسال و هیچ داده‌ای حدس زده نمی‌شود.
- `SearchActivity.returnToHome()` مسیر مشترک دکمه روی صفحه و Back سخت‌افزاری
  است. با `CLEAR_TOP | SINGLE_TOP`، حتی اگر Search پس از Process reclaim ریشه
  Task شده باشد، مقصد Home تضمین می‌شود و کاربر به Launcher پرت نمی‌شود.

---

## ۴. الگوریتم‌ها و فرمول‌های کلیدی

### ۴.۱ تبدیل میلادی به جلالی (`SmartGreeting.kt::gregorianToJalali`)

بدون کتابخانه خارجی، محاسبه مستقیم برای تشخیص نوروز/سیزده‌بدر/یلدا. فرمول
استاندارد epoch-based (مبنا سال ۱۹۷۹ / ۹۷۹ شمسی). تست‌شده و تأییدشده:
`2026-03-21 → 1405/1/1`، `2026-12-21 → 1405/9/30`.

### ۴.۲ منطق سلام هوشمند

ورودی: `hour`, `weekday`, تاریخ جلالی. **هفته به سبک هلند** (نه ایران!):
آخر هفته از **جمعه‌شب (≥16:00)** شروع و شنبه/یکشنبه را می‌گیرد؛ یکشنبه‌شب
(≥21:00) حالت «شب مدرسه/کار فردا» جدا دارد. مناسبت‌ها: کریسمس (۲۴/۲۵/۲۶
دسامبر)، سال نو میلادی (۳۱ دسامبر/۱ ژانویه)، نوروز/سیزده‌بدر/یلدا (شمسی).
هر Bucket چند نسخه متن دارد (`variant` پارامتر) برای دکمه Shuffle.

هر پیام یک `GreetingAction` دارد (`CONTINUE_LAST`, `PICK_MOVIE`,
`PICK_SERIES`, `SURPRISE`, `NONE`) که فقط وقتی داده واقعی پشتش هست فعال
می‌شود (`hasContinue`, `hasCatalog` پارامتر ورودی).

### ۴.۳ الگوریتم انتخاب Direct Play (`BrowserActivity.kt::tryOpenBestStream`)

**منبع واقعی (بازرسی‌شده، نه حدس):** دکمه «پخش آنلاین» FilmRooz یک
`div[onclick]` است، نه لینک. الگوی `onclick`:
```
tmpURL = '/stream/<streamId>/<postId>/?lang=<original|dubbed-fa>&h=<2160|1080|720|480>'
```
اسکریپت JS تزریق‌شده تمام این Regexها را از HTML صفحه جزئیات استخراج
می‌کند: `/\/stream\/\d+\/\d+\/\?[^'"]+/`.

**اولویت انتخاب (دقیقاً به خواسته مالک پروژه):**
1. زبان‌های `preferredLanguages` به ترتیب (پیش‌فرض:
   `["dubbed-fa", "original"]` — دوبله همیشه اول)
2. برای هر زبان، `preferredHeights` به ترتیب (پیش‌فرض `[1080, 720, 480]`
   — **۲۱۶۰ هرگز در لیست نیست، پس هرگز انتخاب نمی‌شود**)
3. اگر هیچ زبان ترجیحی+کیفیت مجاز پیدا نشد → همان کیفیت‌های مجاز با هر
   زبانی
4. اگر نسخه فقط تک‌گزینه‌ای بدون پارامتر کیفیت بود (بدون `h=`) → آن به
   ترتیب زبان انتخاب می‌شود (Fallback آخر)
5. هیچ‌کدام؟ همان صفحه جزئیات باز می‌ماند (بدون خطا)

انتخاب **فقط بر اساس عدد `h=` و کلید `lang=`** است، نه برچسب منبع
(WEBRip/DVDRip/BluRay) — پس نسخه‌های جدید بدون برچسب استاندارد هم درست کار
می‌کنند.

ParsiFlix معادل ندارد (فهرست کیفیت نیست)؛ به‌جایش `directPlay.buttonTextPatterns`
دکمه‌ای مثل «تماشا»/«ادامه تماشا» را پیدا و کلیک می‌کند — با
`excludeButtonTextPatterns` (`زنده`, `شبکه`, `تلویزیون`, `رادیو`, `live`)
که جلوی کلیک اشتباه روی پخش زنده تلویزیونی را می‌گیرد (باگ واقعی در 0.8.2،
رفع در 0.9.1).

**مسیرهایی که Direct Play فعال است:** کارت کاتالوگ، نتایج جستجو (فقط
فیلم)، Continue Watching (هر دو نوع: پخش محلی و Sync حساب)، Recently
Opened، Favorites. تشخیص فیلم/سریال برای دو مورد آخر از
`catalogKindFromUrl()` می‌آید (چون `MovieItem` فیلد Kind ندارد؛ الگوی آدرس
استفاده می‌شود: `/medias/movies|series/` یا `/post/film|series/`).
**سریال‌ها همیشه روی جزئیات می‌مانند** (عمدی، تا انتخاب خودکار قسمت بیاید؛
[بخش ۸](#۸-عمداً-پیاده‌سازی-نشده) را نگاه کن) و **Back بعد از پخش عمداً به
صفحه فیلم برمی‌گردد**، نه Home — کاربر ممکن است بخواهد کیفیت/قسمت را دستی
عوض کند.

### ۴.۴ منابع کاتالوگ واقعی (بازرسی‌شده)

**ParsiFlix:**
- «جدیدترین‌ها» ترکیبی: بخش `جدیدترین‌ها`ی `GET https://api.parsiflix.com/app/home`
  (Header: `Authorization: Bearer <accessToken از localStorage>`)
- فیلم/سریال جدا: `GET /medias?type=MOVIE|SERIES&page=1&size=20` — همان
  Endpointی که خود سایت هنگام باز کردن `/medias/movies` صدا می‌زند (با
  Hook کردن fetch/XHR کشف شد)
- **جستجو فقط فارسی است:** `/medias?title=<query>` — تست شد که `matrix`
  صفر نتیجه می‌دهد ولی «خانه» جواب می‌دهد. این محدودیت خود سایت است.

**FilmRooz:**
- فیلم جدید: `/archive/category/new-films/`
- **سریال — تصمیم مهم:** منبع درست `/archive/series/` («سریال‌های
  بروز شده») است، **نه** `/archive/category/new-tv-show/` («سریال‌های
  جدید»)! تفاوت واقعی: `new-tv-show` فقط عناوین خودشان تازه را می‌دهد
  (Gone، Overdo — هر دو ۲۰۲۶)، ولی `/archive/series/` سریال‌هایی با
  **قسمت تازه** را می‌دهد فارغ از سال ساخت (Love Island از ۲۰۱۵،
  حتی WWE Raw از ۱۹۹۳). این همان چیزی است که واقعاً «تازه» محسوب می‌شود.
  هر آیتم این صفحه برچسب `قسمت XX فصل XX` هم دارد.
- فیلم برگزیده (منبع دوم پیشنهادی، هنوز اضافه نشده):
  `/archive/category/featured-films/` — هم‌پوشانی جزئی با new-films دارد
  ولی فیلتر جداگانه.
- سریال محبوب/ترند:
  `/archive/playlist/show/most-popular-tv-shows/` — در 0.14.0 به ردیف مستقل
  `سریال‌های برگزیده جهان` اضافه شد؛ با Fresh قاطی نمی‌شود.
- جستجو: `GET /?s=<query>` — هم انگلیسی هم فارسی جواب می‌دهد.
- استخراج عنوان از کارت: باید لینک‌داخل‌کارتی انتخاب شود که **دقیقاً به
  همان مسیر** اشاره کند و با `/^قسمت/` شروع نشود (وگرنه متن وضعیت قسمت
  به‌جای عنوان گرفته می‌شود — باگ واقعی که رفع شد).
- پوستر Lazy-load: تصویر واقعی در `data-src`، نه `src`/`currentSrc` (که
  یک SVG خاکستری Placeholder است).

### ۴.۵ بروزرسانی خودکار (`update/UpdateRepository.kt`)

**بدون فایل update.json جدا** — مستقیم از
`https://api.github.com/repos/AminAsadollah25/aminema-tv/releases/latest`
خوانده می‌شود (تگ، توضیحات، لینک Assetها همه در همین یک پاسخ هست).

- تشخیص Asset: اول دقیقاً `*-debug.apk` انتخاب می‌شود؛ اگر نبود فقط زمانی
  یک APK پذیرفته می‌شود که Release دقیقاً یک APK داشته باشد. SHA باید هم‌نام
  همان APK با پسوند `.sha256` باشد.
- تشخیص نسخه: اول خط `versionCode: N` در متن Release منبع حقیقت است. اگر آن
  خط نبود، Tag و `BuildConfig.VERSION_NAME` به‌صورت Semantic با هم مقایسه
  می‌شوند؛ عدد Semantic هرگز با Android versionCode مقایسه نمی‌شود (ریشهٔ
  بنر اشتباه 0.18.2 همین اختلاط دو مقیاس بود).
- دانلود → بررسی `sha256` (فایل `.sha256` هم Asset جداست، فرمت خروجی
  `shasum -a 256`) → `FileProvider` → `Intent(ACTION_VIEW, type=application/vnd.android.package-archive)`
- **هرگز کاملاً بی‌صدا نیست** — طبق سیاست اندروید، تأیید نهایی کاربر همیشه
  لازم است مگر برنامه Device Owner باشد (Aminema عمداً نیست)
- نصب اول: چون Debug-Signed می‌ماند (نه Release/R8)، اندروید صفحه
  «Do you want to **update** this app؟» را نشان می‌دهد (نه Install
  جدید) — تأیید شد که شناسه/امضا با نصب فعلی یکی است
- Skip نسخه: `SettingsRepository.skippedUpdateVersionCode` (DataStore)

### ۴.۶ QuickLinks (`Models.kt::QuickLink`, 0.11.0)

برای صفحه‌های فرعی معمولی که ردیف اختصاصی ندارند. در `services.json` هر
سرویس یک `quickLinks: []` اختیاری با `{id, label, path, prominent}` دارد.
در 0.12.0 فقط **یوتیوب فارسی** (`/youtube-persian`) از این مسیر استفاده
می‌کند و زیر «بیشتر» می‌ماند. Live دیگر QuickLink نیست.

### ۴.۷ پخش زنده بومی (`LiveTvConfig`, `LiveTvSectionRow`, 0.12.0)

منبع واقعی ParsiFlix با Probe موقت Debug-only و حساب لاگین‌شده بررسی شد.
صفحه `/medias/live/` بیست کارت React دارد که `href` و `onclick` عادی
ندارند؛ بنابراین هر کارت واقعاً کلیک شد و فقط مسیر صفحه نهایی، نام و
`img` لوگو ثبت شد. نقشه قطعی:

| کانال | id |
|---|---:|
| شبکه ۱، ۲، ۳، ۴ | 51, 52, 53, 54 |
| تهران، خبر، نسیم، آی‌فیلم، ورزش | 55, 56, 57, 58, 59 |
| ایران اینترنشنال، BBC Persian | 22, 37 |
| AVA، Persiana، Avang، Tapesh، TMTV | 29, 26, 30, 33, 34 |
| ITN، فراتر، Radio Javan، VOA | 69, 72, 67, 62 |

مدل JSON:

```json
"liveTv": {
  "channels": [
    {
      "id": "51",
      "name": "شبکه ۱",
      "path": "/medias/live/51",
      "logoUrl": "https://…/logo.png"
    }
  ]
}
```

- `ServicesRepository` نسخه Bundled این فیلد را به نصب قبلی مهاجرت می‌دهد؛
  چون داده Adapter است، `defaults.liveTv` بر stored value اولویت دارد.
- Home همه `service.liveTv.channels`ها را flatten می‌کند؛ پس منبع آینده
  فقط با افزودن سرویس/کانال در JSON به همان ردیف می‌پیوندد.
- کلیک کارت `BrowserActivity.intent(..., liveTheaterMode=true)` را با
  `service.url + channel.path` باز می‌کند؛ مسیر عادی صفحه است، نه URL مدیا.
- `requestFullscreen()` برای اجرای خودکار قابل اتکا نیست چون User Gesture
  می‌خواهد. راه آزموده‌شده: وقتی `<video>` ظاهر شد، خود آن با CSS
  `position:fixed; inset:0; width:100vw; height:100vh; object-fit:contain`
  روی Activity اندرویدی که از قبل Fullscreen است قرار می‌گیرد.
- اسکریپت چند بار تا ۵٫۵ ثانیه تلاش می‌کند چون SPA ممکن است ویدیو را دیر
  Mount کند؛ سپس فقط `video.play()` را صدا می‌زند. `src/currentSrc`، درخواست،
  توکن، Cookie یا DRM هرگز خوانده نمی‌شود.
- در Live mode، Back/Mouse Back/QuickMenu Back مستقیم Activity را می‌بندد؛
  بنابراین گرید واسط سایت هرگز دیده نمی‌شود و Compose همان Scroll/Focus قبلی
  را نگه می‌دارد.
- روی امولاتور: video rect دقیقاً برابر viewport (`960×540` CSS /
  `1920×1080` فیزیکی)، `readyState=4` و `paused=false`. شبکه ۱ با موس و شبکه
  ۲ با D-pad/OK پخش شدند؛ Back همان کارت فوکوس‌شده را برگرداند.

---

## ۵. اسرار/محدودیت‌های امنیتی رعایت‌شده

- Cookie فقط برای Poster **هم‌دامنه** با صفحه محتوا ضمیمه می‌شود
  (`authenticatedPosterModel` در `CatalogCard.kt`/`TvComponents.kt`) —
  جلوگیری از نشت Cookie به CDN شخص ثالث
- تمام پارس HTML از صفحات سایت (کاتالوگ، جستجو) فقط لینک‌های **هم‌میزبان**
  و مطابق الگوی `contentUrlPatterns` سرویس را می‌پذیرد؛ خروجی JS به‌عنوان
  Untrusted در نظر گرفته می‌شود
- هیچ‌جا لینک `/play/<hash>/…mp4` یا مشابه (که در همان صفحات FilmRooz
  دیده شده) خوانده یا ذخیره نمی‌شود — فقط آدرس صفحه عادی (`/stream/...`)

---

## ۶. روش‌شناسی تست (مهم برای هر مدلی که ادامه می‌دهد)

**هرگز فرض نکن، همیشه با Probe زنده تأیید کن.** یک بار (0.8.2) فرض شد
«اکثر فیلم‌های جدید پخش آنلاین ندارند» چون Probe اول دنبال `<a href>` بود
نه `div[onclick]` — این اشتباه در گزارش رسمی ثبت و بعداً تصحیح شد. درس:
Probeهای Debug-only (`app/src/debug/java/.../probe/*Activity.kt`) قبل از
هر پیاده‌سازی جدید ساخته و بعدش **کامل حذف** می‌شوند (هرگز Commit
نمی‌شوند؛ چک: `unzip -l app-debug.apk | grep Probe` باید خالی باشد).

روند استاندارد هر Feature: (۱) Probe زنده روی امولاتور با حساب واقعی
لاگین‌شده کاربر، (۲) پیاده‌سازی، (۳) Build، (۴) نصب و تست دستی/خودکار روی
امولاتور با اسکرین‌شات، (۵) حذف Probe، (۶) Commit+Tag+Release.

Emulator: AVD نام `Television_1080p`، Android TV API 36. کاربر شخصاً
داخل آن به هر دو سرویس (و هر سرویس جدید) وارد می‌شود؛ مدل هرگز Credential
نمی‌خواهد یا وارد نمی‌کند.

---

## ۷. باگ‌های واقعی که پیدا و رفع شدند (درس‌آموز)

### ۷.۱ مهاجرت فیلد جدید در services.json (حیاتی)

`ServicesRepository.load()` نسخه Bundled (`assets/services.json`) را با
نسخه ذخیره‌شده در `filesDir/services.json` ادغام می‌کند. **قانون:**
فیلدهای مربوط به «قابلیت Adapter» (مثل `directPlay`, `resumeStrategy`)
باید **نسخه Bundled همیشه برنده** باشد (`defaults.x ?: current.x`)، نه
برعکس — وگرنه اصلاح یک باگ در این فیلدها هرگز به نصب‌های قبلی نمی‌رسد.
این دقیقاً همان چیزی بود که یک بار اشتباه پیاده شد (`current ?: defaults`)
و باعث شد فیکس واقعی هیچ‌وقت روی نصب موجود اثر نکند. فیلدهای «محتوای
کاربر» (اسم/URL سرویس‌های اضافه‌شده دستی) برعکس — کاربر برنده است.

### ۷.۲ IntroPreferences روی SharedPreferences نه DataStore

چون تصمیم Cold-start باید **همگام** و قبل از اولین فریم گرفته شود.
DataStore Async است و باعث Flash صفحه اصلی قبل از اینترو می‌شد.

### ۷.۳ استخراج عنوان کارت که متن وضعیت قسمت را می‌گرفت

رشته‌هایی مثل «قسمت ۱۶ فصل اول در حال پخش» گاهی به‌جای عنوان واقعی گرفته
می‌شدند. رفع: لینک عنوان باید (الف) به همان مسیر آیتم اشاره کند و
(ب) با `قسمت` شروع نشود.

### ۷.۴ Direct Play گاه‌به‌گاه کار نمی‌کرد — race شرطی SPA (0.11.0)

`BrowserActivity`‌ دو مسیر ورود به هر صفحه دارد: `onPageFinished` (لود کامل)
و `doUpdateVisitedHistory` (چون «ParsiFlix یک SPA است؛ تغییر مسیر اغلب
`onPageFinished` صدا نمی‌زند» — کامنت خود کد). مسیر دوم فقط
`scheduleSiteContinue()` را صدا می‌زد، نه `scheduleDirectPlay()` را. یعنی
هر بار که ParsiFlix با `pushState`/`replaceState` به صفحه فیلم می‌رسید (نه
لود کامل)، پخش مستقیم بی‌سروصدا رد می‌شد — دقیقاً همان الگوی «گاهی کار
می‌کند، گاهی نه» که کاربر گزارش کرد، چون به مسیر SPA-یا-Full-load بودن
درخواست/سشن بستگی دارد، نه به کد. رفع: `scheduleDirectPlay(view)` هم به
`doUpdateVisitedHistory` اضافه شد؛ پنجرهٔ تلاش مجدد `tryOpenBestStream` هم
از ۳ به ۴ مرحله (تا ۴.۵ ثانیه) رسید برای لود کندتر.

**درس:** هر جا یک هندلر برای «SPA این مسیر را نمی‌گیرد» اضافه می‌شود، باید
همهٔ توابعی که مسیر معمولی صدا می‌زند را هم صدا بزند، نه زیرمجموعه‌ای از
آن‌ها — وگرنه رفتار به مسیر ورود (SPA یا Full-load) وابسته و غیرقابل‌پیش‌بینی
می‌شود.

### ۷.۵ Sync حساب‌ها سقف مصنوعی ۱۲ داشت، ادغام تا ۳۰ می‌پذیرفت (0.11.0)

`AccountSyncActivity` با JS تزریقی، فهرست هر سایت را می‌خواند و خودش
`.slice(0, 12)` می‌زد، در حالی که `LibraryRepository.syncAccountSessions`
از قبل تا ۳۰ آیتم ورودی را می‌پذیرفت (`incoming.take(30)`). یعنی داده‌ای که
لایهٔ ادغام آماده پذیرفتنش بود، هرگز به آن نمی‌رسید. رفع: هر دو `slice` به
۳۰ رسید. برای فیلم خارجی، پیدا کردن باکس «مشاهدات اخیر» هم از یک تلاش با
تأخیر ثابت (۱٫۲ ثانیه) به یک Polling تا ۴ بار با فاصلهٔ ۹۰۰ میلی‌ثانیه تغییر
کرد، چون آن بخش گاهی از یک فراخوانی AJAX کندتر لود می‌شود.

---

## ۸. عمداً پیاده‌سازی‌نشده (با دلیل، برای اینکه دوباره اشتباه گرفته نشود)

- **R8/Minify در 0.14.5 انجام شد.** کانال Update فعلی Debug-signed باقی
  مانده ولی Code/Resource shrink فعال است. تمام کلاس‌ها و متدهای
  `@JavascriptInterface` با Rule صریح حفظ شده‌اند؛ Catalog/Keyboard/
  Playback bridge روی نشست لاگین‌شده تست شدند. APK از حدود 76MB به 22.2MB
  رسید.
- **انتخاب خودکار قسمت سریال:** تیک‌های قسمت FilmRooz روی لپ‌تاپ و امولاتور
  با یک حساب یکسان نبودند و browser-local ثابت شدند. 0.14 فقط آخرین قسمت
  منتشرشده را نشان می‌دهد. Next/Unwatched نیازمند `SeriesProgress` محلی یا
  تأیید «تا این قسمت دیدم» است؛ هیچ قسمت حدس زده نمی‌شود.
- **Home عمودی هنوز `Column`+`verticalScroll` است، نه `LazyColumn`.**
  همه Railهای افقی در 0.14.5 به `LazyRow` منتقل شده‌اند؛ بنابراین پوسترهای
  خارج Viewport دیگر هم‌زمان Decode/Compose نمی‌شوند. LazyColumn فقط پیش
  از رشد محسوس تعداد بخش‌های عمودی لازم است.
- **Cinematic Hover Preview در 0.14.5 انجام شد:** خلاصه بدون اسپویل،
  سال/ژانر/امتیاز/مدت/آخرین قسمت با Dwell برابر 520ms، داده کش‌شده و بدون
  Request شبکه روی هر حرکت فوکوس.

---

## ۹. عملیات گیت‌هاب

- مخزن: `https://github.com/AminAsadollah25/aminema-tv` (Public، مشکلی
  با عمومی‌بودن نیست چون پروژه کاملاً شخصی است)
- Auth: `gh auth login` با Login-with-browser (چون ورود گیت‌هاب با گوگل
  SSO است، یوزرنیم/پسورد جدا لازم نیست)
- `gh` نصب دستی در `~/bin/gh` شده (بدون Homebrew)، در `~/.zshrc` به PATH
  اضافه شده
- **قرارداد هر Release:** باید Notes تمیز داشته باشد (فارسی، خوانا) +
  پاورقی `---\nversionCode: N` برای Parser بروزرسانی خودکار + دو Asset:
  `Aminema-vX.Y.Z-debug.apk` و همان با پسوند `.sha256`

---

## ۱۰. راهنمای بروزرسانی این سند

بعد از هر تغییر مهم، یکی از این کارها را بکن:
- Endpoint/الگوریتم جدید کشف شد → به بخش ۴ اضافه کن با URL/Regex دقیق
- باگ واقعی رفع شد → به بخش ۷ اضافه کن (چی بود، چرا، چطور رفع شد)
- چیزی عمداً کنار گذاشته شد → به بخش ۸ اضافه کن با دلیل
- نسخه جدید Ship شد → یک خط به جدول بخش ۳

هدف: کسی که این سند را می‌خواند نباید مجبور شود دوباره حدس بزند یا از نو
Probe کند چیزی که یک بار قطعی کشف شده.

---

## ۱۰. Episode Navigator نهایی 0.16.3 — 2026-08-08

- ParsiFlix: Container دقیق فصل `_seasonItem_` و Row دقیق قسمت
  `_episodeItem_` است. شماره از `_episodeNumber_` خوانده و Button همان Row
  فقط یک‌بار Click می‌شود.
- FilmRooz: `.eDbox` دانلود و `.eSbox` پخش آنلاین است. مدل Episode فقط Action
  معنایی `#filmrooz-sN-boxN-epnum-N` نگه می‌دارد؛ href دانلود ممنوع است.
- Native callback فصل را صریح همراه Episode می‌فرستد؛ پیدا کردن فصل از روی
  Episode ID تکراری ممنوع است.
- هنگام `smEpisode`، Generic Direct Play و Site Continue نباید اجرا شوند.
- State Machine فقط یک‌بار و فقط روی Detail/Content URL Dispatch می‌شود.
- پوستر Portrait کاتالوگ بر پوستر قدیمی Recent/Continue اولویت دارد؛ Repair
  غیرمخرب است و Login/Cookie/Library پاک نمی‌شود.
- تست مالک: لیسانسه‌ها فصل ۱ و ۲، بامداد خمار فصل ۲ قسمت ۴، Life/Larry و
  پوسترهای مربوط موفق بودند.
- جزئیات کامل: `DEVELOPMENT_LOG_0.16.3.md` و `TEST_REPORT_0.16.3.md`.

## ۱۱. Hotfix بروزرسانی درون‌برنامه‌ای 0.16.4 — 2026-08-09

- `UpdateRepository` مالک یک `StateFlow<UpdateState>` سطح Application است؛
  `HomeViewModel` و `SettingsViewModel` هر دو همین State واحد را مصرف می‌کنند.
- `SettingsViewModel` چک دستی را انجام می‌دهد و فقط Release با `versionCode`
  بزرگ‌تر از Build نصب‌شده را به `UpdateState.Available` تبدیل می‌کند.
- ساختن State جدا در Home یا Settings ممنوع است؛ نسخه، Download progress، Failure
  و Retry باید در هر دو صفحه یکسان باقی بمانند.
- چک دستی عمداً نسخهٔ قبلاً Skipشده را برای همان مراجعه دوباره نشان می‌دهد،
  اما مقدار Skip در DataStore حذف یا بازنویسی نمی‌شود.
- بعد از نتیجه موفق، Settings همان `UpdateBanner` مشترک Home را درجا نشان
  می‌دهد؛ دانلود و Progress همان‌جا دیده می‌شود و Installer معمول Android باز
  می‌شود. با خروج از Settings همان State در Home نیز حفظ می‌شود.
- چک شبکه‌ای دوم و Toast گمراه‌کننده «از صفحه اصلی نصب کنید» حذف شده است.
- جزئیات: `DEVELOPMENT_LOG_0.16.4.md` و `TEST_REPORT_0.16.4.md`.

## ۱۲. Hotfix بارگذاری قسمت‌ها 0.16.5 — 2026-08-09

- ریشه باگ با Silo روی حساب لاگین‌شده بازسازی شد: `onPageFinished` حدود ۶.۲
  ثانیه بعد از شروع می‌رسید و استخراج FilmRooz حدود ۲.۳ ثانیه بعد زمان‌بندی
  می‌شد، اما Timeout سراسری هشت‌ثانیه‌ای WebView را پیش از اجرای Script می‌بست.
- Timeout اکنون ۲۵ ثانیه و همچنان محدود است. اولین استخراج نسبت به پایان واقعی
  صفحه زمان‌بندی می‌شود؛ FilmRooz بعد از ۱.۲ ثانیه و ParsiFlix بعد از ۲ ثانیه.
- نتیجه `null` در SPA شکست قطعی نیست. تا وقتی WebView زنده و Deadline باقی است،
  استخراج هر ۱.۵ ثانیه Retry می‌شود؛ اولین payload دارای Season/Episode جریان
  را با موفقیت تمام می‌کند.
- فقط یک Extraction در هر لحظه Queue می‌شود و `destroy/finish` تمام Callbackها
  و WebView مخفی را آزاد می‌کند؛ Loop یا WebView دائمی ساخته نمی‌شود.
- اسکن و Log غیرضروری همه تصاویر DOM حذف شد. هیچ media URL، Cookie، Token یا
  DRM خوانده یا Log نمی‌شود.
- حالت شکست Native دکمه `تلاش دوباره` دارد و همان `startEpisodeLoad` امن را
  بدون خروج از Spotlight اجرا می‌کند.
- Evidence پس از Fix برای Silo: Extraction در Attempt اول اجرا شد، یک Edition
  معتبر برگشت و هر سه فصل همراه Episode cardها در 1080p دیده شدند.
- جزئیات: `DEVELOPMENT_LOG_0.16.5.md` و `TEST_REPORT_0.16.5.md`.

## ۱۳. تازه‌سازی Intro سینمایی 0.16.5.1 — 2026-08-09

- Asset محلی `res/raw/aminema_intro.mp4` با ویدئوی جدید Mascot جایگزین شد.
- Source کاربر ۸.۲ ثانیه، 16:9 و دارای H.264/AAC Stereo بود؛ با
  `avconvert PresetAppleM4V1080pHD` به فایل Fast Start نرمال شد تا Decoderهای
  Android 9+ برای شروع مجبور به خواندن انتهای فایل نباشند.
- SHA-256 Asset نهایی داخل Source:
  `54c2ed65e42a2d546a059f1b8cf94344c75352b50dea775ed0d70bff39e4369e`.
- در QA واقعی، `AndroidView/VideoView` رویداد Mouse را پیش از Parent Compose
  مصرف می‌کرد؛ Listener لمس/Click مستقیم روی `VideoView` اضافه شد تا
  `ACTION_UP` به `finishOnce()` برسد. پخش فقط Cold Start، Skip با
  Remote/Back، Mute اختیاری، Prepare timeout چهارثانیه و Hard cap بیست‌ثانیه
  حفظ شده‌اند.
- Asset محلی و آفلاین است؛ هیچ Network، Cookie، Login یا Provider state در
  Intro دخالت ندارد.
- Clean Build، Unit، Lint، نصب درجا، پایان طبیعی و Skip با Remote/Mouse موفق
  و Fatal Exception صفر است.
- جزئیات: `DEVELOPMENT_LOG_0.16.5.1.md` و `TEST_REPORT_0.16.5.1.md`.

## ۱۴. پایه کتابخانه Canonical و Dedupe — Candidate 0.16.6

- `CanonicalMedia` یک عنوان مستقل از Provider است و `SourceVariant` فقط صفحه
  عادی همان عنوان در یک Provider را نگه می‌دارد. هیچ Media URL، Token، Cookie،
  Manifest یا DRM وارد مدل نمی‌شود.
- Match محافظه‌کارانه است: Same URL، IMDb+Kind، Title+Year+Kind، یا
  Title+Kind+Credit overlap. عنوان تنها Merge نمی‌شود؛ IMDb/سال متعارض و صفحات
  متفاوت یک Provider جدا می‌مانند.
- Grouping در برابر Bridge متناقض IMDb محافظت شده است تا یک رکورد بدون ID دو
  عنوان دارای ID متفاوت را Transitively یکی نکند.
- `CanonicalText` حروف ی/ک عربی، نیم‌فاصله، خط تیره، فاصله و ارقام فارسی/عربی
  را نرمال می‌کند. سال فقط وقتی از عنوان حذف می‌شود که داخل پرانتز/براکت باشد.
- Search یک Rail Canonical دارد؛ Failure یک Provider نتیجه Provider دیگر را
  حفظ می‌کند. کارت چندمنبعی تعداد Source را نشان می‌دهد.
- `SpotlightItem` با Defaultهای سازگار `canonicalId/sourceVariants` را حمل
  می‌کند. Source switching فقط Loaderهای موقت را عوض می‌کند و Persistent state
  را تغییر نمی‌دهد.
- `CatalogKind.defaultSpotlightAction()` قرارداد مشترک Action بدون Resume است:
  Series همیشه `SELECT_EPISODE` و Movie همیشه `WATCH`. هر دو مسیر ساخت Search
  و Source switch باید از همین قرارداد استفاده کنند؛ در غیر این صورت Label
  انتخاب قسمت می‌تواند با Action بازکردن Browser ناسازگار شود.
- `sourceLabel` برای نام نمایشی Provider از نام دسته جداست و روی تنظیمات ذخیره
  قدیمی فقط به‌صورت Fill-empty Merge می‌شود.
- نمونه واقعی Regression، «لیسانسه‌ها» در ParsiFlix و FilmRooz است. تست‌های
  Matcher و Serialization اضافه شده‌اند.
- Migration کتابخانه/Continue/Favorite/Recent و Provider سوم آگاهانه Pending
  هستند؛ مرحله بعد باید Bridge برگشت‌پذیر و Coverage Lab باشد.

---

## ۹. قابلیت اطمینان Playback و Continue بین‌دستگاهی (0.12.1)

### ۹.۱ ریشه باگ Continue گاه‌به‌گاه

در نسخه‌های قبلی، `tryClickSiteContinue()` و `tryClickWatchButton()` به محض
اینکه نتیجه JavaScript برابر `clicked` می‌شد فلگ success را Set می‌کردند.
در SPA، `element.click()` فقط یعنی Event dispatch شد؛ اگر React هنوز Hydrate
نشده باشد Handler می‌تواند هیچ Navigationی انجام ندهد. چون فلگ Set شده بود،
تمام retryهای بعدی متوقف می‌شدند و کاربر روی Detail می‌ماند.

قانون جدید: **Click فقط Attempt است.** موفقیت فقط از یکی از این شواهد می‌آید:

1. URL جدید با `ServiceAdapter.isPlaybackUrl()` منطبق باشد؛
2. Bridge یک رویداد واقعی HTML5 Video (`play/timeupdate/pause`) بگیرد؛
3. `WebChromeClient.onShowCustomView()` یک Fullscreen player واقعی نشان دهد.

Retryهای Direct در `250, 750, 1400, 2300, 3500, 5000, 7000, 9500,
12000ms` و Continue در `250…10800ms` اجرا می‌شوند. `probeInFlight` جلوی
evaluate هم‌زمان و Cooldown 850ms جلوی Double click را می‌گیرد. کاندید باید
Visible، Enabled و Pointer-enabled باشد؛ Exact text و تگ واقعی button/a
امتیاز بالاتر دارند و episode/season container امتیاز منفی می‌گیرد.

### ۹.۲ Detail URL مساوی Player URL

بعضی Inline playerها `location.href` را همان Detail URL گزارش می‌کنند.
`recordPlayback` آن را به‌عنوان `playbackUrl` ذخیره کرده بود و اجرای بعدی
فکر می‌کرد مقصد پایدار Player دارد. Home اکنون فقط URL متفاوت از
`contentUrl` را Dedicated player می‌داند. برای Movie دارای DirectPlay config:

- FilmRooz local `OPEN_PLAYBACK_PAGE` بدون URL جدا → Resolver عادی
- FilmRooz account movie با Play-online labels → Resolver عادی
- ParsiFlix `CLICK_SITE_CONTINUE` → دکمه دقیق Continue خود سایت، نه Resolver
  generic که ممکن است «تماشا» را بزند

### ۹.۳ Loading سینمایی و Deadline

`PlaybackLoadingView` یک Native View سبک بالای WebView است. Detail باید در
پس‌زمینه Load شود تا سایت کنترل عادی Player را بسازد؛ این زمان شبکه قابل حذف
نیست، اما Flash صفحه با لایه سینمایی پوشانده می‌شود. Deadline برابر 14s است.
پس از آن Detail آشکار و Toast fallback نمایش داده می‌شود. Back حین Loading
Activity را می‌بندد. Login redirect، SSL/Main-frame error و Fullscreen Player
callbackهای pending را Cancel/Confirm می‌کنند تا Click یا Toast دیرهنگام
نرسد.

Asset `aminema_loading_popcorn.png` با ImageGen از هویت مسکات موجود ساخته،
به 640×640 کوچک و به‌صورت drawable محلی 556KB ذخیره شد؛ Runtime network یا
Decode تصویر بزرگ ندارد.

### ۹.۴ Account-authoritative Continue

مشکل تفاوت TV و Emulator دو علت داشت:

1. `AccountSyncActivity` فقط با دکمه دستی اجرا می‌شد.
2. `syncAccountSessions()` incoming را Merge می‌کرد ولی Local-onlyهای قدیمی
   همان Provider را حذف نمی‌کرد.

در 0.12.1، `MainActivity` بعد از Intro از Gate یک‌بارمصرف سطح Process استفاده
می‌کرد. **این مسیر در نسخه 0.14.0 supersede شد:** اکنون
`CatalogBackgroundSync` حساب و کاتالوگ هر Provider را در یک Pass پشت Home
می‌خواند؛ Auto `AccountSyncActivity` و SharedPreferences gate حذف شده‌اند.
`AccountSyncActivity` فقط برای دکمه دستی/تشخیصی باقی است.

امضای Repository اکنون:

```kotlin
syncAccountSessions(serviceId: String, incoming: List<PlaybackSession>)
```

نتیجه موفق هر Provider برای **عضویت** authoritative است: همه sessionهای همان
Provider ابتدا کنار گذاشته و incoming معتبر جایگزین می‌شود. اگر Content ID
در Local و Account مشترک باشد، Local player/position/duration حفظ و metadata
حساب روی آن Merge می‌شود. Providerهای دیگر تغییر نمی‌کنند. اگر Sync قبل از
دریافت نتیجه Fail شود، تابع فراخوانی نمی‌شود و Cache قبلی می‌ماند.

محدودیت مهم: ParsiFlix واقعاً Continue account دارد. FilmRooz پنل
«مشاهدات اخیر» فقط Detail link می‌دهد. Movie با Resolver به Player می‌رسد،
اما Cross-device Series ممکن است Exact episode/quality/time نداشته باشد؛ در
این حالت Aminema بعد از Deadline Detail را برای انتخاب دستی نشان می‌دهد و
هیچ episode یا endpointی را حدس نمی‌زند.

### ۹.۵ فلش مشترک Rail

`RailNavigationControls` در `TvComponents.kt` تنها منبع رفتار فلش‌هاست.
Search هنوز Overload مبتنی بر `ScrollState` را دارد؛ Home از 0.14.5 Overload
مبتنی بر `LazyListState` را استفاده می‌کند. نسخه Lazy به تعداد کارت‌های قابل
مشاهده Page می‌کند و در ابتدا/انتها از `canScrollBackward/Forward` برای
Dim/Disable استفاده می‌کند. `SectionRow`, `CatalogSectionRow` و
`LiveTvSectionRow` همگی keyed `LazyRow` هستند.

### ۹.۶ Metadata Race و Quick Glance در 0.14.5

در ParsiFlix SPA، Router می‌توانست `location.href` را زودتر از Hydrateشدن
Detail DOM تغییر دهد. نتیجه: URL فیلم واقعی بود اما Title از Shell صفحه اصلی.
`captureResumeMetadata()` اکنون `pageUrl: location.href` را هم برمی‌گرداند و
فقط وقتی نتیجه را می‌پذیرد که Requested URL، DOM URL و `webView.url` با
`ContentMetadataPolicy.canonicalContentUrl()` یکسان باشند. Title عمومی
Provider نیز روی Content route رد می‌شود.

برای داده قدیمی، `LibraryRepository.repairMetadata()` URL را با Catalog cache
تطبیق می‌دهد و Title/Poster خراب را ترمیم می‌کند. Home هم دفاع دوم دارد:
Shell entry بدون Match را نشان نمی‌دهد. این Policy چهار Unit test دارد.

`CatalogItem` پنج Field اختیاری سازگار با Cacheهای قدیمی گرفت:
`summary/year/genres/rating/runtime`. خروجی JS محدود و پاک‌سازی می‌شود.
ParsiFlix از JSON کاتالوگ خودش و FilmRooz از ساختار واقعی `.postMeta` شامل
`<spl>` ژانر، Runtime، Rating و Synopsis استفاده می‌کند.
`CinematicHoverPreview` پس از 520ms Hover/Focus ظاهر می‌شود، Layout را جابه‌جا
نمی‌کند، Episode plot یا Video autoplay ندارد و روی Focus move هیچ Request
شبکه‌ای نمی‌زند.

## ۱۰. Candidate محلی 0.17.0 — MyMoviz و کتابخانه چندمنبعی

این Candidate هنوز Commit/Tag/Release نشده است. سرویس `mymoviz` بدون حذف یا
بازنویسی تنظیمات نصب‌های قبلی به `services.json` و `ServicesRepository` اضافه
شده، اما Shortcut بزرگ مستقلی روی Home ندارد. Catalog، Search و Detail عمومی
بدون Login کار می‌کنند؛ Login فقط در مرحله بعد، برای Probe واقعی Watch/Playback
لازم خواهد شد.

### ۱۰.۱ داده و ادغام

- `CatalogBackgroundSync` فهرست عمومی فیلم و سریال MyMoviz را مستقل از دو
  Provider دیگر به‌روزرسانی می‌کند؛ خطای یک سرویس Refresh سرویس‌های دیگر را
  متوقف نمی‌کند.
- `CanonicalLibrary.mergeLatest()` خروجی FilmRooz و MyMoviz را بدون تکرار و با
  توزیع متعادل در ریل‌های «فیلم خارجی» و «سریال خارجی» ادغام می‌کند.
- تطبیق ترجیحاً با IMDb ID و سپس Canonical title/year انجام می‌شود.
- برای فیلم: نسخه دوبله، سپس بهترین کیفیت حداکثر 1080p، سپس FilmRooz در شرایط
  برابر اولویت دارد. برای سریال، تگ دوبله سطح عنوان به‌عنوان شاهد دوبله بودن
  آخرین قسمت استفاده نمی‌شود؛ Resolver قسمت‌به‌قسمت به 0.17.2 موکول شده است.
- Search سه‌منبعی است و ورودی‌هایی مثل `spiderman` را برای MyMoviz به فرم
  `spider man` نیز امتحان می‌کند.

### ۱۰.۲ مرز ایمن Playback

برای MyMoviz هنوز Native Episode Navigator یا Direct Play فعال نشده است.
کلیک سریال فقط Detail عمومی Provider را باز می‌کند. هیچ Selector یا Stream URL
حدس زده نمی‌شود. مرحله 0.17.1 باید فقط بعد از Login مالک، ساختار Watch و رفتار
Cookie/Session را در همان پروفایل موجود مشاهده کند؛ App data، Cookie و Login
نباید پاک شوند.

### ۱۰.۳ اصلاحات UI و پذیرش

- `CatalogCard` روی خود پوستر تگ سبز «دوبله فارسی» و تگ آبی «زیرنویس فارسی»
  را نشان می‌دهد. هر دو می‌توانند هم‌زمان ظاهر شوند و در نبود داده معتبر چیزی
  حدس زده نمی‌شود. برای سریال، این فعلاً Availability سطح عنوان است، نه آخرین
  قسمت دوبله‌شده.
- Crash بالقوه `FocusRequester is not initialized` و جهش Hero پس از رسیدن
  Metadata async اصلاح شده‌اند.
- پذیرش Emulator: ParsiFlix=10، FilmRooz=23 و MyMoviz=24 عنوان بدون خطای Sync؛
  جستجوی `spiderman` و بازشدن Detail عمومی MyMoviz موفق؛ Badgeها بدون هم‌پوشانی
  روی پوستر تأیید شدند.
- Gate نهایی `testDebugUnitTest + lintDebug + assembleDebug` موفق است: 30 تست،
  صفر Failure و Android Lint صفر Issue. نصب با `adb install -r` انجام شد و
  Login/Cookieهای موجود حفظ شدند.

### ۱۰.۴ ثبات پنل Canonical هنگام تغییر منبع

Spider-Man 3 نشان داد MyMoviz امتیاز کاربران 9.2 و IMDb صریح 6.2 دارد؛ Parser
قبلی schema aggregateRating را اشتباهاً IMDb می‌نامید. اکنون لینک صریح IMDb
اولویت دارد. `withPlaybackSource()` نیز انتخاب Provider را فقط به Routing پخش
محدود می‌کند و Metadata/Artwork/Badgeهای پنل را ثابت نگه می‌دارد. Regex دوبله
FilmRooz عبارت‌های `دوبله`، `دو زبانه/دوزبانه` و `صوت فارسی` را پوشش می‌دهد.
Gate پس از این اصلاح 30 تست، صفر Failure و Lint صفر Issue است.
