package com.dastiyar.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.json.JSONArray

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val date: String,
    val time: String? = null,
    val durationMin: Int = 60,
    val category: String = "عمومی",
    val priority: String = "med",
    val done: Boolean = false,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String = "عمومی",
    val targetPerWeek: Int = 3,
    val doneDates: String = "[]",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun doneOn(date: String): Boolean {
        return try {
            val arr = JSONArray(doneDates)
            for (i in 0 until arr.length()) {
                if (arr.optString(i) == date) return true
            }
            false
        } catch (e: Exception) {
            false
        }
    }
}

@Entity(tableName = "slots", indices = [Index("date")])
data class Slot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val time: String,
    val endTime: String,
    val title: String,
    val kind: String,
    val taskId: Long? = null,
    val done: Boolean = false,
    val priority: String = "med",
    val note: String = ""
)

@Entity(tableName = "day_logs")
data class DayLog(
    @PrimaryKey val date: String,
    val wakeTime: String? = null,
    val sleptTime: String? = null,
    val plannedCount: Int = 0,
    val doneCount: Int = 0,
    val procrastinated: String = "[]",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "messages")
data class ChatMsg(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String,
    val content: String,
    val ts: Long = System.currentTimeMillis()
)

@Entity(tableName = "memories")
data class Memory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val tag: String = "",
    val createdAt: Long = System.currentTimeMillis()
)