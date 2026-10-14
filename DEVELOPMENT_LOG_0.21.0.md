# 0.21.0 / code56 — Candidate محلی، 2026-10-13

## تعمیم ده ژانر — 2026-10-14

نمونهٔ عاشقانهٔ پایین اکنون به HORROR/FAMILY/MYSTERY/ACTION/ANIMATION/ROMANCE/
COMEDY/SCI_FI/FANTASY/DOCUMENTARY تعمیم یافته. ArchiveGenre قرارداد واقعی
genre[] فرم است؛ science-fiction برای علمی‌تخیلی. کلاسیک و ماراتون ژانر
نیستند و مسیر پیشین خود را دارند. MainActivity دسته را با Intent داخلی به
Activity غیرexported می‌دهد؛ header و fallback cached نیز همان دسته را دارند.
genre با default ROMANCE به Query افزوده شد تا JSONهای پیشین بدون حذف خوانده
شوند. cache key/next validation شامل ژانر است؛ سقف12صفحه برای کل cache حفظ شد.
هیچ provider scan هنگام startup اضافه نشده و Loader/Playback دست‌نخورده‌اند.
ده submit واقعی مرورگر، هرکدام24نتیجه و لینک صفحه2 با ژانر صحیح، تأیید شدند.
Unit جدید40ترکیب ژانر×نوع×sort و رد cursor ژانر دیگر را می‌آزماید.
Gate123تست، lint/build موفق؛ نصب-r و QA نمونهٔ ترسناک و خانوادگی تأیید.
APK تازه و SHA در TEST_REPORT_0.21.0.md؛ هیچ انتشار جدید انجام نشده.
بخش‌های بعدی شرح نمونهٔ اولیه و محدودیت دانلودروز هستند.

## دامنه
نمونهٔ آرشیو واقعی عاشقانهٔ MyMoviz از مسکات Home. سایر ژانرها هنوز از کش
موجود انتخاب می‌شوند. هیچ Commit/Push/Tag/Release در این مرحله انجام نشده.

## قرارداد تأییدشده
فرم /search خروجی genre=romance، type=movie یا tv و sort=latest یا imdb دارد.
صفحهٔ بعد از لینک واقعی mv-pager__btn با p=2 دریافت شد. استخراج فقط metadata
قابل نمایش از mv-results-grid/mv-ritem است؛ URL پخش، توکن یا Cookie صادر نمی‌شود.
هر صفحه حداکثر24عنوان؛ cache مستقل AtomicFile حداکثر12صفحه، TTL شش ساعت.
query و صفحه و HTTPS/host/path/پارامترها اعتبارسنجی می‌شوند. تغییر origin،
صفحهٔ ناسازگار و cache خراب نباید به overwrite دادهٔ نامرتبط منجر شوند.

## معماری
GenreArchiveActivity فقط هنگام کلیک ساخته می‌شود؛ GenreArchiveLoader یک WebView
موقت با session موجود، تصاویر شبکه مسدود و media نیازمند gesture دارد.
توقف Activity درخواست را لغو و WebView را destroy می‌کند؛ generation guard
پاسخ قدیمی را حذف می‌کند. Home startup هیچ اسکن آرشیو جدیدی ندارد.
GenreArchiveRepository از catalog/library/account cache جداست.
Grid کلید پایدار دارد، posterHeight166dp فقط این صفحه است؛ سایر کارت‌ها unchanged.
کلیک عنوان به Spotlight موجود و نوع فیلم/سریال هدایت می‌شود، نه پلیر جدید.

## باگ پیدا و رفع‌شده در QA
خواندن page!! mutable در lazy items پس از تغییر صفحه باعث NPE شد.
snapshot ثابت displayedPage/displayedItems پیش از ساخت lazy DSL جایگزین شد.
صفحهٔ دوم و تغییر IMDb بعد از اصلاح واقعاً در امولاتور تأیید شدند.

## دانلودروز: هنوز پیاده نشده
در نشست امولاتور /archive/advancedSearch/ مشاهده شد. فرم GET، ژانر Romance=16،
type=both/film/series، order=release/updated/rating/usersRating/metascore/votes
و کشور/زبان/سال/دوبله/زیرنویس دارد. تلاش آزمایشی submit هنوز نتیجهٔ فیلترشدهٔ
معتبر نداد؛ URL فیلتر از حدس ساخته نشود. Probe موقت از source/manifest نهایی حذف شد.
قدم بعد: تأیید submit واقعی، صفحهٔ دوم با حفظ فیلتر، سپس adapter مستقل FilmRooz.

## مرزها
این نسخه تمام ژانرها یا تمام Providerها را online نمی‌کند. IMDb از خود سایت است
و تعداد رأی/کیفیت رتبه‌بندی تضمین نیست. پخش/Continue/login بازنویسی نشده‌اند.
نشست‌ها با install -r حفظ شدند. دستگاه فیزیکی و benchmark سرعت بررسی نشده‌اند.
