# انتشار تأییدشدهٔ Aminema 0.24.4 / code64 — 2026-10-28

مجوز صریح مالک پس از QA: «خب منتشرش کن». متن‌های Candidate/محلی/منتشرنشده در اسناد پایین، تاریخچهٔ پیش از این انتشارند.

## سورس و GitHub

- مخزن: https://github.com/AminAsadollah25/aminema-tv
- Commit سورس تست‌شده: `5675a18b5960d431ad35de6bbf61d9ce06c526af`.
- Tag: `v0.24.4`؛ annotated object: `4699f4f4e8b701396d8fe8aefe18032a5c3b5182`؛ peeled commit همان Commit سورس است.
- Release: https://github.com/AminAsadollah25/aminema-tv/releases/tag/v0.24.4
- Release ID: `427206401`؛ published_at: `2026-10-28T21:24:26Z`.
- API واقعی `/releases/latest`: tag `v0.24.4`، draft=false، prerelease=false؛ آخرین خط غیرخالی body: `versionCode: 64`.
- ابتدا draft با هر دو دارایی uploaded تأیید و سپس عمومی/Latest شد. push معمولی main و تگ، بدون force/reset یا بازنویسی تگ؛ ۱۹ فایل مرتبط در Commit سورس.
- از سه سند مشترک فقط بخش جدید پخش stage شد؛ پژوهش‌های قبلی YT، اسکریپت‌ها/backupهای نامرتبط و CINEMA-DISCOVERY در working tree حفظ و منتشر نشدند. نسخه0.24.3 انتشار جدا ندارد؛ اصلاحاتش همراه0.24.4 منتشر شدند.

## دارایی‌های تأییدشده

- APK: `Aminema-0.24.4-debug.apk`؛ 31,728,747 بایت؛ asset ID `686386073`.
- SHA256: `987ee116f5dd009f27097e9af4173b0920843287b3f984c8836d7406564cba8f`.
- Checksum: `Aminema-0.24.4-debug.apk.sha256`؛91بایت؛asset ID `686386070`.
- دانلود مستقیم APK: https://github.com/AminAsadollah25/aminema-tv/releases/download/v0.24.4/Aminema-0.24.4-debug.apk
- هر دو فایل با gh release download در پوشه موقت تازه دریافت شدند؛ shasum -c موفق، cmp با APK تست‌شده بایت‌به‌بایت یکسان و apksigner موفق بود.
- امضای SHA256 ثابت: `ba6ac8c4c2e1828462e7a6b122ad60856a054a18389af36bc001a1ee38ba13d3`.
- Package `com.amin.tvos.debug`، versionName `0.24.4`، code64. همان فایل قبلاً فقطبا install-r نصب و تست شد؛ داده/ورود پاک نشد. فایل دانلودشده به دلیل برابری کامل cmp دوباره نصب نشد.

## گیت و حدود

گیت پیش از Commit دوباره testDebugUnitTest/lintDebug/assembleDebug با BUILD SUCCESSFUL در۳ثانیه (عمدتاًUP-TO-DATE) و۲۵fixtureJS موفق؛ نتایج۱۸۲unit/صفرfailure/error، lint صفرerror و۱۳۵warning موجود. SHA خروجی با APK تست‌شده تغییر نکرد.

QA واقعیِ همان APK: استخوان‌سوز۱ آغاز1080، Pause واقعی، تغییر720 درPause، Settings/Back باحفظ مکث، بازیابی خودکار گیر82.87→79→90.77 و میانه1994→2012.89؛ تاکسی۲ آغاز1080/time22.53→46.55 و میانه998→1016.98، Pause1113.5/Resume1115.57 و Back/destroy موفق. مرجع دقیق: TEST_REPORT_0.24.4.md. هیچ mediaURL/manifest/token/cookie منتقل نشد؛ موتورYT و سرویس‌های خارجی تغییر نکردند.

پخش همهٔ آرشیو، نبود همهٔ pauseهای داخلی سایت، ad-free کل اثر، صدا یا TVBox/USBmouse فیزیکی تأیید نشده‌اند. شرط دسترسی سایت و تبلیغِ حک‌شده در فایل حفظ‌اند. مرحله بعد بازخورد مالک روی باکس واقعی است.

## مسیر آپدیت

UpdateRepository همان Latest API را می‌خواند؛ code64 بالاتر از code62 نسخهٔ منتشرشده قبلی است. جفت `-debug.apk` و `.apk.sha256` در پاسخ واقعی با نام سازگار وجود دارند و checksum و امضا تأیید شدند. این بررسی به‌معنی تست تازهٔ اعلان یا PackageInstaller از0.24.2 روی باکس مالک نیست؛ برای آن downgrade، uninstall یا clear انجام نشد.

تگ روی سورس تست‌شده باقی می‌ماند؛ گزارش و وضعیت انتشار پس از تأیید سرور در Commit مستندات جدا ثبت می‌شوند.
