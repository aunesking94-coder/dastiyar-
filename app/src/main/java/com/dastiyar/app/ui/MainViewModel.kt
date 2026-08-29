package com.dastiyar.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dastiyar.app.DastiyarApp
import com.dastiyar.app.data.ChatMsg
import com.dastiyar.app.data.Habit
import com.dastiyar.app.data.Memory
import com.dastiyar.app.data.Profile
import com.dastiyar.app.data.Repository
import com.dastiyar.app.data.Settings
import com.dastiyar.app.data.Slot
import com.dastiyar.app.data.Task
import com.dastiyar.app.util.Dates
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = DastiyarApp.repository

    val todaySlots: StateFlow<List<Slot>> = repo.slotsFor(Dates.today())
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())
    val tomorrowSlots: StateFlow<List<Slot>> = repo.slotsFor(Dates.tomorrow())
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())
    val todayTasks: StateFlow<List<Task>> = repo.tasksFor(Dates.today())
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())
    val tomorrowTasks: StateFlow<List<Task>> = repo.tasksFor(Dates.tomorrow())
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())
    val habits: StateFlow<List<Habit>> = repo.habits
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())
    val memories: StateFlow<List<Memory>> = repo.memories
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())
    val profile: StateFlow<Profile> = repo.profile
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), Profile())
    val settings: StateFlow<Settings> = repo.settings
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), Settings())

    private val _chat = MutableStateFlow<List<ChatMsg>>(emptyList())
    val chat: StateFlow<List<ChatMsg>> = _chat.asStateFlow()

    private val _sending = MutableStateFlow(false)
    val sending: StateFlow<Boolean> = _sending.asStateFlow()

    private val _overdue = MutableStateFlow<List<Task>>(emptyList())
    val overdue: StateFlow<List<Task>> = _overdue.asStateFlow()

    private val _weekly = MutableStateFlow<Repository.WeekStats?>(null)
    val weekly: StateFlow<Repository.WeekStats?> = _weekly.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val events: SharedFlow<String> = _events.asSharedFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _overdue.value = repo.overdueTasks()
            _chat.value = repo.chatHistory().reversed()
            _weekly.value = repo.weekStats(profile.value.sleepTime)
        }
    }

    fun addTask(title: String, date: String, time: String?, duration: Int, priority: String, category: String) {
        viewModelScope.launch {
            repo.addTask(title, date, time, duration, priority, category)
            if (date == Dates.today() || date == Dates.tomorrow()) repo.buildFor(date)
            _events.emit(if (date == Dates.today()) "کار به برنامهٔ امروز افزوده شد." else "کار برای ${Dates.faDate(date)} ثبت شد.")
        }
    }

    fun toggleTask(task: Task) = viewModelScope.launch { repo.toggleTask(task) }
    fun deleteTask(task: Task) = viewModelScope.launch { repo.deleteTask(task) }

    fun addHabit(name: String, target: Int) = viewModelScope.launch {
        repo.addHabit(name, target)
        _events.emit("عادت «$name» ثبت شد.")
    }

    fun toggleHabit(habit: Habit) = viewModelScope.launch {
        repo.toggleHabit(habit, Dates.today())
    }

    fun deleteHabit(habit: Habit) = viewModelScope.launch { repo.deleteHabit(habit) }

    fun addMemory(text: String, tag: String) = viewModelScope.launch {
        repo.saveMemory(text, tag)
        _events.emit("در حافظه ذخیره شد.")
    }

    fun deleteMemory(memory: Memory) = viewModelScope.launch { repo.deleteMemory(memory) }

    fun toggleSlot(slot: Slot) = viewModelScope.launch { repo.toggleSlot(slot) }

    fun buildToday() = viewModelScope.launch {
        val o = repo.buildFor(Dates.today())
        _events.emit("برنامهٔ امروز بازسازی شد${if (o.notes.isEmpty()) "." else " — " + o.notes.joinToString(" ")}")
    }

    fun buildTomorrowOnDemand() = viewModelScope.launch {
        val o = repo.buildTomorrow()
        _events.emit("برنامهٔ فردا ساخته شد (${Dates.faDigits(o.slotCount.toString())} بخش).")
    }

    fun logWake(time: String) = viewModelScope.launch {
        val msgs = repo.logWake(Dates.today(), time)
        if (msgs.isEmpty()) _events.emit("بیدار شدنت ثبت شد.")
        else msgs.forEach { _events.emit(it) }
    }

    fun saveProfile(
        name: String = "", wakeTime: String = "", sleepTime: String = "", breakfastTime: String = "",
        lunchTime: String = "", dinnerTime: String = "", workoutTime: String = "",
        workoutDays: List<Int> = emptyList(), workoutMin: Int = -1, bestEnergy: String = "", city: String = ""
    ) = viewModelScope.launch {
        repo.saveProfile(name, wakeTime, sleepTime, breakfastTime, lunchTime, dinnerTime, workoutTime, workoutDays, workoutMin, bestEnergy, city)
        repo.setOnboardingDone()
        repo.buildFor(Dates.today())
        repo.buildTomorrow()
        _events.emit("پروفایل ذخیره شد و برنامه‌ها به‌روز شدند.")
    }

    fun saveSettings(
        geminiKey: String, geminiModel: String, nightlyHour: String, notifyMin: Int,
        proactiveSleepNote: Boolean, useBedtimeEstimate: Boolean
    ) = viewModelScope.launch {
        repo.saveSettings(geminiKey, geminiModel, nightlyHour, notifyMin, proactiveSleepNote, useBedtimeEstimate)
        _events.emit("تنظیمات ذخیره شد.")
    }

    fun sendChat(text: String) {
        if (text.isBlank() || _sending.value) return
        viewModelScope.launch {
            _sending.value = true
            try {
                val reply = repo.sendChat(text)
                _chat.value = repo.chatHistory().reversed()
                if (reply.isBlank()) _events.emit("انجام شد.")
            } catch (e: Exception) {
                _chat.value = repo.chatHistory().reversed()
                _events.emit("خطا: ${e.message}")
            } finally {
                _sending.value = false
            }
        }
    }

    fun clearChat() = viewModelScope.launch {
        repo.clearChat()
        _chat.value = emptyList()
    }
}