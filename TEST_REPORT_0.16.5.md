# گزارش تست Aminema 0.16.5

تاریخ: 2026-08-09  
محیط: AVD `Television_1080p`، حساب FilmRooz لاگین‌شده، نصب درجا روی
`com.amin.tvos.debug`

## بازسازی قبل از اصلاح

- Silo از مسیر واقعی Search باز شد.
- `onPageFinished` ثبت شد، اما قبل از اجرای Extraction، Timeout هشت‌ثانیه‌ای
  `finish(editions=null)` را اجرا کرد.
- UI همان خطای گزارش‌شده «اطلاعات قسمت‌ها یافت نشد» را نشان داد.

## Verification پس از اصلاح

- `git diff --check`: موفق
- `:app:testDebugUnitTest`: موفق
- `:app:lintDebug`: موفق
- `:app:assembleDebug`: موفق
- نصب با `adb install -r`: موفق
- Silo Extraction Attempt 1: موفق
- نمایش سه فصل و Episode cardها در 1920×1080: موفق
- Fatal Exception در Logcat: مشاهده نشد
- Package/Cookie/Login/Data پاک نشد
- Package: `com.amin.tvos.debug`، versionCode `37`، versionName `0.16.5`

## Characterization مسیر اصلاح‌شده

1. Loader Normal same-host detail page را با CookieManager مشترک باز می‌کند.
2. Deadline محدود ۲۵ ثانیه فعال می‌شود.
3. پس از `onPageFinished`، Extraction با تأخیر Provider-specific اجرا می‌شود.
4. Payload معتبر بلافاصله Loader را موفق می‌بندد؛ payload خالی تا Deadline با
   فاصله ۱.۵ ثانیه Retry می‌شود.
5. `finish/destroy` WebView و Callbackهای باقی‌مانده را آزاد می‌کند.
6. در Failure نهایی، کاربر می‌تواند از همان Spotlight «تلاش دوباره» را بزند.

## Artifact نهایی

- فایل: `Aminema-0.16.5-debug.apk`
- اندازه: `22,921,132` بایت
- SHA-256: `54bc877a92e551c991caf0c622303047ce302060ea484df11f10a1ffed272211`
- Digest امضا: `ba6ac8c4c2e1828462e7a6b122ad60856a054a18389af36bc001a1ee38ba13d3`
- بررسی APK برای نبودن `ProviderDomProbe/AminSafeProbe`: موفق

## تست باقی‌مانده روی TV فیزیکی

- نصب 0.16.5 از مسیر Update درون‌برنامه‌ای.
- بازکردن Silo و تأیید فصل‌ها/قسمت‌ها روی سرعت واقعی Android Box.
- یک بار تست «تلاش دوباره» فقط در صورت خطای موقت شبکه یا Provider.
