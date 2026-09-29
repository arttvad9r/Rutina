package com.artt.rutina.ui

/** «1 день», «2 дня», «5 дней» — русские склонения. */
internal fun plural(n: Int, one: String, few: String, many: String): String {
    val mod100 = n % 100
    val mod10 = n % 10
    return when {
        mod100 in 11..14 -> many
        mod10 == 1 -> one
        mod10 in 2..4 -> few
        else -> many
    }
}

internal fun daysWord(n: Int): String = "$n " + plural(n, "день", "дня", "дней")
