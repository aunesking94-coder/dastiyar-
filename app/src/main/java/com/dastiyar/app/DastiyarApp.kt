package com.dastiyar.app

import android.app.Application
import android.content.Context
import com.dastiyar.app.data.Repository
import com.dastiyar.app.notify.Notifications
import com.dastiyar.app.util.Dates
import com.dastiyar.app.work.NightlyPlanWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DastiyarApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Notifications.ensureChannels(this)
        repository = Repository(this)
        appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        appScope.launch {
            NightlyPlanWorker.ensureScheduled(this@DastiyarApp)
            repository.ensureTodayPlan()
            val s = repository.settings.first()
            val now = Dates.nowMinutes()
            if (now >= Dates.timeToMin(s.nightlyHour)) {
                repository.buildTomorrow()
            }
        }
    }

    companion object {
        lateinit var repository: Repository
            private set
        lateinit var appScope: CoroutineScope
            private set

        fun from(context: Context): DastiyarApp = context.applicationContext as DastiyarApp
    }
}