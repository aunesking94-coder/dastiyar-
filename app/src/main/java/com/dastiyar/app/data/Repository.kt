package com.dastiyar.app.data

import android.content.Context
import com.dastiyar.app.ai.ChatAction
import com.dastiyar.app.ai.GeminiClient
import com.dastiyar.app.notify.Scheduler
import com.dastiyar.app.planner.Planner
import com.dastiyar.app.util.Dates
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.coroutineScope

class Repository(context: Context) {

    private val appContext = context.applicationContext
    private val db = AppDatabase.get(appContext)
    private val store = ProfileStore(appContext)
    private val gemini = GeminiClient()

    val profile: Flow<Profile> = store.profile
    val settings: Flow<Settings> = store.settings

    fun tasksFor(date: String): Flow<List<Task>> = db.taskDao().byDate(date)
    fun slotsFor(date: String): Flow<List<Slot>> = db.slotDao().byDate(date)
    val habits: Flow<List<Habit>> = db.habitDao().all()
    val memories: Flow<List<Memory>> = db.memoryDao().all()
    suspend fun chatHistory(): List<ChatMsg> = db.chatDao().recent(20)
    suspend fun pendingTasks(): List<Task> = db.taskDao().pending()

    suspend fun saveProfile(
        name: String = "", wakeTime: String = "", sleepTime: String = "",
        breakfastTime: String = "", lunchTime: String = "", dinnerTime: String = "",
        workoutTime: String = "", workoutDays: List<Int> = emptyList(), workoutMin: Int = -1,
        bestEnergy: String = "", city: String = ""
    ) = store.saveProfile(name, wakeTime, sleepTime, breakfastTime, lunchTime, dinnerTime, workoutTime, workoutDays, workoutMin, bestEnergy, city)

    suspend fun saveSettings(
        geminiKey: String = "", geminiModel: String = "", nightlyHour: String = "",
        notifyMin: Int = -1, proactiveSleepNote: Boolean? = null, useBedtimeEstimate: Boolean? = null
    ) = store.saveSettings(geminiKey, geminiModel, nightlyHour, notifyMin, proactiveSleepNote, useBedtimeEstimate)

    suspend fun setOnboardingDone() = store.setOnboardingDone()

    suspend fun addTask(title: String, date: String, time: String?, durationMin: Int, priority: String, category: String): Long =
        db.taskDao().insert(Task(title = title, date = date, time = time, durationMin = durationMin, priority = priority, category = category))

    suspend fun toggleTask(task: Task) {
        db.taskDao().update(task.copy(done = !task.done))
        rebuildAfterTaskChange(task.date)
    }

    suspend fun deleteTask(task: Task) {
        db.taskDao().delete(task)
        rebuildAfterTaskChange(task.date)
    }

    suspend fun addHabit(name: String, targetPerWeek: Int) {
        db.habitDao().insert(Habit(name = name, targetPerWeek = targetPerWeek))
    }

    suspend fun toggleHabit(habit: Habit, date: String) {
        val list = org.json.JSONArray(habit.doneDates)
        val set = mutableListOf<String>()
        for (i in 0 until list.length()) set.add(list.optString(i))
        if (set.contains(date)) set.remove(date) else set.add(date)
        db.habitDao().update(habit.copy(doneDates = org.json.JSONArray(set).toString()))
    }

    suspend fun deleteHabit(habit: Habit) = db.habitDao().delete(habit)

    suspend fun saveMemory(text: String, tag: String = "") {
        if (text.isBlank()) return
        db.memoryDao().insert(Memory(text = text, tag = tag))
    }

    suspend fun deleteMemory(memory: Memory) = db.memoryDao().delete(memory)

    suspend fun clearChat() = db.chatDao().clear()

    /* ============ برنامه‌ریزی ============ */

    data class BuildOutcome(val date: String, val slotCount: Int, val notes: List<String>, val wasEmpty: Boolean)

    private suspend fun currentProfile(): Profile = profile.first()
    private suspend fun currentSettings(): Settings = settings.first()

    suspend fun ensureTodayPlan(): BuildOutcome? {
        val existing = db.slotDao().byDateOnce(Dates.today())
        if (existing.isNotEmpty()) {
            Scheduler.scheduleDay(appContext, Dates.today(), existing, notifyMinSafe())
            return null
        }
        return buildFor(Dates.today())
    }

    suspend fun buildFor(date: String): BuildOutcome {
        val p = currentProfile()
        val tasks = db.taskDao().byDateOnce(date)
        val existing = db.slotDao().byDateOnce(date)
        val before = existing.isNotEmpty()
        val wakeOverride = if (date == Dates.today()) {
            db.dayLogDao().byDate(date)?.wakeTime
        } else null
        val plan = Planner.buildDay(p, date, tasks, wakeOverride)
        val doneSlots = existing.filter { it.done }
        val merged = (doneSlots + plan.slots).distinctBy { it.time + "|" + it.title }
        db.slotDao().deleteForDate(date)
        db.slotDao().insertAll(merged)
        Scheduler.cancelDay(appContext, date, existing)
        Scheduler.scheduleDay(appContext, date, merged, notifyMinSafe())
        updateDayLog(date, merged)
        return BuildOutcome(date, merged.size, plan.notes, !before)
    }

    suspend fun buildTomorrow(): BuildOutcome = buildFor(Dates.tomorrow())

    suspend fun logWake(date: String, actualWake: String): List<String> {
        val p = currentProfile()
        val tasks = db.taskDao().byDateOnce(date)
        val existing = db.slotDao().byDateOnce(date)
        val res = Planner.replanToday(p, date, tasks, existing, actualWake)
        val doneSlots = existing.filter { it.done }
        val merged = (doneSlots + res.slots).distinctBy { it.time + "|" + it.title }
        db.slotDao().deleteForDate(date)
        db.slotDao().insertAll(merged)
        Scheduler.cancelDay(appContext, date, existing)
        Scheduler.scheduleDay(appContext, date, merged, notifyMinSafe())
        val log = db.dayLogDao().byDate(date) ?: DayLog(date = date)
        db.dayLogDao().upsert(log.copy(wakeTime = actualWake, updatedAt = System.currentTimeMillis()))
        updateDayLog(date, merged)
        return res.messages
    }

    suspend fun toggleSlot(slot: Slot) {
        val updated = slot.copy(done = !slot.done)
        db.slotDao().update(updated)
        if (slot.taskId != null) {
            val t = db.taskDao().byDateOnce(slot.date).firstOrNull { it.id == slot.taskId }
            if (t != null) db.taskDao().update(t.copy(done = updated.done))
        }
        if (updated.done) {
            Scheduler.cancelDay(appContext, slot.date, listOf(slot))
        } else {
            val slotList = listOf(updated)
            Scheduler.cancelDay(appContext, slot.date, slotList)
            Scheduler.scheduleDay(appContext, slot.date, slotList, notifyMinSafe())
        }
        val all = db.slotDao().byDateOnce(slot.date)
        updateDayLog(slot.date, all)
    }

    suspend fun rescheduleAll() {
        Scheduler.scheduleDay(appContext, Dates.today(), db.slotDao().byDateOnce(Dates.today()), notifyMinSafe())
        Scheduler.scheduleDay(appContext, Dates.tomorrow(), db.slotDao().byDateOnce(Dates.tomorrow()), notifyMinSafe())
    }

    suspend fun overdueTasks(): List<Task> {
        val all = db.taskDao().pending()
        return all.filter { it.date < Dates.today() }.sortedBy { it.date }.take(5)
    }

    private suspend fun updateDayLog(date: String, slots: List<Slot>) {
        val today = Dates.today()
        val tasks = slots.filter { it.kind == "task" }
        val done = tasks.count { it.done }
        val now = Dates.nowMinutes()
        val procrastinated = tasks.filter { !it.done && it.time != null && Dates.timeToMin(it.time) < now }.map { it.title }
        val log = db.dayLogDao().byDate(date) ?: DayLog(date = date)
        if (date <= today) {
            db.dayLogDao().upsert(
                log.copy(
                    plannedCount = tasks.size,
                    doneCount = done,
                    procrastinated = org.json.JSONArray(procrastinated).toString(),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    private suspend fun notifyMinSafe(): Int = currentSettings().notifyMin

    private suspend fun rebuildAfterTaskChange(date: String) {
        if (date in setOf(Dates.today(), Dates.tomorrow())) {
            buildFor(date)
        }
    }

    suspend fun morningNote(): String {
        val today = Dates.today()
        val slots = db.slotDao().byDateOnce(today)
        val p = currentProfile()
        val wake = slots.firstOrNull { it.title == "بیدار شدن" }
        val tasks = slots.filter { it.kind == "task" && !it.done }
        val sb = StringBuilder("سلام${if (p.name.isNotBlank()) " ${p.name}" else ""}! ")
        sb.append("برنامهٔ امروز آماده است. ")
        if (wake != null) sb.append("وقت بیداری: ${Dates.faTime(wake.time)}. ")
        if (tasks.isNotEmpty()) sb.append("${Dates.faDigits(tasks.size.toString())} کار برنامه‌ریزی شده.")
        return sb.toString()
    }

    /* ================= چت ================= */

    suspend fun sendChat(userText: String): String = coroutineScope {
        val p = profile.first()
        val s = settings.first()

        if (s.geminiKey.isBlank()) {
            db.chatDao().insert(ChatMsg(role = "user", content = userText))
            val hint = "کلید API جمینی هنوز در تنظیمات ثبت نشده. به بخش «تنظیمات» برو و کلید را وارد کن تا بتوانم برنامه‌ات را بدانم و برنامه بچینم."
            db.chatDao().insert(ChatMsg(role = "assistant", content = hint))
            return@coroutineScope hint
        }

        val history = db.chatDao().recent(16).map { m -> (if (m.role == "assistant") "model" else "user") to m.content }
        db.chatDao().insert(ChatMsg(role = "user", content = userText))

        val today = Dates.today()
        val todaySlots = db.slotDao().byDateOnce(today)
        val tomorrowOn = Dates.tomorrow()
        val tomorrowSlots = db.slotDao().byDateOnce(tomorrowOn)
        val habits = db.habitDao().allOnce()
        val memList = recentMemories()
        val pending = db.taskDao().pending().take(20)
        val overdue = overdueTasks()

        val system = buildString {
            append("تو دستیار برنامه‌ریزی روزانهٔ یک کاربر فارسی‌زبان هستی. مختصر و پرانرژی به فارسی پاسخ بده.\n")
            append("نام کاربر: ${p.name.ifBlank { "ناشناس" }}\n")
            append("روتین روزانه: بیداری ${p.wakeTime}، صبحانه ${p.breakfastTime}، ناهار ${p.lunchTime}، شام ${p.dinnerTime}، باشگاه ${p.workoutTime} (روزهای ${p.workoutDays.joinToString("، ")})\n")
            append("امروز (${Dates.faDate(today)}):\n")
            todaySlots.forEach { append("- ${it.time} ${it.title}${if (it.note.isNotBlank()) " (${it.note})" else ""}\n") }
            append("فردا (${Dates.faDate(tomorrowOn)}):\n")
            tomorrowSlots.forEach { append("- ${it.time} ${it.title}${if (it.note.isNotBlank()) " (${it.note})" else ""}\n") }
            if (habits.isNotEmpty()) append("عادت‌ها: ${habits.joinToString("، ") { it.name }}\n")
            if (memList.isNotEmpty()) append("یادداشت‌های کاربر: ${memList.joinToString("؛ ") { it.text }}\n")
            if (pending.isNotEmpty()) append("کارهای در انتظار: ${pending.joinToString("؛ ") { it.title + " (" + it.date + ")" }}\n")
            if (overdue.isNotEmpty()) append("کارهای عقب‌مانده: ${overdue.joinToString("؛ ") { it.title }}\n")
            append("\nاگر کاربر چیزی خواست که به برنامه یا یادآوری مربوط می‌شود، علاوه بر پاسخ متنی، یک بخش با عنوان دقیق «##ACTIONS##» و بعد یک آرایهٔ JSON بنویس که هر آیتم آن یکی از این شکل‌ها باشد:\n")
            append("{\"type\":\"task_add\",\"title\":\"...\",\"date\":\"YYYY-MM-DD\",\"time\":\"HH:mm\",\"duration\":NUM,\"priority\":\"high|med|low\",\"category\":\"...\"}\n")
            append("{\"type\":\"task_remove\",\"title\":\"...\"}\n")
            append("{\"type\":\"task_done\",\"title\":\"...\"}\n")
            append("{\"type\":\"habit_add\",\"name\":\"...\",\"target\":NUM}\n")
            append("{\"type\":\"memory_add\",\"text\":\"...\"}\n")
            append("{\"type\":\"rebuild\",\"date\":\"today|tomorrow\"}\n")
            append("برای «فردا ساعت ۴ باشگاه دارم» یک task_add با date فردا بنویس. برای «ساعت ۶ یادم بنداز به فلانی زنگ بزنم» تاریخ امروز باشد.\n")
        }

        val reply = gemini.chat(s.geminiKey, s.geminiModel, system, history, userText)
        applyActions(reply.actions)
        db.chatDao().insert(ChatMsg(role = "assistant", content = reply.text))
        reply.text
    }

    suspend fun recentMemories(): List<Memory> = db.memoryDao().all().first().take(15)

    private suspend fun applyActions(actions: List<ChatAction>) {
        val today = Dates.today()
        for (a in actions) {
            when (a) {
                is ChatAction.AddTask -> {
                    val date = a.date ?: today
                    db.taskDao().insert(Task(title = a.title, date = date, time = a.time, durationMin = a.durationMin, priority = a.priority, category = a.category))
                    if (date == today || date == Dates.tomorrow()) buildFor(date)
                }
                is ChatAction.RemoveTask -> {
                    val list = db.taskDao().pending()
                    val match = list.firstOrNull { it.title.contains(a.title, ignoreCase = true) || a.title.contains(it.title, ignoreCase = true) }
                    if (match != null) {
                        db.taskDao().delete(match)
                        byDateRebuild(match.date)
                    }
                }
                is ChatAction.MarkDone -> {
                    val list = db.taskDao().pending()
                    val match = list.firstOrNull { it.title.contains(a.title, ignoreCase = true) || a.title.contains(it.title, ignoreCase = true) }
                    if (match != null) {
                        db.taskDao().update(match.copy(done = true))
                        val slot = db.slotDao().byDateOnce(match.date).firstOrNull { it.taskId == match.id }
                        if (slot != null) db.slotDao().update(slot.copy(done = true))
                    }
                }
                is ChatAction.AddHabit -> {
                    db.habitDao().insert(Habit(name = a.name, targetPerWeek = a.targetPerWeek))
                }
                is ChatAction.AddMemory -> {
                    db.memoryDao().insert(Memory(text = a.text))
                }
                is ChatAction.Rebuild -> {
                    buildFor(if (a.date == "today") today else Dates.tomorrow())
                }
            }
        }
    }

    private suspend fun byDateRebuild(date: String) {
        if (date == Dates.today() || date == Dates.tomorrow()) buildFor(date)
    }

    fun uiContext(): Context = appContext

    data class WeekStats(
        val planned: Int,
        val done: Int,
        val completionPct: Int,
        val workoutDays: Int,
        val sleepAvgMin: Int?,
        val procrastinated: List<String>,
        val habitRows: List<Pair<String, Int>>
    )

    suspend fun weekStats(profileSleep: String? = null): WeekStats {
        val start = Dates.weekStart()
        val end = Dates.today()
        val logs = db.dayLogDao().byRange(start, end)
        val planned = logs.sumOf { it.plannedCount }
        val done = logs.sumOf { it.doneCount }
        val pct = if (planned == 0) 100 else (done * 100 / planned)
        val slots = db.slotDao().byRange(start, end)
        val workoutDays = slots.filter { it.title == "باشگاه" && it.done }.map { it.date }.distinct().size
        val sleepsUnion = logs.mapNotNull { Dates.sleepDurationMin(profileSleep ?: it.sleptTime, it.wakeTime) }
        val avgSleep = if (sleepsUnion.isEmpty()) null else sleepsUnion.sum() / sleepsUnion.size
        val proc = logs.flatMap { l ->
            try {
                val j = org.json.JSONArray(l.procrastinated)
                (0 until j.length()).map { j.optString(it) }
            } catch (e: Exception) {
                emptyList()
            }
        }.distinct().take(6)
        val habitsAll = db.habitDao().allOnce()
        val habitRows = habitsAll.map { h ->
            val cnt = try {
                val j = org.json.JSONArray(h.doneDates)
                var n = 0
                for (i in 0 until j.length()) {
                    val d = j.optString(i)
                    if (d >= start && d <= end) n++
                }
                n
            } catch (e: Exception) {
                0
            }
            h.name to cnt
        }
        return WeekStats(planned, done, pct, workoutDays, avgSleep, proc, habitRows)
    }
}