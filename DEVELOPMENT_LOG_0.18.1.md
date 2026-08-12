# Development Log — Aminema 0.18.1

## هدف

رفع تکراری‌های مشاهده‌شده در ردیف فیلم خارجی پس از ادغام FilmRooz و MyMoviz.

## شواهد واقعی

سه نمونه در کش واقعی امولاتور دو کارت داشتند:

- `Young Washington: A Founder's Story` / `Young Washington`
- `Minions & Monsters` / `Minions And Monsters`
- `Our Hero, Balthazar` با سال‌های 2025 و 2026

علت، نبود IMDb در خروجی FilmRooz و سخت‌گیری هستهٔ Canonical نسبت به تفاوت
عنوان یا سال بود؛ مشکل از LazyRow یا UI نبود.

## تغییرات

- `CanonicalText` اکنون `&` و `and` را یکسان می‌کند.
- فقط زیرعنوان صریح پس از `:` یا خط تیره، آن هم وقتی عنوان کوتاه‌تر هستهٔ
  کامل عنوان بلندتر باشد، alias محسوب می‌شود.
- اختلاف سال فقط تا یک‌سال پذیرفته می‌شود.
- اختلاف سال بزرگ و عنوان‌های مشابه بدون شاهد کافی همچنان مستقل می‌مانند.
- مرز کاتالوگ ایرانی و خارجی تغییر نکرده است.

## تست

- Regression test برای هر سه نمونه اضافه شد.
- Regression test برای جلوگیری از Merge با اختلاف سال زیاد اضافه شد.
- `:app:testDebugUnitTest` موفق.
- `:app:lintDebug` موفق.
- `:app:assembleDebug` موفق.

## وضعیت انتشار

- نسخه: `0.18.1 / versionCode 43`
- Release commit: `842caa36683d812d569b013ed011b606297b0bef`
- Tag: `v0.18.1`
- GitHub Release: `https://github.com/AminAsadollah25/aminema-tv/releases/tag/v0.18.1`
- APK asset: `Aminema-0.18.1-debug.apk`
- SHA-256 asset: `Aminema-0.18.1-debug.apk.sha256`
- APK SHA-256: `51dae0cb750b2e7d871655ea10945d9f25f9ba53a4e4d291cdd7d31a0bc397c6`
- Release عمومی است، Draft/Prerelease نیست.
