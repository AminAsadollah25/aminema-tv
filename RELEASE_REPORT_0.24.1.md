# انتشار تأییدشدهٔ Aminema0.24.1/code61 — 2026-10-16

- مجوز صریح مالک پس از گزارش QA: «منتشر کن ... یه چیزی هم بررسی کن الان سرچ چطوری کار می‌کنه؟».
- Commit سورس:c2d3e137b658cd925ca90f21b41ffe3f0bfc0b6f.
- Tag:v0.24.1؛ annotated object:8204137a8488c0a90617cf3729144a2af34ad2c2؛ peeled commit همان Commit سورس.
- انتشار عمومی:https://github.com/AminAsadollah25/aminema-tv/releases/tag/v0.24.1
- Latest API:v0.24.1،draft=false،prerelease=false؛ آخر body خط `versionCode: 61` تأیید شد.
- APK:Aminema-0.24.1-debug.apk؛42930187بایت؛ فایل هم‌نام.apk.sha256،91بایت ضمیمه است.
- SHA256:ad1472736b41e3b7d6a85031fa994179c942707b20f2ffec1f26a10b69e9c827.
- دارایی منتشرشده از Release دوباره دانلود شد؛ shasum-check،cmp با APK تست‌شده و apksigner موفق.
- Package:com.amin.tvos.debug؛ امضایSHA256:ba6ac8c4c2e1828462e7a6b122ad60856a054a18389af36bc001a1ee38ba13d3.
- همان APK دانلودشده با install-r روی emulator-5554 نصب شد:Success؛ MainActivity COLD start موفق. ورودها/داده‌ها پاک نشدند.
- گیت انتشار دوباره testDebugUnitTest/lintDebug/assembleDebug موفق و SHA عوض نشد؛153تست/صفرfailure/error/skipped،lint0error/135warning و سه fixtureJS موفق.
- APK عادی و فاقد runnerهای موقت QA است؛ hash همان خروجی نهایی TEST_REPORT_0.24.1.md است.
- شاخهmain و تگ با push عادی/fast-forward منتشر شدند؛ force-push/reset/revert/پاک‌کردن داده انجام نشد.
- فقط25فایل کد/تست/اسناد مرتبط وارد commit سورس شد. سه handoff با staging فقط prefix همین نسخه ثبت شدند؛ پژوهش‌های قدیمی، اسکریپت‌هایPython،manifest.bak و پیش‌نمایش0.21.1 محلی ماندند.

## مسیر به‌روزرسانی

مقصد UpdateRepository همان GitHub/releases/latest است؛ body عدد61 و assetهای
هم‌نام APK/SHA لازم را دارد. ReleaseVersionPolicy عدد صریح را با versionCode
نصب‌شده مقایسه می‌کند(61>60)؛ امضای ثابت برای نصب درجا تأیید شد. این شاهد
برابر تست واقعی اعلان/تأیید PackageInstaller از0.24.0 روی TVمالک نیست؛ نسخه
قدیمی روی دستگاه اصلی downgrade/نصب نشد و Owner برای این تست پاک نشد.

## محدودیت‌های پذیرفته‌شده

- کوری9 در Android WebView هنوز شروع نمی‌شود؛ Player1/2 و seek کوچک تشخیصی درمان نبودند. در مرورگر با مسیر رسمی سایت پخش شد؛ علت قطعی مشخص نیست.
- گالیله خودکار با720 و Pause/Resume تأیید شد، اما انتظار شروع در بعضی مسیرها طولانی است.
- 24چرخه جزئیات→Home بدون ANRتازه، نه اثبات علت/رفع قطعی ANRاولیه.
- first-run خالی واقعی و اجرای دوم بدون دریافت اضافی در کاربر جدا PASS؛ همان کاربر موقت حذف شد و Owner محفوظ ماند.
- نبود تبلیغ در کل اثر/پس‌پایان، کل آرشیو و TVBoxواقعی تضمین نشده است. YouTube تغییر نکرد.
- جستجو صرفاً بررسی شد، نه بازنویسی؛ SEARCH_REVIEW_0.24.1_FA.md محدودیت‌های صفحه اول/24نتیجه/نام فارسی-فینگلیش، ترتیب نتایج و ریسک پاسخ قدیمی را ثبت می‌کند.

متن‌های Candidate/«منتشر نشده/اجازه نیست» در اسنادQA تاریخچهٔ قبل از این دستورند؛ این گزارش مرجع واقعی انتشار است.
