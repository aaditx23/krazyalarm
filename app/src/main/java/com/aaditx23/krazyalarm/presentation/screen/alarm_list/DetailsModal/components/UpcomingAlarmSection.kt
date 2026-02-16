package com.aaditx23.krazyalarm.presentation.screen.alarm_list.DetailsModal.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun UpcomingAlarmSection(
    hour: Int,
    minute: Int,
    days: Int,
    scheduledDate: Long? = null
) {
    Column {
        Text(
            text = "Upcoming alarm",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))

        val upcomingText = if (scheduledDate != null) {
            // Show specific scheduled date
            formatScheduledTime(scheduledDate)
        } else if (days == 0) {
            // One-time alarm without specific date
            val now = Calendar.getInstance()
            val alarmTime = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // Only move to next day if alarm time has already passed today
            if (alarmTime.timeInMillis < now.timeInMillis) {
                alarmTime.add(Calendar.DAY_OF_YEAR, 1)
            }

            formatScheduledTime(alarmTime.timeInMillis)
        } else {
            // Recurring alarm - calculate next occurrence based on days
            val now = Calendar.getInstance()
            val alarmTime = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val currentDayOfWeek = now.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon, ..., 7=Sat
            var daysUntilNext = -1

            // Check today first if time hasn't passed
            if (alarmTime.timeInMillis > now.timeInMillis && (days and (1 shl (currentDayOfWeek - 1))) != 0) {
                daysUntilNext = 0
            } else {
                // Check next 7 days
                for (i in 1..7) {
                    val checkDay = (currentDayOfWeek - 1 + i) % 7 + 1
                    if ((days and (1 shl (checkDay - 1))) != 0) {
                        daysUntilNext = i
                        break
                    }
                }
            }

            if (daysUntilNext >= 0) {
                alarmTime.add(Calendar.DAY_OF_YEAR, daysUntilNext)
                formatScheduledTime(alarmTime.timeInMillis)
            } else {
                "No days selected"
            }
        }

        Text(
            text = upcomingText,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatScheduledTime(timeMillis: Long): String {
    // Reset time parts to compare dates only
    val todayStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val alarmDayStart = Calendar.getInstance().apply {
        timeInMillis = timeMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val daysDifference = ((alarmDayStart.timeInMillis - todayStart.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()


    return when (daysDifference) {
        0 -> "Today"
        1 -> "Tomorrow"
        else -> {
            val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
            "Scheduled for ${dateFormat.format(timeMillis)}"
        }
    }
}

