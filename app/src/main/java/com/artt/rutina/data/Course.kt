package com.artt.rutina.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Длительность дела и всё, что из неё следует.
 *
 * Смысл: дело можно завести на срок — «пропить курс 30 дней» — или бессрочно.
 * Срок считается от дня создания, поэтому редактирование длительности позже
 * просто сдвигает конец курса, а не начинает счёт заново.
 *
 * Здесь только чистые функции без Android: их проверяют юнит-тесты.
 */

/** Длительность «без ограничений». */
const val NO_LIMIT = 0

/** День создания дела в локальной зоне. */
internal fun startDay(createdAt: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    runCatching { Instant.ofEpochMilli(createdAt).atZone(zone).toLocalDate() }
        .getOrDefault(LocalDate.of(1970, 1, 1))

/**
 * Последний день курса (включительно) или null, если дело бессрочное.
 * Курс на 30 дней, начатый 1-го, длится по 30-е: день создания уже считается первым.
 */
internal fun courseEnd(start: LocalDate, durationDays: Int): LocalDate? =
    if (durationDays <= 0) null else start.plusDays(durationDays.toLong() - 1)

/** Последний день курса для конкретного дела. */
internal fun courseEnd(habit: Habit, zone: ZoneId = ZoneId.systemDefault()): LocalDate? =
    courseEnd(startDay(habit.createdAt, zone), habit.durationDays)

/** Сколько дней курса осталось на [day], включая сам день. null — дело бессрочное. */
internal fun daysLeft(habit: Habit, day: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Int? {
    val end = courseEnd(habit, zone) ?: return null
    val left = ChronoUnit.DAYS.between(day, end) + 1
    return left.coerceAtLeast(0).toInt()
}

/** Курс закончился до [day]: дело больше не показывается в делах на день. */
internal fun finishedOn(habit: Habit, day: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Boolean {
    if (habit.finishedAt?.let { !day.isBefore(startDay(it, zone)) } == true) return true
    val end = courseEnd(habit, zone) ?: return false
    return day.isAfter(end)
}

/**
 * Дело относится к дню [day]: оно уже создано и курс на этот день ещё не закончился.
 * Прошедшие дни считаются по тем же правилам — листая назад, видно расписание того дня.
 */
internal fun habitOnDay(habit: Habit, day: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Boolean {
    if (day.isBefore(startDay(habit.createdAt, zone))) return false
    return !finishedOn(habit, day, zone)
}

/** Доступные даты календаря; день ручного завершения ещё входит в историю. */
internal fun canMarkDay(habit: Habit, day: LocalDate, today: LocalDate): Boolean =
    !day.isAfter(today) && !day.isBefore(startDay(habit.createdAt)) &&
        (courseEnd(habit)?.let { !day.isAfter(it) } ?: true) &&
        (habit.finishedAt?.let { !day.isAfter(startDay(it)) } ?: true)

/**
 * Подпись о сроке для карточки и истории: «без срока», «осталось 12 дней»,
 * «курс завершён». Пустая строка — когда говорить нечего (бессрочное дело).
 */
internal fun courseLabel(habit: Habit, day: LocalDate, zone: ZoneId = ZoneId.systemDefault()): String =
    courseStatusText(startDay(habit.createdAt, zone), habit.durationDays, day)

/** То же, но по «сырым» значениям — удобно там, где Habit целиком недоступен. */
internal fun courseStatusText(start: LocalDate, durationDays: Int, day: LocalDate): String {
    if (durationDays <= 0) return ""
    val end = courseEnd(start, durationDays) ?: return ""
    if (day.isAfter(end)) return "курс завершён · ${durationDays} " + pluralDays(durationDays)
    val left = (ChronoUnit.DAYS.between(day, end) + 1).coerceAtLeast(0).toInt()
    return when {
        left <= 1 -> "последний день из $durationDays"
        else -> "осталось $left из $durationDays " + pluralDays(durationDays)
    }
}

/** Склонение слова «день» — нужно и подписи срока, и тестам. */
internal fun pluralDays(n: Int): String {
    val mod100 = n % 100
    val mod10 = n % 10
    return when {
        mod100 in 11..14 -> "дней"
        mod10 == 1 -> "день"
        mod10 in 2..4 -> "дня"
        else -> "дней"
    }
}
