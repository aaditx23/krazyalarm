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
        android.util.Log.d("AlarmTimeCalculator", "=== getNextTriggerTime called ===")
        android.util.Log.d("AlarmTimeCalculator", "Alarm ID: ${alarm.id}, hour: ${alarm.hour}, minute: ${alarm.minute}")
        android.util.Log.d("AlarmTimeCalculator", "days: ${alarm.days}, scheduledDate: ${alarm.scheduledDate}")

        val now = System.currentTimeMillis()
        val activeSnooze = alarm.snoozedUntilMillis
        if (activeSnooze != null && activeSnooze > now) {
            android.util.Log.d("AlarmTimeCalculator", "Using active snooze trigger: $activeSnooze")
            return activeSnooze
        }

        val result = if (alarm.days == 0) {
            val trigger = getNextOneTimeTrigger(alarm.hour, alarm.minute, alarm.scheduledDate)
            android.util.Log.d("AlarmTimeCalculator", "One-time alarm trigger: $trigger")
            trigger
        } else {
            val trigger = getNextRepeatingTrigger(alarm.hour, alarm.minute, alarm.days)
            android.util.Log.d("AlarmTimeCalculator", "Repeating alarm trigger: $trigger")
            trigger
        }

        val calendar = Calendar.getInstance().apply { timeInMillis = result }
        android.util.Log.d("AlarmTimeCalculator", "Result: ${calendar.time}")
        android.util.Log.d("AlarmTimeCalculator", "Diff from now: ${(result - now) / 1000 / 60} minutes")

        return result
    }

    /**
     * Calculates the next valid future trigger time for the given hour and minute.
     * Used when only hour/minute are known (e.g., re-enabling a one-time alarm).
     */
    fun getNextOneTimeTrigger(hour: Int, minute: Int, scheduledDate: Long? = null): Long {
        android.util.Log.d("AlarmTimeCalculator", "getNextOneTimeTrigger: hour=$hour, minute=$minute, scheduledDate=$scheduledDate")
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        if (scheduledDate != null) {
            android.util.Log.d("AlarmTimeCalculator", "Scheduled date provided: $scheduledDate")
            // Start from the scheduled date
            calendar.timeInMillis = scheduledDate
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            android.util.Log.d("AlarmTimeCalculator", "Scheduled time: ${calendar.time}, now: ${Calendar.getInstance().time}")

            // If it's in the future, use it
            if (calendar.timeInMillis > now) {
                android.util.Log.d("AlarmTimeCalculator", "Scheduled time is in the future, using it")
                return calendar.timeInMillis
            } else {
                android.util.Log.d("AlarmTimeCalculator", "Scheduled time is in the past")
            }
        }

        // Scheduled date is null or in the past — use today or tomorrow
        android.util.Log.d("AlarmTimeCalculator", "Using today/tomorrow logic")
        calendar.timeInMillis = now
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        android.util.Log.d("AlarmTimeCalculator", "Today's time would be: ${calendar.time}")

        if (calendar.timeInMillis <= now) {
            android.util.Log.d("AlarmTimeCalculator", "Today's time has passed, adding 1 day")
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            android.util.Log.d("AlarmTimeCalculator", "Tomorrow's time: ${calendar.time}")
        }

        return calendar.timeInMillis
    }

    private fun getNextRepeatingTrigger(hour: Int, minute: Int, days: Int): Long {
        android.util.Log.d("AlarmTimeCalculator", "getNextRepeatingTrigger: hour=$hour, minute=$minute, days=$days (binary: ${days.toString(2)})")
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        android.util.Log.d("AlarmTimeCalculator", "Current time: ${Calendar.getInstance().time}")
        android.util.Log.d("AlarmTimeCalculator", "Target time today: ${calendar.time}")
        android.util.Log.d("AlarmTimeCalculator", "Time has passed? ${calendar.timeInMillis <= now}")

        // If today's time has already passed, start checking from tomorrow
        val startOffset = if (calendar.timeInMillis <= now) 1 else 0
        android.util.Log.d("AlarmTimeCalculator", "Starting search from day offset: $startOffset")

        for (i in startOffset..7) {
            val checkCalendar = Calendar.getInstance().apply {
                timeInMillis = calendar.timeInMillis
                add(Calendar.DAY_OF_YEAR, i)
            }
            val checkDay = checkCalendar.get(Calendar.DAY_OF_WEEK) // 1=Sunday, 2=Monday, ..., 7=Saturday
            // UI and Calculator both use: Bit 0=Sun, 1=Mon, 2=Tue, 3=Wed, 4=Thu, 5=Fri, 6=Sat
            val dayBit = checkDay - 1
            val isSelected = (days and (1 shl dayBit)) != 0

            val dayNames = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            android.util.Log.d("AlarmTimeCalculator", "Checking day offset $i: ${checkCalendar.time}, Calendar.DAY_OF_WEEK=$checkDay, bit=$dayBit (${dayNames[dayBit]}), isSelected=$isSelected")

            if (isSelected) {
                android.util.Log.d("AlarmTimeCalculator", "Found matching day! Returning: ${checkCalendar.time}")
                return checkCalendar.timeInMillis
            }
        }

        // Fallback — should never reach here if days != 0
        android.util.Log.d("AlarmTimeCalculator", "No match found in 7 days (should not happen), using fallback")
        calendar.add(Calendar.DAY_OF_YEAR, 7)
        return calendar.timeInMillis
    }
}
