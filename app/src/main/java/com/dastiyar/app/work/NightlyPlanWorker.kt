package com.dastiyar.app.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.dastiyar.app.DastiyarApp
import com.dastiyar.app.notify.Notifications
import com.dastiyar.app.util.Dates
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

class NightlyPlanWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val repo = DastiyarApp.repository
            repo.buildTomorrow()
            val wake = repo.slotsFor(Dates.tomorrow()).first()
                .firstOrNull { it.title == "بیدار شدن" }?.time ?: repo.profile.first().wakeTime
            val tomorrowDate = Dates.faDate(Dates.tomorrow())

            Notifications.post(
                applicationContext,
                1001,
                Notifications.CH_PLAN,
                "برنامهٔ فردا آماده شد",
                "وقت بیداری فردا: ${Dates.faTime(wake)} — اگر امشب دیر شده، بهتره به‌زودی بخوابی."
            )
            ensureScheduled(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_NAME = "nightly-plan"

        suspend fun ensureScheduled(context: Context) {
            val repo = DastiyarApp.repository
            val nightly = Dates.timeToMin(repo.settings.first().nightlyHour)
            val delay = millisToNext(nightly)
            val req = OneTimeWorkRequestBuilder<NightlyPlanWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(UNIQUE_NAME, ExistingWorkPolicy.REPLACE, req)
        }

        private fun millisToNext(nightlyMinutes: Int): Long {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, nightlyMinutes / 60)
                set(Calendar.MINUTE, nightlyMinutes % 60)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (!after(now)) add(Calendar.DAY_OF_YEAR, 1)
            }
            return target.timeInMillis - now.timeInMillis
        }
    }
}