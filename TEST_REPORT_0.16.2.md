# گزارش تست Aminema 0.16.2

## گیت خودکار

- `testDebugUnitTest`: موفق
- `lintDebug`: موفق، خطای مسدودکننده صفر
- `assembleDebug`: موفق
- `git diff --check`: بدون خطای whitespace
- ساخت تمیز نهایی `clean + testDebugUnitTest + assembleDebug`: موفق

## تست Android TV — 1920×1080

- نصب درجا با `adb install -r`: موفق؛ Data و نشست ورود حذف نشد.
- اجرای Home و رندر Hero: موفق.
- Hero منتخب ایرانی/خارجی: پوستر واقعی foreground و بنر backdrop: موفق.
- RTL Hero، دکمه اصلی راست و «بعدی» چپ با جهت فلش درست: موفق.
- کنترل قبلی/بعدی جدید پایینِ چپ بدون پوشاندن Synopsis یا اکشن: موفق.
- حذف عبارت «شمسی/میلادی» و نمایش سال ایرانی با رقم فارسی/خارجی با رقم
  لاتین: موفق.
- خلاصه سریال ایرانی `کوری`: موفق؛ نبود عوامل نامطمئن به‌درستی خالی ماند.
- خلاصه و Metadata خارجی `House of the Dragon` و `The Hawk`: موفق.
- Persian-first public fill-only برای کارگردان/بازیگران House of the Dragon:
  موفق؛ IMDb ID تطبیق دقیق شد.
- Merge متادیتای Continue برای `Michael`: موفق.
- صفحه خارجی با Rating `6.5`: چیپ کهربایی `IMDb • متوسط`: موفق.
- Spotlight «بامداد خمار»: تکمیل واقعی از UI عمومی شیدا با سال ۱۴۰۳، کشور،
  زبان، سه ژانر، نرگس آبیار و چهار بازیگر: موفق.
- Backdrop رسمی افقی بامداد خمار از شیدا و حفظ URL اصلی تماشای پارسی‌فلیکس:
  موفق.
- ورود به Search از Home: موفق.
- دکمه روی صفحه «بازگشت» → `MainActivity`: موفق.
- Back ریموت از Search → `MainActivity`: موفق.
- `FATAL EXCEPTION` پس از QA: صفر.

## Performance snapshot

- Total PSS هنگام Home: حدود 172 MB روی امولاتور.
- فقط اسلاید فعال Compose می‌شود؛ backdrop حداکثر 1280×720 و پوستر حداکثر
  420×630 درخواست می‌شوند.
- WebView مخفی چرخش Hero حذف شده است.
- fallback عمومی فقط در Spotlight و فقط برای فیلدهای خالی اجرا می‌شود؛ miss
  به‌مدت ۳۰ روز Cache می‌شود.

## APK Candidate

- اندازه: `22,708,140` بایت
- SHA-256:
  `7e434aea4f5aa5b81d238eac33e63adf107ced420149a58b1b5d5f552e81580f`
- مسیر محلی:
  `~/Library/Caches/AminemaBuild/app/outputs/apk/debug/app-debug.apk`

## وضعیت انتشار

Candidate آماده است، اما Commit/Tag/Push/GitHub Release فقط پس از تأیید مالک
انجام می‌شود.
