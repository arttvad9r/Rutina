package com.artt.rutina.ui

import java.time.LocalDate
import java.time.YearMonth

/**
 * Календарь текущего месяца: строки — недели Пн→Вс, колонки — дни недели.
 * Дни соседних месяцев, попадающие в первую/последнюю неделю, — null: сетка остаётся
 * прямоугольной, а чужие дни не выглядят «пропущенными» отметками.
 */
internal fun monthGrid(today: LocalDate): List<List<LocalDate?>> {
    val month = YearMonth.from(today)
    val leading = today.dayOfWeek.value - 1 // Пн = 1 → сдвиг 0
    val cells = MutableList<LocalDate?>(leading) { null }
    for (day in 1..month.lengthOfMonth()) {
        cells.add(month.atDay(day))
    }
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
