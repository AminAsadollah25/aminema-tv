# Development Log — Aminema 0.16.3

## هدف

پایدارکردن مسیر Native Episode Navigator برای ParsiFlix و FilmRooz، بدون تغییر معماری عمومی Browser و بدون پاک‌کردن نشست‌های کاربر.

## Evidence واقعی Provider

### ParsiFlix

ساختار زندهٔ تأییدشده:

```text
div[class*="_seasonItem_"]
└─ div[class*="_episodeItem_"]
   ├─ span[class*="_episodeNumber_"]
   └─ button  «تماشا» یا «ادامه تماشا»
```

شماره و دکمه خواهر/برادرند. شماره از Span خوانده و Button همان Row فقط یک‌بار Click می‌شود.

### FilmRooz

ساختار زندهٔ تأییدشده:

```text
select#cseason
div#cseason_N.cseason
└─ div.col-12 (quality block)
   ├─ header.dlbox-color
   └─ .boxtoggle
      ├─ .eDbox > a[href]       # دانلود
      └─ .eSbox > div[onclick]  # پخش آنلاین
```

Action قسمت فقط به شکل معنایی زیر ذخیره می‌شود:

```text
#filmrooz-s{season}-box{qualityBlock}-epnum-{episode}
#parsiflix-s{seasonIndex}-epnum-{episode}
```

هیچ href دانلود، URL مدیا، Cookie یا Token وارد مدل Episode یا Log نمی‌شود.

## تصمیم‌های اجرایی

- Episode callback اکنون `(Episode, Season, SeriesEdition)` است.
- Episode playback همیشه از Detail URL پایدار عنوان شروع می‌شود.
- Generic Direct Play و Site Continue هنگام وجود `smEpisode` اجرا نمی‌شوند.
- State Machine در هر BrowserActivity فقط یک‌بار و فقط روی Content URL Dispatch می‌شود.
- FilmRooz از زبان و کیفیت مجاز بهترین `.eSbox` را انتخاب می‌کند.
- Poster کاتالوگ در Home/Recent/Continue/My Series بر پوستر قدیمی Library اولویت دارد.
- Repair پوستر غیرمخرب است و هیچ Library/Login/Cookie حذف نشده است.

## فایل‌های اصلی تغییرکرده

- `browser/BrowserActivity.kt`
- `browser/PlaybackSessionController.kt`
- `browser/PlaybackLoadingView.kt`
- `ui/spotlight/EpisodeLoader.kt`
- `ui/spotlight/EpisodeNavigator.kt`
- `ui/spotlight/SpotlightActivity.kt`
- `ui/home/HomeScreen.kt`
- `ui/home/HomeViewModel.kt`
- `data/LibraryRepository.kt`
- `data/model/EpisodeModels.kt`

## نتیجه

مالک محصول روی امولاتور، لیسانسه‌ها در فصل‌های ۱ و ۲، بامداد خمار فصل ۲ قسمت ۴، Life/Larry در FilmRooz و پوسترهای مرتبط را موفق تأیید کرد.

## گام بعدی

Multi-edition نادر (Normal/Uncut/Extended/DVD/BluRay) باید در نسخه‌ای جدا و پس از Probe نمونه‌های واقعی طراحی شود؛ این موضوع وارد Fix عمومی 0.16.3 نشد.
