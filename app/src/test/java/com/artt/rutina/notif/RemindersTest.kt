package com.artt.rutina.notif

import com.artt.rutina.ui.daysWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Расчёт ближайшего срабатывания будильника: время в прошлом → срабатывание завтра.
 * Все проверки идут от явно заданного «сейчас», без зависимости от системных часов.
 */
class RemindersTest {

    private val zone: ZoneId = ZoneId.of("Europe/Moscow")

    private fun now(hour: Int, minute: Int): LocalDateTime =
        LocalDateTime.of(2026, 9, 29, hour, minute)

    private fun deltaMinutes(hour: Int, minute: Int, nowHour: Int, nowMinute: Int): Long {
        val nowDt = now(nowHour, nowMinute)
        val target = Reminders.nextTriggerAt(hour, minute, nowDt, zone)
        return (target - nowDt.atZone(zone).toInstant().toEpochMilli()) / 60_000L
    }

    @Test
    fun времяЕщёНеНаступило_срабатываетСегодня() {
        // назначено 08:00, сейчас 07:30 → через 30 минут
        assertEquals(30L, deltaMinutes(8, 0, 7, 30))
    }

    @Test
    fun времяУжеПрошло_срабатываетЗавтра() {
        // назначено 08:00, сейчас 09:00 → через 23 часа
        assertEquals(23L * 60L, deltaMinutes(8, 0, 9, 0))
    }

    @Test
    fun ровноНазначенноеВремя_срабатываетЧерезСутки() {
        assertEquals(24L * 60L, deltaMinutes(8, 0, 8, 0))
    }

    @Test
    fun вечернееНапоминание_срабатываетСегодня() {
        assertEquals(14L * 60L, deltaMinutes(22, 0, 8, 0))
    }

    @Test
    fun минутнаяТочность_срабатываетВТуЖеМинуту() {
        // назначено 22:30, сейчас 22:00 → через 30 минут
        assertEquals(30L, deltaMinutes(22, 30, 22, 0))
    }

    @Test
    fun времяОкруглено_секундыИНаносекундыНулевые() {
        val target = Instant.ofEpochMilli(Reminders.nextTriggerAt(8, 0, now(7, 30), zone))
            .atZone(zone)
            .toLocalDateTime()
        assertEquals(0, target.second)
        assertEquals(0, target.nano)
        assertEquals(8, target.hour)
        assertEquals(0, target.minute)
    }

    @Test
    fun переходЧерезПолночь_датаСледующегоДня() {
        val target = Instant.ofEpochMilli(Reminders.nextTriggerAt(23, 59, now(23, 59), zone))
            .atZone(zone)
            .toLocalDateTime()
        assertEquals(30, target.dayOfMonth)
    }

    @Test
    fun склоненияДней() {
        assertEquals("1 день", daysWord(1))
        assertEquals("2 дня", daysWord(2))
        assertEquals("4 дня", daysWord(4))
        assertEquals("5 дней", daysWord(5))
        assertEquals("11 дней", daysWord(11))
        assertEquals("21 день", daysWord(21))
        assertEquals("22 дня", daysWord(22))
        assertEquals("0 дней", daysWord(0))
        assertEquals("30 дней", daysWord(30))
    }

    @Test
    fun кодыЗапросовРазличаютсяДляБудильникаИОтложенногоНапоминания() {
        assertTrue(Reminders.requestCode(1) != Reminders.requestCode(2))
        assertTrue(Reminders.requestCode(7) != Reminders.snoozeRequestCode(7))
        assertTrue(Reminders.notificationId(1, Reminders.SLOT_REMIND) !=
            Reminders.notificationId(1, Reminders.SLOT_MISSED))
        assertTrue(Reminders.notificationId(1, 0) != Reminders.notificationId(2, 0))
    }
}
