package com.dastiyar.app.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.dastiyar.app.data.Slot
import com.dastiyar.app.util.Dates
import java.util.Calendar

object Scheduler {

    const val ACTION_ALARM = "com.dastiyar.app.ACTION_ALARM"
    private const val EXTRA_TITLE = "title"
    private const val EXTRA_BODY = "body"
    private const val EXTRA_CHANNEL = "channel"
    private const val EXTRA_WAKE = "is_wake"

    fun keyFor(date: String, time: String): Int = (date + "|" + time).hashCode()

    fun cancelDay(context: Context, date: String, slots: List<Slot>) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (s in slots) {
            am.cancel(broadcast(context, date, s.time, s.title, remindChannel(s), s.title == "بیدار شدن"))
        }
    }

    fun scheduleDay(context: Context, date: String, slots: List<Slot>, notifyMin: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val today = Dates.today()
        val now = System.currentTimeMillis()

        for (s in slots) {
            if (s.done || s.title == "خواب") continue
            val trigger = triggerMillis(date, Dates.timeToMin(s.time))
            if (date == today && trigger < now) continue
            val isWake = s.title == "بیدار شدن"
            val channel = remindChannel(s)
            val pi = broadcast(context, date, s.time, s.title, channel, isWake)

            if (isWake) {
                val showIntent = PendingIntent.getActivity(
                    context, keyFor(date, s.time),
                    Intent(context, com.dastiyar.app.MainActivity::class.java).apply {
                        action = ACTION_ALARM
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                try {
                    am.setAlarmClock(AlarmManager.AlarmClockInfo(trigger, showIntent), pi)
                } catch (e: Exception) {
                    am.set(AlarmManager.RTC_WAKEUP, trigger, pi)
                }
            } else {
                val advance = notifyMin.coerceIn(0, 60)
                val reminder = trigger - advance * 60_000L
                if (date == today && reminder < now) {
                    // still schedule exactly at time if near
                    if (trigger >= now) setExact(am, trigger, pi)
                    continue
                }
                setExact(am, reminder, pi)
            }
        }
    }

    fun scheduleAt(context: Context, date: String, time: String, title: String, channel: String) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val trigger = triggerMillis(date, Dates.timeToMin(time))
        val pi = broadcast(context, date, time, title, channel, title == "بیدار شدن")
        setExact(am, trigger, pi)
    }

    private fun setExact(am: AlarmManager, trigger: Long, pi: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
        }
    }

    fun remindChannel(s: Slot): String =
        if (s.title == "بیدار شدن") Notifications.CH_WAKE else Notifications.CH_REMIND

    fun broadcast(context: Context, date: String, time: String, title: String, channel: String, isWake: Boolean): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_BODY, title)
            putExtra(EXTRA_CHANNEL, channel)
            putExtra(EXTRA_WAKE, isWake)
        }
        return PendingIntent.getBroadcast(
            context, keyFor(date, time), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun triggerMillis(dateIso: String, minuteOfDay: Int): Long {
        val parts = dateIso.split("-")
        return Calendar.getInstance().apply {
            set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), minuteOfDay / 60, minuteOfDay % 60, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Scheduler.ACTION_ALARM) {
            val title = intent.getStringExtra(EXTRA_TITLE_TAG) ?: "دستیار هوشمند"
            val body = intent.getStringExtra(EXTRA_BODY_TAG) ?: "وقتش رسید!"
            val channel = intent.getStringExtra(EXTRA_CHANNEL_TAG) ?: Notifications.CH_REMIND
            val isWake = intent.getBooleanExtra(EXTRA_WAKE_TAG, false)
            val code = intent.getStringExtra(EXTRA_TITLE_TAG)?.hashCode() ?: "alarm".hashCode()
            Notifications.post(context, code, channel, title, body)
            if (isWake) {
                Notifications.post(context, "wake_hint".hashCode(), Notifications.CH_WAKE, "وقت بیدار شدن است", "برنامهٔ امروزت آماده است.")
            }
        }
    }

    private companion object {
        const val EXTRA_TITLE_TAG = "title"
        const val EXTRA_BODY_TAG = "body"
        const val EXTRA_CHANNEL_TAG = "channel"
        const val EXTRA_WAKE_TAG = "is_wake"
    }
}