# Candidate0.19.0/code54 — Genre Discovery

## دامنه و تصمیم محصول

با تأیید مالک روی مسکات‌های تولیدشده، یک LazyRow زیر Hero افزوده شد؛ نه
بازطراحی خانه. دوازده دسته: ترسناک، خانوادگی، معمایی، اکشن، انیمیشن، عاشقانه،
کمدی، علمی‌تخیلی، فانتزی، مستند، کلاسیک، ماراتون. مهارت UI/UX فوکوس مشترک
DPAD/Mouse، اندازه ثابت و lazy rendering را هدایت کرد؛ تصویر با دستور مالک
بر اصول آیکون‌های برداری مقدم است. متن واقعی با فونت اپ است، نه متن داخل PNG.

## Asset و هزینه

شیت پیش‌نمایش تأییدشده از image_gen به aminema_genre_atlas.png در drawable-nodpi
کپی شد؛ اصل آن محفوظ است. مسیر منبع:
/Users/aminasadollah/.codex/generated_images/019fc6ff-cbe3-7b12-9cac-25d53a4af9e9/exec-9a303bdc-4015-47d2-9ff3-7d91328ae8cb.png
ابعاد1448×1086، PNG حدود2.3MiB؛ یک decode مشترک ARGB حدود6MiB روی IO فقط هنگام
Compose شدن ردیف. Canvas هر سلول4×3 را با srcOffset/srcSize نمایش می‌دهد، بخش
متن تولیدشده پایین را رسم نمی‌کند. هیچ فایل مستقل یا decode جدا برای۱۲کارت،
انیمیشن دائمی، دریافت شبکه یا WebView تازه برای این ردیف اضافه نشد.
SoftReference مشترک + Mutex از decode تکراری در Back معمول جلوگیری می‌کند؛
در فشار حافظه قابل جمع‌آوری است. crop۷۸٪ هر سلول و Canvas146dp متن تولیدشده
را پنهان و تناسب تصویر را حفظ می‌کنند.
این هزینه صفر نیست؛ سنجش فیزیکی Box هنوز انجام نشده است.

## داده و ناوبری

GenreDiscovery.select فقط پس از کلیک داخل resolver کتابخانه روی Default اجرا
می‌شود. داده ورودی cache کامل CatalogSection از ParsiFlix/FilmRooz/MyMoviz است؛
genres[] موجود و TitleMetadata کش‌شده شاهدند. متن خلاصه، کلمات عنوان یا حدس
رده‌بندی کودک هرگز شاهد ژانر نیست. فارسی ی/ک و نیم‌فاصله و Sci-Fi نرمال می‌شوند.
تطبیق alias دقیق است، نه substring. URL عادی dedupe و CanonicalLibrary برای
ترکیب بین‌منبعی محافظه‌کارانه با سقف2048 نتیجه استفاده می‌شود؛ ورودی دست‌نخورده.

کلاسیک این نسخه فیلتر صریح سال ساخت تا۲۰۰۰ است، نه ادعای شاهکاربودن. سال
شمسی با PublicMatchYear و تلورانس سیاست موجود نرمال می‌شود؛ سال آپلود نامعتبر
ParsiFlix شاهد نیست. ماراتون فعلاً «انتخاب سریال» است، نه صف/auto-next و پخش
خودکار. خانوادگی ژانر سایت است و رده‌بندی کودک تضمین نیست.

Home onOpenLibrary با providerIds خالی برای این مسیر فراخوانی می‌شود؛
CatalogLibraryScreen.allowRemoteLoading=false: دکمه refresh و auto-pagination
Provider و footer دریافت صفحات غیرفعال‌اند؛ صفحه‌بندی cache همچنان فعال است.
فیلتر فیلم/سریال، زبان، ژانر و مرتب‌سازی فعلی حفظ شده‌اند. خالی واقعی پیام
«در آرشیو ذخیره‌شده…» دارد. ورود عنوان از همین Spotlight عادی است؛ پخش/کوکی/
Continue/Favorites تغییر نکرده‌اند. Home Hero با فوکوس ژانر auto-rotate نمی‌کند.

## وضعیت و محدودیت

Baseline0.18.10/0f798c4: Gate موفق۸۸تست، lint صفر خطا/۱۳۱ هشدار.
فایل‌های Python و Manifest.bak قبلی مالک دست‌نخورده‌اند. هیچ app data، cookie،
WebStorage، library یا login پاک نشده. نسخه feature محلی0.19.0/code54 است؛
Commit/Push/Tag/Release نیازمند تأیید جداست. Coverage فقط cache موجود است؛
آرشیو کامل سرویس و مناسب‌بودن سنی ادعا نمی‌شود. وضعیت QA در TEST_REPORT_0.19.0.md.

Gate نهایی۹۳تست، صفر خطا؛ lint صفر خطا/۱۳۱هشدار؛ assemble موفق. موس ترسناک۱۲۴
و DPAD خانوادگی۷۳عنوان را نشان دادند؛ Back به Home موفق، restore دقیق کارت
هنوز تضمین نیست. SoftReference/crop نهایی در بیلد تحویل هستند؛ Release انجام نشده.

انتشار بعدی با مجوز مالک تأیید شد: Commitd2afc27، Tagv0.19.0، دانلود واقعی
APK/SHA و Latest API/code54 مطابق‌اند؛ RELEASE_REPORT_0.19.0.md مرجع جاری است.
