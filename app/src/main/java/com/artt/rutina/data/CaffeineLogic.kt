package com.artt.rutina.data

import java.time.LocalTime

/**
 * Логика трекера кофеина: разбивка дневной нормы на приёмы и расписание этих приёмов.
 *
 * Дозы всегда кратны 50 мг (таблетки по 50), большие уходят на первые приёмы:
 * 250 → 100 + 100 + 50, 200 → 100 + 50 + 50. Последний приём — не позже чем за
 * 8 часов до сна, иначе кофеин мешает засыпанию.
 *
 * Здесь только чистые функции: их проверяют юнит-тесты.
 */
object CaffeineLogic {

    /** Шаг дозы: таблетки по 50 мг. */
    const val STEP_MG = 50

    /** Сколько приёмов в день. */
    const val MOMENTS = 3

    /** Кофеин должен закончиться минимум за столько часов до сна. */
    const val FREE_HOURS_BEFORE_SLEEP = 8

    const val MIN_TARGET = 50
    const val MAX_TARGET = 600
    const val DEFAULT_TARGET = 200

    /** Время сна и подъёма по умолчанию: 01:00 и 08:00. */
    const val DEFAULT_BEDTIME_MINUTES = 1 * 60
    const val DEFAULT_WAKE_MINUTES = 8 * 60

    /** Таймер по умолчанию — 2 часа. */
    const val DEFAULT_TIMER_MINUTES = 120

    /** Шаг кнопок «−» и «+» у таймера. */
    const val TIMER_STEP_MINUTES = 15

    /** Верхняя граница таймера — сутки: больше ждать нечего. */
    const val MAX_TIMER_MINUTES = 24 * 60

    /**
     * Разбивает норму на приёмы, кратные 50 мг. Большие дозы идут первыми:
     * 250 → 100 + 100 + 50, 200 → 100 + 50 + 50.
     *
     * Если норма меньше, чем [moments] × 50, приёмов становится меньше — нулевых
     * доз в расписании быть не должно.
     */
    fun splitDoses(totalMg: Int, moments: Int = MOMENTS): List<Int> {
        val total = totalMg.coerceAtLeast(STEP_MG)
        val fits = (total / STEP_MG).coerceAtLeast(1)
        val n = moments.coerceIn(1, fits)
        val base = (total / n / STEP_MG) * STEP_MG
        var rest = total - base * n
        return List(n) {
            val extra = if (rest >= STEP_MG) {
                rest -= STEP_MG
                STEP_MG
            } else {
                0
            }
            base + extra
        }
    }

    /** Время суток в минутах от полуночи. */
    fun minutesOf(time: LocalTime): Int = time.hour * 60 + time.minute

    fun timeOf(minutes: Int): LocalTime =
        LocalTime.of((minutes % (24 * 60)) / 60, (minutes % (24 * 60)) % 60)

    /**
     * Когда сон наступает раньше подъёма «по часам» (сон 01:00, подъём 08:00),
     * ко сну нужно добавить сутки, чтобы получить реальный интервал бодрствования.
     */
    fun sleepMinutes(wakeMinutes: Int, bedtimeMinutes: Int): Int =
        if (bedtimeMinutes > wakeMinutes) bedtimeMinutes else bedtimeMinutes + 24 * 60

    /** Крайний срок последнего приёма: сон минус 8 часов. */
    fun lastAllowedMinutes(wakeMinutes: Int, bedtimeMinutes: Int): Int =
        sleepMinutes(wakeMinutes, bedtimeMinutes) - FREE_HOURS_BEFORE_SLEEP * 60

    /**
     * Расписание приёмов: от подъёма до крайнего срока, интервал поровну между приёмами.
     * Интервал опускается до пяти минут вниз, чтобы последний приём не уехал за срок.
     * [intervalMinutes] = null — окно слишком узкое, расписание построить нельзя.
     */
    fun schedule(wakeMinutes: Int, bedtimeMinutes: Int, moments: Int = MOMENTS): IntakeSchedule {
        val lastAllowed = lastAllowedMinutes(wakeMinutes, bedtimeMinutes)
        val available = lastAllowed - wakeMinutes
        val n = moments.coerceAtLeast(1)

        if (available < 0) {
            return IntakeSchedule(
                times = emptyList(),
                lastAllowedMinutes = lastAllowed,
                intervalMinutes = null,
            )
        }

        if (n == 1) {
            return IntakeSchedule(
                times = listOf(wakeMinutes),
                lastAllowedMinutes = lastAllowed,
                intervalMinutes = null,
            )
        }

        val raw = available / (n - 1)
        val interval = ((raw / 5) * 5).coerceAtLeast(5)
        return IntakeSchedule(
            times = List(n) { wakeMinutes + interval * it },
            lastAllowedMinutes = lastAllowed,
            intervalMinutes = interval,
        )
    }

    /** Полный план дня: дозы и времена приёмов. */
    fun plan(
        targetMg: Int,
        wakeMinutes: Int,
        bedtimeMinutes: Int,
        moments: Int = MOMENTS,
    ): CaffeinePlan {
        val doses = splitDoses(targetMg, moments)
        val schedule = schedule(wakeMinutes, bedtimeMinutes, doses.size)
        return CaffeinePlan(
            doses = doses,
            schedule = schedule,
        )
    }

    /** Сколько мг выпито за день. */
    fun totalMg(intakes: List<CaffeineIntake>): Int = intakes.sumOf { it.mg }

    /**
     * Рекомендуемое время следующего приёма.
     *
     * Если приёмы уже отмечали — это интервал расписания от фактической отметки: отметка
     * позже расписания сдвигает день, а не отменяет его. До первой отметки рекомендуем
     * ближайшее время из расписания, которое ещё не прошло.
     *
     * null — рекомендовать нечего: приём один (интервала нет), окно дня закрыто или
     * сегодняшние времена уже прошли.
     */
    fun nextIntakeMinutes(
        nowMinutes: Int,
        schedule: IntakeSchedule,
        lastIntakeMinutes: Int?,
    ): Int? {
        if (lastIntakeMinutes != null) {
            val interval = schedule.intervalMinutes ?: return null
            val next = lastIntakeMinutes + interval
            return if (next > schedule.lastAllowedMinutes) null else next
        }
        // Время приёма рекомендуется и в свою минуту: подсказка честно говорит «примите
        // сейчас», а не отправляет к следующему приёму за четыре часа.
        return schedule.times.firstOrNull { it >= nowMinutes }
    }

    /**
     * Сколько минут поставить в таймер: от «сейчас» до рекомендованного приёма, округляя
     * до шага кнопок (время приёма от этого заметно не уезжает, а кнопки остаются рабочими).
     * Если рекомендации нет или она уже прошла — обычные два часа: таймер остаётся ручным.
     */
    fun timerMinutes(nowMinutes: Int, recommendedMinutes: Int?): Int {
        // Приём, рекомендованный «прямо сейчас», даёт минимальный шаг: таймер напомнит
        // о нём, а не отправит ждать два часа.
        val left = recommendedMinutes?.let { (it - nowMinutes).coerceAtLeast(0) }
        val base = left ?: DEFAULT_TIMER_MINUTES
        val rounded = (base + TIMER_STEP_MINUTES / 2) / TIMER_STEP_MINUTES * TIMER_STEP_MINUTES
        return rounded.coerceIn(TIMER_STEP_MINUTES, MAX_TIMER_MINUTES)
    }

    /**
     * Ближайший неотмеченный приём: по нему запускается таймер.
     * Возвращает индекс приёма или null, если всё отмечено.
     */
    fun nextPendingIndex(doses: List<Int>, doneCount: Int): Int? =
        if (doneCount >= doses.size) null else doneCount.coerceAtLeast(0)
}

/** Расписание приёмов на день. */
data class IntakeSchedule(
    /** Времена приёмов в минутах от полуночи. */
    val times: List<Int>,
    /** Крайний срок последнего приёма в минутах от полуночи. */
    val lastAllowedMinutes: Int,
    /** Интервал между приёмами; null — приём один или окно слишком узкое. */
    val intervalMinutes: Int?,
)

/** План дня по кофеину. */
data class CaffeinePlan(
    val doses: List<Int>,
    val schedule: IntakeSchedule,
)
