# QA Candidate0.22.0/code58 — 2026-10-14

## نهایی — معتبرتر از Snapshot اولیهٔ پایین
Gate نهایی testDebugUnitTest/lintDebug/assembleDebug موفق،125تست بدونfailure/error؛
lintصفرخطا/131هشدار. APKنهایی41820707بایت؛ نصب-rموفق:
/private/tmp/Aminema-0.22.0-artwork-preview.apk
SHA256:38645acaeed47e1d133d7d52a88e3424175a395e3806bdc4ae8a4910719523b4
امضای قبلی حفظ شد. بافرcrash در QAنهایی خالی بود؛ داده/ورود پاک نشد.

اسکن موجود520/520query،صفرخطا؛ audit285/285matched/withProviders. فایل audit
پس از نصب جدید نیز285/285است. Marvel«مسیر عظیم»142/142تطبیق در UIواقعی نشان داد؛
این داده روی امولاتور است، نهseedداخلAPK و نه تضمین تمام فصل‌ها/پخش.

QAnative1920×1080: تصاویر هر۱۲کارت در چهارردیف دیده شدند، footerبدونclipping،
بازهٔ سال به شکل2001–2014درست شد. DPADبه باند→25/25، موس بهMadMax→5/5؛
Backاز ردیف آخر، scrollهمان‌جا ماند. تست مستقل DPAD→OK→BackفوکوسMadMaxرا
حفظ کرد. در بازگشت از کلیک موس، موقعیت محفوظ بود؛ ورود دوباره به حالت کنترل
ممکن است ابتداfirstvisibleرا انتخاب کند؛ mixed-inputfocusتضمین کامل ندارد.

علت پرش قبلی: summaryگرید داخلbranch با emptyشروع می‌شد و itemCountبه۲header
افت می‌کرد. انتقال بهparentمانعclampشد؛ هیچ دادهٔ شخصی ذخیره/مهاجرت نشد.
شاهد نهایی بالا:/private/tmp/aminema-marathon-cards-final-022.png
شاهد پایین:/private/tmp/aminema-marathon-dpad-back-final.png
آزمونTVBoxفیزیکی/بزرگنماییfont/تمامplaybackها انجام نشده. پوسترهای داخل لیست
همان64×96قبلی‌اند؛ نمایش برخی پوسترهایMarvelدر اولینفریم خالی بود و باید در
مرحلهٔ بعد بررسی شود. دیدم/پیشرفت و اصلاح پوستر لیست همچنان صف بعدی‌اند.
بدونCommit/Push/Tag/Release.

## گیت پایه و پس از تصاویر
Baseline124تست موفق؛ پس از تصاویر125تست، failure/error/skipped صفر.
testDebugUnitTest، lintDebug و assembleDebug موفق؛ lint صفر خطا/131هشدار موجود.
MarathonArtworkTest پوشش۱۲شناسهٔ مجموعه/منابع یکتا/fallbackشناسه ناشناخته را
بررسی می‌کند؛ JourneyCatalogTest تعداد۸مجموعه تازه و عدم ورود به Marvel را.
git diff --check موفق؛ CatalogCard و GenreArchiveActivity diffندارند.

## APK مرحلهٔ کارت‌های تصویری
مسیر: /Users/aminasadollah/Library/Caches/AminemaBuild/app/outputs/apk/debug/app-debug.apk
اندازه41820955بایت؛ SHA256:
5bb0b0930a6cab4d86f6837dfd5e658dd67f7865788391d19197a29ee1ea0b1e
امضای SHA256 قبلی حفظ است:
ba6ac8c4c2e1828462e7a6b122ad60856a054a18389af36bc001a1ee38ba13d3
۱۲تصویر مجموع626186بایت؛ contact sheet دیده و cropهای Fast/DieHard/Rocky
برای حفظ چهره اصلاح شدند. نصب-r و QA native نهایی هنوز در صف پایان اسکن‌اند.

## تطبیق روی دستگاه
امولاتور موجود Television_1080p، emulator-5554،1920×1080؛ نشست‌ها حفظ شدند.
در ابتدای بررسی فایل journey_lookup.json فقط595بایت بود و auditوجود نداشت؛
تطبیق کاملِ ثبت‌شده در QA تاریخی روی snapshot فعلی موجود نبود. از رابط خود اپ
تطبیق صریح اجرا شد؛520query در registryفعلی. تا423query صفرخطای ثبت‌شده و
کش checkpoint به382KBرسید. این عدد proofوجود کامل همهٔ عناوین نیست.
پایان اسکن/audit و QA کارت‌ها باید در بخش نهایی همین سند اضافه شوند.

## محدودیت
تست فیزیکی TVBox، همهٔ مسیرهای پخش و اندازه‌گیری TTFDانجام نشده‌اند.
Progress/دیدم هنوز اجرا نشده؛ پوستر داخل لیست هنوز همان UIقبلی است.
بدون Commit/Push/Tag/Release؛ هیچ نصب تازه/پاک‌کردن Storage/Logout انجام نشده.
