package com.dastiyar.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "profile")

data class Profile(
    val name: String = "",
    val wakeTime: String = "08:00",
    val sleepTime: String = "23:30",
    val breakfastTime: String = "08:15",
    val lunchTime: String = "13:00",
    val dinnerTime: String = "20:00",
    val workoutTime: String = "17:00",
    val workoutDays: List<Int> = listOf(1, 3, 5),
    val workoutMin: Int = 60,
    val bestEnergy: String = "morning",
    val city: String = "",
    val onboardingDone: Boolean = false
)

data class Settings(
    val geminiKey: String = "",
    val geminiModel: String = "gemini-2.5-flash",
    val nightlyHour: String = "22:00",
    val notifyMin: Int = 10,
    val proactiveSleepNote: Boolean = true,
    val useBedtimeEstimate: Boolean = true
)

class ProfileStore(context: Context) {

    private val store = context.applicationContext.dataStore

    val profile: Flow<Profile> = store.data.map { p ->
        Profile(
            name = p[KEY_NAME] ?: "",
            wakeTime = p[KEY_WAKE] ?: "08:00",
            sleepTime = p[KEY_SLEEP] ?: "23:30",
            breakfastTime = p[KEY_BREAKFAST] ?: "08:15",
            lunchTime = p[KEY_LUNCH] ?: "13:00",
            dinnerTime = p[KEY_DINNER] ?: "20:00",
            workoutTime = p[KEY_WORKOUT] ?: "17:00",
            workoutDays = parseCsvInts(p[KEY_WORKOUT_DAYS] ?: "1,3,5"),
            workoutMin = p[KEY_WORKOUT_MIN] ?: 60,
            bestEnergy = p[KEY_ENERGY] ?: "morning",
            city = p[KEY_CITY] ?: "",
            onboardingDone = p[KEY_ONBOARDED] ?: false
        )
    }

    val settings: Flow<Settings> = store.data.map { p ->
        Settings(
            geminiKey = p[KEY_GEMINI_KEY] ?: "",
            geminiModel = p[KEY_GEMINI_MODEL] ?: "gemini-2.5-flash",
            nightlyHour = p[KEY_NIGHTLY] ?: "22:00",
            notifyMin = p[KEY_NOTIFY_MIN] ?: 10,
            proactiveSleepNote = p[KEY_PROACTIVE] ?: true,
            useBedtimeEstimate = p[KEY_BEDTIME] ?: true
        )
    }

    suspend fun saveProfile(
        name: String = "",
        wakeTime: String = "",
        sleepTime: String = "",
        breakfastTime: String = "",
        lunchTime: String = "",
        dinnerTime: String = "",
        workoutTime: String = "",
        workoutDays: List<Int> = emptyList(),
        workoutMin: Int = -1,
        bestEnergy: String = "",
        city: String = ""
    ) {
        store.edit { p ->
            if (name.isNotEmpty()) p[KEY_NAME] = name
            if (wakeTime.isNotEmpty()) p[KEY_WAKE] = wakeTime
            if (sleepTime.isNotEmpty()) p[KEY_SLEEP] = sleepTime
            if (breakfastTime.isNotEmpty()) p[KEY_BREAKFAST] = breakfastTime
            if (lunchTime.isNotEmpty()) p[KEY_LUNCH] = lunchTime
            if (dinnerTime.isNotEmpty()) p[KEY_DINNER] = dinnerTime
            if (workoutTime.isNotEmpty()) p[KEY_WORKOUT] = workoutTime
            if (workoutDays.isNotEmpty()) p[KEY_WORKOUT_DAYS] = workoutDays.joinToString(",")
            if (workoutMin >= 0) p[KEY_WORKOUT_MIN] = workoutMin
            if (bestEnergy.isNotEmpty()) p[KEY_ENERGY] = bestEnergy
            if (city.isNotEmpty()) p[KEY_CITY] = city
            if (name.isNotEmpty()) p[KEY_ONBOARDED] = true
        }
    }

    suspend fun saveSettings(
        geminiKey: String = "",
        geminiModel: String = "",
        nightlyHour: String = "",
        notifyMin: Int = -1,
        proactiveSleepNote: Boolean? = null,
        useBedtimeEstimate: Boolean? = null
    ) {
        store.edit { p ->
            if (geminiKey.isNotEmpty()) p[KEY_GEMINI_KEY] = geminiKey
            if (geminiModel.isNotEmpty()) p[KEY_GEMINI_MODEL] = geminiModel
            if (nightlyHour.isNotEmpty()) p[KEY_NIGHTLY] = nightlyHour
            if (notifyMin >= 0) p[KEY_NOTIFY_MIN] = notifyMin
            proactiveSleepNote?.let { p[KEY_PROACTIVE] = it }
            useBedtimeEstimate?.let { p[KEY_BEDTIME] = it }
        }
    }

    suspend fun setOnboardingDone() {
        store.edit { it[KEY_ONBOARDED] = true }
    }

    private fun parseCsvInts(csv: String): List<Int> =
        csv.split(",").mapNotNull { it.trim().toIntOrNull() }

    companion object {
        private val KEY_NAME = stringPreferencesKey("name")
        private val KEY_WAKE = stringPreferencesKey("wake")
        private val KEY_SLEEP = stringPreferencesKey("sleep")
        private val KEY_BREAKFAST = stringPreferencesKey("breakfast")
        private val KEY_LUNCH = stringPreferencesKey("lunch")
        private val KEY_DINNER = stringPreferencesKey("dinner")
        private val KEY_WORKOUT = stringPreferencesKey("workout")
        private val KEY_WORKOUT_DAYS = stringPreferencesKey("workout_days")
        private val KEY_WORKOUT_MIN = intPreferencesKey("workout_min")
        private val KEY_ENERGY = stringPreferencesKey("energy")
        private val KEY_CITY = stringPreferencesKey("city")
        private val KEY_ONBOARDED = booleanPreferencesKey("onboarded")

        private val KEY_GEMINI_KEY = stringPreferencesKey("gemini_key")
        private val KEY_GEMINI_MODEL = stringPreferencesKey("gemini_model")
        private val KEY_NIGHTLY = stringPreferencesKey("nightly_hour")
        private val KEY_NOTIFY_MIN = intPreferencesKey("notify_min")
        private val KEY_PROACTIVE = booleanPreferencesKey("proactive_note")
        private val KEY_BEDTIME = booleanPreferencesKey("bedtime_est")
    }
}