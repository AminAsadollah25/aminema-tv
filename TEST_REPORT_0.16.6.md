# گزارش تست Aminema 0.16.6

تاریخ: 2026-08-10  
محیط: macOS، Android Studio JBR، AVD `Television_1080p`، بسته
`com.amin.tvos.debug`

## Baseline پیش از تغییر

- `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug`: موفق.
- هشدار موجود: Debug هم `debuggable` و هم `isMinifyEnabled` است؛ Gradle اعلام
  می‌کند Optimization برای Debug غیرفعال است. این هشدار پیش از تغییر وجود داشت.

## تست‌های واحد پس از پیاده‌سازی

- `:app:testDebugUnitTest`: موفق؛ 26 تست، Failure/Error/Skipped صفر.
- پوشش تازه:
  - نرمال‌سازی ی/ک عربی، نیم‌فاصله و فاصله فارسی؛
  - همسانی `Spider-Man` / `spider man` / `spiderman`؛
  - جداکردن سال پرانتزی بدون خراب‌کردن `Blade Runner 2049`؛
  - Match با IMDb؛
  - عدم Merge با عنوان تنها، سال متعارض یا دو صفحه متفاوت یک Provider؛
  - پذیرش «لیسانسه‌ها» با Title+Kind+Credits؛
  - Serialization سازگار `SpotlightItem` با Canonical ID و SourceVariant؛
  - قرارداد Action امن: Series به Episode Navigator و Movie به Watch.

## پذیرش تصویری اولیه قبل از آخرین Polish

- Search واقعی «لیسانسه ها» در هر دو Provider اجرا شد.
- خروجی یک کارت و برچسب `۲ منبع در دسترس` داشت.
- Spotlight انتخابگر دو Source را نشان داد.
- دو ایراد تصویری مشاهده و در Source اصلاح شد:
  1. Labelهای عمومی «فیلم ایرانی/خارجی» به `ParsiFlix/FilmRooz` تبدیل شد؛
  2. Source chooser از Hero خارج شد تا محتوای Hero بریده نشود.

## QA نهایی

- فرمان نهایی
  `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug`: موفق.
- Android Lint: صفر Issue.
- `git diff --check`: پاک.
- APK نهایی: `34,750,588` بایت.
- SHA-256:
  `88222e4a926f73c9e5798beaa0c89e16138ba90829f14825450805c829558f3a`
- نصب فقط با `adb install -r`: موفق؛ نسخه نصب‌شده code 39.
- شمار فایل‌های Session/Data قبل و بعد نصب نهایی: `716 → 716`؛ هیچ Data clear،
  Uninstall، Cookie clear یا AVD wipe انجام نشد.
- Search واقعی `لیسانسه ها`: یک کارت Canonical به‌جای دو کارت Provider.
- Spotlight اولیه: Hero کامل و بدون بریدگی؛ Action سریال آیکن/رفتار انتخاب
  قسمت دارد.
- `انتخاب قسمت`: داخل Spotlight به Navigator بومی اسکرول کرد و Browser را باز
  نکرد.
- Source bar: نام‌های واقعی ParsiFlix/FilmRooz، پس‌زمینه تیره خوانا و دسترسی
  با Mouse و D-pad.
- تغییر FilmRooz → ParsiFlix با D-pad/OK: Artwork و فصل‌ها به منبع جدید تغییر
  کرد؛ دو فصل ParsiFlix بارگذاری شد.
- پس از تغییر منبع، Action سریال همچنان `انتخاب قسمت` باقی ماند و با OK به
  Episode Navigator رفت؛ Provider page باز نشد.
- Back از Spotlight به همان Search results برگشت.
- `FATAL EXCEPTION`: صفر در Logcat بازه تست.
- تصاویر شاهد محلی:
  - `/tmp/aminema-0166-final-search.png`
  - `/tmp/aminema-0166-final-hero.png`
  - `/tmp/aminema-0166-final-source-bar.png`
  - `/tmp/aminema-0166-final-episode-scroll.png`
  - `/tmp/aminema-0166-final-parsi-switched.png`
  - `/tmp/aminema-0166-final-after-switch-episode.png`
  - `/tmp/aminema-0166-final-back-search.png`

## مواردی که این نسخه ادعا نمی‌کند

- Physical Android Box هنوز برای این Candidate تست نشده است.
- سایت سوم هنوز Adapter ندارد.
- Continue/Favorite/Recent هنوز Canonical migration نشده‌اند.
- هیچ App data، Cookie، Login، Cache یا Library برای تست پاک نشده است.
