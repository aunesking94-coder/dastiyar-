package com.dastiyar.app.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.dastiyar.app.MainActivity
import com.dastiyar.app.R

object Notifications {

    const val CH_WAKE = "ch_wake"
    const val CH_REMIND = "ch_remind"
    const val CH_PLAN = "ch_plan"

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(CH_WAKE, "بیدار شدن و آلارم", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "هشدار بیدار شدن و آلارم‌های دقیق"
                }
            )
            nm.createNotificationChannel(
                NotificationChannel(CH_REMIND, "یادآوری‌ها", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "یادآوری کارها و وعده‌ها"
                }
            )
            nm.createNotificationChannel(
                NotificationChannel(CH_PLAN, "پیام‌های برنامه", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "برنامهٔ روزانه و یادآوری خواب"
                }
            )
        }
    }

    fun post(context: Context, requestCode: Int, channel: String, title: String, body: String) {
        val id = requestCode and 0x7fffffff
        val open = PendingIntent.getActivity(
            context, id,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(if (channel == CH_PLAN) NotificationCompat.PRIORITY_DEFAULT else NotificationCompat.PRIORITY_HIGH)
            .build()

        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(id, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}