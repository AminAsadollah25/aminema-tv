package com.amin.tvos.ui.home

import java.util.Calendar

/** What the greeting's action chip does when the user clicks it. */
enum class GreetingAction { NONE, CONTINUE_LAST, PICK_MOVIE, PICK_SERIES, SURPRISE }

/** A time- and calendar-aware Home greeting. */
data class SmartGreeting(
    val headline: String,
    val subline: String,
    val actionLabel: String,
    val action: GreetingAction
)

/**
 * Builds the Home greeting from the local clock and the Persian calendar.
 *
 * Everything is computed on-device from the system time — no network, no location and no
 * profiling. [variant] rotates the wording so the shuffle control can offer another take on
 * the same moment, and the availability flags keep the action chip honest: it never offers
 * "continue" with an empty Continue row or a surprise pick with an empty catalog cache.
 */
fun buildSmartGreeting(
    now: Calendar,
    variant: Int = 0,
    hasContinue: Boolean = false,
    hasCatalog: Boolean = false
): SmartGreeting {
    val hour = now.get(Calendar.HOUR_OF_DAY)
    val weekday = now.get(Calendar.DAY_OF_WEEK)
    val gMonth = now.get(Calendar.MONTH) + 1
    val gDay = now.get(Calendar.DAY_OF_MONTH)
    val (_, jMonth, jDay) = gregorianToJalali(now.get(Calendar.YEAR), gMonth, gDay)

    // Dutch week: the weekend starts on Friday evening and runs through Sunday.
    val fridayEvening = weekday == Calendar.FRIDAY && hour >= 16
    val isSaturday = weekday == Calendar.SATURDAY
    val isSunday = weekday == Calendar.SUNDAY
    val weekendMood = fridayEvening || isSaturday || isSunday
    // Sunday night still has a Monday behind it.
    val schoolNight = isSunday && hour >= 21

    val occasion = when {
        gMonth == 12 && gDay == 24 -> Occasion.CHRISTMAS_EVE
        gMonth == 12 && (gDay == 25 || gDay == 26) -> Occasion.CHRISTMAS
        gMonth == 12 && gDay == 31 -> Occasion.NEW_YEAR_EVE
        gMonth == 1 && gDay == 1 -> Occasion.NEW_YEAR
        jMonth == 1 && jDay <= 4 -> Occasion.NOWRUZ
        jMonth == 1 && jDay == 13 -> Occasion.SIZDAH
        jMonth == 9 && jDay == 30 && hour >= 17 -> Occasion.YALDA
        jMonth == 10 && jDay == 1 && hour < 5 -> Occasion.YALDA
        else -> null
    }

    val candidates: List<SmartGreeting> = when {
        occasion == Occasion.CHRISTMAS_EVE -> listOf(
            SmartGreeting(
                "شب کریسمس 🎄",
                "چراغ‌ها را کم کن؛ امشب فیلمِ دنج می‌چسبد.",
                "یه چیز شانسی",
                if (hasCatalog) GreetingAction.SURPRISE else GreetingAction.NONE
            ),
            SmartGreeting(
                "Kerstavond 🎄",
                "بهترین شبِ سال برای یک فیلم قدیمی و آشنا.",
                "فیلم‌ها را نشانم بده",
                if (hasCatalog) GreetingAction.PICK_MOVIE else GreetingAction.NONE
            )
        )

        occasion == Occasion.CHRISTMAS -> listOf(
            SmartGreeting(
                "کریسمس مبارک! 🎁",
                "تعطیلات یعنی وقتِ یک ماراتن بی‌عجله.",
                "سریال‌ها را نشانم بده",
                if (hasCatalog) GreetingAction.PICK_SERIES else GreetingAction.NONE
            ),
            SmartGreeting(
                "تعطیلات کریسمس 🎄",
                "کسی جایی نمی‌رود؛ یک فیلم بلند بگذاریم.",
                "فیلم‌ها را نشانم بده",
                if (hasCatalog) GreetingAction.PICK_MOVIE else GreetingAction.NONE
            )
        )

        occasion == Occasion.NEW_YEAR_EVE -> listOf(
            SmartGreeting(
                "شب سال نو 🎆",
                "تا آتش‌بازی وقت هست؛ یک فیلم جا می‌شود.",
                "یه چیز شانسی",
                if (hasCatalog) GreetingAction.SURPRISE else GreetingAction.NONE
            ),
            SmartGreeting(
                "آخرین شبِ سال 🥂",
                "سال را با یک انتخاب درست تمام کن.",
                "فیلم‌ها را نشانم بده",
                if (hasCatalog) GreetingAction.PICK_MOVIE else GreetingAction.NONE
            )
        )

        occasion == Occasion.NEW_YEAR -> listOf(
            SmartGreeting(
                "سال نو مبارک! 🎉",
                "اولین فیلمِ سال را با دقت انتخاب کن.",
                "یه چیز شانسی",
                if (hasCatalog) GreetingAction.SURPRISE else GreetingAction.NONE
            ),
            SmartGreeting(
                "Gelukkig nieuwjaar! 🎉",
                "روزِ اول سال و یک روز کاملِ خالی؛ چه بهتر از این.",
                "سریال‌ها را نشانم بده",
                if (hasCatalog) GreetingAction.PICK_SERIES else GreetingAction.NONE
            )
        )

        occasion == Occasion.NOWRUZ -> listOf(
            SmartGreeting(
                "نوروز مبارک! 🌱",
                "عیدِ خانواده و فیلم؛ یه چیز شاد بذاریم؟",
                "یه فیلم عیدانه",
                if (hasCatalog) GreetingAction.PICK_MOVIE else GreetingAction.NONE
            ),
            SmartGreeting(
                "سال نو مبارک! 🌱",
                "تعطیلاتِ طولانی یعنی وقتِ یه سریال درست‌وحسابی.",
                "سریال‌ها را نشانم بده",
                if (hasCatalog) GreetingAction.PICK_SERIES else GreetingAction.NONE
            )
        )

        occasion == Occasion.SIZDAH -> listOf(
            SmartGreeting(
                "سیزده‌بدر! 🌿",
                "اگر خانه ماندی، سیزده را با یک فیلم به‌در کن.",
                "یه چیز شانسی",
                if (hasCatalog) GreetingAction.SURPRISE else GreetingAction.NONE
            )
        )

        occasion == Occasion.YALDA -> listOf(
            SmartGreeting(
                "شب یلدا مبارک! 🍉",
                "بلندترین شب سال؛ بهترین بهانه برای یک ماراتن.",
                "سریال‌ها را نشانم بده",
                if (hasCatalog) GreetingAction.PICK_SERIES else GreetingAction.NONE
            ),
            SmartGreeting(
                "یلدا شب! 🍉",
                "هندوانه و آجیل آماده است؟ فیلمش با ما.",
                "یه چیز شانسی",
                if (hasCatalog) GreetingAction.SURPRISE else GreetingAction.NONE
            )
        )

        hour < 5 -> listOf(
            SmartGreeting(
                "آخر شب شد…",
                "یه چیز آرام و کوتاه، نه یه سریال ده‌قسمتی.",
                "همان نیمه‌کاره",
                if (hasContinue) GreetingAction.CONTINUE_LAST else GreetingAction.NONE
            ),
            SmartGreeting(
                "نصفه‌شب و بیدار؟",
                "پس بی‌سروصدا ادامه بدهیم.",
                "ادامه بده",
                if (hasContinue) GreetingAction.CONTINUE_LAST else GreetingAction.NONE
            )
        )

        hour < 9 -> listOf(
            SmartGreeting(
                "صبح بخیر ☕",
                "یه شروع سبک برای امروز؟",
                "یه چیز شانسی",
                if (hasCatalog) GreetingAction.SURPRISE else GreetingAction.NONE
            ),
            SmartGreeting(
                "سلامِ صبح ☀️",
                "قهوه دستت باشد، بقیه‌اش با ما.",
                "همان نیمه‌کاره",
                if (hasContinue) GreetingAction.CONTINUE_LAST else GreetingAction.NONE
            )
        )

        hour < 12 -> listOf(
            SmartGreeting(
                "صبح رو به ظهر است",
                "یک فیلم کوتاه تا وقت ناهار جا می‌شود.",
                "فیلم‌ها را نشانم بده",
                if (hasCatalog) GreetingAction.PICK_MOVIE else GreetingAction.NONE
            )
        )

        hour < 15 -> if (weekendMood) listOf(
            SmartGreeting(
                "ظهرِ تعطیل 😌",
                "بعد از ناهار، یک فیلم بلند حق ماست.",
                "فیلم‌ها را نشانم بده",
                if (hasCatalog) GreetingAction.PICK_MOVIE else GreetingAction.NONE
            )
        ) else listOf(
            SmartGreeting(
                "وقت ناهار",
                "یک قسمت کوتاه، نه بیشتر.",
                "همان نیمه‌کاره",
                if (hasContinue) GreetingAction.CONTINUE_LAST else GreetingAction.NONE
            )
        )

        hour < 18 -> listOf(
            SmartGreeting(
                "عصر بخیر",
                "تا شام وقت هست؛ یک قسمت بگذاریم؟",
                "همان نیمه‌کاره",
                if (hasContinue) GreetingAction.CONTINUE_LAST else GreetingAction.NONE
            ),
            SmartGreeting(
                "عصرانه و فیلم؟",
                "یک انتخاب سبک برای این ساعت.",
                "یه چیز شانسی",
                if (hasCatalog) GreetingAction.SURPRISE else GreetingAction.NONE
            )
        )

        hour < 22 -> when {
            fridayEvening -> listOf(
                SmartGreeting(
                    "جمعه‌شب! آخر هفته شروع شد 🍿",
                    "امشب می‌شود یک فیلم بلند و بدون عجله.",
                    "فیلم‌ها را نشانم بده",
                    if (hasCatalog) GreetingAction.PICK_MOVIE else GreetingAction.NONE
                ),
                SmartGreeting(
                    "جمعه‌شب 🎬",
                    "کارِ این هفته تمام؛ یک سریال تازه شروع کنیم؟",
                    "سریال‌ها را نشانم بده",
                    if (hasCatalog) GreetingAction.PICK_SERIES else GreetingAction.NONE
                )
            )

            schoolNight -> listOf(
                SmartGreeting(
                    "یکشنبه‌شب 🌙",
                    "فردا دوشنبه است؛ یک قسمت و تمام.",
                    "همان نیمه‌کاره",
                    if (hasContinue) GreetingAction.CONTINUE_LAST else GreetingAction.NONE
                )
            )

            weekendMood -> listOf(
                SmartGreeting(
                    "آخر هفته است! 🎬",
                    "امشب می‌شود یک فیلم بلند و بدون عجله.",
                    "فیلم‌ها را نشانم بده",
                    if (hasCatalog) GreetingAction.PICK_MOVIE else GreetingAction.NONE
                ),
                SmartGreeting(
                    "شبِ تعطیل 🍿",
                    "پاپ‌کورن آماده؟ بهترین ساعت شب همین است.",
                    "یه چیز شانسی",
                    if (hasCatalog) GreetingAction.SURPRISE else GreetingAction.NONE
                )
            )

            else -> listOf(
                SmartGreeting(
                    "امشب چی می‌بینیم؟",
                    "بهترین ساعتِ شب برای یک انتخاب درست.",
                    "همان نیمه‌کاره",
                    if (hasContinue) GreetingAction.CONTINUE_LAST else GreetingAction.NONE
                ),
                SmartGreeting(
                    "شب بخیر 🌙",
                    "${persianWeekday(weekday)} شب؛ یک قسمت بگذاریم؟",
                    "سریال‌ها را نشانم بده",
                    if (hasCatalog) GreetingAction.PICK_SERIES else GreetingAction.NONE
                )
            )
        }

        else -> when {
            schoolNight -> listOf(
                SmartGreeting(
                    "یکشنبه‌شب، دیروقت 🌙",
                    "فردا دوشنبه است… یک قسمت، قول؟",
                    "همان نیمه‌کاره",
                    if (hasContinue) GreetingAction.CONTINUE_LAST else GreetingAction.NONE
                )
            )

            weekendMood -> listOf(
                SmartGreeting(
                    "شبِ تعطیل، عجله‌ای نیست 🌙",
                    "فردا صبح که زنگ ساعت ندارد.",
                    "یه چیز شانسی",
                    if (hasCatalog) GreetingAction.SURPRISE else GreetingAction.NONE
                ),
                SmartGreeting(
                    "آخر هفته و بیدار 🌙",
                    "یک فیلم دیگر هم ضرر ندارد.",
                    "فیلم‌ها را نشانم بده",
                    if (hasCatalog) GreetingAction.PICK_MOVIE else GreetingAction.NONE
                )
            )

            else -> listOf(
                SmartGreeting(
                    "دیروقت شد…",
                    "یک فیلم کوتاه یا ادامهٔ همان قبلی؟",
                    "همان نیمه‌کاره",
                    if (hasContinue) GreetingAction.CONTINUE_LAST else GreetingAction.NONE
                ),
                SmartGreeting(
                    "آخرِ شب 🌙",
                    "فردا کار داری‌ها… یک قسمت، قول؟",
                    "سریال‌ها را نشانم بده",
                    if (hasCatalog) GreetingAction.PICK_SERIES else GreetingAction.NONE
                )
            )
        }
    }

    val chosen = candidates[
        ((variant % candidates.size) + candidates.size) % candidates.size
    ]
    // An action the current data cannot serve is shown as plain text instead.
    return if (chosen.action == GreetingAction.NONE) chosen.copy(actionLabel = "") else chosen
}

private enum class Occasion {
    CHRISTMAS_EVE, CHRISTMAS, NEW_YEAR_EVE, NEW_YEAR, NOWRUZ, SIZDAH, YALDA
}

private fun persianWeekday(day: Int): String = when (day) {
    Calendar.SATURDAY -> "شنبه"
    Calendar.SUNDAY -> "یکشنبه"
    Calendar.MONDAY -> "دوشنبه"
    Calendar.TUESDAY -> "سه‌شنبه"
    Calendar.WEDNESDAY -> "چهارشنبه"
    Calendar.THURSDAY -> "پنجشنبه"
    else -> "جمعه"
}

/**
 * Gregorian → Jalali (Solar Hijri) conversion, so the greeting can recognise Nowruz and
 * Yalda without pulling in a calendar dependency.
 */
fun gregorianToJalali(gYear: Int, gMonth: Int, gDay: Int): Triple<Int, Int, Int> {
    val gDaysInMonth = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    val jDaysInMonth = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)

    val gy = gYear - 1600
    val gm = gMonth - 1
    val gd = gDay - 1

    var dayCount = 365 * gy + (gy + 3) / 4 - (gy + 99) / 100 + (gy + 399) / 400
    for (index in 0 until gm) dayCount += gDaysInMonth[index]
    val isLeap = (gYear % 4 == 0 && gYear % 100 != 0) || gYear % 400 == 0
    if (gm > 1 && isLeap) dayCount++
    dayCount += gd

    var jDayNo = dayCount - 79
    val cycles = jDayNo / 12053
    jDayNo %= 12053
    var jYear = 979 + 33 * cycles + 4 * (jDayNo / 1461)
    jDayNo %= 1461
    if (jDayNo >= 366) {
        jYear += (jDayNo - 1) / 365
        jDayNo = (jDayNo - 1) % 365
    }
    var month = 0
    while (month < 11 && jDayNo >= jDaysInMonth[month]) {
        jDayNo -= jDaysInMonth[month]
        month++
    }
    return Triple(jYear, month + 1, jDayNo + 1)
}
