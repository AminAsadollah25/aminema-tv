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

این دو Id هرگز در UI نمایش داده نمی‌شوند — فقط «ایرانی»/«خارجی».

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
│   ├── HomeScreen.kt               صفحه اصلی (Column+verticalScroll — هنوز Lazy نشده، نگاه کن به بخش ۸)
│   ├── HomeViewModel.kt            State تمام ردیف‌های Home + Update state
│   ├── SmartGreeting.kt            سلام هوشمند + تبدیل جلالی (0.8.1)
│   ├── CinematicBackground.kt      پس‌زمینه محو از آخرین پوستر
│   ├── CatalogSectionRow.kt        ردیف «تازه‌ها» با فیلتر همه/فیلم/سریال
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
| **0.14.0 Candidate** | Series Pulse: قسمت/فصل، سریال‌های من، برگزیده‌ها، Sync مستقل و پس‌زمینه‌ای |

**قرارداد Versioning:** بعد از 0.9 → 0.10 → 0.11 … نه 1.0. باگ‌فیکس هم
نسخه جدا می‌گیرد (0.9.1، 0.9.2، …)، نه Patch روی نسخه قبلی.

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

- تشخیص Asset: نامش به `.apk` یا `.sha256` ختم شود
- تشخیص نسخه: اول دنبال خط `versionCode: N` در متن Release می‌گردد
  (استاندارد این پروژه — هر Release باید این خط را در پاورقی داشته باشد)؛
  اگر نبود، از خود Tag (`vX.Y.Z`) عدد می‌سازد: `major*10000+minor*100+patch`
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

- **R8/Minify** جدا از 0.10 نگه داشته شد. Build دیباگ بدون فشرده‌سازی
  می‌ماند چون: (۱) تغییر سطح کل Build است، (۲) می‌تواند WebView JS
  Bridge، Coil (Reflection)، kotlinx.serialization (Reflection) را
  بشکند، (۳) فقط با امولاتور قابل تأیید کامل نیست. نیاز به نسخه جدا با
  تست دقیق‌تر (فعلاً بدون شماره).
- **انتخاب خودکار قسمت سریال:** تیک‌های قسمت FilmRooz روی لپ‌تاپ و امولاتور
  با یک حساب یکسان نبودند و browser-local ثابت شدند. 0.14 فقط آخرین قسمت
  منتشرشده را نشان می‌دهد. Next/Unwatched نیازمند `SeriesProgress` محلی یا
  تأیید «تا این قسمت دیدم» است؛ هیچ قسمت حدس زده نمی‌شود.
- **Home هنوز `Column`+`verticalScroll` است، نه `LazyColumn`.** با ۵
  ردیف اولیه و دو ردیف سبک 0.14 در امولاتور مشکل نداشت، ولی **قبل از افزودن
  ردیف‌های ژانری/سنگین باید
  LazyColumn شود** وگرنه روی Box ضعیف کند می‌شود.
- **Cinematic Hover Preview:** هدف 0.15؛ خلاصه بدون اسپویل، سال/ژانر/امتیاز/
  مدت، آخرین قسمت و Backdrop محو با Dwell حدود 600ms و کش سبک.

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
می‌کرد. **این مسیر در Candidate 0.14.0 supersede شد:** اکنون
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

`RailNavigationControls(scrollState)` در `TvComponents.kt` تنها منبع رفتار
فلش‌هاست. Header و Row همان `ScrollState` را Share می‌کنند. هر عمل 82٪
`viewportSize` (حداقل 360px) را با `animateScrollTo` جابه‌جا می‌کند؛ در ابتدا
و انتها `FocusableCard(enabled=false)` Dim می‌شود. این کامپوننت در
`SectionRow`, `CatalogSectionRow`, `LiveTvSectionRow` و Search `ResultGroup`
استفاده می‌شود، بنابراین رفتار موس/DPAD همه ردیف‌ها یکسان می‌ماند.
