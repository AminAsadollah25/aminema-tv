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
- Release commit و GitHub URL پس از انتشار در همین فایل ثبت می‌شود.
