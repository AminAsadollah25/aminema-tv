# Candidate0.20.0/code55 — QA مرحلهٔ اول، 2026-10-13

به‌روزرسانی انتشار: نسخهٔ نهایی0.20.0/code55 با Commit97699e0/Tagv0.20.0
منتشر و دانلودشده راستی‌آزمایی شد؛ RELEASE_REPORT_0.20.0.md مرجع است.
گزارش‌های Candidate و artifact مرحلهٔ اول پایین تاریخی‌اند؛ QA مرحلهٔ دوم
و SHA نهایی، خروجی منتشرشده را توصیف می‌کنند.

## Gate

- Baseline پیش از کد: Unit/Lint/Assemble موفق،93تست موجود.
- Final: testDebugUnitTest/lintDebug/assembleDebug موفق؛101تست، صفر failure/error.
- Lint صفر Error و131Warning، همان شمار baseline. هشدارهای Gradle/debug-minify
  موجودند؛ build configuration تغییر نکرد.
-8تست جدید:38فیلم MCU، شروع فصل در release order، membership مشترک بدون duplicate،
  حذف روز انتشار آینده، match دقیق سال/نوع، تطبیق فصل بعد با سال اصلی، جلوگیری
  از چند IMDb متناقض، جدایی Marathon/Marvel. Snapshot امروز تست ثابت2026-10-13 است.
- git diff --check موفق؛ هیچ Probe جدید ساخته نشد.

## UI واقعی

Television_1080p / emulator-5554 /1920×1080؛ نصب فقط adb install-r.
دو ورودی زیر genre rail دیده و هر دو با موس باز شدند. Hero مارول با تصویر Thor
و ماراتون با مسکات فعلی دیده شد؛ DPAD collection/formats/timeline را پیمود و
فوکوس border روشن نمایش داد. Back از ماراتون به همان ناحیه خانه برگشت.
فیلتر فقط تطبیق‌ها با موس فعال شد؛8تطبیق cached در مسیر عظیم این snapshot
نمایش داده شد. این آمار availability کل Provider یا «تعداد آمادهٔ پخش» نیست.
کارت واقعی X-Men'97 با پوستر به Spotlight رفت؛ FilmRooz/MyMoviz variants حفظ شدند
و دکمهٔ انتخاب قسمت دیده شد. Protected playback داده یا URL ثبت نشده.
Iron Man بدون match به dialog توضیح رفت؛ Back dialog و سپس hub را بست.
Date bidi در QA اولیه روز/سال را برعکس نشان می‌داد؛ TextDirection.Ltr مستقل
برای تاریخ افزوده شد. پوستر matched کوچک bounded و با helper same-host موجود است.
Buffer crash در QA خالی بود. نصب‌های جایگزین داده/کوکی‌ها را حذف نکردند.

Screenshots:
- /private/tmp/aminema-020-entries.png — ورودی‌ها
- /private/tmp/aminema-020-marvel.png — Hero مارول
- /private/tmp/aminema-020-marathon.png — Hero ماراتون
- /private/tmp/aminema-020-dpad.png — collection focus
- /private/tmp/aminema-020-missing.png — fallback
- /private/tmp/aminema-020-matched.png — فیلتر و پوستر واقعی
- /private/tmp/aminema-020-open.png — X-Men'97 Spotlight با sourceهای موجود

بعضی تصاویر مربوط به iteration قبل از اصلاح عضویت YFN در MCU هستند؛ در build
نهایی همان رکورد Spider به MCU هم عضو است، duplicate جدیدی به complete اضافه نشده.

## Artifact نهایی

APK: /private/tmp/Aminema-0.20.0-debug.apk
Size:41329769bytes
SHA256:fcb19f2b368e36fedf77ef2c8a1ca37b3e7507971e208989fb2512eff707c44d
CertificateSHA256:ba6ac8c4c2e1828462e7a6b122ad60856a054a18389af36bc001a1ee38ba13d3
Package:com.amin.tvos.debug؛ name0.20.0/code55.

## تست‌نشده / باقی‌مانده

پخش end-to-end جدید، account auth جدید، physical Box/RAM/startup عددی، بزرگ‌ترین
فونت سیستمی و تمام حالات offline تست نشده‌اند. دستور adb نصب جایگزین، نه تست
مستقل وضعیت auth همه Providerها. کم‌مصرف‌بودن از lazy/background/bounded design
پشتیبانی می‌شود؛ ادعای benchmark نداریم. مرجع جامع Marvel/DC هنوز کامل نیست؛
year-only chronology دقیق ندارد. انتخاب فصل خودکار، progress و جستجو از کارت
unmatched هنوز اضافه نشده‌اند. Commit/Push/Tag/GitHub Release انجام نشده.

## QA مرحلهٔ دوم — تطبیق واقعی و محل جدید بنرها

مرجع نهایی همین بخش است؛ SHA/آمار بخش اول بالا تاریخی‌اند.
Gate نهایی:115تست، صفر failure/error؛ Build/Lint موفق، صفر Error و130Warning.
دو خطای compile در حین توسعه (enabled ناموجود و this مربوط به CoroutineScope)
پیش از نصب رفع شدند؛ جزو baseline نبودند و Gate نهایی آن‌ها را ندارد.

356جستجو،178روی FilmRooz و178روی MyMoviz، با همان SiteSearchEngine واقعی
انجام شد؛ صفر query error. checkpoint/cancel و ادامه پس از restart/install
حفظ شدند؛ جستجوهای موفق24h دوباره اجرا نشدند. ابتدا344query و سپس aliasهای
تأییدشده اجرا شدند. نتیجهٔ ممیزی نهاییِ خود کد روی دستگاه:
211رکورد مرجع،211match، unmatched=[]؛ این شمار شامل فصل‌های تکرارشوندهٔ یک
سریال است و «۲۱۱فیلم» یا «تمام تاریخ Marvel/DC» نیست.

نام‌های One-Shot، Skeleton Crew، تفاوت تولید2011/اکران2012 Ghost Rider و
تعارض Guardians2014 با metadata2017/IMDbVol2 اصلاح شدند. Guardians به منبع
هم‌هویت وصل شد، نه اینکه IMDb متناقض را به زور ادغام کند. The Gifted دو صفحهٔ
FilmRooz با اسم/سال2017 یکسان داشت؛ خواندن واقعی detail با Loader موجود
tt4396630/آمریکا وtt9136312/تایلند را نشان داد. فقط نسخهٔ آمریکایی عضو X-Men
شد. duplicateهای یک Provider بدون detail id، match قطعی معرفی نمی‌شوند.

Screenshot محل بنرها زیر هر دو ردیف featured:
/private/tmp/aminema-final-entry2.png
Screenshot ممیزی کامل:
/private/tmp/aminema-211-final.png
App فایل‌های journey_lookup.json و journey_lookup_audit.json را نگه می‌دارد؛
این فایل‌های runtime داخل APK/Source Bundle تعبیه نشده‌اند. روی TV جدید/بدون
این کش، باید دکمهٔ «تطبیق همهٔ فهرست‌ها با سایت‌ها» یک بار اجرا شود. cache
خود امولاتور به repo یا GitHub export نشده و هیچ credential منتشر نمی‌شود.

DNS قبل از QA خراب بود: unknown host هر دو سایت، IP ping موفق و MacHTTP200.
همان AVD با DNS1.1.1.1,8.8.8.8 و no-snapshot-load، بدون wipe، restart شد؛ DNS
پس از آن موفق بود. Browser/cookies/library حذف نشدند. پس از install-r گاهی
Launcher جلوتر از Activity بود؛ am start-W دوباره و ResumedActivity واقعی پیش
از تعامل کنترل شدند، crash buffer خالی بود. این رفتار install/test harness
به‌عنوان کرش اپ یا benchmark startup گزارش نشده است.

APK نهایی:/private/tmp/Aminema-0.20.0-final-debug.apk
Size:41422805bytes
SHA256:1dab77a44341c3ff0d3d62d167dda58dad1b544b50868059c43b5367a41466f6
Certificate SHA256 همان ba6ac8c4c2e1828462e7a6b122ad60856a054a18389af36bc001a1ee38ba13d3.
code55/name0.20.0؛ روی امولاتور install-r شد. فایل candidate مرحلهٔ اول در tmp
حفظ شده، جای آن overwrite نشده. Commit/Push/Tag/Release همچنان انجام نشده.

matched یعنی صفحهٔ عنوان درست، نه تایید موجودبودن/پخش تک‌تک فصل‌ها، تمام زبان‌ها
و کیفیت‌ها یا stream end-to-end. Physical TV، بزرگ‌ترین فونت، تمام حالت‌های
offline و benchmark RAM/سرعت عددی هنوز تایید نشده‌اند. دادهٔ مرجع تاریخی ناقص
است؛ dateهای year-only هنوز ترتیب روزانه را اثبات نمی‌کنند.

آخرین تغییر فقط MainActivity بود: bestItems روی Dispatchers.Default محاسبه
می‌شود، نه روی Main؛ الگوریتم/دادهٔ211match تغییر نکرد. Gate115تست/بیلد/Lint
دوباره پاس و APK نهایی install-r شد؛ candidateهای قبلی در tmp حفظ شدند.
آزمون Native واقعی: ماراتون Middle-earth شش‌ازشش نشان داد؛ Fellowship2001
از کارت به SpotlightActivity رفت، پوستر/IMDb/دوبله و دو Source دیده شدند:
/private/tmp/aminema-middleearth-matched.png
/private/tmp/aminema-fellowship-open.png
در این تست دکمهٔ تماشا زده نشد؛ نتیجه را موفقیت stream/fullscreen معرفی نکنید.
