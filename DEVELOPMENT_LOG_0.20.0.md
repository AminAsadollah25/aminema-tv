# 0.20.0/code55 — Candidate محلی، مرحلهٔ اول Journey Hubs

## دامنهٔ انجام‌شده

دو ورودی مستقل زیر ژانرها: ماراتون و مارول با مسکات ثور تأییدشده. ورودی قبلی
ماراتونِ «همهٔ سریال‌ها» از rail ژانری برداشته شد؛ enum قدیمی و قرارداد تست آن
برای جلوگیری از تغییر بی‌ربط باقی مانده‌اند. مسیرهای native جدید در MainActivity:
marathon و marvel؛ Browser، Login، Continue و playback adapter تغییر نکردند.

JourneyCatalog یک registry مرجع lazy است، نه دادهٔ Provider. چهار مجموعهٔ مارول
MCU/Spider-Man/X-Men/Complete و دو مسیر vast/doomsday دارد؛ vast و complete
فعلاً همان union ثبت‌شده‌اند. Doomsday مجموعهٔ پیشنهادی سه خانواده است، نه ادعای
ارتباط داستانی رسمی یا ضروری‌بودن همهٔ آثار. ماراتون اولیه Middle-earth، Star Wars،
DCEU و Wizarding World دارد. این کل پروژهٔ جامع Marvel/DC نیست.

## مدل و الگوریتم

JourneyWork: id مستقل از لیست، title، release با precision روز یا سال، format،
season، premiereYear و collections/aliases. فیلم مشترک، مثل Homecoming، یک رکورد
و چند membership دارد. فصل دوم Loki در تاریخ2023 قرار می‌گیرد ولی با سال اصلی
سریال2021 تطبیق می‌شود. فصل‌ها با شروع انتشار فصل مرتب می‌شوند، نه episode-level.
رکورد روزانهٔ آینده نسبت به LocalDate.now فعال نمی‌شود؛ year-only نیز سال آینده
را حذف می‌کند. داخل یک سالِ بدون روز دقیق، ترتیب عنوان deterministic است و UI
صریحاً می‌گوید «ترتیب داخل سال ممیزی نشده»؛ chronology دقیق ادعا نمی‌شود.

resolve روی Dispatchers.Default فقط snapshot کامل کش و metadata موجود را می‌گیرد.
عنوان نرمال‌شده + سال اصلی + movie/series باید دقیقاً منطبق باشند؛ prefix/fuzzy
franchise detection نداریم. candidates محدود سپس با CanonicalLibrary ترکیب می‌شوند؛
اگر چند هویت متناقض باقی بماند media=null. همهٔ sourceVariants واقعی به Spotlight
می‌روند. هیچ URL حدسی یا stream/manifest/token ساخته یا ثبت نمی‌شود.

UI: LazyColumn/LazyRow با کلید work.id، فیلتر قالب و فقط تطبیق‌ها؛ AsyncImage
مسکات با size480/640 و poster160×240 و authenticatedPosterModel موجود (same-host
cookies). دادهٔ registry تنها با ورود به hub ساخته می‌شود؛ provider WebView یا
network discovery در Home اضافه نشده. پوستر remote فقط ردیف‌های visible load می‌شود.
عنوان بدون تطبیق قابل تمرکز است و کلیک آن توضیح نشان می‌دهد تا DPAD روی فهرست
پر از عنوانِ unresolved گیر نکند؛ لینک پخش ندارد.

کارت فصل به Spotlight با SELECT_EPISODE موجود می‌رود. انتخاب خودکار همان فصل
اضافه نشده و صریحاً «اطلاعات و انتخاب فصل» نوشته شده. watched/progress جدید
پیاده نشده؛ شناسهٔ مرجع برای مرحلهٔ آینده آماده است، ولی پیشرفت ساختگی نداریم.

## منابع و محدودیت مهم

MARATHON-UNIVERSES-RESEARCH-FA.md مرجع پژوهش اصلی است. MCU38فیلم، فصل‌های modern
و legacy ثبت‌شده،5One-Shot،3Special، هستهٔ سینمایی Spider/X-Men و بخشی از legacy
به کد منتقل شدند. تمام animation/TV قدیمی،3TeamThor، Shorts وبی، فصل‌های کامل
Legion/Gifted و همهٔ شاخه‌های DC/Star Wars هنوز منتقل و ممیزی نشده‌اند. UI برچسب
در حال تکمیل دارد؛ «هیچ چیز از قلم نیفتاده» یا «نبودن در سایت» نباید ادعا شود.
Harry Potter/Fantastic Beasts اسامی و ترتیب مرجع:
https://my.harrypotter.com/fantastic-beasts
https://my.harrypotter.com/legals
Philosopher/Sorcerer alias صریح است، نه حدس شباهت عنوان.

## وضعیت

baseline93تست/Lint/Build موفق؛8تست جدید پاس. candidate منتشر نشده است.
تغییرات قبلی docs/research/design و اسکریپت‌های شخص دیگری حفظ شده‌اند.
هیچ Commit/Push/Tag/Release مجاز یا انجام‌شده نیست. TEST_REPORT_0.20.0.md مرجع QA.
قدم بعد: تکمیل registry با منبع و تاریخ دقیق، بهبود match با public IMDb واقعی
و یک flow جستجوی صریح؛ سپس journey progress/shared completion و director hubs.

## مرحلهٔ دوم — درخواست مالک، 2026-10-13

ورودی‌های Marathon/Marvel از زیر genre rail به بعد از BOTH featured rows
«برگزیده‌های سینمای ایران» و «برگزیده‌های سینمای جهان» منتقل شدند؛ قبل از
YouTube/Recents باقی می‌مانند. ترتیب در Home LazyColumn قطعی است و screenshot
/private/tmp/aminema-journeys-new-position.png آن را روی امولاتور نشان می‌دهد.

تطبیق فقط کش قبلی نبود: JourneySyncActivity جدید، exported=false، یک اجرای
صریح سایت‌های filmrooz/mymoviz است. JourneyLookupPlanner یک query برای نام
مشترک چند فصل/نسخه می‌سازد؛ kind/year در resolve همچنان مستقل و قطعی‌اند.
دو worker، حداکثر یک WebView برای هر Provider، timeout20s، قطع پس از2failure
متوالی. cancel/خروج/background ادامهٔ پنهان ندارد؛ checkpoint موفق24h معتبر
است و اجرای بعد ادامه می‌دهد. queue activity-scoped است، نه app background.

SiteSearchEngine برای این flow metadataOnly=true دارد: تصاویر Home سایت load
نمی‌شوند و بعد از onPageCommitVisibleِ همان host جستجو اجرا می‌شود. جستجوی عادی
پیش‌فرض false و همان onPageFinished900ms قبلی را نگه داشته؛ هیچ endpoint/selector
جدیدی حدس زده نشد. Log تشخیصی فقط serviceId و مرحله دارد، نه URL/auth/HTML.

نتیجه‌ها در journey_lookup.json اختصاصی (AtomicFile +Mutex +IO)، نه catalog.json،
ذخیره می‌شوند. no-result/failure قبلی را پاک نمی‌کند؛ آینده فقط hit مرتبط با
مرجع را نگه می‌دارد، چون سایت query را OR می‌کند و نتیجهٔ بی‌ربط فراوان می‌دهد.
cache پراکندهٔ QA اولیه حذف نشده است. بازخوانی .bak هم پشتیبانی شده؛ corrupt
cache overwrite نمی‌شود. Lazy app repository فقط در Hub/Sync initialized می‌شود.

نام‌های full پنج One-Shot با نتیجهٔ واقعی SiteSearch و public IMDb id آن‌ها
ثبت شدند؛ prefix Marvel One-Shot به شکل alias صریح است. عنوان کوتاه The Consultant
با سریال2023 یا فیلم2022 یکی نیست. aliasهای تأییدشده برای CanonicalLibrary به
نام مرجع یکسان می‌شوند؛ contradictory IMDb همچنان مانع ادغام است. برای عنوان
ellipsisشده IMDb عمومی معتبر می‌تواند شاهد قوی‌تر باشد؛ substring/fuzzy نداریم.

گزارش داخلی journey_lookup_audit.json شامل workId/title/year/season/matched و
providerIds است، بدون stream/URL/credential. این match به صفحهٔ عنوان است، نه
تأیید اینکه هر season واقعاً پخش‌پذیر است؛ UI پایان این تفاوت را صریح می‌گوید.

علت شکست QA اول DNS امولاتور بود: Mac HTTP200 برای هر دو origin؛ Android hostname
unknown ولی ping1.1.1.1 موفق. همان AVD به‌صورت عادی بسته و با -no-snapshot-load
و -dns-server1.1.1.1,8.8.8.8 باز شد، بدون wipe/uninstall/clear data. DNS بعدی
موفق بود و344query واقعی (172هرسایت) بدونfailure تمام شد. فایل‌های library،
playback_sessions، catalog و Cookieها حذف نشدند. UI بعدی134/142رکورد Marvel
پیش از افزودن One-Shot aliases وصل کرد؛ این اعداد snapshot مرحلهٔ QA‌اند.

Candidate همچنان0.20.0/code55 و منتشرنشده است؛ تغییر نام/کد نسبت به candidate
قبلی لازم نبود چون Release عمومی هنوز0.19.0/code54 است. Gate مرحلهٔ دوم109تست
موفق. TEST_REPORT_0.20.0.md برای گزارش و SHA نهایی به‌روز می‌شود.

### نتیجهٔ نهایی مرحلهٔ دوم

356query (178هرprovider) بدونfailure؛211/211رکورد فعلی به صفحهٔ عنوان صحیح
وصل شدند، unmatched=[] روی امولاتور.115تست موفق. bestItems هم در Hub و هم
Audit خارج Main محاسبه می‌شود. کش داخلی دستگاه در APK bundle نشده؛ TV یک بار
دکمهٔ تطبیق را لازم دارد. تکمیل تاریخ Marvel/DC و availability هرseason هنوز
موضوع جداست. Fellowship2001 واقعاً از ماراتون به Spotlight رفت؛ stream تست نشد.

موارد ممیزی اضافه: Guardians2014 در کش FilmRooz دارای metadata2017/tt3896198
بود؛ این تعارض به معنی ادغام Vol1/Vol2 نیست. Reference tt2015381 از نتیجهٔ
عمومی واقعی MyMoviz تایید و منبع متناقض کنار گذاشته شد، بدون پاک‌کردن متادیتای
قبلی. Ghost Rider در سایت2011/tt1071875 ولی مرجع اکران2012 است؛ فقط شناسهٔ
تاییدشده اجازهٔ اتصال می‌دهد، نه drift حدسی سال. Skeleton Crew دارای پیشوند
Star Wars بود. The Gifted50800 آمریکا/tt4396630 و5729612 تایلند/tt9136312
هردو کارت2017 داشتند؛ detail loader فعلی هویت را خواند و فقط US عضو X-Men شد.
برای duplicate همان Provider، نبود id اجازهٔ merge قطعی نمی‌دهد. این محافظ
مخصوص Journey است، نه بازنویسی عمومی Search/CanonicalLibrary.

relevantItems برای کشِ تازه، hit با شناسهٔ مرجع و سال تولید متفاوت را حفظ
می‌کند؛ کاندید exact-title/kind با اختلاف حداکثر1سال هم برای بررسی detail
نگه می‌ماند ولی صرف این اختلاف، اجازهٔ playback match نمی‌دهد. بهترین نسخهٔ
هرURL از query مستقیم، public id، سال و تازگی checkpoint انتخاب می‌شود؛
incidental OR hit قدیمی نباید نتیجهٔ query عنوان خودش را shadow کند.
