# Aminema 0.18.1 — Canonical Duplicate Fix

## اصلاح اصلی

- ادغام تکراری‌های واقعی بین FilmRooz و MyMoviz در فهرست فیلم‌های خارجی.
- نرمال‌سازی `&` و `and` برای عنوان‌هایی مثل Minions.
- تشخیص زیرعنوان صریح مثل `Young Washington: A Founder's Story`.
- تحمل اختلاف انتشار حداکثر یک‌سال برای عنوان کاملاً همخوان.
- جلوگیری از ادغام عنوان‌های مبهم، اختلاف سال جدی و مواردی مثل عناوین ایرانی
  که عمداً در دسته‌بندی‌های متفاوت باقی می‌مانند.

## آزمون و امنیت

- Unit Test، Lint و Build موفق.
- Login، Cookie، WebStorage، Library و داده‌های Provider پاک یا مهاجرت داده
  نشده‌اند.
- هیچ media URL، token، manifest یا DRM value استخراج یا ذخیره نشده است.

versionCode: 43
