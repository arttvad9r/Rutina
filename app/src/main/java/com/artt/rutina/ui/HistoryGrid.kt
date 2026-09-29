package com.artt.rutina.ui

import java.time.LocalDate

/**
 * Раскладка календаря истории: `days` дней, заканчивая сегодняшним, разбитые по неделям Пн→Вс.
 * Пустые ячейки в начале первой недели и в конце последней — null (нужны, чтобы все ряды
 * были одинаковой ширины при растяжении на экран).
 */
internal fun historyGrid(today: LocalDate, days: Int = 60): List<List<LocalDate?>> {
    require(days > 0) { "дней должно быть больше нуля" }
    val first = today.minusDays((days - 1).toLong())
    val leading = first.dayOfWeek.value - 1 // Пн = 1 → сдвиг 0
    val cells = MutableList<LocalDate?>(leading) { null }
    repeat(days) { cells.add(first.plusDays(it.toLong())) }
    // добиваем последнюю неделю пустыми ячейками до полных 7
    while (cells.size % 7 != 0) cells.add(null)
    return cells.chunked(7)
}

/**
 * Подписи месяцев для сетки, где **колонка = неделя**, а строка = день недели.
 *
 * Ключ — индекс недели-колонки, значение — дата, которой подписывается эта колонка.
 * Подпись ставится на колонку, в которой встречается первое число месяца: тогда она
 * стоит ровно над своим месяцем и не съезжает. Первая колонка подписывается всегда,
 * иначе сетка начинается без контекста.
 */
internal fun monthLabels(rows: List<List<LocalDate?>>): Map<Int, LocalDate> {
    val labels = mutableMapOf<Int, LocalDate>()

    rows.forEachIndexed { weekIndex, week ->
        week.firstOrNull { it?.dayOfMonth == 1 }?.let { labels[weekIndex] = it }
    }

    // первая колонка: подписываем самый ранний видимый день
    rows.firstOrNull()?.firstOrNull { it != null }?.let { earliest ->
        if (!labels.containsKey(0)) labels[0] = earliest
    }
    return labels
}
