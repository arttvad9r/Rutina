package com.artt.rutina.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * Срок дела: расчёт конца курса, остатка дней и того, попадает ли дело в конкретный день.
 * Тесты держат границы — день создания и последний день курса, — потому что ошибка ровно
 * на один день тут выглядит как «случайно исчезло» и её легко не заметить глазами.
 */
class CourseTest {

    private val zone: ZoneId = ZoneId.of("Europe/Moscow")

    private fun millisAt(date: LocalDate): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    private fun habit(start: LocalDate, days: Int = NO_LIMIT) =
        Habit(id = 1, name = "Лекарство", createdAt = millisAt(start), durationDays = days)

    @Test
    fun `бессрочное дело не имеет конца курса`() {
        val h = habit(LocalDate.of(2026, 9, 1), NO_LIMIT)
        assertNull(courseEnd(h, zone))
        assertNull(daysLeft(h, LocalDate.of(2026, 9, 1), zone))
    }

    @Test
    fun `курс на 30 дней включает день начала`() {
        val start = LocalDate.of(2026, 9, 1)
        // 1 сентября — первый день, значит тридцатый — 30 сентября
        assertEquals(LocalDate.of(2026, 9, 30), courseEnd(start, 30))
        // и наоборот: курс на 1 день заканчивается в день создания
        assertEquals(start, courseEnd(start, 1))
    }

    @Test
    fun `остаток считается включая текущий день`() {
        val h = habit(LocalDate.of(2026, 9, 1), 30)
        assertEquals(30, daysLeft(h, LocalDate.of(2026, 9, 1), zone))
        assertEquals(1, daysLeft(h, LocalDate.of(2026, 9, 30), zone))
        assertEquals(0, daysLeft(h, LocalDate.of(2026, 10, 1), zone))
    }

    @Test
    fun `последний день курса ещё показывается, следующий уже нет`() {
        val h = habit(LocalDate.of(2026, 9, 1), 30)
        assertFalse(finishedOn(h, LocalDate.of(2026, 9, 30), zone))
        assertTrue(habitOnDay(h, LocalDate.of(2026, 9, 30), zone))

        assertTrue(finishedOn(h, LocalDate.of(2026, 10, 1), zone))
        assertFalse(habitOnDay(h, LocalDate.of(2026, 10, 1), zone))
    }

    @Test
    fun `до дня создания дело в списке не показывается`() {
        val h = habit(LocalDate.of(2026, 9, 10))
        assertFalse(habitOnDay(h, LocalDate.of(2026, 9, 9), zone))
        assertTrue(habitOnDay(h, LocalDate.of(2026, 9, 10), zone))
    }

    @Test
    fun `бессрочное дело показывается и в прошлом, и в будущем`() {
        val h = habit(LocalDate.of(2026, 9, 10), NO_LIMIT)
        assertTrue(habitOnDay(h, LocalDate.of(2026, 9, 10), zone))
        assertTrue(habitOnDay(h, LocalDate.of(2027, 1, 1), zone))
    }

    @Test
    fun `подпись срока говорит о остатке а потом о завершении`() {
        val h = habit(LocalDate.of(2026, 9, 1), 30)
        assertEquals("осталось 30 из 30 дней", courseLabel(h, LocalDate.of(2026, 9, 1), zone))
        assertEquals("осталось 2 из 30 дней", courseLabel(h, LocalDate.of(2026, 9, 29), zone))
        assertEquals("последний день из 30", courseLabel(h, LocalDate.of(2026, 9, 30), zone))
        assertEquals("курс завершён · 30 дней", courseLabel(h, LocalDate.of(2026, 10, 5), zone))
        // у бессрочного дела подписи нет — лишний текст в карточке не нужен
        assertEquals("", courseLabel(habit(LocalDate.of(2026, 9, 1), NO_LIMIT), LocalDate.of(2026, 9, 5), zone))
    }

    @Test
    fun `склонение слова день`() {
        assertEquals("день", pluralDays(1))
        assertEquals("дня", pluralDays(3))
        assertEquals("дней", pluralDays(5))
        assertEquals("дней", pluralDays(11))
        // 21 — исключение из «много»: 21 день, но 25 дней
        assertEquals("день", pluralDays(21))
        assertEquals("дня", pluralDays(22))
        assertEquals("дней", pluralDays(25))
    }
}
