package com.artt.rutina.notif

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.artt.rutina.RutinaApp
import com.artt.rutina.data.finishedOn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Срабатывает ровно в назначенное время: показывает напоминание и планирует следующий день. */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? RutinaApp ?: return
        val habitId = intent.getLongExtra(Reminders.EXTRA_HABIT_ID, -1L)
        if (habitId < 0) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val habit = app.repo.habit(habitId)
                val today = LocalDate.now()
                if (habit != null && habit.active && habit.hour >= 0 && !finishedOn(habit, today)) {
                    if (!app.repo.isDone(habitId, today)) {
                        Notifier.post(context, habit, today)
                    }
                    // планируем на следующий день
                    Reminders.schedule(context, habit)
                } else {
                    // курс закончился — снимаем будильник, чтобы напоминания не тянулись дальше
                    habit?.let { Reminders.cancel(context, it) }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
