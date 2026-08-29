package com.dastiyar.app.util

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.abs

object Dates {

    val ISO_DATE: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val FA_DIGITS = mapOf(
        '0' to '۰', '1' to '۱', '2' to '۲', '3' to '۳', '4' to '۴',
        '5' to '۵', '6' to '۶', '7' to '۷', '8' to '۸', '9' to '۹'
    )
    private val MONTHS_FA = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )
    private val DAYS_FA = mapOf(
        "MONDAY" to "دوشنبه", "TUESDAY" to "سه‌شنبه", "WEDNESDAY" to "چهارشنبه",
        "THURSDAY" to "پنجشنبه", "FRIDAY" to "جمعه", "SATURDAY" to "شنبه", "SUNDAY" to "یکشنبه"
    )

    fun today(): String = LocalDate.now().format(ISO_DATE)
    fun tomorrow(): String = LocalDate.now().plusDays(1).format(ISO_DATE)
    fun daysFromToday(n: Long): String = LocalDate.now().plusDays(n).format(ISO_DATE)
    fun weekStart(): String = weekStartFrom(today())

    fun dayOfWeek(date: String): java.time.DayOfWeek = LocalDate.parse(date).dayOfWeek

    fun weekStartFrom(date: String): String {
        return try {
            LocalDate.parse(date).with(WeekFields.of(Locale.getDefault()).firstDayOfWeek).format(ISO_DATE)
        } catch (e: Exception) {
            date
        }
    }

    fun timeToMin(time: String): Int {
        return try {
            val t = LocalTime.parse(time)
            t.hour * 60 + t.minute
        } catch (e: Exception) {
            0
        }
    }

    fun minToTime(min: Int): String {
        val m = ((min % 1440) + 1440) % 1440
        return String.format(Locale.US, "%02d:%02d", m / 60, m % 60)
    }

    fun nowMinutes(): Int = LocalTime.now().hour * 60 + LocalTime.now().minute
    fun nowTime(): String = LocalTime.now().format(TIME)

    fun addMinutes(time: String, minutes: Int): String = minToTime(timeToMin(time) + minutes)

    fun faDigits(input: String): String = input.map { FA_DIGITS[it] ?: it }.joinToString("")
    fun faTime(time: String): String = faDigits(time)

    fun faDate(date: String): String {
        val dow = DAYS_FA[LocalDate.parse(date).dayOfWeek.name] ?: ""
        val (y, m, d) = jalaali(LocalDate.parse(date).year, LocalDate.parse(date).monthValue, LocalDate.parse(date).dayOfMonth)
        return "$dow $d ${MONTHS_FA[m - 1]} $y"
    }

    fun faDateShort(date: String): String {
        val (_, m, d) = jalaali(LocalDate.parse(date).year, LocalDate.parse(date).monthValue, LocalDate.parse(date).dayOfMonth)
        return "$d ${MONTHS_FA[m - 1]}"
    }

    fun sleepDurationMin(slept: String?, wake: String?): Int? {
        if (slept.isNullOrBlank() || wake.isNullOrBlank()) return null
        var d = timeToMin(wake) - timeToMin(slept)
        if (d < 0) d += 1440
        return if (d > 720) null else d
    }

    fun formatDuration(min: Int): String {
        val m = abs(min)
        return if (m >= 60) faDigits("${m / 60} ساعت ${m % 60} دقیقه") else faDigits("$m دقیقه")
    }

    private fun jalaali(gy: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
        val gdm = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        val gy2 = if (gm > 2) gy + 1 else gy
        var days = 355666 + (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) + ((gy2 + 399) / 400) + gd + gdm[gm - 1]
        var jy = -1595 + (33 * (days / 12053))
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }
        val jm: Int
        val jd: Int
        if (days < 186) {
            jm = 1 + (days / 31)
            jd = 1 + (days % 31)
        } else {
            jm = 7 + ((days - 186) / 30)
            jd = 1 + ((days - 186) % 30)
        }
        return Triple(jy, jm, jd)
    }
}