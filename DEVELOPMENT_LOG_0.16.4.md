# Development Log — Aminema 0.16.4

## مسئله واقعی

در نسخه 0.16.3، Settings می‌توانست Release جدید را از GitHub پیدا کند و پیام
«از صفحه اصلی نصب کنید» نشان دهد، اما بنر صفحه اصلی ظاهر نمی‌شد.

## ریشه باگ

- `SettingsViewModel.checkForUpdate()` فقط Release را به callback همان صفحه
  برمی‌گرداند.
- `HomeViewModel` یک `UpdateState` جدا داشت و از نتیجه Settings باخبر نمی‌شد.
- اگر کاربر قبلاً همان versionCode را با «بعداً» Skip کرده بود، چک خودکار Home
  نیز عمداً آن نسخه را پنهان می‌کرد.

بنابراین پیام Settings درست بود، ولی State قابل نمایش Home همچنان `Idle` باقی
می‌ماند.

## اصلاح کوچک و برگشت‌پذیر

1. `UpdateRepository` مالک یک `StateFlow<UpdateState>` سطح Application شد.
2. `HomeViewModel` و `SettingsViewModel` هر دو همان State را مصرف می‌کنند.
3. چک دستی Settings فقط نسخه‌ای بالاتر از Build فعلی را به
   `UpdateState.Available` تبدیل می‌کند.
4. Settings همان `UpdateBanner` مشترک را درجا نمایش می‌دهد؛ Download progress
   و Retry نیز از State مشترک می‌آیند و Installer معمول Android باز می‌شود.
5. در صورت خروج از Settings، State بنر در Home باقی می‌ماند.
6. مقدار Skip در DataStore حذف نمی‌شود؛ درخواست دستی فقط برای همان مراجعه بر
   تصمیم قبلی «بعداً» غلبه می‌کند.

## فایل‌های تغییرکرده

- `app/src/main/java/com/amin/tvos/update/UpdateRepository.kt`
- `app/src/main/java/com/amin/tvos/ui/home/HomeViewModel.kt`
- `app/src/main/java/com/amin/tvos/ui/settings/SettingsViewModel.kt`
- `app/src/main/java/com/amin/tvos/ui/settings/SettingsScreen.kt`
- `app/build.gradle.kts`
- مستندات Release و Handoff

## مرز امنیت و داده

- هیچ Cookie، WebStorage، DataStore، Library یا Cache کاربر پاک نشد.
- Package و Signing identity ثابت ماند.
- Updater همچنان فقط Release با `remote versionCode > installed versionCode`
  را می‌پذیرد و نصب نهایی نیازمند تأیید معمول Android است.

## محدودیت تست

GitHub Latest هنگام QA محلی همان 0.16.3 بود؛ بنابراین callback نسخه جدید با
سرور واقعی روی Build 0.16.4 قابل تحریک نبود. Wiring مشترک Settings/Home،
Navigation، Build، Lint و نبود Crash بررسی شد؛ Download/Checksum/Installer
همان مسیر منتشرشده قبلی است و منطق آن دوباره‌نویسی نشد.

Clean Lint یک ناسازگاری از قبل موجود بین Artifact `runtime-lint` در Compose
1.7.2 و Lint 31.13.2 را آشکار کرد: چند Detector پیش از تحلیل با
`NoClassDefFoundError` Crash می‌کنند. AGP 8.13.2 اجازه Pinکردن Lint پایین‌تر را
نمی‌دهد و Upgrade Runtime Compose برای Hotfix انجام نشد. ۱۴ Check متعلق به همان
Artifact موقتاً غیرفعال و Lintهای Android/UI/Material/Navigation و بقیه پروژه
اجرا شدند؛ این فهرست باید همراه Upgrade آینده Compose حذف شود.

## Artifact نهایی

- فایل: `Aminema-0.16.4-debug.apk`
- اندازه: `22,904,748` بایت
- SHA-256: `330208a7326b1c9b74069b4174bfafa023c8f12a4115925ab6827c940581556f`
- Package و certificate digest با 0.16.3 یکسان‌اند؛ update درجا معتبر است.
