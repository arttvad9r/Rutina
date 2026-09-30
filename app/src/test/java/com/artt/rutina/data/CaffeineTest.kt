package com.artt.rutina.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

/**
 * Разбивка нормы кофеина на приёмы и расписание этих приёмов.
 *
 * Тесты держат два обещания: дозы всегда кратны 50 мг и последний приём не заезжает
 * за «8 часов до сна». Ошибка на границе тут не видна глазом, но именно из-за неё
 * кофеин мешает засыпанию.
 */
class CaffeineTest {

    @Test
    fun `норма делится на приёмы кратными пятидесяти и большие идут первыми`() {
        // ровно примеры из задачи
        assertEquals(listOf(100, 100, 50), CaffeineLogic.splitDoses(250))
        assertEquals(listOf(100, 50, 50), CaffeineLogic.splitDoses(200))
        // приёмы отличаются не больше чем на шаг: 500 → 150×3 + остаток 50 первому
        assertEquals(listOf(200, 150, 150), CaffeineLogic.splitDoses(500))
        assertEquals(listOf(150, 100, 100), CaffeineLogic.splitDoses(350))
        assertEquals(listOf(50, 50, 50), CaffeineLogic.splitDoses(150))
    }

    @Test
    fun `сумма приёмов всегда равна заданной норме`() {
        for (total in 50..600 step 50) {
            assertEquals("норма $total", total, CaffeineLogic.splitDoses(total).sum())
        }
    }

    @Test
    fun `каждая доза кратна пятидесяти`() {
        for (total in 50..600 step 10) {
            CaffeineLogic.splitDoses(total).forEach { mg ->
                assertEquals("норма $total, доза $mg", 0, mg % CaffeineLogic.STEP_MG)
            }
        }
    }

    @Test
    fun `маленькая норма не даёт нулевых приёмов`() {
        // 50 мг — это один приём, а не три по «50 + 0 + 0»
        assertEquals(listOf(50), CaffeineLogic.splitDoses(50))
        assertEquals(listOf(50, 50), CaffeineLogic.splitDoses(100))
        assertTrue(CaffeineLogic.splitDoses(50).none { it == 0 })
    }

    @Test
    fun `последний приём ровно за восемь часов до сна`() {
        // подъём 08:00, сон 01:00 → сон на следующие сутки, крайний срок 17:00
        val s = CaffeineLogic.schedule(wakeMinutes = 8 * 60, bedtimeMinutes = 1 * 60)
        assertEquals(17 * 60, s.lastAllowedMinutes)
        assertEquals(listOf(8 * 60, 12 * 60 + 30, 17 * 60), s.times)
        assertEquals(270, s.intervalMinutes)
    }

    @Test
    fun `интервал опускается вниз до пяти минут чтобы не проехать срок`() {
        // окно 08:00..17:00 = 540 минут на 2 промежутка → ровно 270
        val s = CaffeineLogic.schedule(wakeMinutes = 9 * 60, bedtimeMinutes = 1 * 60)
        // подъём 09:00, крайний срок 17:00, окно 480 → 240
        assertEquals(240, s.intervalMinutes)
        assertEquals(17 * 60, s.times.last())
    }

    @Test
    fun `интервал не превышает крайний срок`() {
        // проверяем на разных подъёмах, что последний приём не позже срока
        for (wakeHour in 4..14) {
            val wake = wakeHour * 60
            val s = CaffeineLogic.schedule(wakeMinutes = wake, bedtimeMinutes = 1 * 60)
            if (s.times.isNotEmpty()) {
                assertTrue(
                    "подъём $wakeHour: последний приём ${s.times.last()} позже ${s.lastAllowedMinutes}",
                    s.times.last() <= s.lastAllowedMinutes,
                )
            }
        }
    }

    @Test
    fun `сон раньше подъёма считается через сутки`() {
        // сон 01:00 при подъёме 08:00 — это следующие сутки, а не предыдущие
        assertEquals(25 * 60, CaffeineLogic.sleepMinutes(8 * 60, 1 * 60))
        // сон 23:00 при подъёме 08:00 — те же сутки
        assertEquals(23 * 60, CaffeineLogic.sleepMinutes(8 * 60, 23 * 60))
    }

    @Test
    fun `слишком поздно встал — расписания нет вместо приёма ночью`() {
        // подъём 23:00, сон 01:00: крайний срок 17:00 уже прошёл
        val s = CaffeineLogic.schedule(wakeMinutes = 23 * 60, bedtimeMinutes = 1 * 60)
        assertTrue(s.times.isEmpty())
        assertNull(s.intervalMinutes)
    }

    @Test
    fun `план дня связывает дозы и время приёмов`() {
        val plan = CaffeineLogic.plan(
            targetMg = 250,
            wakeMinutes = 8 * 60,
            bedtimeMinutes = 1 * 60,
        )
        assertEquals(listOf(100, 100, 50), plan.doses)
        assertEquals(3, plan.schedule.times.size)
    }

    @Test
    fun `ближайший приём по числу отмеченных`() {
        val doses = listOf(100, 100, 50)
        assertEquals(0, CaffeineLogic.nextPendingIndex(doses, doneCount = 0))
        assertEquals(2, CaffeineLogic.nextPendingIndex(doses, doneCount = 2))
        assertNull(CaffeineLogic.nextPendingIndex(doses, doneCount = 3))
    }

    @Test
    fun `минуты и время суток переводятся туда и обратно`() {
        assertEquals(LocalTime.of(8, 0), CaffeineLogic.timeOf(8 * 60))
        assertEquals(8 * 60, CaffeineLogic.minutesOf(LocalTime.of(8, 0)))
        assertEquals(LocalTime.of(23, 59), CaffeineLogic.timeOf(23 * 60 + 59))
    }
}
