package com.amin.tvos.data

/** Public date claims use Gregorian years; Iranian catalog dates can be upload years. */
fun publicMatchYear(serviceId: String, catalogYear: String, knownYear: String): Int? {
    fun parse(value: String): Int? = value.map { c ->
        when (c) {
            in '۰'..'۹' -> ('0'.code + c.code - '۰'.code).toChar()
            in '٠'..'٩' -> ('0'.code + c.code - '٠'.code).toChar()
            else -> c
        }
    }.joinToString("").filter(Char::isDigit).take(4).toIntOrNull()
    val known = parse(knownYear)
    val catalog = parse(catalogYear)
    val iranian = serviceId.equals("parsiflix", true)
    val value = if (iranian && known in 1300..1499) known else catalog ?: known
    return when {
        value in 1300..1499 -> value?.plus(621) // Matching permits +/-1 for calendar boundaries.
        iranian -> null
        value in 1900..2200 -> value
        else -> null
    }
}
