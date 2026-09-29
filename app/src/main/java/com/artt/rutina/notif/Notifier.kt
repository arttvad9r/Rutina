package com.artt.rutina.notif

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.artt.rutina.MainActivity
import com.artt.rutina.R
import com.artt.rutina.data.Habit
import java.time.LocalDate

object Notifier {

    fun post(context: Context, habit: Habit, day: LocalDate, force: Boolean = false) {
        Reminders.ensureChannel(context)

        val open = PendingIntent.getActivity(
            context,
            9000 + Reminders.requestCode(habit.id),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val done = PendingIntent.getBroadcast(
            context,
            1000 + Reminders.requestCode(habit.id),
            Intent(context, ActionReceiver::class.java).apply {
                action = Reminders.ACTION_MARK_DONE
                putExtra(Reminders.EXTRA_HABIT_ID, habit.id)
                putExtra(Reminders.EXTRA_DAY, day.toString())
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val snooze = PendingIntent.getBroadcast(
            context,
            2000 + Reminders.requestCode(habit.id),
            Intent(context, ActionReceiver::class.java).apply {
                action = Reminders.ACTION_SNOOZE
                putExtra(Reminders.EXTRA_HABIT_ID, habit.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val time = if (habit.hour >= 0) String.format("%02d:%02d", habit.hour, habit.minute) else ""
        val text = if (force) "${habit.name} · сейчас" else "${habit.name} · $time"

        val builder = NotificationCompat.Builder(context, Reminders.CHANNEL)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle(context.getString(R.string.notif_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(open)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .addAction(0, context.getString(R.string.action_done), done)
            .addAction(0, context.getString(R.string.action_snooze), snooze)

        if (force) {
            builder.setDefaults(Notification.DEFAULT_ALL)
            builder.setOngoing(false)
        } else {
            builder.setDefaults(Notification.DEFAULT_ALL)
        }

        try {
            NotificationManagerCompat.from(context)
                .notify(Reminders.notificationId(habit.id, Reminders.SLOT_REMIND), builder.build())
        } catch (_: SecurityException) {
            // нет разрешения POST_NOTIFICATIONS — молча выходим
        }
    }

    fun clear(context: Context, habitId: Long) {
        val mgr = context.getSystemService(NotificationManager::class.java) ?: return
        mgr.cancel(Reminders.notificationId(habitId, Reminders.SLOT_REMIND))
        mgr.cancel(Reminders.notificationId(habitId, Reminders.SLOT_MISSED))
    }
}
