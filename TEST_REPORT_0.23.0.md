# QA محلی 0.23.0 / code59

تاریخ: 2026-10-14. وضعیت: پیش‌نمایش؛ برای انتشار نهایی آماده اعلام نشده.

- testDebugUnitTest: 134 تست، صفر failure/error/skip.
- lintDebug: صفر error، 132 warning؛ نسبت به ثبت قبلی یک warning بیشتر، منشأ دقیق تفکیک نشده.
- assembleDebug: موفق؛ Java از JBR Android Studio، cache خارج فولدر همگام‌شده.
- نصب امولاتور: adb install -r موفق، بدون پاک‌کردن داده یا نشست.
- Native UI: گرید594، 17گروه؛ انتخاب علمی‌تخیلی37، تصاویر محلی و طرح جایگزین خوانا.
- کیبورد محلی: تایپ «ب»، بستن و نمایش4خانواده در گروه علمی‌تخیلی با حفظ فیلتر تأیید شد.
- نسخه نصب‌شده59/0.23.0، crash buffer خالی؛ امضای APK همان debug SHA256
  `ba6ac8c4c2e1828462e7a6b122ad60856a054a18389af36bc001a1ee38ba13d3`.
- مرجع Star Wars: 37/37 تطبیق ذخیره‌شده در رابط قابل مشاهده، ورود به لیست موفق.
- Alien discovery: ورود و نمایش loading/error سالم؛ دریافت موفق تایید نشد.
  دو provider timeout20s؛ JourneyArtwork خطای UnknownHostException؛ ping origin داخل emulator
  unknown host ولی HEAD عمومی از Mac200. هیچ cookie/token/URL خصوصی ثبت نشده.
- تست دستی همهٔ خانواده‌ها، پخش، دریافت موفق شبکه و پیشرفت در این گزارش تأیید نمی‌شوند.

APK: `/Users/aminasadollah/Library/Caches/AminemaBuild/app/outputs/apk/debug/app-debug.apk`

Size: 31631415 bytes

SHA256: `c55bb98863797c76253a4fe33df60ce8c900f032781f7701dfdb83fbf833fab6`

Commit/Tag/Release: انجام نشده.
