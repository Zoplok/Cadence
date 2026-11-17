package com.example.core

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    private val isoFormat: SimpleDateFormat
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private val prettyDayFormat: SimpleDateFormat
        get() = SimpleDateFormat("EEEE, MMM d", Locale.US)

    private val timeFormat: SimpleDateFormat
        get() = SimpleDateFormat("HH:mm", Locale.US)

    fun todayIso(): String = isoFormat.format(Date())

    fun isoDaysAgo(daysAgo: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        return isoFormat.format(cal.time)
    }

    fun shiftIsoDate(isoDate: String, deltaDays: Int): String {
        val parsed = runCatching { isoFormat.parse(isoDate) }.getOrNull() ?: Date()
        val cal = Calendar.getInstance().apply {
            time = parsed
            add(Calendar.DAY_OF_YEAR, deltaDays)
        }
        return isoFormat.format(cal.time)
    }

    fun prettyDate(isoDate: String): String {
        val today = todayIso()
        val yesterday = isoDaysAgo(1)
        return when (isoDate) {
            today -> "Today · ${formatParsed(isoDate)}"
            yesterday -> "Yesterday · ${formatParsed(isoDate)}"
            else -> formatParsed(isoDate)
        }
    }

    private fun formatParsed(isoDate: String): String {
        val parsed = runCatching { isoFormat.parse(isoDate) }.getOrNull() ?: return isoDate
        return prettyDayFormat.format(parsed)
    }

    /** Returns ISO weekday 1 (Monday) .. 7 (Sunday) */
    fun isoWeekdayToday(): Int {
        val cal = Calendar.getInstance()
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
    }

    fun formatTimeMs(epochMs: Long): String = timeFormat.format(Date(epochMs))

    fun todayAtHourMinute(hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun formatDurationSeconds(totalSeconds: Int): String {
        val safe = totalSeconds.coerceAtLeast(0)
        val hours = safe / 3600
        val mins = (safe % 3600) / 60
        val secs = safe % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, mins, secs)
        } else {
            String.format(Locale.US, "%02d:%02d", mins, secs)
        }
    }
}
