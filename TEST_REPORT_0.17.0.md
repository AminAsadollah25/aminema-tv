# گزارش تست Aminema 0.17.0

تاریخ: 2026-08-10  
محیط: macOS، Android Studio JBR، AVD `Television_1080p`، بسته
`com.amin.tvos.debug`

## Baseline

- پیش از تغییر، `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug` موفق بود.
- هشدار موجود Debug Minify از قبل وجود داشت و به این نسخه مربوط نیست.

## تست خودکار

- فرمان نهایی `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug`: موفق.
- 30 تست؛ Failure، Error و Skipped صفر.
- Android Lint: صفر Issue.
- پوشش تازه:
  - Interleave متوازن دو Provider؛
  - حذف Duplicate با IMDb مشترک؛
  - اولویت فیلم دوبله حتی در برابر Original با کیفیت بالاتر؛
  - اولویت FilmRooz در دوبله/کیفیت برابر.
  - ثابت‌ماندن پنل Canonical هنگام تغییر منبع پخش.

## QA واقعی روی امولاتور

- نصب فقط با `adb install -r`: موفق؛ App data و نشست‌ها حفظ شدند.
- کش عمومی پس از Cold-start:
  - ParsiFlix: 10 All، 24 Movie، 24 Series؛
  - FilmRooz: 23 All، 8 Movie، 15 Series؛
  - MyMoviz: 24 All، 20 Movie، 20 Series؛
  - Error هر سه Section خالی بود.
- MyMoviz Movies: Poster/IMDb/Summary/Quality برای هر 20 عنوان موجود بود.
- MyMoviz Series: Poster/IMDb/Summary برای هر 20 عنوان موجود بود؛ 11 عنوان Tag
  کلی دوبله داشتند، ولی این Tag برای Episode selection استفاده نشد.
- ردیف «سریال خارجی» اجتماع دو Provider را نشان داد. Furious مشترک یک‌بار دیده
  شد و عنوان‌های فقط-MyMoviz مانند Dream to You نیز در همان Rail حاضر بودند.
- Spotlight سریال فقط-MyMoviz باز شد؛ EpisodeLoader اجرا نشد و Fatal Exception
  صفر بود.
- Race واقعی FocusRequester بازسازی، اصلاح و دوباره تست شد.
- Badgeهای سبز/آبی دوبله و زیرنویس روی Rail واقعی دیده شدند و با Tag نوع محتوا
  هم‌پوشانی نداشتند.
- جستجوی واقعی `spiderman` با Mouse و کیبورد درون‌اپ انجام شد؛ نتایج یکپارچه و
  Badgeها نمایش داده شدند.
- Dream to You پس از تکمیل Metadata از بالای Hero باقی ماند؛ عنوان و CTA دیگر
  با Auto-scroll اولیه از کادر خارج نشدند.
- CTA سریال MyMoviz به‌درستی «تماشا» بود و صفحه عمومی Title را باز کرد؛ Login و
  Direct Play خودکار اجرا نشد.
- صفحه واقعی Spider-Man 3 در MyMoviz بررسی شد: 9.2 امتیاز کاربران MyMoviz و
  6.2 امتیاز صریح IMDb است. Extractor دیگر این دو را جابه‌جا نمی‌کند.
- Spider-Man 3 در نسخه اصلاح‌شده با IMDb 6.3 و Badge سبز «دوبله فارسی» از
  Canonical FilmRooz نمایش داده شد؛ انتخاب Provider دیگر Metadata پنل را عوض
  نمی‌کند.
- نسخه نصب‌شده: `0.17.0 / versionCode 40`.
- APK: `34,825,588` بایت.
- SHA-256:
  `803331c34f13911cac3f71c53162b814a437c65c780d0ef3c77c75f5df3c2a5e`
- `git diff --check`: پاک.
- تصاویر شاهد:
  - `/tmp/aminema-017-final-badges-series.png`
  - `/tmp/aminema-017-final-search-spiderman.png`
  - `/tmp/aminema-017-final-hero-top.png`
  - `/tmp/aminema-017-final-mymoviz-detail.png`
  - `/tmp/aminema-source-fix-spotlight-initial.png`

## محدودیت باقی‌مانده

- تست کوتاه Owner روی Android Box فیزیکی هنوز لازم است.
- Login/Watch MyMoviz عمداً هنوز تست نشده و بخشی از 0.17.0 نیست.

هیچ Uninstall، Clear storage، Cookie clear، Logout یا AVD wipe انجام نشد.
