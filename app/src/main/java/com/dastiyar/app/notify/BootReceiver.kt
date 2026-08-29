package com.dastiyar.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dastiyar.app.DastiyarApp
import com.dastiyar.app.work.NightlyPlanWorker
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        DastiyarApp.appScope.launch {
            try {
                DastiyarApp.repository.rescheduleAll()
                NightlyPlanWorker.ensureScheduled(context.applicationContext)
            } finally {
                pendingResult.finish()
            }
        }
    }
}