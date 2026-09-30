package com.artt.rutina.notif

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.artt.rutina.MainActivity
import com.artt.rutina.data.Habit
import com.artt.rutina.data.finishedOn
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Планирование напоминаний через AlarmManager: на каждый день — повтор
 * ровно в заданное время (будильник, пробивается сквозь doze).
 */
object Reminders {

    const val CHANNEL = "rutina_reminders"

    fun ensureChannel(context: Context) {
        val mgr = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL,
            "Напоминания о рутине",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Ровно в назначенное время: прими лекарство, закапай капли и т.п."
            enableVibration(true)
        }
        mgr.createNotificationChannel(channel)
    }

    /** Код запроса для PendingIntent будильника. */
    fun requestCode(habitId: Long) = habitId.toInt()

    /** Отдельный код для отложенного напоминания, чтобы не перетирать основной будильник. */
    fun snoozeRequestCode(habitId: Long) = -habitId.toInt()

    private fun pending(context: Context, habit: Habit): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMIND
            putExtra(EXTRA_HABIT_ID, habit.id)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode(habit.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun schedule(context: Context, habit: Habit) {
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        // Курс, у которого срок вышел, замолкает сам: будильник на завтра уже не нужен.
        if (habit.hour < 0 || !habit.active || finishedOn(habit, LocalDate.now())) {
            cancel(context, habit)
            return
        }
        val pi = pending(context, habit)
        val trigger = nextTriggerAt(habit.hour, habit.minute)
        val show = PendingIntent.getActivity(
            context,
            requestCode(habit.id),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val info = AlarmManager.AlarmClockInfo(trigger, show)
        try {
            alarm.setAlarmClock(info, pi)
        } catch (e: SecurityException) {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
        }
    }

    fun cancel(context: Context, habit: Habit) {
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        alarm.cancel(pending(context, habit))
    }

    /** Ближайший момент наступления hour:minute — сегодня или завтра. */
    fun nextTriggerAt(hour: Int, minute: Int): Long =
        nextTriggerAt(hour, minute, LocalDateTime.now(ZoneId.systemDefault()), ZoneId.systemDefault())

    fun nextTriggerAt(hour: Int, minute: Int, now: LocalDateTime, zone: ZoneId): Long {
        var target = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!target.isAfter(now)) target = target.plusDays(1)
        return target.atZone(zone).toInstant().toEpochMilli()
    }

    /** Отложить конкретное напоминание на N минут (разово), не меняя расписание. */
    fun snooze(context: Context, habit: Habit, minutes: Int) {
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMIND
            putExtra(EXTRA_HABIT_ID, habit.id)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            snoozeRequestCode(habit.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val trigger = System.currentTimeMillis() + minutes * 60_000L
        try {
            alarm.setAlarmClock(AlarmManager.AlarmClockInfo(trigger, null), pi)
        } catch (e: SecurityException) {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
        }
    }

    /** Перепланировать всё (после перезагрузки, смены часового пояса). */
    fun rescheduleAll(context: Context, habits: List<Habit>) {
        habits.forEach { schedule(context, it) }
    }

    const val ACTION_REMIND = "com.artt.rutina.REMIND"
    const val ACTION_MARK_DONE = "com.artt.rutina.MARK_DONE"
    const val ACTION_SNOOZE = "com.artt.rutina.SNOOZE"
    const val EXTRA_HABIT_ID = "habit_id"
    const val EXTRA_DAY = "day"
    const val EXTRA_SLOT = "slot"

    fun notificationId(habitId: Long, slot: Int): Int = habitId.toInt() * 10 + slot
    const val SLOT_REMIND = 0
    const val SLOT_MISSED = 1
}
