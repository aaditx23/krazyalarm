package com.aaditx23.krazyalarm.presentation.screen.alarm_list.components

import android.annotation.SuppressLint
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aaditx23.krazyalarm.domain.models.Alarm
import com.aaditx23.krazyalarm.presentation.screen.DetailsModal.components.TimePickerDialog
import java.text.SimpleDateFormat
import java.util.Locale

@SuppressLint("DefaultLocale")
@Composable
fun AlarmItemCard(
    alarm: Alarm,
    isSelectMode: Boolean,
    isSelected: Boolean,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onLongClick: () -> Unit,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onTimeChange: (hour: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showTimePicker by remember { mutableStateOf(false) }
    var pickerHour by remember(alarm.hour) { mutableIntStateOf(alarm.hour) }
    var pickerMinute by remember(alarm.minute) { mutableIntStateOf(alarm.minute) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = if (isSelectMode) onSelect else onEdit,
                onLongClick = if (!isSelectMode) onLongClick else null
            )
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = when {
                    isSelectMode && isSelected -> MaterialTheme.colorScheme.tertiaryContainer
                    alarm.enabled -> MaterialTheme.colorScheme.secondaryContainer
                    else -> CardDefaults.cardColors().containerColor
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        // Show "Not Scheduled" when disabled, otherwise show date/days
                        val displayText = if (!alarm.enabled) {
                            "Not Scheduled"
                        } else if (alarm.scheduledDate != null) {
                            val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
                            "Scheduled for ${dateFormat.format(alarm.scheduledDate)}"
                        } else {
                            getDaysOfWeekText(alarm.days)
                        }

                        if (displayText.isNotEmpty()) {
                            Text(
                                text = displayText,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Normal
                            )
                        }

                        // Label
                        alarm.label?.let { label ->
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Thin,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }

                    // Time — clickable with rounded corners, opens time picker
                    val hour12 = if (alarm.hour == 0) 12 else if (alarm.hour > 12) alarm.hour - 12 else alarm.hour
                    val amPm = if (alarm.hour < 12) "AM" else "PM"

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0f), // transparent
                        onClick = {
                            if (!isSelectMode) {
                                pickerHour = alarm.hour
                                pickerMinute = alarm.minute
                                showTimePicker = true
                            }
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = String.format("%02d:%02d", hour12, alarm.minute),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,

                            )
                            Text(
                                text = amPm.lowercase(),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Light,
                                modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                            )
                        }
                    }
                }

                // Toggle switch
                Switch(
                    checked = alarm.enabled,
                    onCheckedChange = onToggle,
                )
            }
        }
    }

    // Inline time picker dialog
    if (showTimePicker) {
        TimePickerDialog(
            initialHour = pickerHour,
            initialMinute = pickerMinute,
            onHourChange = { pickerHour = it },
            onMinuteChange = { pickerMinute = it },
            onDismiss = { showTimePicker = false },
            onConfirm = {
                showTimePicker = false
                onTimeChange(pickerHour, pickerMinute)
            }
        )
    }
}

private fun getDaysOfWeekText(days: Int): String {
    val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val selectedIndices = mutableListOf<Int>()

    for (i in 0..6) {
        if ((days and (1 shl i)) != 0) {
            selectedIndices.add(i)
        }
    }

    val selectedDays = selectedIndices.map { dayNames[it] }

    return when {
        selectedDays.size == 7 -> "Every day"
        selectedDays.size == 5 && !selectedDays.contains("Sat") && !selectedDays.contains("Sun") -> "Weekdays"
        selectedDays.size == 2 && selectedDays.contains("Sat") && selectedDays.contains("Sun") -> "Weekends"
        selectedDays.isEmpty() -> "One time"
        else -> {
            // Group consecutive days into ranges
            val ranges = mutableListOf<String>()
            var rangeStart = selectedIndices[0]
            var rangeEnd = selectedIndices[0]

            for (i in 1 until selectedIndices.size) {
                if (selectedIndices[i] == rangeEnd + 1) {
                    // Consecutive day, extend range
                    rangeEnd = selectedIndices[i]
                } else {
                    // Non-consecutive, save current range and start new one
                    ranges.add(formatRange(dayNames, rangeStart, rangeEnd))
                    rangeStart = selectedIndices[i]
                    rangeEnd = selectedIndices[i]
                }
            }
            // Add the last range
            ranges.add(formatRange(dayNames, rangeStart, rangeEnd))

            ranges.joinToString(", ")
        }
    }
}

private fun formatRange(dayNames: List<String>, start: Int, end: Int): String {
    return if (start == end) {
        dayNames[start]
    } else if (end == start + 1) {
        "${dayNames[start]}, ${dayNames[end]}"
    } else {
        "${dayNames[start]}-${dayNames[end]}"
    }
}
