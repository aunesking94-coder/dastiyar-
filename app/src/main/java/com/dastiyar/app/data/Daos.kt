package com.dastiyar.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE date = :date ORDER BY id")
    fun byDate(date: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE date BETWEEN :start AND :end ORDER BY date, id")
    suspend fun byRange(start: String, end: String): List<Task>

    @Query("SELECT * FROM tasks WHERE date = :date")
    suspend fun byDateOnce(date: String): List<Task>

    @Query("SELECT * FROM tasks WHERE done = 0 ORDER BY date ASC, createdAt DESC")
    suspend fun pending(): List<Task>

    @Insert
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)
}

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY id")
    fun all(): Flow<List<Habit>>

    @Query("SELECT * FROM habits")
    suspend fun allOnce(): List<Habit>

    @Insert
    suspend fun insert(habit: Habit): Long

    @Update
    suspend fun update(habit: Habit)

    @Delete
    suspend fun delete(habit: Habit)
}

@Dao
interface SlotDao {
    @Query("SELECT * FROM slots WHERE date = :date ORDER BY time")
    fun byDate(date: String): Flow<List<Slot>>

    @Query("SELECT * FROM slots WHERE date = :date ORDER BY time")
    suspend fun byDateOnce(date: String): List<Slot>

    @Query("SELECT * FROM slots WHERE date BETWEEN :start AND :end")
    suspend fun byRange(start: String, end: String): List<Slot>

    @Query("DELETE FROM slots WHERE date = :date")
    suspend fun deleteForDate(date: String)

    @Insert
    suspend fun insertAll(slots: List<Slot>)

    @Update
    suspend fun update(slot: Slot)
}

@Dao
interface DayLogDao {
    @Query("SELECT * FROM day_logs WHERE date = :date")
    suspend fun byDate(date: String): DayLog?

    @Query("SELECT * FROM day_logs WHERE date BETWEEN :start AND :end")
    suspend fun byRange(start: String, end: String): List<DayLog>

    @Query("SELECT * FROM day_logs ORDER BY date")
    suspend fun all(): List<DayLog>

    @Upsert
    suspend fun upsert(log: DayLog)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM messages ORDER BY id ASC LIMIT :limit")
    suspend fun recent(limit: Int): List<ChatMsg>

    @Insert
    suspend fun insert(msg: ChatMsg): Long

    @Query("DELETE FROM messages")
    suspend fun clear()
}

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories ORDER BY createdAt DESC")
    fun all(): Flow<List<Memory>>

    @Insert
    suspend fun insert(memory: Memory): Long

    @Delete
    suspend fun delete(memory: Memory)
}