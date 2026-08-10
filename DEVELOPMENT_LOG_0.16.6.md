# گزارش توسعه Aminema 0.16.6

تاریخ: 2026-08-10  
وضعیت: Candidate محلی؛ هنوز Commit، Tag، Push یا Release نشده است.

## مسئله محصول

با اضافه‌شدن منبع سوم، نمایش مستقل نتیجه هر Provider باعث می‌شد یک عنوان
مشترک چند بار دیده شود و انتخاب نسخه دوبله بهتر به عهده کاربر بماند. قبل از
نوشتن آداپتر سوم باید هویت عنوان از هویت صفحه Provider جدا می‌شد:

- `CanonicalMedia`: یک فیلم یا سریال در Aminema؛
- `SourceVariant`: صفحه عادی همان عنوان در یک Provider؛
- یک کارت در Search، چند منبع قابل انتخاب در Spotlight.

این مرحله عمداً In-memory است و فایل‌های پایدار کاربر را Migration نمی‌کند.

## Probe واقعی نمونه «لیسانسه‌ها»

روی همان امولاتور لاگین‌شده و بدون پاک‌کردن Data بررسی شد:

- ParsiFlix: `https://app.parsiflix.com/medias/series/338`
- FilmRooz: `https://sean.robert-redford.net/post/series/56308/لیسانسه-ها`
- هر دو نوع Series دارند.
- نام با فاصله/نیم‌فاصله متفاوت است.
- Metadata ذخیره‌شده ParsiFlix شامل IMDb `tt9191330`، کارگردان سروش صحت و
  بازیگران بود.
- صفحه FilmRooz سال 2016، کارگردان سروش صحت و بازیگران هم‌پوشان داشت.

در ابتدای Probe، DNS خود امولاتور قطع بود. همان AVD بدون Wipe و بدون حذف اپ با
DNS صریح دوباره اجرا شد؛ هر دو نشست ورود حفظ شدند. این مشکل کد اپ نبود.

## قرارداد تطبیق محافظه‌کارانه

اولویت Match:

1. URL عادی یکسان؛
2. IMDb یکسان و نوع یکسان؛
3. عنوان نرمال‌شده + سال یکسان + نوع یکسان؛
4. عنوان نرمال‌شده + نوع یکسان + کارگردان مشترک یا حداقل دو بازیگر مشترک.

قواعد ایمنی:

- عنوان تنها هرگز Merge نمی‌شود.
- IMDb یا سال متعارض رکوردها را جدا نگه می‌دارد.
- دو صفحه متفاوت از یک Provider فقط با عنوان/سال Merge نمی‌شوند.
- یک رکورد بدون IMDb نمی‌تواند دو IMDb متناقض را به‌صورت زنجیره‌ای به هم وصل
  کند.
- سال انتهای عنوان فقط وقتی جدا می‌شود که داخل پرانتز/براکت باشد؛ بنابراین
  `Blade Runner 2049` خراب نمی‌شود.
- Canonical ID پایدار با SHA-1 از شناسه عمومی/شواهد هویتی ساخته می‌شود؛ این
  Hash امنیتی یا شناسه Media نیست.

## UX پیاده‌شده

- Search دو آرشیو را هم‌زمان جستجو می‌کند، ولی خروجی یک Rail محتوایی است.
- Card مشترک `۲ منبع در دسترس` نشان می‌دهد.
- Source chooser فقط برای عنوان چندمنبعی ظاهر می‌شود.
- انتخاب منبع Loaderهای قبلی Metadata/Episode را می‌بندد، State موقت را Reset
  می‌کند و همان صفحه عادی Provider انتخاب‌شده را بارگذاری می‌کند.
- Source chooser بیرون Skeleton ثابت Hero قرار گرفت تا عنوان، خلاصه و CTA در
  1080p از کادر خارج نشوند.
- دکمه `انتخاب قسمت` با وجود Source chooser همچنان به خود Episode Navigator
  اسکرول می‌کند.
- QA واقعی دو مسیر پنهان را آشکار کرد: نتیجه Canonical سریال و سریال پس از
  تغییر Source، Label «انتخاب قسمت» داشتند اما Action داخلی `WATCH` می‌ماند و
  Provider page را باز می‌کرد. `CatalogKind.defaultSpotlightAction()` به‌عنوان
  قرارداد مشترک اضافه شد؛ Series بدون Resume همیشه `SELECT_EPISODE` و Movie
  همیشه `WATCH` می‌گیرد. این مسیر بعد از Source switch نیز با D-pad دوباره
  تأیید شد.
- `sourceLabel` جدا از نام دسته اضافه شد تا انتخابگر به‌جای «فیلم ایرانی» و
  «فیلم خارجی»، نام واقعی Provider را نشان دهد. سرویس‌های ذخیره‌شده فقط با
  مقدار پیش‌فرض خالی تکمیل می‌شوند و بازنویسی مخرب ندارند.

## فایل‌های اصلی

- `data/CanonicalLibrary.kt`
- `data/model/CanonicalModels.kt`
- `data/model/SearchModels.kt`
- `data/model/SpotlightModels.kt`
- `ui/search/SearchActivity.kt`
- `ui/spotlight/SpotlightActivity.kt`
- `ui/spotlight/SpotlightScreen.kt`
- `ui/components/CatalogCard.kt`
- `data/ServicesRepository.kt`
- `assets/services.json`
- تست‌های `CanonicalLibraryTest` و `SpotlightItemTest`

## ریسک‌ها و مرزها

- **Critical مهارشده:** Package، امضا، Cookie، Login، WebView session،
  Local storage و Library دست نخورده‌اند؛ نصب فقط باید `adb install -r` باشد.
- **High:** انتخاب منبع باید Episode Loader همان Provider را استفاده کند؛ Guard
  URL جلوی برگشت Callback قدیمی روی منبع جدید را می‌گیرد.
- **Medium:** Metadata ناقص می‌تواند Duplicate مشکوک را جدا نگه دارد. این
  تصمیم آگاهانه بهتر از Merge اشتباه است.
- **Medium:** Continue/Favorite/Recent هنوز URLمحورند؛ Migration بدون طراحی
  reversible در این نسخه انجام نشد.
- **Low:** آداپتر سایت سوم و سیاست Dub-first هنوز وارد این Candidate نشده‌اند.

## قدم بعدی پیشنهادی

1. تأیید کوتاه همین Candidate روی Android Box واقعی توسط مالک؛
2. طراحی Bridge غیرمخرب برای Continue/Favorite/Recent با نگه‌داشتن URLهای قبلی؛
3. ساخت Coverage Lab و مقایسه حداقل 100 عنوان؛
4. سپس Probe و Adapter مستقل سایت سوم و Best Source Resolver با اولویت نسخه
   دوبله بهتر.
