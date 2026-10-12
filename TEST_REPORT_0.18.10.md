# QA Candidate0.18.10/code53 — 2026-10-12

## Gate

- مبنا76 تست و Gate موفق؛ فایل‌های مالک دست‌نخورده‌اند.
- خطای محیطی EOF کش Gradle داخل iCloud رخ داد. با --project-cache-dir بیرون
  iCloud، بدون حذف داده/کش، Gate کامل موفق شد.
- Gate پیش از اصلاح تقویم بدون Probe: ۸۵ تست، صفر failure/error، lint صفر خطا/۱۳۱ هشدار.
- تست‌های جدید: عنوان/سال، زبان/کشور، compact قسمت بدون حدس، source dedupe
  با حفظ route فعال، photos مرتبط ایرانی/رد نام مشابه، کنتراست تمام گام‌های
  خاکستری، رد SVGplaceholder و حفظ artwork Canonical هنگام refresh source.

## شواهد واقعی

- FilmRooz Ted Lasso، داده عادی poster762×1142 از صفحه logged-in. viewport
  مخفی2×2 placeholder SVG381×571 دارد؛ ابعاد آن پذیرفته نشده، فایل data-src
  واقعی اندازه‌گیری و در cache ثبت شد. Native همین پوستر شفاف‌تر را نشان داد:
  /private/tmp/aminema-01810-filmrooz.png.
- ParsiFlix پوست شیر /medias/series/181: ۸ بازیگر+۱ کارگردان با imageUrl
  عمومی مرتبط از Filmnet. شش چهره در viewport Native دیده شدند، بقیه روی
  ردیف افقی‌اند: /private/tmp/aminema-01810-iranian-actors.png.
- کامپوننت واقعی HomeHero با کارت واقعی کلاغ /series/345 در preview موقت
  بررسی شد: زمینهٔ روشن واقعی پوستر، متن تیره، پوستر کامل و CTA خوانا:
  /private/tmp/aminema-01810-light-hero.png. این شاهد component QA است، نه
  ادعای اینکه تمام هیروها/پوسترهای روشن یکسان‌اند؛ preview حذف شد.
- Home اصلی و DPAD navigation با focus قابل‌مشاهده بررسی شد؛ Back از
  Spotlight به Home کار کرد: /private/tmp/aminema-01810-home-dpad.png.

## حافظه و مرز ادعا

نمونه‌های تشخیصی PSS121031KB و199979KB در مسیرهای متفاوت (قبل/بعدِ بازکردن
WebView و تصاویر) قابل مقایسه benchmark نیستند و بهبود عددی نشان نمی‌دهند.
بهبود تأییدشده ساختاری است: سقف heap cache10/15%، decode1/2، network4/host2،
سایزهای ثابت، reuse thumbnail همان عنوان و حذف retry تکراری به خاطر نبود backdrop.
Cache disk/نشست/کتابخانه حذف نشده‌اند؛ API36/software graphics تحت فشار RAM.
Android Box فیزیکی، cold-start benchmark، offline/error اجباری و exhaustive
پخش/Continue تمام Providerها در این چرخه انجام نشده‌اند. playback code عوض نشده.

## وضعیت تحویل

نسخه محلی است؛ Commit/Push/Tag/Release جدید انجام نشده. نصب نهایی فقط
درجاست. APK نهایی بدون Probe نصب شد؛ versionCode53/versionName0.18.10 و
Manifest فاقد Probe تأیید شدند. APK /private/tmp/Aminema-0.18.10-debug.apk،
SHA256 d7bf63ec425f57ba81790858eb92a315a47773e759043f13eb5e175123744993.
certificate SHA256 ba6ac8c4c2e1828462e7a6b122ad60856a054a18389af36bc001a1ee38ba13d3
همان نصب قبلی است. هیچ APK آزمایشی Probe تحویل/منتشر نمی‌شود.

پس از نصب APK نهایی، مسیر واقعی Home → سریال‌های برگزیده → Ted Lasso
با موس اجرا شد؛ Selector فقط MyMoviz و FilmRooz داشت، نه MyMoviz تکراری.
زبان/کشور/قسمت‌ها یکدست و پوستر واقعی دیده شدند؛ شاهد:
/private/tmp/aminema-01810-final-sources.png. Home نهایی نیز ثبت شد:
/private/tmp/aminema-01810-final-home.png. crash buffer خالی و checksum تحویل OK.

QA چیدمان آخر با APK تحویل: کارت واقعی Ted Lasso از Home باز شد؛ خلاصه،
منبع‌های بدون تکرار، شماره قسمت و «انتخاب قسمت» هم‌زمان در viewport دیده شدند.
شاهد /private/tmp/aminema-01810-final-cta-ted.png. ژانرها به ردیف جزئیات پایین
منتقل شده‌اند؛ هیچ ژانر یا مقصد پخش از مدل حذف نشده است.
# Gate نهایی پس از اصلاح تقویم

۸۸ تست، صفر failure/error؛ lint صفر خطا و۱۳۱ هشدار؛ assemble موفق.
نصب درجای APK نهایی روی امولاتور موفق، package com.amin.tvos.debug/code53.
امضای SHA256 همان ba6ac8c4c2e1828462e7a6b122ad60856a054a18389af36bc001a1ee38ba13d3.
SHA256 نهایی (جایگزین تمام checksumهای Candidate پایین):
50d9653049b3f6ca7040c9028ab3a04a04371ad33f71506fab8049c616459008

