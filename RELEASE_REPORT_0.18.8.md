# انتشار Aminema 0.18.8 — 2026-10-10

## مشخصات

- نسخه `0.18.8`، کد نصب `51`؛ نسخهٔ قبلی منتشرشده `0.18.7/code49`.
- Candidate روز قبل `0.18.8/code50` بود؛ افزایش code اجازهٔ آپدیت روی آن را می‌دهد.
- اجازهٔ Commit/Push/Tag/Release در همین گفتگو از مالک دریافت شد.
- دامنه: اصلاح‌های Startup/Catalog و تغییرات قبلی Live TV موجود در همان APK.
- اسکریپت‌های موقت Python و `AndroidManifest.xml.bak` عمداً منتشر نمی‌شوند.

## Review تغییرات قبلی

Live TV دارای صف application-owned، pause وقتی کل برنامه خارج foreground
است، الزام mute بومی پیش از Probe، UNKNOWN به‌جای outage حدسی، حفظ شاهد
پخش موفق قبلی و ثبت پخش واقعی کاربر است. Repository انتشار State را در
قفل persistence انجام می‌دهد. ۱۱ تست LiveHealthPolicy در مجموعهٔ تست‌ها
وجود دارد. این Review و تست policy جای تست جامع پخش واقعی شبکه‌ها نیست.

## امنیت و تطبیق

پکیج debug و تنظیم امضا تغییر نمی‌کنند؛ نصب فقط `adb install -r`.
هیچ نشست/کوکی/داده پاک نمی‌شود. ابزارهای موقت و حساب‌ها نباید وارد Git شوند.
StartupDiagnostics فقط زمان/stack کلاس‌ها را در DEBUG ثبت می‌کند؛ sampler
فقط با extra صریح فعال است، بدون URL یا دادهٔ حساب. HealthProbe جزء محصول
است، نه ابزار موقت استخراج لینک.

شرح فنی و شواهد QA قبلی: `STARTUP-DEEP-DIVE-2026-10-09.md` و
`TEST_REPORT_STARTUP_CATALOG_2026-10-09.md`.

## وضعیت اجرای انتشار

Gate نسخهٔ نهایی موفق: ۶۵ تست، Failure/Error/Skipped صفر، lint صفر خطا و
۱۳۱ هشدار. APK برابر 25,585,855 بایت؛ SHA-256:
`982ce869461d77989ace14a323e18e13cfd10e79d5b812adbb383d63334da40d`.
نام Asset سازگار با updater: `Aminema-0.18.8-debug.apk` و checksum هم‌نام.

انتشار کامل شد:

- Commit کد: `1265ce54dfab828e63ff99ea170c2039292f2b59`.
- Tag: `v0.18.8` روی همین Commit؛ هر دو به origin Push شدند.
- Release: https://github.com/AminAsadollah25/aminema-tv/releases/tag/v0.18.8
- Latest API واقعاً `v0.18.8`، draft=false، prerelease=false و code51 است.
- هر دو Asset حالت uploaded دارند؛ فایل APK دوباره از GitHub دانلود شد
  و `shasum -a 256 -c` نتیجهٔ OK داد؛ digest API نیز با فایل محلی یکسان است.
- امضای APK دانلودشده، Candidate قبلی و بیلد نهایی یکسان:
  `ba6ac8c4c2e1828462e7a6b122ad60856a054a18389af36bc001a1ee38ba13d3`.
- نصب درجا روی emulator-5554 موفق؛ dumpsys code51/name0.18.8 را تأیید کرد.
  خانهٔ واقعی با هیرو و پوستر دیده شد؛ لاگ AndroidRuntime PID14300 خطا نداشت.
  شاهد: `/private/tmp/aminema-0.18.8-release-home.png`.
- سازگاری قرارداد updater بررسی شد: Latest، پسوند debug.apk، checksum
  هم‌نام و خط code51 صحیح‌اند. اجرای دوبارهٔ گردش نصب از داخل برنامهٔ
  code49/50 و تست Box واقعی انجام نشده؛ پاک‌کردن/دانگرید محیط مجاز نیست.
- هیچ فایل خصوصی، اسکریپت موقت یا بکاپ Manifest وارد این Commit نشد؛
  فایل‌های untracked قبلی دست‌نخورده باقی‌اند.
