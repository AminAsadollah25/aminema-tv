# Candidate 0.18.9 / code52 — 2026-10-12

## دامنه و وضعیت

سه باگ درخواستی: ادامهٔ قسمت سریال ایرانی، عکس بازیگران ایرانی/خارجی،
و جهت فلش‌های پوستر هیرو. پایه `1b5fe9e`، هنوز Commit/Push/Release نشده.
فایل‌های موقت قبلی مالک دست‌نخورده‌اند؛ نصب فقط درجا، بدون حذف نشست یا داده.

## علت و اصلاح ادامه

PlaybackSession پیش‌تر فصل/قسمت/کیفیت معنایی نداشت؛ ParsiFlix inline player
با contentUrl یکسان باعث از دست رفتن هویت قسمت می‌شد. native picker از قبل
توکن معتبر `#parsiflix-s0-epnum-4` داشت اما رکورد Playback آن را ذخیره نمی‌کرد.
مدل اکنون episodeAction/episodeSeason/episodeQuality اختیاری دارد؛ JSON قدیمی
بدون migration مخرب خوانده می‌شود. Bridge پس از Play واقعی این هویت را همراه
زمان ذخیره می‌کند و Timeline قسمت بعد از قسمت قبل ارث نمی‌برد. CHECKPOINT
روی ویدئوی هرگز پخش‌نشده نباید رکورد Continue بسازد.

Home → Spotlight → Browser برای ادامهٔ دارای هویت، از صفحهٔ عادی عنوان و
همان native state machine قسمت مشخص شروع می‌کند؛ generic Site Continue
وقتی smEpisode موجود باشد از قبل خاموش است. هنگام تغییر Provider، هویت
قسمت و Timeline پاک می‌شوند تا توکن ParsiFlix روی Provider دیگر اجرا نشود.
رکورد قدیمی سریال ایرانی بدون هویت، انتخاب قسمت را نشان می‌دهد نه قسمت اول
حدسی. کلیک روی episodeRow معمولی سایت مشاهده می‌شود؛ pending selection فقط
پس از Play جدید ثبت می‌شود، نه Pause ویدئوی قبلی. انتخاب خودکار قسمت بعدی
در داخل پلیر بدون DOM episodeRow، هنوز مستقلاً ممیزی نشده است.

Probe واقعی ParsiFlix `/medias/series/365`: یک seasonItem، هفده episodeItem؛
شماره‌های «قسمت 1…5» در episodeNumber و کنترل button واقعی تأیید شدند.
قسمت چهارم با HTML5 playback شروع و در رکورد action4/season0 ذخیره شد؛
سپس انتخاب مستقیم قسمت ششم (بدون Continue) action6 و زمان مستقل ثبت کرد.
قسمت پنجم سایت در تست کلیپ ۱۷ثانیه‌ای بود؛ برای سنجش Continue نیمه‌کاره
از قسمت ششم با مدت حدود ۲۵۵۲ ثانیه استفاده شد، بدون حدس دربارهٔ علت کلیپ.
QA این عنوان را واقعاً پخش کرد و رکورد تماشای آن به‌روز شد؛ داده حذف نشد.

## علت و اصلاح عکس‌ها

PersonRef.profileUrl صفحهٔ HTML شخص است، نه تصویر. Avatar اشتباهاً همین URL
را به Coil می‌داد. imageUrl مستقل، validation HTTP(S) بدون credential،
192×192 decode و fallback حرف اول نام برای خطای تصویر اضافه شد. Cookie فقط
با همان helper موجود و شرط same-host ارسال می‌شود؛ CDN عمومی Cookie نمی‌گیرد.
Schema Person.image و تصویر واقعی داخل لینک credit پشتیبانی می‌شوند.

سایت ایرانی نمونه اصلاً تصویر/لینک اشخاص نداشت. صفحهٔ خارجی Ted Lasso سه
credit بدون عکس داشت. مسیر موجود Wikipedia/Wikidata به نسخهٔ lookup6 ارتقا
یافت: پس از همان تطبیق سختگیرانهٔ عنوان/سال/IMDb، فقط اشخاص مرتبط با عنوان
در P161/P57 پذیرفته می‌شوند؛ تطبیق دقیق نام/alias یا QID و رد ابهام لازم است.
P18 منبع تصویر است؛ URL عمومی Wikimedia Commons با درخواست سایز192 ساخته
می‌شود. عکس Provider اولویت دارد. Probe واقعی Ted Lasso: cast=3/images=0
سپس public_cast=3/public_images=3. این نتیجه یافتن URL معتبر را ثابت می‌کند،
نه هنوز نمایش هر سه تصویر در UI یا پوشش تمام اشخاص ایرانی.

منابع قرارداد عمومی: https://www.wikidata.org/wiki/Property:P18
و https://commons.wikimedia.org/wiki/Commons:FAQ
انتقال Cookie/Token/URL سرویس به منابع عمومی ممنوع؛ فقط عنوان و متادیتای
عمومیِ موجود استفاده می‌شوند. اعتبار منبع P18 برای عکس جدید باید در ادامه
به پنل اطلاعات/منابع افزوده شود؛ این نسخه فایل عکس را توزیع نمی‌کند.

peopleImageVersion1 باعث یک retry غیرمخرب parser credits قدیمی می‌شود.
mergePeoplePrefer عکس معتبر قبلی را در refresh name-only پاک نمی‌کند.
بعد از Sheyda fallback، یک lookup محدود عکس برای credits تازه اضافه‌شده
انجام می‌شود؛ recursion به زنجیرهٔ fallback نداریم. نبود هویت/عکس/شبکه
باید به initials ختم شود؛ هیچ عکس حدسی یا تطبیق صرفاً مشابه انجام نمی‌شود.

## جهت فلش

در Hero، dots راست‌به‌چپ‌اند ولی دکمهٔ Next به راست اشاره می‌کرد. آیکون
Next اکنون چپ و Previous راست است؛ handlers، focus و layout ثابت مانده‌اند.
UI/UX skill اصول جهت قابل پیش‌بینی و حفظ فوکوس را هدایت کرد، نه بازطراحی.
Shared LazyRow controls بدون شاهد باگ دست‌کاری نشدند؛ اگر منظور مالک ردیف
دیگری باشد باید همان ردیف و جهت واقعی آن مستقلاً بررسی شود.

## آزمون و محیط

Baseline Gate موفق (۶۵ تست). تست‌های جدید برای backward compatibility،
semantic action، Timeline مستقل، URL امن، هویت عکس، عدم تطبیق مبهم و حفظ
عکس بعد از refresh افزوده شدند. نتیجهٔ Gate نهایی و شواهد UI بعداً در
TEST_REPORT_0.18.9.md ثبت می‌شود. امولاتور API36/1080p با کمبود RAM و
software graphics بالا آمده؛ هیچ عدد این اجرا benchmark سرعت محسوب نمی‌شود.
Probe debug Activity و Manifest موقت قبل از APK نهایی باید حذف شوند.

## تکمیل QA و دریافت تصویر

Continue واقعی قسمت6 از حدود 151266ms دوباره شروع شد و پس از پخش
position170313ms ثبت شد؛ Header قسمت6 و ویدئوی واقعی دیده شدند.
برای Coil، آدرس عکس معتبر به‌تنهایی کافی نبود: default User-Agent با HTTP403
رد شد. header معرف واقعی Aminema فقط برای commons/upload.wikimedia.org
اضافه شد، بدون Cookie/Referer سرویس. پس از این تغییر هر سه عکس در Native
Avatar دیده شدند (`/private/tmp/aminema-native-cast-ua-fixed.png`).
سیاست رسمی: https://foundation.wikimedia.org/wiki/Policy:Wikimedia_Foundation_User-Agent_Policy
مدل nullِ مرحلهٔ آماده‌سازی به Coil ارسال نمی‌شود؛ اول placeholder نام و
سپس درخواست آماده نمایش داده می‌شود. لاگ AvatarQA و هر دو Launcher/Manifest
موقت حذف شدند. ابزارهای JS موقت فقط در private/tmp هستند و وارد APK نمی‌شوند.

## یافتهٔ کیفیت پوستر — درخواستی جدا

کاربر کیفیت کلی پوسترها را پایین حس می‌کند. فایل واقعی MyMoviz برای
Ted Lasso فقط 190×281 است. Home Hero حداکثر420×630 decode می‌خواهد؛
Spotlight پوستر را تا ارتفاع TV بزرگ می‌کند. فایل کوچک با درخواست decode
بزرگ‌تر جزئیات تازه پیدا نمی‌کند. باید thumbnail کارت از پوستر باکیفیتِ
تأییدشدهٔ همان عنوان در Hero/Spotlight جدا شود؛ تنها پس از Probe صفحهٔ
معمولی/منبعِ مجازِ موجود، بدون حدس URL و با cache محدود تصاویر درشتِ فوکوس‌شده.
پس‌زمینهٔ محو عمداً نرم است و معیار کیفیت پوستر جلو نیست. این ایده هنوز
در آن مرحله پیاده نشده بود؛ اندازهٔ واقعی تصویر FilmRooz به علت پاسخ نامعتبر تأیید نشده.

## تکمیل کیفیت پوستر خارجی — 2026-10-12

با اجازهٔ مالک مسیر بزرگ جدا شد: detailPosterUrl روی TitleMetadata/SpotlightItem،
بدون تغییر thumbnail کاتالوگ. در HTML واقعی MyMoviz Ted Lasso انتخابگر
`.mv-detail__posterbtn img` و Schema/OG همان فایل `10986410_mid.jpg` را نشان
دادند؛ اندازهٔ دانلود واقعی364×536 است، در برابر thumbnail190×281.
Loader موجود فقط روی میزبان MyMoviz، currentSrc و naturalWidth/Height همین
container را می‌خواند؛ URL resize یا endpoint جدید حدس نمی‌زند. Policy فقط
تصویر بارگذاری‌شده حداقل300×450 با نسبت0.55..0.8 و URL امن را می‌پذیرد.
artworkLookupVersion1 به کش قدیمی اجازهٔ retry معمولی Provider می‌دهد؛ merge
جزئی تصویر بزرگ قبلی را پاک نمی‌کند. ایرانی و FilmRooz استخراج تازه ندارند.

LargeTitlePoster در Hero420×630 و Spotlight720×1080 decode محدود دارد؛ تا
موفقیت تصویر بزرگ یا در خطای آن thumbnail باقی می‌ماند. Home فقط artwork
کش‌شده را می‌گیرد؛ هیچ WebView/Request تازه روی فوکوس یا cold-start اضافه
نشده. پس‌زمینهٔ سینمایی و wide backdrop تغییر نکرده‌اند. راهنمای UI/UX حفظ
هندسه و سقف مصرف تصویر را هدایت کرد، نه بازطراحی. همهٔ عنوان‌ها HD تضمین
نمی‌شوند؛ اندازهٔ منبع واقعی سقف کیفیت است. سه تست Policy/Merge/Legacy
افزوده شد و Gate کامل بعد از تکمیل مسیر Recent/Continue موفق است.
