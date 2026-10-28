# QA Candidate0.24.4/code64 — 2026-10-28

وضعیت: گیت خودکار و تست زندهٔ اصلاح نهایی و نمونهٔ دوم موفق؛ آمادهٔ تست مالک روی باکس واقعی، نه تضمین تمام شرایط.
هیچ Commit/Push/Tag/Release تازه انجام نشده است. بخش‌های پایین تاریخچهٔ مراحل میانی‌اند.

## نتیجهٔ آخرین APK — 2026-10-28

- ۱۸۲ تست واحد، صفر failure/error؛ ۲۵ fixture جاوااسکریپت موفق؛ lint صفر error و ۱۳۵ warning موجود؛ build موفق (۲ دقیقه و ۲۲ ثانیه).
- package: `com.amin.tvos.debug`، versionName `0.24.4`، code64. نصب فقط `adb install -r`؛ هیچ ورود/کش/داده‌ای پاک نشد.
- امضای ثابت: `ba6ac8c4c2e1828462e7a6b122ad60856a054a18389af36bc001a1ee38ba13d3`.
- SHA256 آخرین APK: `987ee116f5dd009f27097e9af4173b0920843287b3f984c8836d7406564cba8f`؛ 31,728,747 بایت.
- انتخاب استخوان‌سوز/فصل۱/قسمت۱ از UI؛ تطبیق عمومی نام/قسمت در هر دو سایت مستقل بررسی شد. آدرس مدیا خوانده نشد.
- شروع خودکار1080: time9.89→33.92، ready4/error0، بدون فرمان Play از ابزار.
- قاب پلیر دقیقاً x0/y0/w960/h540 در viewport960×540؛ تصویر1920×1080. parent همان `.video-container` ماند، نه body.
- علت قاب نامناسبِ candidate قبلی، `backdrop-filter` والد `.dtsingle` بود؛ نه اثبات قدیمی‌بودن WebView یا پشتیبانی‌نکردن inset. اصلاح فقط خنثی‌سازی backdrop والدهای پلیر و CSS fixed صریح است.
- Pause با دکمهٔ واقعیِ پایین‌چپ: time61.12/userPausedtrue. seek آزمایشی به82.87 و کنترل رسمی720 (کلیک تشخیصی DOM) انجام شد؛ Pause حفظ شد.
- Android Settings با action رسمی باز و با Back بسته شد؛ suspendedtrue در پس‌زمینه، time82.87 ثابت و مکث دستی پس از برگشت حفظ شد؛ تنظیمی تغییر نکرد.
- Resume با DPAD_CENTER: گیر آماده‌سازی720/ready1/seekingtrue در82.87 بازتولید شد. **بدون دستور کمکی QA**، اپ پس از حدود۲۰ثانیه فقط یک بار به79 برگشت؛ سپس ready4، time80.65→90.77، کیفیت720 ثابت و error0. لاگ داخلی `stall-retry-backwards-seek` تأیید شد؛ fallback کیفیت در این نوبت لازم نشد.
- seek معمول به میانه1994 موفق؛ time1996.87→2012.89/ready4/error0 و تصویر واقعی میانه دیده شد. callback داخلی سایت پس از seek یک مکث کوتاه داشت و خود سایت ادامه داد؛ ادعای «هیچ pause داخلی» نمی‌شود.
- ابزارها فقط عدد/boolean، عنوان و لینک عمومی، و CSS را خواندند؛ هیچ media URL/manifest/cookie/token ذخیره یا منتقل نشد. امولاتور API36/softwareGPU/no-audio؛ تأیید شنیداری یا USBMouse فیزیکی نیست.
- شواهد محلی: `/private/tmp/aminema-0244-final-start-real.jsonl`، `aminema-0244-final-recovery-pass.jsonl`، `aminema-0244-final-middle.jsonl` و تصاویر `final-fullscreen.png`/`final-middle-screen.png` با همان پیشوند.

## تاریخچهٔ تست‌های میانی — نتیجهٔ نهایی بالا مقدم است

### رگرسیون نهایی تاکسی قسمت۲ و خروج

قسمت۲ از ردیف دیده‌شده با فوکوس/تأیید ریموت انتخاب شد. تلاش‌های اولیهٔ tap که Activity پلیر را باز نکردند و observer فقطno-allowed-page داشت، اصلاً شروع پخش یا زمان تأخیر پلیر حساب نشدند؛ آن زمان هنوز Spotlight فعال بود. ادعای تأیید تمام تعامل‌های کارت با USBmouse نمی‌شود.

- شروع خودکار1080/ready4/error0: time22.53→46.55، بدون Play کمکی.
- seek عادی به میانه998: time1000.91→1016.98/ready4/error0. callback سایت پس ازseek مکث کوتاه داشت و خود ادامه داد؛ انتظار زنجیره‌ای طولانی قبلی تکرار نشد.
- DPAD_CENTER: time1113.5/pausedtrue/userPausedtrue دردو مشاهده ثابت؛ Resume واقعی1115.57/pausedfalse/userPausedfalse.
- Back بهSpotlight برگشت و فهرست WebViewهای تشخیصی `[]` شد؛ پلیر باقی نماند. forwardهای موقت55799/56553 حذف شدند، نه نشست یا داده اپ.
- شواهد: `/private/tmp/aminema-0244-final-taxi-real.jsonl` و `/private/tmp/aminema-0244-final-taxi-middle.jsonl`.
- APK دقیق تست‌شده با کپی بایت‌یکسان در `outputs/Aminema-0.24.4-debug.apk` و checksum کناری آماده است؛ هیچ انتشار GitHub انجام نشد. مرجع امضا/SHA/اندازه بخش نتیجهٔآخرینAPK بالاست.

### ادامهٔ تاریخچهٔ میانی

## گیت خودکار و نصب

- baseline0.24.3:174unit/lint/build موفق. fixture تازهٔvideo replacement هنگامPause قبل‌ازfix
  شکستِواقعیfalse!==true داشت؛بعداصلاح پاس شد.
-174unit/صفرfailure/error و۱۸fixtureJS موفق؛regressionهایad/gate/quality/intent/theater ثابت.
-test/lint/assembleDebug باBUILD SUCCESSFUL در2m38s؛امضایAPK همانdebugپیشین:
 ba6ac8c4c2e1828462e7a6b122ad60856a054a18389af36bc001a1ee38ba13d3.
-install-r موفق؛packagecom.amin.tvos.debug وcode64؛بدونclear/uninstall/wipe/logout،بدون انتقال
 cookie/token/mediaURL. ابزارهایQA فقطCDPمحلی و/private/tmp،نه داخلAPK. API36/softwareGPU/no-audio.

## مشاهدهٔموس درbaseline0.24.3

کلیکِوسطِدرحال‌پخش فقطکنترل‌ها را آشکار می‌کرد؛دکمهٔواقعیPause درمختصات اندازه‌گیری‌شده
کلیک شد وtime107.26 ثابت ماند؛Playمرکزی پس‌ازآن به107.69 جلو رفت. ورودیtrusted وپاسخ واقعی
رصدشد. این گزارش ادعایقبلیِ«موس کارنمی‌کند» را اصلاح می‌کند؛رفتار طبیعیtouch پلیر است.
USBMouseفیزیکی تست نشده است. توضیح روش/راهنمایUI درDEVELOPMENT_LOG_0.24.4.md.

## استخوان‌سوز قسمت۱ روی0.24.4

-کارت/قسمت واقعی دیده وکلیک شد؛public-match موفق و1080 واقعی/1920×1080/error0.
 observer بخشnativeانتخاب راno-allowed-page ثبت کرد؛این زمان مربوط بهPlayer نیست.
-ازplayer باtime0.08 سپس3.84→6.1→35.92→62.81 بدون فرمانPlay ابزار جلو رفت؛
 pause/playهای داخلی کوتاه آغاز ازcallbackسایت باقی‌اند،نه انتظار۳۰ثانیه‌ای قبلی.
-سپسباUIواقعی نوارکنترل وPause کلیک شد:time82.87/pausedtrue/userPausedtrue.
 ازgear→Quality→720بامختصات منوی باز وکلیک واقعی رفتیم؛720selected،videoHeight720،
 time82.87 وpausedtrue ماند؛نه بازگشت اجباری به1080 و نهPlay ناخواسته.
 videoIdتشخیصی1 ماند؛این quality change واقعی **تعویض objectvideo نبود**؛تعویضobject درfixture پوشش دارد.
-درهمینPause،AndroidSettings ازActionرسمی بازشد؛suspendedtrue وtime82.87 ثابت ماند.
 Back پس‌ازانیمیشن واقعاًبهFarsiLandActivity برگشت؛suspendedfalse،userPausedtrue،time82.87 ثابت.
 هیچ تنظیمی تغییر نکرد؛این اثبات شنیداری نشت صدا نیست.

## ادامهٔکیفیت/تکرار/Taxi/خروج/Artifact

درResume720ازtime82.87،۲۴ثانیه مشاهده ready1/pausedfalse/timeثابت داشت؛قبولی نیست.
سپسProgressGuard/Recoveryاضافهشد؛۱۸۲unit/۲۱JS/lint0error135warning/buildموفق.
درAPKاین‌مرحله کیفیت720درPause نگه‌داشته وResumeشد؛پس‌از۲۰ثانیه،stall-fallback-quality=1080
واقعاًثبت وکنترل1080انتخاب شد،امامدیا بازدر82.87/ready1گیرماند. درحدود۴۵ثانیهٔبعدی
Player1ازمسیررسمی recoveryقبلی امتحانشد؛**نبایدگفت fallbackپخش را موفق برگرداند.**

آخریناصلاح درحالساخت/QA:حذفDOMreparentبرایتمام‌صفحه،حفظهمانparentپلیرومحلvideo.
فرضیهٔاختلالseekباdetachقطعی نیست؛نتیجهٔاینbuildهنوزباید ثبتشود.نسخهٔقبل موفقه خوانده نشود.

## حدود

تست0.24.3درگزارش مستقل است و نباید همهٔنتایجش بهAPK0.24.4 نسبت داده شود.
تمام‌آرشیو/تمام‌اثر/ad-freeهمیشگی/TVBoxفیزیکی یا صدا تضمین نیست. login/DRM/accessgate حفظ شد.
تغییرات محلیYouTube/پژوهش ونسخه‌های قدیمی حفظ ودرهیچCommitجدید وارد نشدند.
