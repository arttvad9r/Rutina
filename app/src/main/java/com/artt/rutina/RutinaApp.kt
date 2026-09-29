package com.artt.rutina

import android.app.Application
import com.artt.rutina.data.RutinaDb
import com.artt.rutina.data.Repo
import com.artt.rutina.notif.Reminders

class RutinaApp : Application() {

    val repo: Repo by lazy { Repo(RutinaDb.get(this)) }

    override fun onCreate() {
        super.onCreate()
        Reminders.ensureChannel(this)
    }
}
