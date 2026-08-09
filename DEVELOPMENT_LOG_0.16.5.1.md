# Development Log — Aminema 0.16.5.1

## هدف

جایگزینی Intro قبلی با ویدئوی جدید Mascot بدون تغییر رفتار Cold Start، Skip،
Mute یا مسیر ورود به Home.

## بررسی Source

- فایل دریافتی: `0809.mp4`
- مدت: حدود `8.22s`
- کادر: 16:9 / 1080p
- Video: H.264 (`avc1`)
- Audio: AAC Stereo، 44.1kHz، حدود 141kbps
- حجم Source: `12,468,312` بایت
- Source دارای Fast Start نبود.

## تصمیم فنی

Source با `avconvert` و Preset `PresetAppleM4V1080pHD` نرمال شد. Codec و کیفیت
اصلی حفظ شدند، اما Container برای Fast Start بهینه شد تا Android TV برای Prepare
کردن فایل محلی نیاز به خواندن انتهای Asset نداشته باشد.

خروجی نهایی:

- مدت: `8.20s`
- حجم: `12,463,177` بایت
- Video/Audio: H.264 + AAC Stereo
- SHA-256:
  `54c2ed65e42a2d546a059f1b8cf94344c75352b50dea775ed0d70bff39e4369e`

## فایل‌های تغییرکرده

- `app/src/main/res/raw/aminema_intro.mp4`
- `app/src/main/java/com/amin/tvos/intro/IntroOverlay.kt`
- `app/build.gradle.kts`
- مستندات Release، Roadmap و Handoff

## اصلاح ورودی Mouse

در تست واقعی مشخص شد `AndroidView/VideoView` رویداد Pointer را پیش از Parent
Compose می‌گیرد؛ بنابراین `pointerInput` روی `Box` به‌تنهایی Click موس را به
`finishOnce()` نمی‌رساند. Listener لمس و Click مستقیماً روی `VideoView` اضافه
شد تا Mouse/Air-mouse با `ACTION_UP` Intro را قطعی رد کند. Atomic guard موجود
همچنان از پایان دوباره جلوگیری می‌کند و مسیر Remote تغییری نکرده است.

## رفتار حفظ‌شده

- Intro فقط یک بار در هر Process و فقط در Cold Start اجرا می‌شود.
- OK/Enter/DPAD Center/Back و Click موس آن را رد می‌کنند.
- Mute preference کاربر حفظ می‌شود.
- Prepare timeout چهار ثانیه و hard cap بیست ثانیه Home را از Decoder خراب
  محافظت می‌کنند.
- هیچ Login، Cookie، WebStorage، Library یا Provider data دست‌کاری نمی‌شود.

## وضعیت

Clean Build، Unit test، Lint، نصب `adb install -r`، پایان طبیعی، Remote skip و
Mouse skip همگی موفق‌اند و Fatal Exception صفر است. مالک نتیجه را تأیید کرد و
نسخه همراه APK و SHA-256 با Tag `v0.16.5.1` در GitHub منتشر شد.
