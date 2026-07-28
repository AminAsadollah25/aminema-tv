# تحویل جاری Aminema برای Cloud / برنامه‌نویس بعدی

این فایل خلاصهٔ عملیاتیِ همیشه‌به‌روز پروژه است. قبل از هر تغییر جدید، همراه
با `ENGINEERING-HANDOFF-FA.md` و `ROADMAP.md` خوانده شود.

## قرارداد همکاری دو برنامه‌نویس

بعد از هر Feature، Fix یا Release، برنامه‌نویسی که کار را انجام داده باید
این موارد را در اسناد پروژه ثبت کند:

1. نسخه و `versionCode` فعلی
2. مسئله یا خواستهٔ کاربر و تصمیم UX نهایی
3. فایل‌ها، مدل‌ها و مسیرهای اجرایی تغییرکرده
4. داده‌ها/DOM/Endpointهایی که واقعاً Probe و تأیید شده‌اند
5. تست‌های انجام‌شده با نتیجهٔ قابل‌اندازه‌گیری
6. مرز امنیتی و چیزهایی که عمداً خوانده یا ذخیره نشده‌اند
7. محدودیت‌ها، کارهای عمداً عقب‌افتاده و قدم بعدی پیشنهادی
8. وضعیت Commit، Tag، Release و Assetهای منتشرشده

فقط نوشتن Release Notes کافی نیست. جزئیات فنی ماندگار باید در
`ENGINEERING-HANDOFF-FA.md`، برنامه‌های آینده در `ROADMAP.md` و شواهد تست
در `TEST_REPORT.md` هم ثبت شوند.

---

## وضعیت جاری

- محصول: **Aminema**
- نسخه: **0.12.0**
- `versionCode`: **25**
- Package نصب واقعی: `com.amin.tvos.debug`
- Package پایه: `com.amin.tvos`
- شاخه: `main`
- Release: **منتشرشده**
- Tag: `v0.12.0`
- Release commit: `5163c8f`
- URL: `https://github.com/AminAsadollah25/aminema-tv/releases/tag/v0.12.0`
- امضا و Application ID تغییر نکرده‌اند؛ نصب روی نسخه قبلی Update است و
  Cookie/Session/Library حفظ می‌شوند.

## خواسته‌ای که در 0.12.0 حل شد

پیاده‌سازی Live در 0.11.0 فقط یک دکمه در هدر بود که گرید کانال‌های خود سایت
را باز می‌کرد. UX نهایی مورد توافق:

- ردیف بومی و سینمایی «پخش زنده» پایین Home
- کارت مستقل هر شبکه با لوگوی واقعی
- OK کنترل یا کلیک موس → پخش مستقیم همان شبکه
- بدون صفحهٔ واسط و بدون کلیک دوم برای Fullscreen
- Back → همان Scroll و همان کارت فوکوس‌شده در Home
- قابل‌گسترش از JSON، نه Hardcode در UI

## کشف واقعی قبل از پیاده‌سازی

یک Activity موقت فقط در `app/src/debug/` ساخته شد. Probe فقط نام کارت،
`img` لوگو و مسیر عادی صفحه را بعد از کلیک واقعی ثبت کرد. هیچ
`video.src/currentSrc`، Request/Response، Cookie، Local Storage، Token یا DRM
خوانده نشد.

نقشهٔ ۲۰ شبکه:

| کانال | مسیر |
|---|---|
| شبکه ۱ | `/medias/live/51` |
| شبکه ۲ | `/medias/live/52` |
| شبکه ۳ | `/medias/live/53` |
| شبکه ۴ | `/medias/live/54` |
| شبکه تهران | `/medias/live/55` |
| شبکه خبر | `/medias/live/56` |
| شبکه نسیم | `/medias/live/57` |
| آی‌فیلم | `/medias/live/58` |
| شبکه ورزش | `/medias/live/59` |
| ایران اینترنشنال | `/medias/live/22` |
| BBC Persian | `/medias/live/37` |
| AVA | `/medias/live/29` |
| Persiana | `/medias/live/26` |
| Avang | `/medias/live/30` |
| Tapesh | `/medias/live/33` |
| TMTV | `/medias/live/34` |
| ITN | `/medias/live/69` |
| فراتر | `/medias/live/72` |
| Radio Javan | `/medias/live/67` |
| VOA | `/medias/live/62` |

بعد از کشف، تمام فایل‌ها و Manifest مربوط به Probe حذف شدند. بررسی Dex نهایی
هیچ کلاس `LiveTvProbe` یا package پروژه‌ای `probe/debug` پیدا نکرد.
`DebugProbesKt.bin` که در Zip APK دیده می‌شود Resource استاندارد
Kotlin Coroutines است و ارتباطی با Probe پروژه ندارد.

## تغییرات کد 0.12.0

### مدل و JSON

- `Models.kt`
  - `LiveChannel(id, name, path, logoUrl)`
  - `LiveTvConfig(channels)`
  - فیلد اختیاری `StreamingService.liveTv`
- `services.json`
  - QuickLink قدیمی Live حذف شد.
  - QuickLink یوتیوب فارسی باقی ماند.
  - بلوک `liveTv.channels` با ۲۰ مسیر و لوگوی تأییدشده اضافه شد.
- `ServicesRepository.kt`
  - `defaults.liveTv ?: current.liveTv`
  - علت: این داده قابلیت Adapter است و باید به نصب‌های موجود مهاجرت کند.

### Home

- فایل جدید `ui/home/LiveTvSectionRow.kt`
  - کارت 214×142، لوگو، نشان LIVE، فوکوس/هاور قرمز
  - ردیف افقی D-pad friendly با Auto-scroll
  - لوگوها با Coil در اندازه محدود `320×140` Decode می‌شوند تا RAM Box با
    تصویر بزرگ منبع هدر نرود.
- `HomeScreen.kt`
  - دکمه Live از هدر حذف شد.
  - همهٔ `service.liveTv.channels`ها به یک ردیف بومی تبدیل می‌شوند.
  - کلیک، URL عادی `service.url + channel.path` را با
    `liveTheaterMode=true` باز می‌کند.

### Browser

- `BrowserActivity.intent` پارامتر `liveTheaterMode` دارد.
- صفحهٔ Live چند بار در بازهٔ 250ms تا 5.5s برای ظاهرشدن `<video>` بررسی
  می‌شود، چون صفحه React SPA است.
- به‌جای `requestFullscreen()` که بدون User Gesture قابل اتکا نیست، ویدئوی
  قابل‌مشاهده با CSS زیر روی Activity تمام‌صفحهٔ Android قرار می‌گیرد:

```css
position: fixed;
inset: 0;
width: 100vw;
height: 100vh;
object-fit: contain;
z-index: 2147483647;
background: #000;
```

- `video.controls = true` و `video.play()` اجرا می‌شوند؛ Source و Request
  ویدئو هرگز خوانده نمی‌شوند.
- Back، Mouse Back و QuickMenu Back در Live mode مستقیم Activity را
  می‌بندند تا گرید واسط سایت دیده نشود.

## تست‌های قطعی

- JSON معتبر و Build موفق: `assembleDebug`
- Unit task: `testDebugUnitTest` (در حال حاضر `NO-SOURCE`)
- نصب با `adb install -r`: موفق و بدون پاک‌شدن داده
- نسخه نصب‌شده: `0.12.0`, code `25`
- شبکه ۱ با کلیک موس: پخش مستقیم تمام‌صفحه
- شبکه ۲ با D-pad و OK: پخش مستقیم تمام‌صفحه
- اندازه Video بعد از Theater mode:
  - Viewport: `960×540` CSS
  - Video rect: `960×540` CSS
  - خروجی فیزیکی: `1920×1080`
  - `readyState=4`, `paused=false`
- D-pad میان شبکه‌ها حرکت کرد و Horizontal Row خودکار اسکرول شد.
- Back همان Scroll و Focus کارت شبکه ۲ را حفظ کرد.
- Logcat نهایی: بدون `FATAL EXCEPTION`

## Asset نسخه

- `Aminema-v0.12.0-debug.apk`
- اندازه GitHub Asset: `75,570,599` بایت
- SHA-256:
  `0d9c0ea28a72b45bfaca5b464b897e1c1635b4a6d8a3e9775f26bf8271fd5329`
- `Aminema-v0.12.0-debug.apk.sha256`
- Release notes: `RELEASE_NOTES_0.12.0.md`
- وضعیت GitHub: Draft=false، Prerelease=false، هر دو Asset با state=uploaded

## مرز امنیتی

- فقط صفحهٔ عادی وبِ سرویس باز می‌شود.
- هیچ Authentication Bypass وجود ندارد.
- هیچ Stream/Media URL استخراج یا ذخیره نمی‌شود.
- هیچ Cookie، Token، DRM یا Header احراز هویت Log/Export نمی‌شود.
- Login و Session همان WebView قبلی و تحت کنترل سایت باقی می‌ماند.

## قدم بعدی پیشنهادی

اولویت بعدی طبق ROADMAP، **بازطراحی کامل کیبورد** است. پیشنهاد مکمل بعد از
آن برای Live: Channel Zapping با چپ/راست در حین پخش و یک Overlay بسیار کوتاه
نام شبکه، بدون بازکردن صفحه واسط. قبل از افزودن ردیف‌های ژانری، Home باید از
`Column + verticalScroll` به `LazyColumn/LazyRow` مهاجرت کند.
