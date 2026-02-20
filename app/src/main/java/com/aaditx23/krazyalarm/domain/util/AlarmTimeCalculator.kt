package com.aaditx23.krazyalarm.domain.util

import com.aaditx23.krazyalarm.domain.models.Alarm
import java.util.Calendar

/**
 * Single source of truth for calculating the next trigger time for an alarm.
 * Used by AlarmScheduler, ViewModels, and ToggleAlarmUseCase.
 */
object AlarmTimeCalculator {

    /**
     * Calculates the next valid future trigger time for the given alarm.
     * Guarantees the returned time is always in the future.
     */
    fun getNextTriggerTime(alarm: Alarm): Long {
        return if (alarm.days == 0) {
            getNextOneTimeTrigger(alarm.hour, alarm.minute, alarm.scheduledDate)
        } else {
            getNextRepeatingTrigger(alarm.hour, alarm.minute, alarm.days)
        }
    }

    /**
     * Calculates the next valid future trigger time for the given hour and minute.
     * Used when only hour/minute are known (e.g., re-enabling a one-time alarm).
     */
    fun getNextOneTimeTrigger(hour: Int, minute: Int, scheduledDate: Long? = null): Long {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        if (scheduledDate != null) {
            // Start from the scheduled date
            calendar.timeInMillis = scheduledDate
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            // If it's in the future, use it
            if (calendar.timeInMillis > now) {
                return calendar.timeInMillis
            }
        }

        // Scheduled date is null or in the past — use today or tomorrow
        calendar.timeInMillis = now
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        if (calendar.timeInMillis <= now) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return calendar.timeInMillis
    }

    private fun getNextRepeatingTrigger(hour: Int, minute: Int, days: Int): Long {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If today's time has already passed, start checking from tomorrow
        val startOffset = if (calendar.timeInMillis <= now) 1 else 0

        for (i in startOffset..7) {
            val checkCalendar = Calendar.getInstance().apply {
                timeInMillis = calendar.timeInMillis
                add(Calendar.DAY_OF_YEAR, i)
            }
            val checkDay = checkCalendar.get(Calendar.DAY_OF_WEEK)
            if ((days and (1 shl (checkDay - 1))) != 0) {
                return checkCalendar.timeInMillis
            }
        }

        // Fallback — should never reach here if days != 0
        calendar.add(Calendar.DAY_OF_YEAR, 7)
        return calendar.timeInMillis
    }
}
