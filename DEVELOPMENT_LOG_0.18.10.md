# Candidate 0.18.10 / code53 — 2026-10-12

## دامنه

شش بهبود با اجازه مالک؛ مبنا Release0.18.9/81e7669 و docs0c886f0.
Commit/Push/Release جدید انجام نشده. فایل‌های Python و Manifest.bak مالک
دست‌نخورده‌اند؛ هیچ App data، کوکی، نشست یا کتابخانه حذف نشده است.

## تصاویر خارجی و بارگذاری

Probe محدود داخلی با نشست موجود: FilmRooz Ted Lasso واقعاً پوستر762×1142
روی همان صفحه عادی دارد، نه حدس مسیر170-256. DOM mobile آن داخل
postMeta / playTheStream است؛ همین قرارداد Desktop نیز بررسی شد، چون اپ
User-Agent سرویس را رعایت می‌کند. ابعاد واقعی و HTTP(S) امن قبل از پذیرش
detailPosterUrl الزامی‌اند. انتخاب thumbnail و backdrop همچنان جداست.

LargeTitlePoster ابتدا thumbnail کش‌شده همان عنوان را نگه می‌دارد؛ هیچ
تصویر عنوان قبلی روی عنوان جدید نمایش داده نمی‌شود. memory key شامل
URL و سایز decode است؛ rail360×540، Hero420×630، Spotlight720×1080.
request identity شامل pageUrl و سایز است تا request قدیمی موقتاً برنگردد.
پس‌زمینه Spotlight960×540 شد، نه decode بی‌قید. Cache Disk موجود دست‌نخورده است.

## یکدستی نمایش

PresentationPolish صرفاً UI است: حذف suffix سال تنها با سال برابر، زبان
انگلیسی/بریتانیا/آمریکایی یک chip، کشورهای هم‌معنا یکی، قسمت/فصل کوتاه با
ارقام فارسی و بدون وضعیت اضافه. شماره ناشناخته ساخته نمی‌شود. منابع در
Selector بر حسب Provider یک دکمه دارند اما همه routes اصلی حفظ‌اند؛ route
انتخاب‌شده همان Provider اولویت دارد، نه تغییر مقصد پخش.

## عکس عوامل ایرانی

صفحه عمومی دقیق Filmnet «پوست شیر»، /contents/scoQbJZ/The-Lion-Skin، در
NextData artists[].person.avatar_image.path عکس واقعی مرتبط با همان عنوان
را می‌دهد. فقط title نرمال‌شده دقیق، role بازیگر/کارگردان، میزبان tv.filmnet.ir
و نام/alias دقیق پذیرفته می‌شوند؛ عکس Provider برنده می‌ماند. داده عمومی
به source routing دست نمی‌زند؛ هیچ نشست سرویس به Filmnet نمی‌رود.
lookup7 به کش منفی قبلی اجازه یک retry می‌دهد؛ نبود هویت/عکس initials است.
QA اولیه: ۸ بازیگر+۱ کارگردان در cache؛ ۶ صورت قابل‌مشاهده Native تأیید شدند.
Loading عکس نیز initials دارد؛ LazyRow کلید پایدار نام و یک scrim خوانا دارد.

## هیرو روشن و حافظه

رنگ از خود portrait گرفته می‌شود؛ سطح غالبِ روشن/کم‌اشباع با نوشته قرمز نباید
کل Hero را قهوه‌ای کند. brightness و کنتراست black/white از luminance خطی
محاسبه می‌شوند؛ copy region opaque و هندسه ثابت است. UI/UX skill محدودیت
تصویر، عدم پرش و خوانایی را هدایت کرد، نه بازطراحی محصول.

ImageLoaderFactory فقط هنگام اولین تصویر lazy ساخته می‌شود؛ سقف memory
در low-RAM10% و معمول15%، decode هم‌زمان1/2، شبکه کل4/هرمیزبان2 است.
WebView جدید روی فوکوس/cold-start اضافه نشد؛ railهای پایین همچنان Lazy هستند.
کوکی تصویر تنها same-host و در IO آماده می‌شود، حتی نمونه کوچک رنگ هیرو.
سرعت و RAM روی Android Box واقعی هنوز benchmark نشده‌اند.

## محیط و تست

Baseline76 تست و Gate موفق. کش Gradle زیر iCloud دچار EOF executionHistory.bin
شد؛ هیچ کش یا فایل حذف نشد. Gate با --project-cache-dir
/Users/aminasadollah/Library/Caches/AminemaBuild/project-cache موفق شد.
Git pack نیز موقتاً timeout خواندن iCloud داشت؛ انتشار جدید در این مرحله نیست.
درخواست DevTools port forwarding توسط بررسی امنیت رد شد و انجام نشد؛
بازرسی محدودِ خود اپ بدون پورت و بدون خروج نشست ادامه یافت.
Probe/Manifest موقت باید پیش از APK نهایی حذف و نبودشان بررسی شود.

## علت نهایی و تکمیل QA

viewport مخفی2×2 عکس SVG placeholder381×571 داشت، ولی data-src همان عنصر
آدرس واقعی762×1142 را داشت. naturalWidth به‌تنهایی proof کیفیت نیست!
Loader فقط data-src مشاهده‌شده/ordinary currentSrc را می‌گیرد؛ یک Image
برای اندازه‌گیری همان فایل با deadline3600ms، cleanup handler و cancel در
timeout دارد. metadata deadline18s حفظ شده است؛ href پلیر خوانده نمی‌شود.
artworkLookupVersion4 به رکورد staging قبلی اجازه retry می‌دهد؛ pending
تصویر شکست‌خورده، موفقیت کش‌شده محسوب نمی‌شود. شرط اجباری backdrop برای
freshness حذف شد؛ عنوانِ بدون banner نباید هر بار WebView جدید بخواهد.

QA Native FilmRooz واقعاً پوستر بزرگ شفاف را نشان داد؛ URL همان فایل عادی
در cache با version4 ذخیره شد. پوستر روی پنلِ عنوان Canonical با تعویض source
عوض نمی‌شود؛ withMetadata فقط detailPoster خالی را تکمیل می‌کند.
Hero کلاغ با رنگ روشن خودِ کاغذ پوستر و متن مشکی دیده شد؛ Hero palette از
portrait است، نه wide banner. paper-majority45% + luminance foreground test
از رنگ‌گرفتن کل بنر با حروف قرمز جلوگیری می‌کند. chips/year scrim مستقل دارند.
پنل عکس عوامل scrim0.92 دارد تا نام و نقش روی پوستر زمینه خوانا بماند.

Probe و Manifest آزمایشی حذف شدند. Gate نهایی بدون Probe: ۸۵ تست، صفر
failure/error، lint صفر خطا و۱۳۱ هشدار (همان پایه)، assembleDebug موفق.
جزئیات نهایی نصب/Asset/محدودیت‌ها در TEST_REPORT_0.18.10.md.

QA مسیر واقعی Home→Ted Lasso منابع MyMoviz/FilmRooz را یک‌بار نشان داد.
در همین پنلِ پرمتادیتا، خلاصه CTA را زیر viewport می‌برد؛ اصلاح محدود آخر:
متن body وزن باقی‌مانده را می‌گیرد و Row دکمه‌ها خارج آن باقی می‌ماند.
خلاصه ellipsis دارد؛ مسیر پخش/انتخاب قسمت و focus handlers تغییر نکرده‌اند.
Gate پس از این اصلاح هم۸۵ تست و lint موفق بود.

ژانرها در پنل Native به جزئیات پایین‌تر (بعد از بخش قسمت‌ها، قبل از عوامل)
منتقل شدند؛ داده حذف نشده و index1 سریال برای Episode Navigator حفظ است.
Header با rating/country/language/availability جمع‌وجور می‌ماند تا خلاصه
و CTA هر دو در دید باشند. Gate پس از این تغییر هم موفق است.
# اصلاح نهایی تقویم

PublicMatchYear رقم‌های فارسی/عربی را یکسان می‌کند؛ سال شمسی معتبر با +621
به سال میلادی تقریبی تبدیل و با تلورانس موجود ±1 تطبیق می‌شود. سال آپلود
میلادی ParsiFlix شاهد سال تولید نیست؛ سال شمسی شناخته‌شده اولویت دارد.
بدون شاهد تاریخ، تطبیق عنوان باید حداقل0.88 باشد. LOOKUP_VERSION=8 برای
بازآزمایی cache منفی قبلی؛ سه تست مستقل برای این سیاست اضافه شد.
