package com.artt.rutina.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Сетка истории: ровно N дней, выравнивание по неделям Пн→Вс, последняя ячейка — сегодня. */
class HistoryGridTest {

    private val tuesday = LocalDate.of(2026, 9, 29) // вторник
    private val monday = LocalDate.of(2026, 9, 28)
    private val sunday = LocalDate.of(2026, 10, 4)

    @Test
    fun ровноШестьдесятДней() {
        val cells = historyGrid(tuesday).flatten()
        assertEquals(60, cells.count { it != null })
    }

    @Test
    fun дниИдутПодрядИЗаканчиваютсяСегодня() {
        val real = historyGrid(tuesday).flatten().filterNotNull()
        assertEquals(tuesday, real.last())
        assertEquals(tuesday.minusDays(59), real.first())
        real.zipWithNext { a, b ->
            assertEquals("дни должны идти без пропусков", a.plusDays(1), b)
        }
    }

    @Test
    fun колонкаСоответствуетДнюНедели() {
        historyGrid(tuesday).forEach { week ->
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
        val first = historyGrid(tuesday).first()
        // 29 сентября 2026 минус 59 дней = 1 августа 2026, суббота → 5 пустых ячеек перед ней
        assertEquals(5, first.count { it == null })
        assertNull(first[0])
        assertEquals(LocalDate.of(2026, 8, 1), first[5])
    }

    @Test
    fun вКаждойСтрокеРовноСемьЯчеек() {
        historyGrid(tuesday).forEach { week -> assertEquals(7, week.size) }
        historyGrid(sunday).forEach { week -> assertEquals(7, week.size) }
        historyGrid(LocalDate.of(2026, 8, 9), days = 8).forEach { week -> assertEquals(7, week.size) }
    }

    @Test
    fun последняяСтрокаДополненаПустымиЯчейками() {
        // окно 60 дней от вторника: 5+60=65 → последняя строка 2 дня + 5 пустых в конце
        val last = historyGrid(tuesday).last()
        assertEquals(2, last.count { it != null })
        assertEquals(5, last.count { it == null })
        assertEquals(tuesday, last.last { it != null })
    }

    @Test
    fun пустыхЯчеекРовноСдвигуПервогоДняОкна() {
        // сдвиг определяется днём недели самого раннего дня окна, а не сегодняшним днём
        listOf(tuesday, monday, sunday).forEach { today ->
            val expected = today.minusDays(59).dayOfWeek.value - 1
            assertEquals(expected, historyGrid(today).first().count { it == null })
        }
    }

    @Test
    fun еслиПервыйДеньОкнаПонедельникПустыхЯчеекНет() {
        // 2026-08-03 — понедельник; окно из 7 дней назад от 2026-08-09 (воскресенье)
        val grid = historyGrid(LocalDate.of(2026, 8, 9), days = 7)
        assertEquals(0, grid.first().count { it == null })
        assertEquals(7, grid.flatten().count { it != null })
    }

    @Test
    fun еслиОкноНачинаетсяСВоскресеньяСдвигШесть() {
        // 2026-08-02 — воскресенье; 8 дней назад от 2026-08-09
        val grid = historyGrid(LocalDate.of(2026, 8, 9), days = 8)
        assertEquals(6, grid.first().count { it == null })
        assertEquals(8, grid.flatten().count { it != null })
    }

    @Test
    fun тридцатьДнейДаютТридцатьЯчеек() {
        val cells = historyGrid(tuesday, days = 30).flatten()
        assertEquals(30, cells.count { it != null })
        assertEquals(tuesday, cells.filterNotNull().last())
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

    @Test
    fun подписьМесяцаСтоитНадКолонкойПервогоЧисла() {
        // сетка: колонка = неделя. Каждая подпись, кроме первой, должна стоять
        // на колонке, где встречается первое число месяца.
        val weeks = historyGrid(tuesday)
        val labels = monthLabels(weeks)

        labels.forEach { (weekIndex, day) ->
            if (weekIndex == 0) return@forEach // первая колонка подписывается всегда
            assertEquals(
                "подпись ${day} должна стоять на колонке с первым числом",
                1,
                day.dayOfMonth,
            )
            assertTrue(
                "дата ${day} должна быть на неделе $weekIndex",
                weeks[weekIndex].contains(day),
            )
        }
    }

    @Test
    fun сентябрьПодписанНадСвоейКолонкой() {
        val weeks = historyGrid(tuesday)
        val labels = monthLabels(weeks)
        val sept = labels.entries.first { it.value.monthValue == 9 }
        assertEquals(LocalDate.of(2026, 9, 1), sept.value)
        assertTrue(
            "1 сентября должно лежать в колонке ${sept.key}",
            weeks[sept.key].contains(LocalDate.of(2026, 9, 1)),
        )
    }

    @Test
    fun перваяКолонкаВсегдаПодписана() {
        listOf(tuesday, monday, LocalDate.of(2026, 9, 15)).forEach { today ->
            val labels = monthLabels(historyGrid(today))
            assertTrue("первая колонка должна быть подписана", labels.containsKey(0))
        }
    }

    @Test
    fun подписейМесяцевНеБольшеЧемМесяцевВОкне() {
        val labels = monthLabels(historyGrid(tuesday))
        // 60 дней — максимум 3 календарных месяца
        assertTrue("подписей ${labels.size}, ожидалось не больше 3", labels.size <= 3)
    }
}
