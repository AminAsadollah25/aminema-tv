# انتشار تأییدشدهٔ Aminema 0.20.0/code55

تاریخ: 2026-10-13؛ مجوز صریح مالک: «منتشر کن لطفا».

- Commit کد و اسناد تحقیق: 97699e0؛ Tag: v0.20.0؛ push اتمیک main/tag موفق.
- Release: https://github.com/AminAsadollah25/aminema-tv/releases/tag/v0.20.0
- Latest API: v0.20.0؛ draft=false؛ prerelease=false؛ خط versionCode: 55.
- Assets: Aminema-0.20.0-debug.apk و Aminema-0.20.0-debug.apk.sha256.
- اندازهٔ APK: 41422805 bytes.
- SHA256: 1dab77a44341c3ff0d3d62d167dda58dad1b544b50868059c43b5367a41466f6
- CertificateSHA256: ba6ac8c4c2e1828462e7a6b122ad60856a054a18389af36bc001a1ee38ba13d3
- Package: com.amin.tvos.debug؛ name0.20.0/code55.

پیش از انتشار Gradle Unit/Lint/Assemble موفق بود؛115تست، صفر failure/error.
تسک‌های بیلد/تست معتبر up-to-date بودند؛ اجرای مجدد اجباری همهٔ تست‌ها انجام
نشد. SHA خروجی بیلد دقیقاً با APK نهاییِ تست‌شده در TEST_REPORT_0.20.0.md برابر بود.
نسخهٔ نصب‌شدهٔ امولاتور code55/name0.20.0 و crash buffer خالی تأیید شد؛ نشست
یا فایل برنامه پاک نشد. UI/DPAD/موس/Back و211match قبلاً در همان گزارش ثبت‌اند.

دارایی‌های واقعی از GitHub مجدداً در
/private/tmp/aminema-0200-verify.nz24jY دانلود شدند. shasum-c نتیجه OK؛ امضا
و package/version با apksigner/aapt بررسی شدند. Latest API هر دو asset و خط
versionCode55 را برگرداند. تست جدید دانلود/Installer از داخل خود اپ انجام
نشده؛ سازگاری اطلاعات انتشار و بستهٔ دانلودشده تأیید شده است.

تطبیق‌ها device-local هستند؛ مالک روی TV باید یک بار دکمهٔ تطبیق همهٔ فهرست‌ها
را اجرا کند.211رکورد شامل فیلم/فصل‌اند، نه211فیلم یا تمام اقتباس‌های تاریخی.
اتصال صفحهٔ عنوان، تضمین پخش تک‌تک فصل‌ها نیست. Box فیزیکی و stream جامع
تست نشده‌اند. پیشرفت ماراتون/انتخاب فصل خودکار و تکمیل تاریخی هنوز بازند.

اسناد اطلس و کشف سینما در Git ثبت شدند، ولی جوایز/موضوع‌ها/اقتباس‌ها به
runtime اضافه نشده‌اند. اسکریپت‌های Python و AndroidManifest.xml.bak مالک
در Commit نیستند؛ هیچ cache/cookie/runtime data یا credential منتشر نشده.
