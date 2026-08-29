package com.dastiyar.app.planner

import com.dastiyar.app.data.Profile
import com.dastiyar.app.data.Slot
import com.dastiyar.app.data.Task
import com.dastiyar.app.util.Dates

data class PlanResult(
    val slots: List<Slot>,
    val notes: List<String>
)

data class ReplanResult(
    val slots: List<Slot>,
    val messages: List<String>,
    val wakeTime: String
)

private data class Interval(var start: Int, val end: Int) {
    fun len(): Int = end - start
}

private data class Anchor(val minute: Int, val title: String, val routine: Boolean)

object Planner {

    fun buildDay(
        profile: Profile,
        date: String,
        tasks: List<Task>,
        overrideWake: String? = null
    ): PlanResult {
        val notes = mutableListOf<String>()

        val sleepMin = Dates.timeToMin(profile.sleepTime)
        val wakeMin = Dates.timeToMin(overrideWake ?: profile.wakeTime)
        val dayEnd = if (overrideWake != null) 1440 else sleepMin

        val anchors = mutableListOf<Anchor>()
        fun addAnchor(m: Int, label: String, routine: Boolean = true) {
            val existing = anchors.any { it.minute == m }
            val minute = if (existing) m + 5 else m
            if (existing) notes.add("«$label» به ${Dates.faTime(Dates.minToTime(minute))} منتقل شد.")
            anchors.add(Anchor(minute, label, routine))
        }

        addAnchor(wakeMin, "بیدار شدن")
        addAnchor(Dates.timeToMin(profile.breakfastTime), "صبحانه")
        addAnchor(Dates.timeToMin(profile.lunchTime), "ناهار")
        addAnchor(Dates.timeToMin(profile.lunchTime) + 75, "استراحت")
        addAnchor(Dates.timeToMin(profile.dinnerTime), "شام")

        val dow = try { Dates.dayOfWeek(date).value } catch (e: Exception) { 0 }
        if (profile.workoutDays.contains(dow)) {
            addAnchor(Dates.timeToMin(profile.workoutTime), "باشگاه", routine = true)
        }

        if (overrideWake == null && Dates.timeToMin(profile.wakeTime) <= sleepMin) {
            addAnchor(sleepMin, "خواب")
        } else {
            notes.add("امشب برنامهٔ خواب ثبت نشد.")
        }

        val fixedTasks = tasks
            .filter { !it.done && !it.time.isNullOrBlank() }
            .sortedBy { Dates.timeToMin(it.time!!) }

        val flexibleTasks = tasks
            .filter { !it.done && it.time.isNullOrBlank() }
            .sortedByDescending { it.durationMin }

        // free intervals between anchors and fixed tasks
        val breakpoints = (anchors.map { it.minute } + fixedTasks.map { Dates.timeToMin(it.time!!) })
            .filter { it > wakeMin && it < dayEnd }
            .sorted()
        val intervals = mutableListOf<Interval>()
        var segStart = wakeMin
        for (b in breakpoints) {
            if (b - segStart >= 20) intervals.add(Interval(segStart, b))
            segStart = b
        }
        if (dayEnd - segStart >= 20 && segStart < dayEnd) intervals.add(Interval(segStart, dayEnd))

        fun energyHint(profile: Profile): Int = when (profile.bestEnergy) {
            "morning" -> wakeMin + 120
            "evening" -> maxOf(Dates.timeToMin(profile.lunchTime), wakeMin + 240)
            else -> wakeMin + 180
        }

        val placed = mutableListOf<Slot>()
        for (t in flexibleTasks) {
            val hint = energyHint(profile)
            val candidate = intervals.firstOrNull { iv ->
                val s = maxOf(iv.start, hint)
                s < iv.end && iv.end - s >= t.durationMin
            }
            if (candidate != null) {
                val s = maxOf(candidate.start, hint)
                placed.add(
                    Slot(date = date, time = Dates.minToTime(s), endTime = Dates.minToTime(s + t.durationMin),
                        title = t.title, kind = "task", taskId = t.id, priority = t.priority, note = "")
                )
                candidate.start = s + t.durationMin
            } else {
                val last = intervals.filter { it.len() >= 20 }.maxByOrNull { it.len() }
                if (last != null) {
                    val s = last.end - minOf(last.len(), t.durationMin)
                    placed.add(
                        Slot(date = date, time = Dates.minToTime(s), endTime = Dates.minToTime(last.end),
                            title = t.title, kind = "task", taskId = t.id, priority = t.priority, note = "با هم‌پوشانی جزئی")
                    )
                    last.start = s
                } else {
                    notes.add("«${t.title}» در برنامهٔ امروز جا نگرفت.")
                }
            }
        }

        val routineSlots = anchors
            .filter { it.minute <= dayEnd || it.title == "خواب" }
            .sortedBy { it.minute }
            .map { a ->
                val duration = if (a.title == "باشگاه") profile.workoutMin else if (a.title == "خواب") 420 else 40
                Slot(date = date, time = Dates.minToTime(a.minute), endTime = Dates.minToTime(a.minute + duration),
                    title = a.title, kind = "routine", priority = if (a.title == "خواب") "low" else "med", note = "")
            }

        val fixedSlots = fixedTasks.map { t ->
            Slot(date = date, time = t.time!!, endTime = Dates.minToTime(Dates.timeToMin(t.time!!) + t.durationMin),
                title = t.title, kind = "task", taskId = t.id, priority = t.priority, note = "")
        }

        val all = (routineSlots + fixedSlots + placed).sortedBy { Dates.timeToMin(it.time) }
        return PlanResult(all, notes)
    }

    fun replanToday(
        profile: Profile,
        date: String,
        tasks: List<Task>,
        existing: List<Slot>,
        actualWake: String
    ): ReplanResult {
        val plannedWake = existing.minOfOrNull { Dates.timeToMin(it.time) }
        val wakeMin = Dates.timeToMin(actualWake)
        val prevWake = plannedWake ?: Dates.timeToMin(profile.wakeTime)
        val delta = wakeMin - prevWake
        val messages = mutableListOf<String>()

        val doneTaskIds = existing.filter { it.done && it.taskId != null }.mapNotNull { it.taskId }.toSet()
        val liveTasks = tasks.filter { it.id !in doneTaskIds && !it.done }.toMutableList()

        if (delta > 15) {
            messages.add("${Dates.faDigits("$delta")} دقیقه دیرتر بیدار شدی؛ برنامهٔ امروز را دوباره می‌چینم.")
        } else if (delta < -15) {
            messages.add("زودتر از برنامه بیدار شدی؛ خوب!")
        }

        val smart = liveTasks.map { t ->
            val tm = t.time?.let { Dates.timeToMin(it) }
            if (tm != null && tm < wakeMin) {
                t.copy(time = null)
            } else t
        }

        val plan = buildDay(
            profile = profile.copy(wakeTime = actualWake),
            date = date,
            tasks = smart,
            overrideWake = actualWake
        )

        if (delta > 30) {
            val dropped = plan.slots.filter { it.kind == "task" && it.priority == "low" }
            if (dropped.isNotEmpty()) {
                messages.add("کارهای کم‌اهمیت «${dropped.map { it.title }.distinct().joinToString("، ")}» را برای امروز حذف کردم.")
            }
        }

        val finalSlots = if (delta > 30) plan.slots.filterNot { it.kind == "task" && it.priority == "low" } else plan.slots
        return ReplanResult(finalSlots, messages, actualWake)
    }

    fun pendingOverdue(tasks: List<Task>, today: String): List<Task> =
        tasks.filter { !it.done && it.date < today }
            .sortedWith(compareBy<Task> { it.priority != "high" }.thenBy { it.date })
            .take(5)

    fun overdueNote(tasks: List<Task>, today: String): List<String> {
        val list = pendingOverdue(tasks, today)
        return if (list.isEmpty()) emptyList() else listOf(
            "${list.size} کار از روزهای قبل عقب مانده: ${list.joinToString("، ") { it.title }}"
        )
    }
}