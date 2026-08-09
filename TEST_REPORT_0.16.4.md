# گزارش تست Aminema 0.16.4

تاریخ: 2026-08-09  
محیط: AVD `Television_1080p`، نصب درجا روی `com.amin.tvos.debug`

## Baseline پیش از تغییر

- `:app:testDebugUnitTest`: موفق
- `:app:lintDebug`: در اجرای Incremental موفق بود؛ Clean run ناسازگاری ابزار
  `UnrememberedMutableState` را آشکار کرد (شرح پایین)
- `:app:assembleDebug`: موفق
- Working tree فقط شامل سه اصلاح تأییدنشدهٔ همین Hotfix بود و فایل نامرتبطی
  بازنویسی نشد.

## Verification پس از تغییر

- `git diff --check`: موفق
- `:app:testDebugUnitTest`: موفق
- `:app:lintDebug`: موفق به‌جز ۱۴ Check متعلق به Artifact ناسازگار
  `compose-runtime-lint 1.7.2`
- `:app:assembleDebug`: موفق
- نصب با `adb install -r`: موفق
- اجرای Home در 1920×1080: موفق
- بازکردن Settings با Mouse: موفق
- Fatal Exception در Logcat: مشاهده نشد
- Package/Cookie/Login/Data پاک نشد.
- APK نهایی: `22,904,748` بایت
- SHA-256: `330208a7326b1c9b74069b4174bfafa023c8f12a4115925ab6827c940581556f`
- Package: `com.amin.tvos.debug`، versionCode `36`، versionName `0.16.4`
- Digest امضا با 0.16.3 یکسان:
  `ba6ac8c4c2e1828462e7a6b122ad60856a054a18389af36bc001a1ee38ba13d3`

### استثناء ابزار Lint

Artifact `compose-runtime-lint 1.7.2` با Lint 31.13.2 پیش از تحلیل
`MainActivity.kt` در چند Detector دچار `NoClassDefFoundError` می‌شود. AGP
8.13.2 نسخه Lint پایین‌تر را رد می‌کند؛ برای جلوگیری از Upgrade پرریسک Runtime
در این Hotfix، ۱۴ Check همان Artifact موقتاً غیرفعال شدند. Lintهای Android،
Compose UI، Material، Navigation و سایر کتابخانه‌ها، Unit tests و APK Build
اجرا شدند. این استثناء در Upgrade بعدی Compose باید حذف و دوباره فعال شود.

## Characterization مسیر اصلاح‌شده

1. Settings نسخه را با `UpdateRepository` و versionCode فعلی بررسی می‌کند.
2. Release معتبر در State واحد `UpdateRepository` منتشر می‌شود.
3. Settings و Home همان `StateFlow` سطح Application را مشاهده می‌کنند.
4. State به `Available(release)` تغییر و Settings همان `UpdateBanner` را نمایش
   می‌دهد.
5. دکمه بروزرسانی از مسیر مشترک Home دانلود، Progress، SHA-256 و Installer را
   اجرا می‌کند؛ خروج از Settings همان State را در Home حفظ می‌کند.

## تست باقی‌مانده روی TV فیزیکی

- پس از انتشار 0.16.4، روی نسخه قدیمی «بررسی بروزرسانی» زده شود و نمایش بنر
  نصب و Progress در همان Settings تأیید شود.
- دانلود APK، بررسی SHA-256 و بازشدن Installer Android تأیید شود.
- موفقیت امولاتور جای پذیرش نهایی Android Box را نمی‌گیرد.
