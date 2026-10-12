# QA Candidate0.19.0/code54 — 2026-10-12

## Gate

Baseline۸۸تست و بیلد/lint موفق، بدون خطای قبلی؛ فایل‌های مالک حفظ شدند.
Gate نهایی پس از cache تصویر: ۹۳ تست، صفر failure/error؛ lint صفر خطا و۱۳۱هشدار
قبلی؛ assembleDebug موفق. پنج تست ژانر: alias دقیق، رد حدس از خلاصه و substring،
خانوادگی بدون حدس سنی، تقویم کلاسیک، ماراتون فقط سریال، ترتیب هویت atlas،
metadata cached و dedupe غیرمخرب. git diff --check موفق.

## Emulator QA واقعی

نصب فقط adb install -r روی Television_1080p/API36، 1920×1080 و نشست‌های موجود.
- ردیف مسکات‌ها واقعاً بین Hero و Continue نمایش داده شد؛ تصاویر/labels native
  و فضای کارت‌ها بررسی شدند: /private/tmp/aminema-0190-dpad.png.
- کلیک موس روی ترسناک، فهرست۱۲۴عنوان از آرشیو موجود را باز کرد؛ نوع فیلم/سریال
  و پوسترها دیده شدند: /private/tmp/aminema-0190-horror-loaded.png.
- DPAD_DOWN/RIGHT/OK به خانوادگی رسید؛۷۳عنوان، warning رده‌بندی کودک و کنترل‌های
  فیلتر/مرتب‌سازی دیده شدند: /private/tmp/aminema-0190-dpad-open.png.
- Back از هر دو دسته به Home برگشت. بازگشت دقیق به همان کارت تضمین نیست؛ Home
  فوکوس عمومی فعلی را بازیابی می‌کند و ممکن است Continue انتخاب شود.
- تصویر atlas در بازگشت اولیه دوباره decode می‌شد؛ SoftReference مشترک + Mutex
  و initial cached state اضافه شد؛ زیر فشار RAM قابل بازپس‌گیری است، دائمی نیست.
- در crop اولیه نوک نوشتهٔ تولیدشده دیده شد؛ ارتفاع تصویر به۷۸٪ هر سلول
  کاهش یافت و Canvas146dp تناسب تصویر را حفظ کرد؛ labels واقعی اپ مستقل‌اند.
  این تغییر و SoftReference در Gate نهایی‌اند. همه۱۲کارت با DPAD تا ماراتون
  پیموده شدند؛ ماراتون۷۲۲عنوان سریال نشان داد: /private/tmp/aminema-0190-marathon.png.

پخش Providerها، fullscreen، account sync یا episode resolver تغییر نکرده‌اند؛
در این چرخه exhaustive playback جدید انجام نشد. Box واقعی، RAM/سرعت عددی،
فونت سیستمی بزرگ و همه حالات offline تست نشده‌اند. آفلاین بودن مسیر discovery
از کد cache-only ثابت است، نه ادعای اینکه تمام پوسترهای دوردست قبلاً دانلود شده‌اند.

## Artifact محلی، منتشرنشده

APK: /private/tmp/Aminema-0.19.0-debug.apk
SHA256: 38ecef214ecb93839cb134767fdf476a3e0d3ba5cff0668b558227fa7a0932bf
CertificateSHA256: ba6ac8c4c2e1828462e7a6b122ad60856a054a18389af36bc001a1ee38ba13d3
هیچ فایل لاگین/کوکی/کتابخانه پاک نشد. Commit/Push/Tag/GitHub Release انجام نشده.

## تأیید نصب نهایی

APK نهایی پس از crop۷۸٪ با install-r نصب شد؛ code54/name0.19.0 تأیید شد.
همان certificate، checksum فایل تحویل OK، manifest فاقد Probe و crash buffer خالی.
DPAD از Continue به دسته خانوادگی رفت؛ ردیف نهایی بدون نوشتهٔ دوباره و با
فوکوس واضح دیده شد: /private/tmp/aminema-0190-preview-final.png.
این تصویر نسخه تحویل است؛ screenshots قبلی مراحل QA پیش از crop آخرند.

## انتشار بعد از QA

با مجوز مالک Commitd2afc27/Tagv0.19.0 منتشر شدند. Build دوباره موفق و SHA
مطابق همین APK؛ GitHub Latest API/code54 و دارایی‌های دانلودشده/امضا تأیید شدند.
RELEASE_REPORT_0.19.0.md مرجع وضعیت جاری است؛ عبارت‌های Candidate بالا تاریخی‌اند.
