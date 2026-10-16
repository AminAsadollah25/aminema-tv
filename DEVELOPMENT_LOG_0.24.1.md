# Aminema 0.24.1 / code61 — Candidate محلی، 2026-10-16

هدف مالک: ابتدا پایداری/کندی، سپس بازگشت به تحقیق یوتیوب و ad-block.
این مرحله اجازهٔ commit/push/tag/release ندارد. نسخهٔ عمومی همچنان0.24.0 است.
فایل‌های dirty پژوهش/مستندات و اسکریپت‌های قبلی حفظ شدند؛ در این مرحله منتشر نشده‌اند.

## علت‌ها و اصلاح‌های محدود

ادامهٔ همین Candidate به دستور مالک برای بستن گیت انتشار: Home اکنون collectorها
را lifecycle-aware دارد، چرخش Hero فقط در RESUMED مجاز است و clock greeting در
پس‌زمینه متوقف می‌شود. آرشیو/کتابخانه/فوکوس reset نمی‌شوند؛ بازگشت آخرین state
را می‌گیرد. این کاهش کار مخفی است، نه نسبت‌دادن قطعی ANR قدیمی به همین علت.

1. کش خالی ایرانی بعد از مهاجرت: auto-sync عمومی عمداً به‌علت WebView/jank خاموش بود
   و Home ردیف خالی را مخفی می‌کرد. `CatalogBootstrapPolicy` فقط پس از آماده‌شدن
   کش کامل، حضور منبع farsiland، پایان intro، RESUMED بودن Home و1500ms فرصت رسم،
   یک دریافت صفحهٔ اول می‌خواهد. وجود هر ردیف ایرانی مانع دریافت خودکار است.
   تلاش در savedInstanceState حفظ می‌شود؛ شکست باعث retry بی‌پایان نیست.
2. `CatalogBackgroundSync`: فقط برای FarsiLand، `loadDataWithBaseURL` با HTML خالی
   و HTTPS origin واقعی جای رندر Home سایت را گرفت. همان چهار مسیر عمومی و
   DOMParser قبلی؛ تصویر شبکه‌ای در این میزبان غیرفعال و تأخیر1200ms حذف شد.
   درخواست‌های آرشیو در دو جفت، حداکثر دو هم‌زمان؛ نه crawl کل آرشیو.
3. `FarsiLandAsyncGuard`: epoch/request ticket برای inspect و recovery. پاسخ قبلی
   پس از navigation/retry/pause/destroy یا watchdog معتبر نیست؛ timeout قدیمی
   نمی‌تواند recovery جدید را fail کند. JS در حال اجرا لغو نمی‌شود؛ فقط اعمال
   پاسخ/timeout قدیمی جلوگیری می‌شود. تست قطعی race، جای اثبات هر خطای شبکه نیست.
4. Player تعویض‌شده latch کلیک نسخهٔ قبلی را به ارث نمی‌برد؛ کلاس theater فقط
   یک بار روی قاب افزوده می‌شود. Pause دستی، gate و محدودیت2160 دست‌نخورده‌اند.
5. باگ DPAD روی امولاتور بازتولید شد: WebView OnKeyListener به فوکوس وابسته بود،
   CENTER گاهی پخش را متوقف نمی‌کرد. کلیدهای CENTER/Enter/Media PlayPause در
   dispatchKeyEvent و فقط برای player فعال، بدون خطا/gate، دریافت می‌شوند.
   listener قبلی حذف شد تا toggle دوبار رخ ندهد. RestrictedApi فقط روی همین
   override، مطابق قرارداد موجود Main/Browser، suppress شد؛ lint عمومی خاموش نشد.
6. `EpisodeLoader`: فقط FarsiLand از میزبان خالی با origin صفحهٔ واقعی استفاده
   می‌کند؛ HTML عمومی عنوان fetch و rows قبلی parse می‌شوند. fallback عنوان+ALL
   و timeout8s هر fetch حفظ است؛ URL قسمت تولید/حدس زده نمی‌شود. تصاویر میزبان
   decode نمی‌شوند و delay اضافی حذف شد. Bridge به WebView منبع متصل است تا
   پاسخ late پس از load بعدی روی آن اعمال نشود. سایر providerها همان مسیر قبلی‌اند.
7. گزارش debug وضعیت paused اکنون مقدار واقعی paused را همراه state برمی‌گرداند؛
   قبلاً نبود فیلد باعث چاپ false حتی برای phase=paused بود.
8. تشخیص ANR فقط با profile_startup=true در DEBUG/API30+: ApplicationExitInfo
   خود برنامه، thread پس‌زمینه، حداکثر40 frame از main؛ نه trace خام، URL، cookie
   یا token. در شروع عادی اجرا نمی‌شود. مرجع API:
   https://developer.android.com/reference/android/app/ApplicationExitInfo#getTraceInputStream()

## حفاظت و محدودیت

### کشف تکمیلی کوری9 و پلیر جایگزین

مقایسهٔ عادی مرورگر، پخش واقعی1080 با Player2 را نشان داد؛ اپ در Player1
رویready1/time0 می‌ماند. recovery قبلی فقط Player1 را پیشنهاد می‌کرد، بنابراین
اگر همین پلیر از قبل فعال بود هیچ موتور جایگزینی انتخاب نمی‌شد. اکنون فقط دو
کنترل رسمی و واقعاً مشاهده‌شده Player1/2 فهرست می‌شوند؛ گزینهٔ فعال نیز ثبت
می‌شود تا به موتور شکست‌خورده برنگردیم. هر کدام حداکثر یک بار در هر begin؛
اگر هر دو امتحان شده‌اند همان fallback کیفیت قبلی ادامه می‌یابد. سقف کل120s،
مهلت هر مرحله45s، gate و سیاست کیفیت/2160 تغییر نکردند. Player3 خودکار اضافه
نشد و هیچ src/currentSrc/href/onclick یا نشانی فایل خوانده نمی‌شود. سه unit
جدید و fixture کنترل‌های پنهان/نام نامعتبر/active اضافه شد. نتیجهٔ runtime
این اصلاح و APK نهایی باید از TEST_REPORT_0.24.1.md خوانده شود.

بسته/امضای debug ثابت؛ فقط install-r؛ هیچ clear/uninstall/logout/wipe انجام نشد.
نشانی media/manifest/src/currentSrc، cookie و token خوانده یا ثبت نشده‌اند.
هیچ auth/access gate دور زده نشد. انتخاب کیفیت و پخش فقط کنترل رسمی سایت است.
متادیتای عمومی/صفحهٔ عادی با لینک فایل یا تضمین سلامت فایل یکی نیست.

## یافتهٔ حل‌نشدهٔ QA

آخرین گیت153تست/سهfixtureJS/lint/build موفق و APK عادیSHAad147273… است؛
TEST_REPORT_0.24.1.md انتهای سند جزئیات نهایی دارد. bootstrap خالی در کاربر
جدا و COLD start دوم تأیید شد؛ user10 موقت پس از تست حذف و Owner حفظ شد.
24چرخهٔ جزئیات→Home بدون ANR تازه. گالیله خودکار در720 روی آخرین کد هم
زمان جلو برد وPause/Resume درست بود، ولی انتظار مسیر با تعویض موتور حدود114s
ماند. کوری9 هنوز شروع نشد؛ مقایسه مرورگر دلیل خرابی فایل نیست. پاراگراف‌های
زیر یافته‌های اولیه‌اند؛ first-run و گالیله اکنون شاهد تکمیلی دارند، نه کوری9.

ANR روی Candidate اولیه، هنگام Spotlight→Home در11:16:45 ثبت شد:
FocusEvent(hasFocus=false)، بیش از5002ms؛ سپس process3159 متوقف شد.
گزارش CPU بار بالای اپ/سیستم دارد، اما علت را صرفاً حافظه/شبکه فرض نمی‌کنیم.
AndroidRuntime fatal exception ثبت نشد. shell به trace مجوز نداشت؛ adbd این
AVD production روت نمی‌شود. API خود اپ رکورد ANR یافت، ولی در تلاش نخست
frame قابل‌استفاده نداد. تکرار Back بعدی موفق بود؛ این رفع قطعی ANR نیست.
قبل از انتشار، تکرار سریع رفت‌وبرگشت و بررسی گزارش جدید لازم است.
قسمت9کوری، fallback خودکار گالیله، بافر بعضی فایل‌ها و QA اولین‌بار کش خالی
تا شاهد تازه، حل‌شده محسوب نشوند. نتایج مرجع در TEST_REPORT_0.24.1.md.
