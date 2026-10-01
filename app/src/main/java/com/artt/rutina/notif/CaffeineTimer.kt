package com.artt.rutina.notif

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.artt.rutina.MainActivity
import com.artt.rutina.R

/**
 * Таймер до следующего приёма кофеина.
 *
 * Пользователь запускает его сам — автоматических напоминаний по расписанию нет.
 * Но сигнал в конце таймера нужен: иначе он бесполезен, когда экран погашен.
 */
object CaffeineTimer {

    const val CHANNEL = "rutina_caffeine_timer"
    private const val REQUEST_CODE = 2001
    const val NOTIFICATION_ID = 2001

    fun ensureChannel(context: Context) {
        val mgr = context.getSystemService(NotificationManager::class.java) ?: return
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val channel = NotificationChannel(
            CHANNEL,
            "Таймер кофеина",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Сигнал, когда прошёл интервал до следующего приёма кофеина"
            setSound(alarmSound, attrs)
            enableVibration(true)
            vibrationPattern = longArrayOf(0L, 400L, 200L, 400L)
        }
        mgr.createNotificationChannel(channel)
    }

    private fun pending(context: Context): PendingIntent {
        val intent = Intent(context, CaffeineTimerReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** Поставить сигнал на момент [endMs]. */
    fun schedule(context: Context, endMs: Long) {
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = pending(context)
        try {
            alarm.setAlarmClock(AlarmManager.AlarmClockInfo(endMs, null), pi)
        } catch (e: SecurityException) {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endMs, pi)
        }
    }

    fun cancel(context: Context) {
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        alarm.cancel(pending(context))
    }
}

/** Показывает уведомление, когда отсчёт дошёл до нуля. */
class CaffeineTimerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        CaffeineTimer.ensureChannel(context)
        val mgr = context.getSystemService(NotificationManager::class.java) ?: return

        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CaffeineTimer.CHANNEL)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle("Пауза завершена")
            .setContentText("Таймер кофеина закончился.")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()

        mgr.notify(CaffeineTimer.NOTIFICATION_ID, notification)
    }
}
