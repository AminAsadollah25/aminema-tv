# راهنمای شروع برای عاملِ توسعه (Antigravity / Codex / هر IDE عامل‌محور)

این سند برای ابزاری است که **به فایل‌سیستم و ترمینال این مک دسترسی دارد**.
اگر ابزارت فقط چت است (مثل AI Studio)، بخش «چرخه کار» را نمی‌توانی اجرا کنی و
باید هر دستور را به مالک پروژه بدهی.

قبل از هر کاری این دو را کامل بخوان — قوانین، تصمیم‌های گرفته‌شده و تاریخچه
باگ‌های واقعی آن‌جاست و بدون آن‌ها دوباره‌کاری می‌کنی:

1. `ENGINEERING-HANDOFF-FA.md`
2. `ROADMAP.md`

---

## پروژه در یک خط

**Aminema** — اپ شخصی Android TV با Kotlin/Compose که فقط یک Hub روی دو سایت
استریمینگ است که مالکش خودش در آن‌ها اشتراک دارد. اپ هرگز خودش سرویس پخش
نیست؛ فقط WebView روی سایت‌های خودشان.

---

## محیط — این‌ها را بدان وگرنه وقت تلف می‌کنی

### جاوا

روی این مک جاوای مستقل نصب **نیست**. از JBR داخل Android Studio استفاده کن:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

### مسیر خروجی بیلد — بیرون از پوشه پروژه

پروژه زیر `~/Documents` است و **iCloud آن را سینک می‌کند**. iCloud وسط بیلد
کپی تکراری می‌سازد (`BuildConfig 2.class`، `banner 2.png`) و باعث می‌شود
Resource parser نام دارای فاصله را رد کند یا R8 دو تعریف از یک کلاس ببیند و
بشکند. پاک‌کردن دستی جواب نمی‌دهد چون کپی‌ها برمی‌گردند.

برای همین `layout.buildDirectory` در `build.gradle.kts` به بیرون منتقل شده.
**این را به هم نزن.** مسیر APK:

```
~/Library/Caches/AminemaBuild/app/outputs/apk/debug/app-debug.apk
```

اگر بیلدی بی‌دلیل کند شد یا خطای عجیب رزورس/R8 داد، اول این را چک کن:

```bash
find ~/Library/Caches/AminemaBuild -name "* [0-9].*" | wc -l
```

### امولاتور

AVD به نام `Television_1080p`. **هر دو سرویس روی آن لاگین‌اند** — این ارزشمند
است، چون بازرسی صفحات واقعی فقط با نشست لاگین‌شده ممکن است.

- `adb` در `~/Library/Android/sdk/platform-tools/adb`
- **هرگز `adb uninstall` نزن** مگر واقعاً لازم باشد؛ نشست لاگین از بین می‌رود.
  همیشه `adb install -r` (بروزرسانی درجا).

### گیت‌هاب

`gh` در `~/bin/gh` نصب و احراز هویت شده، روی مخزن
`AminAsadollah25/aminema-tv` (عمومی).

---

## چرخه کار

```bash
cd "/Users/aminasadollah/Documents/Codex/2026-07-24/amin-tv-os-application/outputs/AminTVOS-Source" \
  && JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDebug \
  && ~/Library/Android/sdk/platform-tools/adb install -r ~/Library/Caches/AminemaBuild/app/outputs/apk/debug/app-debug.apk \
  && ~/Library/Android/sdk/platform-tools/adb shell am force-stop com.amin.tvos.debug \
  && ~/Library/Android/sdk/platform-tools/adb shell am start -n com.amin.tvos.debug/com.amin.tvos.MainActivity
```

خطای واقعی کامپایل فقط خط‌هایی است که با `e:` شروع می‌شوند.

برای دیدن نتیجه روی صفحه:

```bash
adb shell input keyevent KEYCODE_DPAD_CENTER   # رد کردن اینتروی شروع
adb shell screencap -p /sdcard/s.png && adb pull /sdcard/s.png /tmp/s.png
```

بعد خودت به `/tmp/s.png` نگاه کن. **ادعای «تست شد» بدون دیدن تصویر نکن.**

بررسی کرش:

```bash
adb logcat -d | grep -c "FATAL EXCEPTION"
```

---

## قوانین سختِ پروژه

1. **هیچ Endpoint، ساختار DOM یا فیلد داده‌ای حدس زده نمی‌شود.** هر مسیر و
   Regex در کد از بازرسی واقعی روی حساب لاگین‌شده آمده. برای چیز تازه، اول
   بازرسی کن (روش پایین)، بعد کد بنویس.
2. **کیفیت پخش:** اولویت دوبله فارسی، بعد ۱۰۸۰ ← ۷۲۰ ← ۴۸۰. **۲۱۶۰ هرگز
   خودکار انتخاب نمی‌شود.** انتخاب فقط از روی `h=` و `lang=` عددی در URL، نه
   از روی اسم فایل (اسم‌ها استاندارد نیستند: DVDRip، WEBRip…).
3. **سریال‌ها Direct Play نمی‌گیرند** تا وقتی انتخاب خودکار قسمت ساخته شود.
4. **امنیت:** هیچ لینک مدیا، توکن یا مقدار DRM خوانده/ذخیره/لاگ نمی‌شود —
   فقط آدرس صفحه عادی سایت. Cookie فقط وقتی به تصویر ضمیمه می‌شود که هاست
   تصویر با هاست همان سرویس یکی باشد.
5. **شناسه بسته و امضا ثابت:** `com.amin.tvos.debug`. بروزرسانی باید درجا
   روی نصب موجود بنشیند. `applicationId` و امضا را عوض نکن.
6. **دو تصویر متفاوت برای هر عنوان:** `thumbnail` عمودی (~۰.۸:۱) برای کارت
   پوستری، `cover` عریض (~۱.۸:۱) برای Hero و بنر. جای هم استفاده نمی‌شوند —
   همین اشتباه باعث بریده‌شدن پوسترها شده بود و در ۰.۱۶.۰ رفع شد.

---

## روش بازرسی (Probe) — وقتی به داده واقعی سایت نیاز داری

ابزار موقت زیر `app/src/debug/` بساز (فقط در Build دیباگ کامپایل می‌شود)، یک
Activity که صفحه را در WebView با کوکی‌های خود اپ باز کند و نتیجه JS را در
Logcat بریزد. نکته‌ها:

- `evaluateJavascript` نمی‌تواند `await` کند و برای کد async رشته
  `"[object Promise]"` برمی‌گرداند. نتیجه را روی یک متغیر سراسری بگذار و از
  سمت Kotlin آن را Poll کن.
- خروجی طولانی را تکه‌تکه Log کن؛ Logcat خط بلند را می‌بُرد.
- برای فهمیدن شکل تصویر **اندازه‌اش را واقعاً اندازه بگیر**
  (`new Image()` و `naturalWidth/naturalHeight`)، از روی اسم فیلد حدس نزن.
- **بعد از تمام‌شدن، ابزار را پاک کن** و تأیید کن که در APK نمانده:
  ```bash
  unzip -l <apk> | grep -i probe
  ```

---

## انتشار نسخه

بعد از `0.9` → `0.10` → `0.11` … ادامه دارد. **هرگز به `1.0` نمی‌رویم.**
باگ‌فیکس هم نسخه جدای خودش را می‌گیرد.

مراحل:

1. `versionCode` و `versionName` را در `app/build.gradle.kts` بالا ببر
2. `RELEASE_NOTES_X.Y.Z.md` فارسی و تمیز بنویس — **خط آخرش باید
   `versionCode: N` باشد**، چون مکانیزم بروزرسانی درون‌برنامه‌ای همان را
   می‌خواند
3. تست کامل:
   ```bash
   ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
   ```
4. مطمئن شو ابزار دیباگ در APK نمانده
5. `ROADMAP.md` و `ENGINEERING-HANDOFF-FA.md` را به‌روز کن
6. Commit، `git tag vX.Y.Z`، Push
7. انتشار با APK و فایل `.sha256`:
   ```bash
   gh release create vX.Y.Z <apk> <apk>.sha256 \
     --repo AminAsadollah25/aminema-tv \
     --title "..." --notes-file RELEASE_NOTES_X.Y.Z.md
   ```

**قبل از Push و انتشار از مالک پروژه تأیید بگیر.**

---

## سبک کار مورد انتظار

مالک پروژه فارسی صحبت می‌کند و برنامه‌نویس نیست. خواسته توضیح‌ها **آموزشی**
باشد: وقتی چیزی را درست می‌کنی بگو ریشه‌اش چه بود و چرا آن راه‌حل کار می‌کند،
به زبان ساده. جواب کوتاه و مستقیم را ترجیح می‌دهد. اگر ادعایی می‌کنی که
تأییدش نکرده‌ای، صریح بگو تأیید نشده.
