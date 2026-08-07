# Aminema — Idea Backlog

آخرین بازبینی: ۲ اوت ۲۰۲۶

این فایل محل ثبت ایده‌هاست؛ قرارگرفتن یک مورد در این فهرست به معنی ورود فوری
آن به نسخه بعدی نیست. هر ایده پیش از اجرا باید از مسیر «تحقیق → طراحی UX →
تصمیم معماری → Prototype → QA امولاتور و Android Box» عبور کند.

## دروازه‌های تحقیق فعلی

### پخش مستقیم از لینک دانلود — Download-to-Stream Bridge

- **مسئله:** بعضی عنوان‌های قدیمی فقط Download عادی دارند و Player آنلاین
  Provider برای آن‌ها فعال نیست.
- **راه سریع:** گرفتن رویداد عادی `DownloadListener` پس از کلیک خود کاربر و
  نمایش انتخاب `پخش مستقیم | دانلود فایل`.
- **راه کامل:** دادن URL مجاز، Cookie/User-Agent/Referer همان نشست و زیرنویس
  عادی به Media3؛ تشخیص Range/206، Resume، Seek، Expiry و Refresh لینک.
- **قانون ذخیره:** فایل کامل ذخیره نمی‌شود؛ فقط Buffer کوتاه RAM/Cache موقت.
- **Fallback صادقانه:** نبود Range یعنی پخش از ابتدا با Seek محدود؛ ZIP/RAR،
  DRM یا مسیر محافظت‌شده دور زده نمی‌شود.
- **هم‌افزایی:** هسته `RemoteFile Player` بعداً برای Telegram و حافظه شخصی نیز
  استفاده می‌شود.
- **وضعیت:** Research candidate؛ ابتدا روی یک عنوان قدیمی واقعی Probe شود.

### Episode Navigator

- فصل/قسمت Native، ادامه قسمت بعد، آخرین انتشار و انتخاب دستی.
- Progress فقط بر اساس Evidence معتبر یا Baseline دستی.
- **وضعیت:** اولویت اجرای بعدی؛ هدف 0.16.3.

### Third Source Coverage Lab

- نمونه حداقل ۱۰۰ عنوان، اندازه‌گیری Coverage gap، مزیت دوبله و کیفیت Match.
- ورود Provider سوم فقط پس از Canonical Library و Dedupe.
- **وضعیت:** تحقیق فوری؛ اجرای محصولی پس از 0.16.4.

### Telegram Personal Library

- TDLib رسمی، QR/2FA، Allowlist کانال‌ها، جست‌وجوی فایل و پخش Media3.
- معماری ARVIO/Tvgram فقط با بررسی License و بازمهندسی مستقل استفاده شود.
- API ID/Hash پروژه دیگر هرگز کپی نشود؛ Buildهای ABI-specific برای کنترل حجم.
- **وضعیت:** تحقیق فوری؛ اجرای محصولی پس از RemoteFile Player و Canonical ID.

## برنامه قطعی پیش از 1.0

1. Canonical Library: یک عنوان، یک کارت، چند SourceVariant.
2. Smart Search و Query normalization فارسی/انگلیسی.
3. Provider سوم با Dub-first/Original-first/Ask.
4. Telegram Personal Library.
5. Aminema Home 2.0 و Design/Motion System اختصاصی.
6. My Series، قسمت جدید و اعلان کم‌تعداد و معتبر.
7. Provider Health، Keyboard/Back، Cache و Performance hardening.
8. RC و Physical Android Box acceptance پیش از 1.0.0.

## ایده‌های ۲۰۲۷

- Local Media از USB/HDD/NAS با تشخیص فیلم از ویدئوی شخصی و Metadata امن.
- YouTube و سپس Providerهای بین‌المللی، هرکدام با Adapter و Research مستقل.
- People/Filmography، Follow بازیگر و کارگردان و اعلان اثر جدید.
- Geek Mode: MCU، Star Wars، LOTR، Harry Potter و ترتیب‌های مختلف تماشا.
- گسترش Live TV و برنامه‌های ورزشی مجاز با Health check.
- پروفایل‌ها، Sync چند دستگاه و Backup/Restore.
- AI اختیاری برای پیشنهاد، انتخاب حال‌وهوا، Metadata repair و Subtitle tools.
- مدل عمومی/اشتراکی Aminema بدون وابستگی به Provider خاص.

## قالب اجباری هر ایده تازه

1. مسئله واقعی کاربر چیست؟
2. راه سریع و مقاوم چیست؟
3. راه کامل و بلندمدت چیست؟
4. چه Dependencies و Riskهایی دارد؟
5. چطور کلیک کمتر و تصمیم سریع‌تر می‌سازد؟
6. Definition of Done و تست تلویزیون واقعی چیست؟

