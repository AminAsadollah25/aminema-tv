# Series Lab — FilmRooz

تاریخ: ۲ اوت ۲۰۲۶  
وضعیت: تحلیل تصویری و DOM probe لاگین‌شده روی Friends و The Office انجام شد؛
Probe کلیک Player و آستانه Progress هنوز باقی است.

## شواهد مشاهده‌شده

چهار Screenshot واقعی از صفحه سریال FilmRooz این رفتار را نشان می‌دهند:

1. فصل با یک Dropdown در سطح عنوان انتخاب می‌شود. DOM probe نشان داد همه فصل‌ها
   از ابتدا در صفحه موجودند و با Containerهای مخفی/نمایان جابه‌جا می‌شوند؛ برای
   تعویض فصل Network hydration تازه لازم نیست.
2. داخل کیفیت اصلی (نمونه 1080p) قسمت‌ها قبل از فعال‌شدن حالت آنلاین خاکستری
   هستند و Action پایین ردیف `پخش آنلاین` است.
3. پس از فشردن `پخش آنلاین`، همان قسمت‌ها آبی و قابل پخش می‌شوند و Action
   پایین ردیف به `دانلود` تغییر می‌کند؛ بنابراین سایت یک Toggle واقعی بین
   Online و Download mode دارد، نه دو صفحه مستقل.
4. آیکن Play یعنی قسمت در حالت آنلاین قابل انتخاب است؛ Checkmark جای همان
   آیکن را برای قسمت‌هایی می‌گیرد که Provider آن‌ها را دیده‌شده می‌داند.
5. وضعیت Checkmark می‌تواند ناپیوسته باشد: در The Handmaid's Tale فصل ۶ فقط
   قسمت‌های ۴ و ۵ تیک دارند، با اینکه ۱ تا ۳ در دستگاه/پلتفرم دیگری دیده
   شده‌اند. پس «اولین قسمت بدون تیک» الگوریتم قابل‌اعتماد Next Episode نیست.
6. نمونه Silo فصل ۳، قسمت‌های ۱ تا ۴ تیک و قسمت ۵ Play دارد. این یک پیشنهاد
   ادامه با Confidence متوسط است، نه اثبات Account-wide.

## ترجیح قطعی پخش

ترتیب پیش‌فرض انتخاب خودکار کیفیت:

1. `1080p`
2. `720p`
3. `480p`

حتی اگر `2160p` موجود باشد، خودکار انتخاب نمی‌شود و فقط به‌عنوان انتخاب دستی
باقی می‌ماند. این تصمیم هم مصرف و فشار روی Android Box را کنترل می‌کند و هم
کیفیت پیش‌فرض را پایدار نگه می‌دارد.

درون نسخه انتخاب‌شده، `دوبله فارسی/دو زبانه 1080p` اولویت اول است؛ چون Player
داخلی FilmRooz اجازه می‌دهد وسط پخش Audio track به زبان اصلی تغییر کند.

ترتیب Resolver درون یک Edition:

1. دوبله فارسی یا دو زبانه `1080p`
2. زبان اصلی `1080p`
3. دوبله فارسی یا دو زبانه `720p`
4. زبان اصلی `720p`
5. دوبله فارسی یا دو زبانه `480p`
6. زبان اصلی `480p`

انتخاب دستی کاربر همیشه بر Resolver خودکار مقدم است.

## نتیجه DOM probe لاگین‌شده — ۲ اوت ۲۰۲۶

Probe روی صفحات واقعی Friends و The Office این قراردادهای Provider را تأیید کرد:

- Season control یک `select#cseason` واقعی است؛ مقدار Optionها شماره فصل است.
- Container هر فصل با الگوی `#cseason_{seasonNumber}` از ابتدا در DOM وجود دارد.
- هر Quality/Edition یک Row مستقل دارد و Header همان Row شامل Source، Resolution،
  Edition، زبان، مدت تقریبی، حجم و Label `Master` است.
- Episodeهای دانلود در `.eDbox` و Episodeهای پخش آنلاین در `.eSbox` جدا هستند.
- Action تغییر حالت با `data-action="stream"` و `data-action="dl"` قابل تشخیص است.
- وضعیت دیده‌شده با `svg[data-icon="check"]` و وضعیت قابل پخش با
  `svg[data-icon="play-circle"]` مشخص می‌شود.
- لینک‌های دانلود Modal جداگانه با `.noAjax[data-toggle="modal"]` دارند و نباید
  با Episodeهای Stream اشتباه گرفته شوند.

این قراردادها از متن یا رنگ ظاهری پایدارترند، اما Provider adapter باید Fallback
معنایی داشته باشد و به زنجیره طولانی کلاس‌های Bootstrap وابسته نشود.

پیامد عملکردی مهم: چون تمام فصل‌ها Preload شده‌اند، Aminema می‌تواند Catalog
فصل/قسمت/Edition را در یک Pass بخواند، آن را محلی Cache کند و فقط هنگام پخش، فصل
و ردیف دقیق را در WebView فعال کند. این مسیر سریع‌تر و کم‌خطاتر از کلیک‌کردن
پیاپی تمام فصل‌هاست.

## Edition با Quality فرق دارد

برخی عنوان‌های پرمصرف چند تدوین/نسخه موازی دارند و Edition نباید مثل یک
Quality عادی مدل شود:

- **The Office:** نسخه عادی `WEBRip 1080p` با مدت حدود `23:01` و نسخه
  `WEBRip 1080p | Uncut` با مدت حدود `38:16`؛ هر دو 1080p هستند اما محتوای
  تدوینی یکسان ندارند.
- **Friends:** نسخه `DVDRip | Uncut` با مدت حدود `29:39` و نسخه
  `BluRay 1080p` با مدت حدود `22:49`. خود Provider نیز توضیح داده که بعضی
  صحنه‌ها از BluRay حذف شده و فقط در DVD موجودند.

مدت هر Edition باید در سطح Season/Quality ذخیره شود، نه به‌صورت یک عدد عمومی
برای کل سریال؛ مثلاً Friends فصل اول `DVDRip Uncut 29:39` و `BluRay 22:49`
دارد، اما فصل دهم مدت‌های متفاوتی نشان می‌دهد.

پس انتخاب پخش دو مرحله مستقل دارد:

1. انتخاب و به‌خاطر سپردن **Edition/Cut**.
2. انتخاب بهترین **زبان + کیفیت** درون همان Edition.

Aminema نباید نسخه کامل را فقط به‌دلیل Resolution بالاتر نسخه کوتاه‌تر، بی‌صدا
عوض کند. Edition یک Edge case نادر ولی مهم است و نباید Flow عادی تمام سریال‌ها
را پیچیده کند. اگر فقط یک Edition وجود دارد، هیچ کنترل یا مرحله اضافه‌ای نشان
داده نمی‌شود. اگر چند Edition وجود دارد و هنوز Preference صریحی ثبت نشده، بار
اول یک انتخابگر کوچک و بدون اصطکاک نمایش داده می‌شود:

- `نسخه کامل (Uncut) — 29:39`
- `نسخه BluRay — 22:49`

انتخاب برای همان عنوان ذخیره می‌شود. History می‌تواند فقط یک گزینه را پیشنهاد
کند، اما حق ندارد بی‌صدا Preference را تغییر دهد؛ چون ممکن است کاربر صرفاً یک
Episode را برای مقایسه در Edition دیگری دیده باشد.

در صفحه انتخاب Episode، کنترل فرعی و کم‌اهمیت `نسخه‌های دیگر` فقط برای عنوان‌های
Multi-edition نمایش داده می‌شود. انتخاب Edition جایگزین دو Action روشن دارد:

- `فقط همین قسمت با این نسخه`
- `از این به بعد این نسخه`

Action اول Preference سریال را تغییر نمی‌دهد. به این ترتیب کاربر Geek می‌تواند
یک قسمت Uncut را برای مقایسه ببیند، بدون اینکه Continue قسمت بعدی ناخواسته از
مسیر عادی خارج شود.

## کشف خودکار Editionهای ناشناخته

پیاده‌سازی نباید به فهرست دستی Friends و The Office وابسته باشد؛ مالک محصول نیز
لیست کاملی از عنوان‌های Multi-edition ندارد. Provider adapter هنگام Refresh هر
صفحه، Rowها را به‌صورت عمومی Fingerprint می‌کند:

1. Source و Resolution (`WEBRip`, `BluRay`, `1080p` و غیره) جدا شوند.
2. زبان، حجم، مدت و Label `Master` جدا شوند.
3. متن باقی‌مانده به‌عنوان Candidate Edition نگهداری شود؛ مثل `Uncut`،
   `Extended`، `Director's Cut`، `Theatrical`، `IMAX`، `Open Matte`،
   `Complete` یا Label ناشناخته Provider.
4. اگر دو Row در یک فصل Episode set مشابه ولی Label یا مدت معنادار متفاوت
   داشته باشند، عنوان Multi-edition Flag شود.

Confidence سه سطح دارد:

- **High:** Label صریحی مثل Uncut/Extended وجود دارد.
- **Medium:** Episode set یکسان و اختلاف مدت قابل‌توجه وجود دارد.
- **Low:** فقط Source متفاوت است؛ مثلاً BluRay و WEBRip با مدت تقریباً یکسان.

فقط High و Medium UI `نسخه‌های دیگر` را فعال می‌کنند. Low صرفاً چند Quality/
Source عادی محسوب می‌شود تا BluRay و WEBRip معمولی اشتباهاً Edition جدا نشوند.

Label ناشناخته هرگز دور ریخته یا با نسخه دیگر Merge نشود؛ Raw label ذخیره و در
صفحه داخلی Diagnostics فهرست می‌شود تا بدون نیاز به دانستن عنوان‌ها از قبل،
Normalizer در نسخه‌های بعدی بهتر شود. این داده فقط محلی است و شامل URL رسانه،
Token یا محتوای محافظت‌شده نیست.

Edition تنها Edge case محتمل نیست. Parser باید بدون خراب‌کردن Flow عادی این
موارد را نیز تحمل کند:

- قسمت دوتایی که در یک نسخه Combined و در نسخه دیگر Split شده است.
- Episodeهای Special، Bonus، Reunion، Webisode یا شماره صفر.
- Episode گمشده یا شماره‌گذاری متفاوت میان Sourceها.
- نسخه‌ای که فقط بعضی فصل‌ها یا بعضی Episodeها را پوشش می‌دهد.
- Labelهایی مانند Director's Cut، Theatrical، Extended، IMAX/Open Matte یا
  Remastered که الزاماً همگی به معنی محتوای متفاوت نیستند.

بنابراین Episode identity فقط یک Integer نیست و Raw label/range نیز نگهداری
می‌شود. در Mapping مبهم، Aminema نباید حدس خطرناک بزند؛ Variantها جدا می‌مانند
و انتخاب Provider به کاربر نشان داده می‌شود.

## Progress وابسته به Content/Edition، نه Quality

ظاهر سایت Checkmark را داخل هر ردیف Quality تکرار می‌کند، اما شواهد رفتاری کاربر
نشان می‌دهد State واقعی به Episode همان Edition تعلق دارد، نه به Resolution یا
Audio rendition:

- اگر Episode در 720p دیده شود، همان Episode در ردیف 1080p نیز تیک می‌خورد.
- فیلمی که تا دقیقه ۴۰ با دوبله 1080p دیده شده، با انتخاب 720p زبان اصلی از همان
  Progress ادامه می‌دهد.
- در Player می‌توان بدون خروج Resolution، Audio language و حتی بعضی Audio
  channel layoutها را تغییر داد.

در مقابل، Timelineهای واقعاً متفاوت Progress مشترک ندارند:

- The Office نسخه عادی و Uncut وضعیت تماشای متفاوت دارند.
- Friends BluRay و DVD Uncut به‌دلیل Cut و Duration متفاوت Progress مستقل دارند.

نتیجه:

- Quality و Audio فقط Rendition هستند و Progress میان آن‌ها مشترک است.
- تیک Provider بین Editionهای دارای Timeline متفاوت Merge نشود.
- Next Episode از تیک Provider یک Edition دیگر حدس زده نشود.
- Progress محلی با کلید `series + season + episode + edition/timeline` ذخیره شود؛
  Resolution و Audio داخل کلید Progress نباشند.
- هنگام تعویض Edition می‌توان Action اختیاری `این قسمت را در نسخه دیگری دیدم`
  ارائه کرد، ولی این عمل باید صریح باشد و Evidence اصلی Provider را بازنویسی نکند.

برای UX ساده، Progress دو لایه دارد:

1. **Canonical episode progress:** آیا داستان این Episode در هر Edition توسط
   Aminema واقعاً تا آستانه Completion دیده شده است؟ این لایه تعیین می‌کند قسمت
   بعدی چیست.
2. **Variant progress:** آخرین Position و Edition دقیق برای Resume همان Playback.

قانون Continue:

- اگر Episode نیمه‌کاره است، دقیقاً همان Edition و Position Resume شود.
- اگر Episode کامل شده، Episode بعدی با Preferred Edition سریال باز شود.
- تماشای یک‌باره Edition جایگزین، Preferred Edition را تغییر ندهد.
- فقط Action صریح `از این به بعد این نسخه` Preference را عوض کند.

## مدل Rendition داخل Player

Quality/Audio قبل از پخش فقط Seed اولیه Player هستند. Resolver ردیف ترجیحی را
برای ورود انتخاب می‌کند (`دوزبانه 1080 → زبان اصلی 1080 → 720 → 480`)، اما پس
از ورود، تغییر Quality یا Audio داخل Player یک Playback جدید محسوب نمی‌شود و
Continue/Progress را Reset نمی‌کند.

```text
PlaybackIdentity
  titleId
  seasonNumber?
  episodeKey?
  editionTimelineId

RenditionPreference
  initialQuality = 1080
  preferDualAudio = true
  audioChannelPreference?
```

این تفکیک اجازه می‌دهد UI Native ساده بماند: دکمه اصلی بهترین Seed را انتخاب
می‌کند و تنظیمات ریز Quality/Audio به Player واگذار می‌شود.

## Auto-next و مشکل زنجیره Back

FilmRooz پس از پایان کامل Episode و تیتراژ، داخل همان فصل به Episode بعدی
Navigate می‌کند؛ Episode بعدی آماده است ولی کاربر هنوز باید Play را بزند. این
رفتار از آخرین Episode یک فصل به فصل بعد عبور نمی‌کند.

چون کاربر ممکن است بعد از انتخاب اولیه چند Episode پشت سر هم جلو برود، Aminema
نباید `currentEpisodeKey` را برابر Episode آغاز Session ثابت نگه دارد. هر انتقال
خودکار تأییدشده به Player قسمت بعد باید Session state و Continue Watching را به
Episode جدید به‌روزرسانی کند؛ وگرنه بعد از شش قسمت، Continue هنوز به قسمت اول
Session اشاره خواهد کرد.

هر Auto-next یک Navigation تازه در WebView history می‌سازد. در Binge چندقسمتی،
Back عادی کاربر را Episode‌به‌Episode عقب می‌برد و برای Android TV UX نامناسب
است. راه‌حل Aminema نباید چندبار Back زدن یا دستکاری شکننده History سایت باشد.

یک `PlaybackSessionController` باید هنگام ورود به Player این State را نگه دارد:

```text
parentSeriesUrl
seasonNumber
currentEpisodeKey
editionTimelineId
sessionActive
navigationGeneration
```

قانون Back:

1. اگر Dialog، Keyboard یا منوی موقت Player باز است، Back ابتدا همان Overlay را
   می‌بندد.
2. در غیر این صورت اگر Playback session فعال است، یک Back هم Fullscreen custom
   view را می‌بندد و هم مستقیماً Native Series/Episode Navigator را باز می‌کند؛
   صفحه غیر‌Fullscreen واسطه و WebView episode history نمایش داده نمی‌شوند.
3. خارج از Playback session، Back عادی WebView history را اجرا می‌کند.
4. Mouse Back/right-click که به Android Back تبدیل می‌شود دقیقاً همین مسیر را
   طی می‌کند.

در انتهای فصل، Aminema می‌تواند با دانستن Catalog فصل بعد، CTA بومی
`فصل بعد • قسمت ۱` نشان دهد. Auto-play بدون ورودی کاربر تا زمان Probe سیاست
Autoplay اجرا نشود؛ CTA یک‌کلیکی امن‌تر و قابل‌پیش‌بینی‌تر است.

## ماشین حالت Provider

```text
DETAIL_READY
  -> SELECT_SEASON
  -> WAIT_SEASON_CONTENT
  -> SELECT_QUALITY
  -> ONLINE_DISABLED? CLICK_PLAY_ONLINE
  -> WAIT_EPISODES_PLAYABLE
  -> CLICK_EXACT_EPISODE
  -> VERIFY_PLAYER
  -> PLAYING | SAFE_VISIBLE_FALLBACK
```

هر Transition باید با تغییر واقعی DOM/Player تأیید شود؛ `element.click()` به
تنهایی Success محسوب نمی‌شود.

## مدل داده پیشنهادی

```text
SeriesNavigatorState
  seasons[]
  selectedSeason
  qualities[]
  preferredQuality
  episodes[]

EpisodeState
  seasonNumber
  episodeNumber
  editionId
  availability = UNKNOWN | DOWNLOAD_ONLY | ONLINE_READY
  watchedEvidence = NONE | PROVIDER_LOCAL | AMINEMA_PLAYBACK | MANUAL_BASELINE

EditionVariant
  id
  label
  type = NORMAL | UNCUT | EXTENDED | DVD_CUT | BLURAY_CUT | DIRECTORS_CUT | UNKNOWN
  representativeDuration
  qualities[]
```

Provider tick به‌تنهایی فقط `PROVIDER_LOCAL` است. واژه `دیده‌نشده` تنها وقتی
مجاز است که Progress پیوسته از Aminema یا Baseline صریح کاربر وجود داشته باشد.

## UX پیشنهادی Aminema

### تصمیم‌های Scope پیش از توسعه

- `Spoiler Shield` جزو 0.16.3 نیست. FilmRooz خلاصه کلی سریال را می‌دهد، نه
  خلاصه Episode؛ خلاصه معمولی نیز ذاتاً Spoiler محسوب نمی‌شود.
- Episode summary فقط در آینده و پس از اتصال Source معتبر خارجی/فارسی یا ترجمه
  کنترل‌شده با AI اضافه می‌شود. نبود Summary نباید UI را با Placeholder یا متن
  ساختگی پر کند.
- `تنظیمات شروع پخش` در Native UI ساخته نمی‌شود. Resolver بهترین Seed را انتخاب
  می‌کند و تغییر Quality/Audio/Channel به رابط خوب Player موجود واگذار می‌شود.
- امکانات پرمیوم مانند Recap فارسی، My Series، اعلان قسمت جدید و Binge mode در
  مسیر آینده می‌مانند، اما هسته 0.16.3 باید ساده و قابل‌اعتماد بماند.

### وضعیت مطمئن

- `ادامه: فصل ۳ • قسمت ۵`
- Action ثانویه: `آخرین قسمت منتشرشده`
- Action سوم: `انتخاب فصل و قسمت`

### وضعیت ناقص یا ناپیوسته

- از روی اولین دکمه بدون تیک Next Episode حدس زده نشود.
- پیام کوتاه: `وضعیت تماشا بین دستگاه‌ها کامل نیست`.
- دو انتخاب کم‌اصطکاک:
  - `تا این قسمت دیدم`
  - `قسمت را انتخاب می‌کنم`

### Reality Show

- تنظیم per-series: `همیشه آخرین انتشار`.
- Primary Action مستقیماً آخرین قسمت را انتخاب می‌کند و Progress داستانی را
  دخالت نمی‌دهد.

## Automation امن پیشنهادی

1. فقط صفحه عادی و لاگین‌شده عنوان باز شود.
2. Dropdown فصل با Label/Value واقعی همان DOM انتخاب شود.
3. اگر چند Edition وجود دارد، Edition ذخیره‌شده یا انتخاب صریح کاربر اعمال شود.
4. کیفیت ترجیحی کاربر با Resolver `1080 → 720 → 480` در همان Edition پیدا شود.
5. اگر Episodeها خاکستری‌اند، Action قابل‌مشاهده `پخش آنلاین` یک‌بار کلیک شود.
6. تا آبی/قابل‌کلیک‌شدن Episode موردنظر صبر شود.
7. همان Episode مشخص کلیک شود.
8. Success فقط با Player route یا عنصر واقعی Player ثبت شود.
9. در هر Failure صفحه Provider قابل‌استفاده باقی بماند؛ URL رسانه، Token، DRM
   یا لینک محافظت‌شده خوانده و ذخیره نشود.

## Probeهای باقی‌مانده

1. بررسی اینکه Online toggle در سطح Quality است یا State میان چند ردیف Share
   می‌شود؛ DOM وجود Action مستقل در هر Row را نشان می‌دهد ولی رفتار کلیک باید
   جداگانه Verify شود.
2. بررسی اینکه تغییر Season حالت Online را Reset می‌کند یا خیر.
3. بررسی دقیق حالت Disabled علاوه بر Play و Checkmark؛ Play/Check قرارداد DOM
   پایدار و مشخص دارند.
5. تست کنترل‌شده آستانه Checkmark در ۱۰٪، ۵۰٪، ۷۵٪، ۹۰٪ و پایان قسمت؛ عدد
   حدود ۷۰٪ فعلاً Hypothesis است و نباید در کد Hardcode شود.
6. بررسی ماندگاری Checkmark بعد از Reload، App restart و دستگاه دیگر.
7. بررسی دوبله/زبان اصلی و اینکه Episode set برای Qualityهای مختلف یکسان است.
8. گسترش Normalizer Label برای `Extended` و `Director's Cut`؛ `Uncut`، `DVD` و
   `BluRay` روی نمونه واقعی تأیید شدند.

## نتیجه معماری

Screenshotها برای طراحی 0.16.3 کافی‌اند و فرض قبلی را اصلاح می‌کنند:

- FilmRooz یک Episode Navigator قابل Automation دارد.
- قبل از Episode click یک Online activation step واقعی لازم است.
- Checkmarkها برای پیشنهاد محلی مفیدند، اما Truth حساب کاربر نیستند.
- Baseline دستی و Aminema Playback باید Truth اصلی Progress باشند.
