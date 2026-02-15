package com.aaditx23.krazyalarm.domain.usecase

import java.util.Calendar

class CalculateNextAlarmTimeUseCase {

    operator fun invoke(hour: Int, minute: Int, days: Int): Long {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis

        // Set the alarm time for today
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        var alarmTime = calendar.timeInMillis

        // If the time has already passed today, move to next occurrence
        if (alarmTime <= now) {
            if (days == 0) {
                // One-time alarm that has passed - schedule for tomorrow
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                alarmTime = calendar.timeInMillis
            } else {
                // Repeating alarm - find next day
                alarmTime = getNextRepeatingAlarmTime(hour, minute, days, now)
            }
        } else {
            // Time hasn't passed yet today
            if (days == 0) {
                // One-time alarm - use today's time
                // alarmTime is already set correctly
            } else {
                // Repeating alarm - check if today is selected
                val today = calendar.get(Calendar.DAY_OF_WEEK) - 1 // 0 = Sunday, 6 = Saturday
                val todayBit = 1 shl today
                if ((days and todayBit) == 0) {
                    // Today is not selected, find next day
                    alarmTime = getNextRepeatingAlarmTime(hour, minute, days, now)
                }
                // If today is selected, use today's time (alarmTime is already set)
            }
        }

        return alarmTime
    }

    private fun getNextRepeatingAlarmTime(hour: Int, minute: Int, days: Int, fromTime: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = fromTime

        // Start from tomorrow
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // Check each day of the week (0 = Sunday, 6 = Saturday)
        for (i in 0..6) {
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
            val dayBit = 1 shl dayOfWeek

            if ((days and dayBit) != 0) {
                // This day is selected
                return calendar.timeInMillis
            }

            // Move to next day
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        // This should never happen if days != 0
        return calendar.timeInMillis
    }
}
