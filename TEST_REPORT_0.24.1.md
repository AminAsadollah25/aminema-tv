# QA — Candidate محلی0.24.1/code61

وضعیت جدیدتر: پس از این QA،مالک صریحاً انتشار را خواست و همان APK منتشر شد؛
RELEASE_REPORT_0.24.1.md مرجع واقعی Commit/Tag/Latest/نصب مجدد دارایی دانلودشده
است. متن‌های «منتشر نشده» زیر snapshot زمانی QA هستند، نه منع انتشار فعلی.

## ادامهٔ گیت انتشار — 2026-10-16، محلی و بدون انتشار

- Home:9collector به lifecycle متصل، گردش Hero و clock فقط در RESUMED.
  12چرخه پیش و12چرخه پس از اصلاح، همگی جزئیات→خانه موفق؛ ANR تازه ثبت نشد.
  زمان دور شامل launch/metadata/مکث است، نه زمان خالص Back. علت/رفع قطعی ANR
  قدیمی11:16:45 همچنان ثابت نیست؛ کار مخفی Home کاهش یافته، نه تضمین حذف هنگ.
- گالیله خودکار:1080 رویready1/time0، سپس720 و decode1280×720،time2.06→12.31؛
  تیتراژ واقعی تمام‌صفحه و Back تأیید شد. حدود68s انتظار؛ نه تضمین سرعت کل آرشیو.
- کوری9 پیش از اصلاح جدید:12قسمت واقعی از EpisodeLoader؛ انتخاب9،1080→720→480
  درهمهready1/time0، سپس deadline حدود123s. این شکست واقعی، نه خطای ابزار است.
  مرورگر ابتدا با محدودیت review ابزار باز نشد؛ پس از زمان اعلام‌شده، همان
  فراخوانی مجاز شد. از لینک Episode9→HD→1080 و مرحلهٔ رسمی «View a short ad»
  به پخش رسید:Player2 فعال(nonex)،1920×1080،duration3091.712،time29.96→121.10
  و تصویر واقعی زن کنار پنجره پس از Seek دیده شد. Pause و بستن تب انجام شد.
  پس شکست اپ را به خرابی فایل نسبت ندهید. نسخهٔ قبلی recovery فقطPlayer1
  را می‌شناخت؛ اصلاح محدود Player1/2 با ثبت فعال/امتحان‌شده در QA واقعاً بین
  دو موتور جابه‌جا شد ولی به‌تنهایی کوری9 را حل نکرد. در هر دو موتور فقط ویدئوی
  اصلی آمادهٔ metadata بود؛ نمونهٔ2 نیزready1/time0/network1/error0، بافر تا2.624s
  و gate متن top-document=false داشت. این رد همهٔ iframeهای دسترسی نیست.
  آزمایش تشخیصی جدا:bufferStart=0.041992،seek به0.091992؛ همانready1 و زمان
  ثابت ماند. این seek درمان نبود و به production اضافه نشده است. native Back
  در runner تک‌Activity پس از1.5s هنوزdestroyed=false داد؛ پس از پایان runner
  Launcher resumed و ANR جدیدی نبود؛ آن نمونه تأیید Back→Home/navigator نیست.
- first-run واقعی PASS: AVD جدید به حداقل فضای7.37GB نیاز داشت و boot نشد؛
  به جای حذف دادهٔ مالک، کاربر مجزای AminemaIsolatedQA/id10 در همان AVD ساخته شد.
  قبل از launch،files وجود نداشت. APK عادی SHA2ad8863ca352c8947ebdfea681523c71e7b594658d661a92404775354ac9d7f0
  بدون refresh دستی در16:12:26.702→16:12:28.660،pages1،60فیلم+60سریال/120all
  و120نشانی پوستر ذخیره کرد. Hero ایرانی/پوستر خواب‌نما واقعاً روی Home دیده شد.
  پس از force-stop فقط کاربر10 و COLD start، همین کش حفظ و start/result تازه‌ای
  در لاگ دیده نشد. این first-run خالی است؛ upgrade با کش خارجیِ تنها صرفاً unit
  پوشش دارد. کد bootstrap پس از این آزمون تغییر نکرد. سپس Owner0 باز شد؛ کاربر10
  با remove-user --wait حذف شد. داده/ورود Owner پاک نشد؛ شمار خارجی39/92 و638/614
  و ایرانی120/119 محفوظ بود. هیچ فایل/AVD اصلی برای آزادکردن فضا حذف نشد.
- ابزار QA قدیمی برای navigation یک timeout و delegate-reflection error داشت؛
  ابزار جدید نیز یک خطای compile در ثابت Activity.RESULT_OK داشت که اصلاح شد؛
  هیچ‌کدام شکست محصول محسوب نشوند. ابزارهای موقت باید قبل APK نهایی حذف شوند.

### تاریخچهٔ مراحل قبلی این Candidate

SHA50b1fa… و اعداد150تست در بخش‌های زیر پیش از آخرین اصلاح پلیر هستند؛
خروجی معتبر جدید در بخش «خروجی نهایی پس از QA تکمیلی» انتهای سند است.

2026-10-16؛ منتشر نشده. این سند فقط شواهد همین مرحله است، نه تضمین همهٔ آرشیو.

## تست‌های خودکار

- baseline بدون تغییر runtime: testDebugUnitTest/lintDebug/assembleDebug موفق.
- پس از policy/guard:150تست، صفر failure/error/skipped.
- سه fixture آفلاین JS پاس: playback، episodes، catalog.
  شامل Pause/seek صفر، ویدئوی جایگزین، gate، ویدئوی نامرتبط، fallback با ALL
  صحیح و رد parent/title/host نامعتبر، میزبان عمومی قسمت‌ها، چهار آرشیو و سقف2درخواست.
- چند گیت ساخت موفق؛ یک lint failure حین توسعه برای RestrictedApi override جدید
  اصلاح شد؛ lintChecks خاموش یا baseline جدید ساخته نشد. هشدارهای قبلی باقی‌اند.
- آخرین گیت و SHA خروجی نهایی پس از QA در بخش پایانی این سند ثبت می‌شوند.

## امولاتور موجود و حفظ داده

Television_1080p/API36،1920×1080؛ همان profile، نصب-r و همان امضای قبلی:
`ba6ac8c4c2e1828462e7a6b122ad60856a054a18389af36bc001a1ee38ba13d3`.
داده، ورود، cookie، تنظیمات و cache حذف نشده‌اند؛ ورود مجدد درخواست نشده است.
این معادل تأیید زندهٔ احراز هویت همهٔ سرویس‌های خارجی در این مرحله نیست.

### دریافت آرشیو واقعی با میزبان جدید

روی کش موجود، هیچ bootstrap غیرضروری هنگام cold start دیده نشد.
Refresh ردیف ایرانی در11:13:18.587 شروع و11:13:22.936 نتیجه داد: حدود4.349s؛
صفحه2،60movie/60series ورودی. پس از dedup/حفظ tail:120فیلم/119سریال،pageLimit2.
خارجی‌ها همان39/92 برای FilmRooz و638/614 برای MyMoviz ماندند.
این آزمون manual refresh میزبان جدید است؛ مسیر واقعی upgrade با کش ایرانیِ
کاملاً خالی هنوز end-to-end آزمایش نشده. policy خالی/خطا/موجود/عدم‌حضور منبع
با4تست واحد پوشش دارد. AVD لاگین‌شده برای شبیه‌سازی first-run پاک نشد.

### پخش، کنترل و برگشت

- دور امیر فصل1قسمت2 از navigator native انتخاب شد؛ تصویر واقعی1920×1080
  تمام‌صفحه، بدون هدر سایت، و time9→59 در نمونهٔ اول مشاهده شد.
- CENTER در Candidate اولیه نتوانست Pause کند؛ باگ واقعی ثبت و اصلاح شد.
- پس از نصب اصلاح dispatch، همان قسمت از time5→65 پخش شد؛ CENTER روی70
  Pause کرد و time70 در سه نمونه طی حدود30s ثابت ماند. MediaPlayPause زمان
  را دوباره72→81 جلو برد. فیلد paused=false در لاگ قدیمی این نوبت، ناشی از
  نبود فیلد JSON است؛ state=paused معیار درست بود؛ آن logging نیز اصلاح شد.
- Back به navigator برگشت؛ media_session صفر شد. این تست شنیداری تخصصی نیست.
- Back بعدی به MainActivity موفق بود و ANR جدید در این تکرار ثبت نشد.
- PNGهای موقت: /private/tmp/aminema-0241-dore.png، aminema-0241-episodes.png،
  aminema-0241-back.png؛ موقت و خارج APK، نه asset محصول.
- در نمونهٔ اول بعد از خروج و هنگام Spotlight→Home، ANR واقعی11:16:45 ثبت شد
  (FocusEvent5002ms). AndroidRuntime fatal نبود؛ علت دقیق و رفع قطعی تأیید نیست.
  رکورد خود اپ قابل‌مشاهده است؛ اولین تلاش بازیابی main stack،0frame داد.

### اندازه‌گیری شروع — ادعای درصد بهبود نداریم

یک am-start-W قدیمی3027ms و یک Candidate1899ms شد؛ نمونه کافی/کنترل‌شده نیست.
Home واقعی در هر دو حدود10.2s ظاهر شد، عمدتاً پس از intro حدود9.9s.
decode/cache با بار سیستم تغییر کرد؛ اندازه آرشیو هم حین QA بزرگ‌تر شد.
امولاتور software GL و هشدار فشار حافظه دارد. سرعت دستگاه واقعی/درصد بهبود
و حذف کامل بافر ادعا نمی‌شود؛ intro/ترجیحات کاربر خودسرانه تغییر نکرد.

## موارد باز پیش از انتشار

1. تکرار ANR برگشت و stack قابل‌استفاده؛ یک تکرار موفق رفع قطعی نیست.
2. تکمیل نمونه‌های runtime بیشتر دریافت عمومی قسمت‌ها؛ fallback فان ابوطالب
   در خروجی نهایی تأیید شد (بخش زیر)، نمونهٔ مستقیم/foreign regression بیشتر لازم است.
3. قسمت9کوری و مسیر خودکار گالیله؛ موفقیت Player1 دستیِ نسخه قبلی کافی نیست.
4. first-run واقعی کش خالی در profile/AVD جدا؛ حفظ profile فعلی الزام است.
5. TV box واقعی، USB/air mouse فیزیکی، همهٔ آرشیو/تبلیغ پایان/تماشای بلندمدت.
6. YouTube/ad-block هنوز تحقیق جداست؛ در این Candidate تغییر نکرده است.

## خروجی نهایی این مرحله — 2026-10-16

گیت نهایی testDebugUnitTest/lintDebug/assembleDebug با BUILD SUCCESSFUL در46s؛
150تست،0failure/error/skipped؛ lint برابر0error/135warning است، نه صفرهشدار.
سه تست JS روی همین source پس از ساخت نیز موفق‌اند. git diff --check موفق.
APK عادی بدون instrumentation موقت،42924103بایت (نسخه عمومی قبلی42891715بایت؛
رشد32388بایت، asset سنگین تازه اضافه نشده). همان هویت/امضا، install-r موفق.
SHA256: `50b1fa9f044b8c6ba3f1a41ffad3926473247957418d7a317cc6b9d08b64dc32`.
مسیر: `/Users/aminasadollah/Library/Caches/AminemaBuild/app/outputs/apk/debug/app-debug.apk`.

فان ابوطالب روی APK نهایی از navigator واقعی: load11:32:02.627، شروع استخراج
11:32:04.633، finish11:32:07.740؛ حدود5.1s کل/3.1s استخراج و fallback.
کارت قسمت4 با پیمایش افقی واقعاً دیده شد؛ تصویر aminema-0241-final-fun-more.png
در /private/tmp. هیچ hint یا URL قسمت حدس زده نشد. تست آفلاین مسیر مستقیم
و fallback هر دو میزبان را پوشش می‌دهد؛ این runtime مخصوص fallback است.

تشخیص opt-in خود اپ: رکورد ANR قبلی tracePresent=true ولی0frame قابل‌چاپ؛
از این شاهد نمی‌توان علت دقیق را نسبت داد. پس از تکرار Back دوم و بازکردن
این سریال، رخداد ANR تازه/AndroidRuntime fatal دیده نشد؛ رفع قطعی ادعا نمی‌شود.
هیچ commit/push/tag/release انجام نشده است.

روی همین خروجی نهایی، فان ابوطالب قسمت4 از کارت واقعی انتخاب شد؛ time2→26،
تصویر واقعی تمام‌صفحه بدون هدر سایت دیده شد. CENTER پخش را درtime28 متوقف
کرد؛ state=paused و paused=true در سه نمونه طی25s ثابت بود. Back→Back سریع
به Home برگشت، رخداد ANR تازه ثبت نشد. این نمونه جای تماشای کل اثر یا
تضمین همهٔ تبلیغات را نمی‌گیرد. screenshot: /private/tmp/aminema-0241-final-fun-play.png.

تست فیلم روی همین خروجی: خواب‌نما از ردیف فیلم ایرانی→Spotlight→تماشا، بدون
انتخاب تشخیصی player؛ شروع11:36:32، time0در11:36:51 و سپس12→24→36→48.
تصویر واقعی فیلم تمام‌صفحه با نسبت محفوظ و بدون هدر/پنجره سایت دیده شد:
/private/tmp/aminema-0241-final-khabnama-frame.png. Back به Spotlight و پس از
اتمام انتقال صفحه، Back مجدد به Home؛ نشست media_session صفر بود. دو کلید
خیلی سریع ممکن است قبل از انتقال هر دو به Activity در حال بسته‌شدن برسند؛
Snapshot اولیه هنوز Spotlight بود، لذا با Back پس از انتقال جدا تکمیل شد.
تا پایان نمونه، ANR جدید یا AndroidRuntime fatal ثبت نشد.
این تست فقط ابتدای فیلم است؛ وسط/پایان و تمام آرشیو را تأیید نمی‌کند.

## خروجی نهایی پس از QA تکمیلی — 2026-10-16

- APK عادی0.24.1/code61 پس از حذف هر دو runner موقت ساخته شد؛
  testDebugUnitTest/lintDebug/assembleDebug با BUILD SUCCESSFUL در1m16s.
  153تست،0failure/error/skipped؛ lint0error/135warning؛ هر سه fixtureJS موفق.
  git diff --check موفق. manifest/DEX فاقد هر دو runner،pm list instrumentation
  خالی؛ ابزار موقت از source و APK حذف شده است.
- SHA256:`ad1472736b41e3b7d6a85031fa994179c942707b20f2ffec1f26a10b69e9c827`؛
  42930187بایت،38472بایت بیشتر از نسخهٔ عمومی0.24.0. مسیر همان APK خارج repo:
  `/Users/aminasadollah/Library/Caches/AminemaBuild/app/outputs/apk/debug/app-debug.apk`.
  aapt:com.amin.tvos.debug/versionCode61/versionName0.24.1؛ امضای قبلی ثابت.
- رگرسیون گالیله روی کد نهایی تولید(با runner موقت فقط برای مشاهده):شروعPlayer2،
  1080/time0، سپسPlayer1/1080/time0، سپس720؛ درelapsed118.90s زمان4.62 و در
  127.25s زمان12.28/ready4/decode1280×720 بود. بنابراین اولین پیشرفت پیش از
  deadline120s رخ داد و بعد پخش ادامه یافت. Pause باCENTER واقعاًpaused=true
  و طی4s زمان ثابت؛ MediaPlayPause دوباره بیش از2s زمان را جلو برد. این نوبت
  screenshot مستقل نگرفت؛ تصویر واقعی720 در نمونهٔ قبلی همین سند تأیید شده است.
  انتظار حدود114s تا اولین تصویر در این مسیر طولانی است؛ ادعای سریع‌شدن شروع
  یا بسته‌شدن مشکل بافر نمی‌کنیم. مانند runner کوری، Back→Home از این ابزار
  تک‌Activity استنباط نشود؛ ماتریس24چرخهٔ جدا مرجع navigation است.
- کوری9 با Player1 وPlayer2، کیفیت‌های1080/720 و نمونهٔ قبلی480 آغاز نشد؛
  seek کوچک تشخیصی هم درمان نبود. Chromium میزبان با مرحلهٔ رسمی دسترسی
  پخش کرد؛ سلامت همه دستگاه‌ها/خرابی فایل/علت codec یا CDN از این ثابت نیست.
  decoder سیستم c2.goldfish.h264.decoder و MIMEvideo/avc مشاهده شد؛ خطای
  MediaCodec «Media Quality Service not found» در رخدادهای عمومی سیستم هم
  وجود داشت، پس علت اختصاصی کوری تلقی نشود. برای نتیجهٔ قطعی Android به QA
  بیشتر روی WebView/TV واقعی نیاز است؛ فایل/manifest/source استخراج نشده است.
- Candidate محلی باقی است؛ هیچ commit/push/tag/GitHubRelease انجام نشده.
  کش خالی و24چرخه Back پوشش داده شدند، ولی کوری9 و علت ANR اولیه بازند؛
  «همه‌چیز بدون باگ/آماده انتشار قطعی» اعلام نشود. YouTube تغییر نکرده است.
- install-r خروجی عادی نهایی موفق؛ MainActivity درOwner0 واقعاًresumed و Home
  دیده شد؛ ANR/crash تازه در بافر رخداد این boot نبود. فقطemulator-5554 بهADB
  متصل بود، نه TV فیزیکی. screenshot:
  `/private/tmp/aminema-stability-qa.nNoIFq/final-normal-home.png`.
