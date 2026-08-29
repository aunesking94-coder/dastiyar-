package com.dastiyar.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Task::class, Habit::class, Slot::class, DayLog::class, ChatMsg::class, Memory::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun habitDao(): HabitDao
    abstract fun slotDao(): SlotDao
    abstract fun dayLogDao(): DayLogDao
    abstract fun chatDao(): ChatDao
    abstract fun memoryDao(): MemoryDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dastiyar.db"
                ).build().also { instance = it }
            }
        }
    }
}