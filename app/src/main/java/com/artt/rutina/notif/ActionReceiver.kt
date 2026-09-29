package com.artt.rutina.notif

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.artt.rutina.RutinaApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Кнопки прямо в уведомлении: «Сделано» и «+15 минут». */
class ActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? RutinaApp ?: return
        val habitId = intent.getLongExtra(Reminders.EXTRA_HABIT_ID, -1L)
        if (habitId < 0) return

        when (intent.action) {
            Reminders.ACTION_MARK_DONE -> {
                val day = intent.getStringExtra(Reminders.EXTRA_DAY) ?: LocalDate.now().toString()
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        app.repo.setDone(habitId, LocalDate.parse(day), true)
                        Notifier.clear(context, habitId)
                    } finally {
                        pending.finish()
                    }
                }
            }

            Reminders.ACTION_SNOOZE -> {
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val habit = app.repo.habit(habitId)
                        if (habit != null) Reminders.snooze(context, habit, 15)
                        Notifier.clear(context, habitId)
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }
}
