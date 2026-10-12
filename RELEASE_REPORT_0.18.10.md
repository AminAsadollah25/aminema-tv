# انتشار تأییدشده Aminema0.18.10 — 2026-10-12

- انتشار با مجوز صریح مالک؛ code53، package com.amin.tvos.debug.
- Commit کد: 68627fb؛ Tag: v0.18.10؛ main و tag به origin push شدند.
- Release: https://github.com/AminAsadollah25/aminema-tv/releases/tag/v0.18.10
- Latest API: v0.18.10، versionCode53، draft=false، prerelease=false.
- Assets: Aminema-0.18.10-debug.apk و Aminema-0.18.10-debug.apk.sha256.
- SHA256 نهایی: 50d9653049b3f6ca7040c9028ab3a04a04371ad33f71506fab8049c616459008
- SHA256 امضا: ba6ac8c4c2e1828462e7a6b122ad60856a054a18389af36bc001a1ee38ba13d3

هر دو asset از GitHub به /private/tmp/aminema-01810-release-check.JGGJ9r
دانلود شدند؛ shasum -c نتیجه OK و apksigner امضای قبلی را تأیید کرد.
APK نهایی با install -r روی امولاتور نصب شد؛ code53 و اجرای MainActivity
تأیید، بافر crash خالی. هیچ نشست/کتابخانه‌ای پاک نشده؛ فایل‌های نامرتبط مالک
در commit وارد نشده‌اند. Probe در manifest نهایی وجود ندارد.

Gate: ۸۸ تست، صفر failure/error؛ lint صفر خطا/۱۳۱ هشدار؛ assemble موفق.
QA واقعی تصاویر/عوامل/هیرو و محدودیت‌های آزمون در TEST_REPORT_0.18.10.md.
Box فیزیکی و بهبود عددی سرعت سنجیده نشده‌اند؛ پوشش تمام اشخاص تضمین نیست.
اصلاح آخر فقط تطبیق تقویم عمومی است و سه تست جدید دارد؛ آزمون بصری مفصل
پیش از آن انجام شده، پس از آن نصب نهایی و اجرای Home دوباره تأیید شدند.
