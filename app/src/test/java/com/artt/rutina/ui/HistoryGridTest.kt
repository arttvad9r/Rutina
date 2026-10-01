package com.artt.rutina.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Календарь месяца: строки — недели Пн→Вс, ячейки только текущего месяца, сегодня в конце. */
class HistoryGridTest {
    @Test
    fun сеткаНеЗависитОтВыбранногоДняМесяца() {
        for (month in 1..12) {
            val first = LocalDate.of(2026, month, 1)
            for (day in 1..first.lengthOfMonth()) {
                assertEquals(monthGrid(first), monthGrid(first.withDayOfMonth(day)))
            }
        }
    }

    @Test
    fun сегодняшняяОтметкаПродолжаетВчерашнююСерию() {
        val days = setOf("2026-09-29", "2026-09-30")
        assertEquals(2, streakOf(days, LocalDate.of(2026, 10, 1)))
        assertEquals(3, streakOf(days + "2026-10-01", LocalDate.of(2026, 10, 1)))
    }

    private val tuesday = LocalDate.of(2026, 9, 29) // вторник

    @Test
    fun вСеткеВсеДниТекущегоМесяца() {
        val cells = monthGrid(tuesday).flatten().filterNotNull()
        assertEquals(30, cells.size) // в сентябре 2026 — 30 дней
        assertEquals(LocalDate.of(2026, 9, 1), cells.first())
        assertEquals(LocalDate.of(2026, 9, 30), cells.last())
    }

    @Test
    fun колонкаСоответствуетДнюНедели() {
        monthGrid(tuesday).forEach { week ->
            week.forEachIndexed { column, day ->
                if (day != null) {
                    assertEquals(
                        "день ${day} должен стоять в колонке своего дня недели",
                        day.dayOfWeek.value - 1,
                        column,
                    )
                }
            }
        }
    }

    @Test
    fun перваяНеделяДополненаПустымиЯчейками() {
        // 1 сентября 2026 — вторник → 1 пустая ячейка (Пн) перед ним
        val first = monthGrid(tuesday).first()
        assertEquals(1, first.count { it == null })
        assertNull(first[0])
        assertEquals(LocalDate.of(2026, 9, 1), first[1])
    }

    @Test
    fun вКаждойСтрокеРовноСемьЯчеек() {
        monthGrid(tuesday).forEach { week -> assertEquals(7, week.size) }
        // февраль, начинающийся с понедельника (невисокосный): 2026-02-01 — воскресенье,
        // поэтому берём март 2026: 1 марта — воскреседельник → сдвиг 6
        monthGrid(LocalDate.of(2026, 3, 15)).forEach { week -> assertEquals(7, week.size) }
    }

    @Test
    fun последняяНеделяДополненаПустымиЯчейками() {
        val last = monthGrid(tuesday).last()
        // 30 сентября 2026 — среда → колонка 2, дальше 4 пустых
        assertEquals(3, last.count { it != null })
        assertEquals(4, last.count { it == null })
        assertEquals(LocalDate.of(2026, 9, 30), last.last { it != null })
    }

    @Test
    fun вСеткеНетДнейДругихМесяцев() {
        monthGrid(tuesday).flatten().filterNotNull().forEach { d ->
            assertEquals(9, d.monthValue)
            assertEquals(2026, d.year)
        }
    }

    @Test
    fun рекордСерииИщетсяПоВсейИстории() {
        // 1–3 сентября подряд + 10 сентября: рекорд 3, текущая 0
        val days = setOf("2026-09-01", "2026-09-02", "2026-09-03", "2026-09-10")
        assertEquals(3, bestStreak(days, LocalDate.of(2026, 9, 29)))
    }

    @Test
    fun рекордРавенОдномуПриРазбросанныхОтметках() {
        val days = setOf("2026-09-01", "2026-09-05", "2026-09-20")
        assertEquals(1, bestStreak(days, LocalDate.of(2026, 9, 29)))
    }

    @Test
    fun рекордНульПриПустойИстории() {
        assertEquals(0, bestStreak(emptySet(), LocalDate.of(2026, 9, 29)))
    }

    @Test
    fun рекордУчитываетИдущуюСерию() {
        val today = LocalDate.of(2026, 9, 29)
        val days = (0L until 5L).map { today.minusDays(it).toString() }.toSet()
        assertEquals(5, bestStreak(days, today))
    }
}
