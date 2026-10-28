# QA Candidate 0.24.2 / code62 — 2026-10-28

مجوز تازهٔ انتشار: مالک پس از تحویل APK گفت «خب روی گیت هاب پابلیشش کن دیگه».
نتیجهٔ واقعی انتشار پس از تأیید سرور در RELEASE_REPORT_0.24.2.md ثبت می‌شود؛
متن محلی/بدون انتشار در ادامه، وضعیت زمان انجام QA را توصیف می‌کند.

وضعیت: محلی و بدون انتشار. GitHub همچنان نسخهٔ منتشرشدهٔ قبلی است؛ این سند
به معنی commit/push/tag/release نیست. خروجی نهایی پس از حذف ابزار QA در انتها ثبت می‌شود.

## baseline و بازتولید

- baseline بدون تغییر: testDebugUnitTest/lintDebug/assembleDebug موفق، بخش عمده UP-TO-DATE؛
  مجموعهٔ قبلی153تست. هیچ داده یا نشست پاک نشد. AVD موجود بدون wipe راه‌اندازی شد.
- FarsiLand/Harchi To Begi → farsiland.buzz کیفیت480/720/1080؛ oldQualityCount=0 چون
  .btn-row حذف شده بود. اپ روی phase=quality باقی می‌ماند؛ انتخاب تشخیصی کنترل واقعی1080
  انجام شد و به مسیر Buzz/movies رفت که policy موجود مسدودش می‌کند. در مرورگر نیز همان
  مسیر JW خطای102630 داد. این مشاهده مساوی خرابی فایل یا کل سایت FarsiPlex نیست.
- تلاش am-start مستقیم Activity غیرexported با Permission Denial مواجه شد؛ خطای روش
  تست بود، نه شکست محصول. هیچ Activity تولیدی برای رفع آن exported نشد؛ ابزار موقت
  Instrumentation در debug فقط Activity واقعی خود اپ را با همان UID باز کرد.
- بررسی فرضیهٔ popup/POST شاهد کافی نداشت؛ تغییر معماری پنجره/کپی payload پیاده نشد.

## تست‌های خودکار فعلی

- ۱۶۸ تست واحد،0failure/0error. پوشش تازه: مرز URL، عنوان دقیق/نوع، رد ابهام و لینک
  دارایquery، فصل/قسمت یکتا، عدم حدس قسمت اول، تضاد عنوان ALL و heading ناقص تاکسی۲.
- ۹ fixture JS مستقل در research/farsiland-playback-regression-20261028.mjs پاس شد:
  کیفیت بدونwrapper، شروعJW پیش ازvideo، توقف رویgate، HD قسمت واقعی، کیفیت مجاز و
  انتخاب فقط یک بار در هر سند حتی پس از تعویض ویدئو، حفظ Pause، خطای واقعی و theater.
- lint/build موفق؛ شمار هشدار و SHA فایل عادی نهایی در بخش خروجی نهایی ثبت می‌شود.
  fixture جای تست پخش/تبلیغ زنده را نمی‌گیرد. یک failure اولیهٔ mock به‌علت play() ساختگی
  که paused را تغییر نمی‌داد اصلاح شد؛ این failure محصول نبود.

## QA واقعی روی امولاتور موجود

API36/Television_1080p/1920×1080/software GPU؛ emulator با no-audio اجرا شد، پس
نشت‌نکردن صدا شنیداری یا رفتار باکس واقعی از این تست نتیجه‌گیری نمی‌شود.

### فیلم Harchi To Begi

- public-page-match=true؛ از صفحهٔ عادی عنوان FP و HD واقعی به پلیر جدید رفت، نه Buzz.
- duration4520،decode1280×720؛ درخواست seek واقعی به50% و سپس2265→2295/ready4/error0.
- تست CENTER/MediaPlayPause اجرا شد؛ شاهد درخواست دکمه را با موفقیت مکث اشتباه نگیرید.
  گذارهای اولیهٔ paused در راه‌اندازی/تعویض پلیر نیز ثبت شدند. توقف درفریم۱ مرحلهٔ قبلی
  با موفقیت تماشای میانه در مرحلهٔ بعد تفاوت دارد؛ علت همهٔ pauseهای سایت قطعی نیست.
- این یک نمونهٔ آغاز/میانه است، نه تماشای کامل یا گواه کل آرشیو.

### نمونهٔ دقیق مالک: Taxi قسمت۲

- FL h1 برابر «Season Episode» و ناقص، اما h2 واقعی Taxi EP02 و ALL=Taxi است.
  FP search صفحهٔ Taxi واقعی را داد؛ ردیف1-2 همان URL واقعی /episode/taxi-ep2/ را معرفی کرد.
  اختلاف ep02/ep2 با ساختن slug رفع نشد؛ لینک واقعاً از فهرست سریال گرفته شد.
- public-page-match=true؛ پخش1080 واقعی1920×1080،duration1997،error0؛ پس از seek
  به میانه، زمان1001→1062/ready4 جلو رفت. تصویر واقعیِ تمام‌صفحه بدون هدر سایت دیده شد:
  /private/tmp/aminema-farsi-fix.10aeWc/taxi2.png .
- بعد از seek یک paused واقعی998 ثبت شد و CENTER دوباره پخش را از سر گرفت؛ پیام ابزار
  centerPauseRequested فقط قصد ابزار بود و در آن لحظه عملاً Resume انجام داد. بنابراین
  از این مورد «CENTER حتماً Pause کرد» استنباط نشود. MediaResume در این نوبت درخواست نشد.
- تکرار نهایی روی آخرین منطق کیفیت/theater بدون هیچ دستور seek/quality/play از ابزار
  انجام شد: PID4617،1080 واقعی،error0،زمان9→11→13→15→19→23→25. ready2/بافر مقطعی
  هم دیده شد، پس ادعای بی‌بافربودن یا شروع فوری نداریم. مرحلهٔ اولیه paused نیز داشت،
  ولی بدون فرمان کنترل از ابزار به پخش جلو‌رونده رسید. این آخرین نوبت فقط آغاز را می‌آزماید؛
  تست میانه مربوط به نوبت قبل است. کد runtime بعد از آن تغییر نکرد؛ فقط runner حذف شد.
- screenshot دیرهنگامِ نوبت نهایی پس از پایان Instrumentation گرفته شد و Launcher بود؛
  به after-qa-launcher.png تغییرنام داده شد و **شاهد پخش نیست**. تصویر معتبر پخش میانهٔ
  تاکسی در artifact محلی Aminema-0.24.2-Taxi2-QA.png حفظ شده است.

## حفظ وضعیت و حدود

فقط install-r با package/signature قبلی؛ بدون clear/uninstall/wipe/logout، بدون انتقال
cookie/account به درخواست‌های عمومی متادیتا. providerهای خارجی/یوتیوب/آرشیو/Library
تغییر نکردند. این «تأیید دوبارهٔ لاگین زندهٔ همهٔ providerها» نیست.
سرور قدیمی برای بعضی عنوان‌های غیرمشترک ممکن است همچنان معیوب بماند؛ نبود تطبیق معتبر
به معنی اجازهٔ حدس لینک یا انتخاب فیلم/قسمت دیگر نیست. Full-archive،محدودیت24h سایت،
تبلیغ وسط/پایان اثر و TVBox فیزیکی تضمین نشدند. Access gate دور زده/پنهان نشد.

## خروجی نهایی

- ابزار موقت FarsiPlaybackQa و manifest debug حذف شدند؛ کلاس در DEX و instrumentation
  در manifest نهایی وجود ندارد. پس از install-r، pm list instrumentation نیز فاقد آن است.
- گیت نهایی پس از حذف ابزار: testDebugUnitTest/lintDebug/assembleDebug با BUILD SUCCESSFUL؛
 168تست/0failure/0error،lint0error/135warning. 9fixtureJS نیز موفق‌اند. هشدارهای قدیمی خاموش نشدند.
- APK عادی:31,728,851بایت؛ SHA256:
 `ed0150cf032637cff2c92d35dd555e17256e452baa950f05feb4a42f1e27a551`.
- package=com.amin.tvos.debug،versionName0.24.2/versionCode62،امضای قبلی:
 `ba6ac8c4c2e1828462e7a6b122ad60856a054a18389af36bc001a1ee38ba13d3`.
- APK با install-r روی Owner موجود نصب شد؛ MainActivity و Home واقعی با Hero/پوستر/فونت
  دیده شدند. intro12,463,177بایت،فونت‌ها و assets اصلی در APK بررسی شدند. هیچ تغییر packaging/
  dependency یا پروژهٔ سبک‌سازی در این task انجام نشد؛ کاهش حجم نسبت به گزارش انتشار قبلی
  به این fix نسبت داده نشود، بنچمارک اندازه/علت کاهش انجام نشده است.
- در بافر boot فعلی AndroidRuntime fatal متعلق به اپ دیده نشد؛ این اثبات نبود هر ANR/
  ایراد روی باکس واقعی نیست. پخش تست با پایان runner متوقف شد و در تحویل Home عادی باز است.
- فایل نصب محلی:
  /Users/aminasadollah/Documents/Codex/2026-07-24/amin-tv-os-application/outputs/Aminema-0.24.2-debug.apk
  و فایل هم‌نام .apk.sha256. تصویر Taxi۲ در همان outputs/Aminema-0.24.2-Taxi2-QA.png.
- GitHub/Commit/Tag/Push/Release انجام نشده‌اند؛ این Candidate محلی است، نه انتشار عمومی.
