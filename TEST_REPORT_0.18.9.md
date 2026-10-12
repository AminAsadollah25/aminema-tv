# QA Candidate 0.18.9/code52 — 2026-10-12

## تأیید واقعی

- Baseline قبلی Unit/Lint/assemble موفق، ۶۵ تست.
- ParsiFlix DOM قسمت‌ها واقعاً بررسی شد؛ seasonItem/episodeItem و شمارهٔ
  episodeNumber مطابق state machine و click observer هستند.
- انتخاب Native قسمت۴، پخش واقعی، رکورد action4/season0/position6556ms
  و duration2694096ms ثبت شد. شاهد: `/private/tmp/aminema-episode4-qa.png`.
- انتخاب قسمت۶ بدون استفاده از Continue، رکورد action6/position33466ms
  و duration2552085ms ثبت کرد. قسمت۵ سایت کلیپ۱۷ثانیه‌ای بود و برای تست
  Continue نیمه‌کاره مناسب نبود؛ آن نمونه کامل‌دیده حذفِ فیلتر عادی می‌شود.
- Home → کارت Continue → Spotlight: زمان ۲:۳۱ نمایش داده شد. دکمهٔ ادامه
  واقعاً قسمت۶ را پخش کرد؛ بعد از شروع، position170313ms با همان action6
  ثبت شد. شاهد: `/private/tmp/aminema-continue6-panel.png` و
  `/private/tmp/aminema-resumed6-player.png`. عنوان QA رکورد واقعی پخش دارد؛
  هیچ کتابخانه/کوکی/داده‌ای برای تست پاک نشده است.
- Ted Lasso: Provider سه credit بدون تصویر؛ lookup عمومیِ جدید برای همان
  سه نفر سه imageUrl معتبر پیدا کرد. صرف URL proof نمایش کامل Coil نیست؛
  screenshot نهایی UI و نتیجهٔ Gate در ادامه ثبت می‌شوند.
- Hero در Screenshot دارای Next چپ و Previous راست است. سایر LazyRow
  controls از روی حدس معکوس نشده‌اند. دو handler و layout مستقل حفظ‌اند.

## کنترل بصری تصاویر

در اجرای واقعی Native، سه عکس Ted Lasso در قاب‌های گرد دیده شدند؛ شاهد
`/private/tmp/aminema-native-cast-ua-fixed.png`. HTTP403 اولیهٔ Coil با
User-Agent واقعی مطابق سیاست Wikimedia رفع شد؛ لاگ آن و Launcher آزمایشی
از سورس نهایی حذف شده‌اند. نام/URL معتبر هنوز برای افراد بدون هویت منبع
یا خطای شبکه کافی نیست؛ fallback initials عمداً باقی است.

## Gate نهایی و پوستر واقعی

- نسخهٔ بدون Probe و AvatarQA ساخته و درجا نصب شد؛ Unit/Lint/assemble
  موفق، ۷۳ تست بدون Failure/Error/Skipped، lint صفر خطا و ۱۳۱ هشدار.
- کاربر به پوستر غایب در تست توجه داد: Launcher اولیه فقط title/URL داشت
  و posterUrl نداشت؛ این تست هرگز مدرک صحت پوستر/بنر نبود.
- مسیر واقعی Home → سریال‌های برگزیده → کارت Ted Lasso اجرا شد. پوستر
  کارت و پوستر/زمینهٔ Spotlight پس از بارگذاری دیده شدند؛ Source پیش‌فرض
  MyMoviz بود. شواهد `/private/tmp/aminema-ted-real-card-loaded.png` و
  `/private/tmp/aminema-real-ted-poster-settled.png`.
- در همین مسیر واقعی با APK نهایی، سه عکس بازیگر در Avatar دیده شدند؛
  برخی کارگردان‌ها که هویت/تصویر مرتبطِ قطعی نداشتند initials باقی ماندند.
  شاهد: `/private/tmp/aminema-real-card-cast-final.png`.
- اندازهٔ واقعی فایل عادی پوستر MyMoviz با sips: **190×281**. بزرگ‌نمایی
  تا قاب تمام‌قد TV عامل کم‌کیفیتی همین نمونه است، نه عدم دریافت تصویر.
  تصویر FilmRooz از درخواست عمومی فایل معتبر برنگرداند؛ اندازهٔ واقعی آن
  در این بررسی تأیید نشده و صرف path170-256 اندازه‌گیری محسوب نمی‌شود.
- کیفیت پوستر یک مسئلهٔ جدا ثبت شد؛ URL بزرگ‌تر یا پوستر متفاوت از روی
  حدس جایگزین نشده و API/وابستگی جدید اضافه نشده است.
- APK محلی: `/private/tmp/Aminema-0.18.9-debug.apk`.
  SHA-256: `fb39a4cbb54196a3dc2bbb8e08702c65826761506bd0d6f38e3f779c44b1507e`.
  certificate SHA256 همان `ba6ac8c4c2e1828462e7a6b122ad60856a054a18389af36bc001a1ee38ba13d3`.

## تکمیل QA کیفیت پوستر خارجی

- Gate دوباره موفق: ۷۶ تست، صفر Failure/Error/Skipped، lint صفر خطا و۱۳۱
  هشدار (همان تعداد پایه)، assembleDebug موفق. بیلد بعد از تکمیل Home paths است.
- MyMoviz در HTML واقعی `.mv-detail__posterbtn img` دارد؛ فایل مشاهده‌شده
  `https://imgsources.cc/images/cover/10/10986410_mid.jpg` واقعاً364×536 است.
  فایل کوچک190×281 جایگزین/بزرگ‌نمایی ساختگی نشده؛ artwork واقعی همان عنوان است.
- مسیر واقعی Home → سریال‌های برگزیده → Ted Lasso → MyMoviz اجرا شد؛
  پوستر بزرگ جدید در Spotlight دیده شد و detailPosterUrl/artworkLookupVersion1
  در رکورد همان URL ذخیره شدند. شاهد `/private/tmp/aminema-ted-detail-artwork.png`.
- Home ایرانی «آقای قاضی» و پنل Ted Lasso از مسیر Continue/FilmRooz با پوستر
  قبلی سالم دیده شدند. برای FilmRooz فایل بزرگ‌تر تأیید نشده؛ HD عمومی ادعا نمی‌شود.
- تست تصویر با APK پیش از آخرین تکمیل propagation در Recent انجام شد؛
  همان UI/parser در APK نهایی حفظ است. نصب درجا نهایی موفق؛ package52/0.18.9
  و checksum تحویل تطبیق شدند، crash buffer خالی بود. نشست/داده حذف نشد.
- APK جدید این مرحله: `/private/tmp/Aminema-0.18.9-poster-candidate.apk`،
  SHA256 `8b44979c123e89c13d58ac3466008361bd3ffdbfcdabcbe11673421cdf6907db`.
  امضا همان certificate قبلی است. این فایل جای خروجی قدیمی مرحلهٔ قبل را
  برای تحویل امروز می‌گیرد؛ نسخه همان Candidate52 است، انتشار انجام نشده.
- سه تست جدید قبول/رد ابعاد و URL، حفظ artwork در partial merge، default
  کش قدیمی را پوشش می‌دهند. آزمون شبکهٔ خرابِ اجباری و Box فیزیکی انجام نشده.

## محدودیت عمومی نسخه

پخش خودکار قسمت بعد از داخل پلیر بدون episodeRow، تماشای روی دستگاه دیگر،
کامل‌دیده/نیمه‌دیدهٔ حساب و همهٔ افراد ایرانی/خارجی در این چرخه تأیید نشده‌اند.
عکس غایب/تطبیق مبهم/شبکه خراب باید initials دهد؛ تصاویر ساختگی نداریم.
AVD موجود تحت فشار RAM با software rendering بالا آمده؛ benchmark نیست.
نسخه محلی است و Push/Tag/Release نیازمند اجازهٔ مستقل مالک است.
