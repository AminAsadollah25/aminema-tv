# گزارش توسعه Aminema 0.17.0

تاریخ: 2026-08-10  
وضعیت: Candidate محلی؛ هنوز Commit، Tag، Push یا GitHub Release نشده است.

## هدف محصول

MyMoviz به‌عنوان منبع تکمیلی آرشیو خارجی اضافه شد، بدون ساختن بخش شلوغ یا کارت
تکراری تازه در Home:

- کاتالوگ عمومی MyMoviz بدون Login خوانده می‌شود؛
- جدیدترین فیلم‌ها و سریال‌های FilmRooz و MyMoviz در همان ردیف‌های خارجی قبلی
  ادغام می‌شوند؛
- عنوان مشترک فقط با قرارداد محافظه‌کارانه Canonical نسخه 0.16.6 یکی می‌شود؛
- Home حداکثر 24 کارت نگه می‌دارد و ترتیب دو Provider متوازن Interleave می‌شود،
  چون Timestamp انتشار قابل‌مقایسه بین دو سایت وجود ندارد.

## Probe واقعی MyMoviz

صفحات عمومی زیر بدون Login بررسی شدند:

- فیلم: `/_modern/classic?sort=latest`
- سریال: `/_modern/classic?sort=latest&type=tv`
- صفحه دوم: پارامتر `p=2`
- جستجو: `/_modern/search?q=...`

ساختار کارت واقعی از `.mv-results-grid .mv-ritem` و لینک عادی
`/_modern/title/{id}/{slug}` خوانده می‌شود. سه Pick تبلیغاتی ابتدای صفحه که خارج
از Results Grid هستند وارد کاتالوگ نمی‌شوند.

هر صفحه 10 نتیجه واقعی دارد. دو صفحه برای فیلم و دو صفحه برای سریال خوانده
می‌شود؛ در Probe امولاتور 20 فیلم و 20 سریال با Poster، IMDb و Summary ذخیره شد.
هیچ صفحه Watch، لینک Media، Token، Manifest یا DRM خوانده نشد.

## سیاست انتخاب منبع

برای فیلم مشترک:

1. نسخه دارای دوبله فارسی؛
2. بهترین کیفیت عادی تا سقف انتخاب خودکار 1080؛
3. در شرایط برابر FilmRooz؛
4. سپس کامل‌بودن Metadata و Artwork.

برای سریال، Tag کلی «دوبله» به‌تنهایی برای انتخاب Source کافی نیست، چون دوبله
ممکن است از انتشار اصلی عقب‌تر باشد. انتخاب Episode-level دوبله به نسخه 0.17.2
موکول است و باید از Badge واقعی هر فصل/قسمت تأیید شود.

## رفتار امن پیش از Login

- MyMoviz در کارت بزرگ «ورود مستقیم به سینماها» ظاهر نمی‌شود؛ منبع پشت همان
  ردیف‌های خارجی و Search است.
- وضعیت معتبر دوبله و زیرنویس با Badge سبز/آبی کوچک در بالای Poster دیده می‌شود؛
  اگر داده‌ای موجود نباشد Badge ساخته نمی‌شود.
- Movie Direct Play و Native Episode Navigator برای MyMoviz هنوز فعال نیست.
- کلیک روی عنوان MyMoviz ابتدا Spotlight بومی و سپس صفحه عادی Public عنوان را
  باز می‌کند.
- Login فقط در مرحله آینده Probe مسیر Watch لازم خواهد شد و مالک باید آن را
  شخصاً در امولاتور انجام دهد.

## باگ کشف‌شده در QA

Spotlight گاهی پیش از Attached شدن دکمه اصلی، `FocusRequester` را اجرا می‌کرد و
با `FocusRequester is not initialized` کرش می‌کرد. عنوان فقط-MyMoviz این Race
قدیمی را قابل تکرار کرد. فوکوس حالا پس از Layout واقعی دکمه درخواست می‌شود و
مسیر Dream to You بدون Fatal Exception باز شد.

همچنین Label دکمه از خود `SpotlightAction` خوانده می‌شود؛ سریال MyMoviz که هنوز
Navigator بومی ندارد، به‌دروغ «انتخاب قسمت» نشان نمی‌دهد و Action امن «تماشا»
دارد.

Metadata کامل بعضی عنوان‌ها پس از ورود به Spotlight می‌رسید و جابه‌جایی دکمه
فوکوس‌شده، LazyColumn را ناخواسته پایین می‌برد. Focus حالا پس از پایدارشدن
Metadata اعمال و Hero به ابتدای خود برگردانده می‌شود؛ عنوان، Summary و CTA از
همان نمای تصمیم‌گیری کامل شروع می‌شوند.

در QA چندمنبعی Spider-Man 3، MyMoviz دو امتیاز جدا داشت: امتیاز کاربران
MyMoviz برابر 9.2 و IMDb برابر 6.2. Extractor قبلی `aggregateRating` را به‌اشتباه
IMDb می‌نامید. اکنون امتیاز صریح لینک IMDb اولویت دارد. همچنین انتخاب FilmRooz
یا MyMoviz فقط مسیر پخش را عوض می‌کند؛ عنوان، Artwork، خلاصه، IMDb، عوامل و
Badgeهای پنل Canonical ثابت می‌مانند. FilmRooz نیز عبارت‌های «دوبله»، «نسخه
دو زبانه» و «صوت فارسی» را به‌عنوان شاهد دوبله فارسی تشخیص می‌دهد.

## فایل‌های اصلی

- `assets/services.json`
- `browser/CatalogBackgroundSync.kt`
- `browser/SiteSearchEngine.kt`
- `data/CanonicalLibrary.kt`
- `data/CatalogRepository.kt`
- `data/ServicesRepository.kt`
- `data/model/CatalogModels.kt`
- `data/model/SearchModels.kt`
- `data/model/SpotlightModels.kt`
- `ui/home/HomeScreen.kt`
- `ui/search/SearchActivity.kt`
- `ui/spotlight/SpotlightActivity.kt`
- `ui/spotlight/SpotlightScreen.kt`
- `CanonicalLibraryTest.kt`
- `SpotlightSourceSelectionTest.kt`

## مرز نسخه و قدم بعدی

0.17.0 فقط Foundation عمومی و ادغام Discovery است. برای مرحله Playback باید:

1. مالک در WebView امولاتور وارد MyMoviz شود؛
2. مسیر عادی Title → Watch و رفتار Movie/Series جداگانه Probe شود؛
3. هیچ لینک محافظت‌شده یا Token ذخیره نشود؛
4. سپس Episode-level availability، Dub status و Resolver پخش ساخته شود.
