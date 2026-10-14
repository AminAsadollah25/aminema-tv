# QA محلی 0.21.0 / code56

## Gate تعمیم ده ژانر — 2026-10-14، معتبرتر از شواهد قدیمی پایین

- Baseline122تست/lint/build موفق، سپس Gate123تست، صفرfailure/error/skipped.
- testDebugUnitTest، lintDebug، assembleDebug موفق؛ diff --check بدون خروجی.
- مقادیر ده ژانر از inputهای فرم واقعی خوانده شد. Submit واقعی در مرورگر:
  horror9238، family4068، mystery4417، action10074، animation3170،
  romance10105، comedy19172، science-fiction4014، fantasy3662، documentary7030.
  همه24ردیف و cursor صفحه2 با همان genre/type داشتند؛ شمارش‌ها ادعای سایت در زمان تست‌اند.
- تست قرارداد40ترکیب ژانر/نوع/sort و جداسازی cursor/cache موفق.
- APK نهایی install-r روی امولاتور موجود موفق؛ ترسناک از Home باز شد،
  صفحه2با24عنوان متفاوت دریافت شد. Back جای Home را حفظ کرد.
- خانوادگی از مسکات Home با24عنوان مستقل و total4068 دریافت شد.
- خانوادگی IMDb عنوان‌ها را عوض کرد؛ تغییر به سریال24عنوان/total41 آورد.
  فوکوس اولیه D-pad روی بازگشت قابل رؤیت بود. PID10912 در بررسی AndroidRuntime
  خطایی نداشت. lint نهایی0خطا/131هشدار. تست کامل همه مسیرهای D-pad باقی است.
- این Gate تمام40ترکیب را در UI امولاتور تست نکرد؛ نه تست دستگاه فیزیکی،
  نه benchmark عملکرد و نه تأیید پخش. FilmRooz هنوز pending است.
- APK جدید: /private/tmp/Aminema-0.21.0-all-genres-debug.apk
- Size:41745189bytes؛ SHA256:bd00e0123153fa8177d0956a7f1dd5db620846d727ebb7626df8391cc8f64265
- امضا همان ba6ac8c4c2e1828462e7a6b122ad60856a054a18389af36bc001a1ee38ba13d3 تأیید شد.
- عکس‌ها: /private/tmp/aminema-genres-horror.png،
  /private/tmp/aminema-genres-horror-page2.png، /private/tmp/aminema-genres-family-ready.png
- Candidate هنوز0.21.0/code56، منتشرنشده؛ APK پایین نمونهٔ اولیه است.

- Gate نهایی: testDebugUnitTest، lintDebug، assembleDebug موفق.
- 122تست، صفر failure/error؛ lint صفر خطا و131هشدار.
- نصب درجای com.amin.tvos.debug روی Television_1080p؛ sessionها پاک نشدند.
- Home → مسکات عاشقانه →24عنوان newest، پوستر/سال/IMDb: تأیید بصری APK نهایی.
- تغییر IMDb: عنوان‌ها متفاوت، The Greatest Gift Ever Given در ابتدای صفحه1.
- صفحه2 IMDb:24عنوان متفاوت، Older/Kumbalangi Nights/Gol Maal؛ نتیجه تأیید شد.
- totalCount طبق سایت10105 در تست فیلم؛ این تعداد cache محلی نیست.
- newest صفحه2 در build اصلاحی نیز با The Love Hypothesis تأیید شد.
- خطای NPE ابتدایی lazy page snapshot اصلاح شد؛ آن خطا ANR نبود.
- FilmRooz filtered submit تأیید نشده؛ adapter آماده نیست.
- سریال IMDb:24عنوان، totalCount طبق سایت306؛ کارت‌ها سریال بودند.
- Tell Me That You Love Me به Spotlight با دکمهٔ انتخاب قسمت باز شد؛
  Back همان فیلتر سریال/IMDb و صفحه1 را حفظ کرد.
- AndroidRuntime برای PID9598 نسخه نهایی تا این تست خروجی خطا نداشت.
- پخش واقعی، TV فیزیکی، benchmark، offline و تمام مسیرهای DPAD تأیید نشده‌اند.

APK: /private/tmp/Aminema-0.21.0-debug.apk
Size:30769397bytes
SHA256:add84e8e95b8db69659443ad4130be5a63d172c3151bb78975c1352b4d99968a
Certificate SHA256:ba6ac8c4c2e1828462e7a6b122ad60856a054a18389af36bc001a1ee38ba13d3
Screenshots: /private/tmp/aminema-021-final-archive.png،
/private/tmp/aminema-021-final-imdb.png، /private/tmp/aminema-021-final-next-result.png

Candidate محلی؛ منتشر نشده. اسکریپت‌های نامرتبط کاربر و پژوهش جوایز حفظ شدند.
