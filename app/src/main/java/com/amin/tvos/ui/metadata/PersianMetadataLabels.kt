package com.amin.tvos.ui.metadata

/** Small presentation-only translations for common provider/Wikidata genre labels. */
fun String.toPersianMetadataLabel(): String {
    val normalized = trim()
        .lowercase()
        .replace('_', ' ')
        .replace('-', ' ')
        .replace(Regex("""\s+"""), " ")
    return PERSIAN_METADATA_LABELS[normalized] ?: trim()
}

private val PERSIAN_METADATA_LABELS = mapOf(
    "action" to "اکشن",
    "adventure" to "ماجراجویی",
    "animation" to "انیمیشن",
    "biography" to "زندگی‌نامه",
    "comedy" to "کمدی",
    "crime" to "جنایی",
    "documentary" to "مستند",
    "drama" to "درام",
    "family" to "خانوادگی",
    "fantasy" to "فانتزی",
    "game show" to "مسابقه",
    "history" to "تاریخی",
    "horror" to "ترسناک",
    "music" to "موسیقی",
    "musical" to "موزیکال",
    "mystery" to "معمایی",
    "reality tv" to "رئالیتی‌شو",
    "romance" to "عاشقانه",
    "romantic comedy film" to "کمدی عاشقانه",
    "sci fi" to "علمی‌تخیلی",
    "science fiction" to "علمی‌تخیلی",
    "short" to "کوتاه",
    "short film" to "فیلم کوتاه",
    "slice of life" to "برشی از زندگی",
    "social" to "اجتماعی",
    "sport" to "ورزشی",
    "thriller" to "دلهره‌آور",
    "war" to "جنگی",
    "western" to "وسترن",
    "kids" to "کودک",
    "farsi film" to "فیلم ایرانی",
    "drama television series" to "سریال درام"
)
