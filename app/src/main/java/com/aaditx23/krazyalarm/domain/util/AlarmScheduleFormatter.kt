package com.aaditx23.krazyalarm.domain.util

import com.aaditx23.krazyalarm.domain.models.Alarm
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Single source of truth for human-readable alarm schedule text.
 *
 * Provides two levels of description:
 *  - [snackbarMessage] — full sentence used in snackbar after toggling on
 *    e.g. "Alarm scheduled in 3 hours and 20 minutes"
 *         "Alarm scheduled for tomorrow at 07:30"
 *
 *  - [cardLabel] — short label shown on the alarm card
 *    e.g. "Every day" / "Weekdays" / "Mon-Wed" / "Tomorrow" / "Sat, Mar 7"
 */
object AlarmScheduleFormatter {

    // ── Card label ────────────────────────────────────────────────────────────

    /**
     * Short text shown inside the alarm card when the alarm is enabled.
     * Returns "Not Scheduled" when [alarm.enabled] is false.
     */
    fun cardLabel(alarm: Alarm): String {
        if (!alarm.enabled) return "Not Scheduled"

        // One-time alarm with a pinned date
        if (alarm.days == 0 && alarm.scheduledDate != null) {
            val triggerTime = AlarmTimeCalculator.getNextTriggerTime(alarm)
            return when {
                isTomorrow(triggerTime) -> "Tomorrow"
                isToday(triggerTime)    -> "Today"
                else -> {
                    val fmt = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
                    fmt.format(triggerTime)
                }
            }
        }

        // Repeating alarm
        return getDaysOfWeekText(alarm.days)
    }

    // ── Snackbar message ──────────────────────────────────────────────────────

    /**
     * Full sentence used in the snackbar / bottom-sheet after saving/enabling an alarm.
     */
    fun snackbarMessage(alarm: Alarm): String {
        val now = System.currentTimeMillis()
        val triggerTime = AlarmTimeCalculator.getNextTriggerTime(alarm)
        val diffMillis = triggerTime - now

        val totalMinutes = kotlin.math.ceil(diffMillis / 60000.0).toLong()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        return when {
            isToday(triggerTime) -> {
                if (hours > 0) {
                    "Alarm scheduled in $hours hour${if (hours != 1L) "s" else ""} and $minutes minute${if (minutes != 1L) "s" else ""}"
                } else {
                    "Alarm scheduled in $minutes minute${if (minutes != 1L) "s" else ""}"
                }
            }
            isTomorrow(triggerTime) -> {
                val hour12 = when {
                    alarm.hour == 0  -> 12
                    alarm.hour > 12  -> alarm.hour - 12
                    else             -> alarm.hour
                }
                val amPm = if (alarm.hour < 12) "AM" else "PM"
                val timeStr = String.format(Locale.getDefault(), "%02d:%02d %s", hour12, alarm.minute, amPm)
                "Alarm scheduled for tomorrow at $timeStr"
            }
            else -> {
                val fmt = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
                "Alarm scheduled for ${fmt.format(triggerTime)}"
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun isToday(timeMillis: Long): Boolean {
        val target = Calendar.getInstance().apply { this.timeInMillis = timeMillis }
        val today  = Calendar.getInstance()
        return target.get(Calendar.YEAR)        == today.get(Calendar.YEAR) &&
               target.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    }

    private fun isTomorrow(timeMillis: Long): Boolean {
        val target   = Calendar.getInstance().apply { this.timeInMillis = timeMillis }
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        return target.get(Calendar.YEAR)        == tomorrow.get(Calendar.YEAR) &&
               target.get(Calendar.DAY_OF_YEAR) == tomorrow.get(Calendar.DAY_OF_YEAR)
    }

    private fun getDaysOfWeekText(days: Int): String {
        val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val selectedIndices = (0..6).filter { (days and (1 shl it)) != 0 }
        val selectedDays = selectedIndices.map { dayNames[it] }

        return when {
            selectedDays.size == 7 -> "Every day"
            selectedDays.size == 5 &&
                !selectedDays.contains("Sat") &&
                !selectedDays.contains("Sun") -> "Weekdays"
            selectedDays.size == 2 &&
                selectedDays.contains("Sat") &&
                selectedDays.contains("Sun") -> "Weekends"
            selectedDays.isEmpty() -> "One time"
            else -> {
                val ranges = mutableListOf<String>()
                var rangeStart = selectedIndices[0]
                var rangeEnd   = selectedIndices[0]
                for (i in 1 until selectedIndices.size) {
                    if (selectedIndices[i] == rangeEnd + 1) {
                        rangeEnd = selectedIndices[i]
                    } else {
                        ranges.add(formatRange(dayNames, rangeStart, rangeEnd))
                        rangeStart = selectedIndices[i]
                        rangeEnd   = selectedIndices[i]
                    }
                }
                ranges.add(formatRange(dayNames, rangeStart, rangeEnd))
                ranges.joinToString(", ")
            }
        }
    }

    private fun formatRange(dayNames: List<String>, start: Int, end: Int): String = when {
        start == end       -> dayNames[start]
        end == start + 1   -> "${dayNames[start]}, ${dayNames[end]}"
        else               -> "${dayNames[start]}-${dayNames[end]}"
    }
}

