package com.artt.rutina.notif

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.artt.rutina.RutinaApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** После перезагрузки, обновления приложения или смены часового пояса будильники нужно расставить заново. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? RutinaApp ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val habits = app.repo.habits.first()
                Reminders.rescheduleAll(context, habits)
                val settings = app.repo.caffeineSettingsNow()
                settings.timerEndMs?.takeIf { settings.enabled && it > System.currentTimeMillis() }
                    ?.let { CaffeineTimer.schedule(context, it) }
            } finally {
                pending.finish()
            }
        }
    }
}
