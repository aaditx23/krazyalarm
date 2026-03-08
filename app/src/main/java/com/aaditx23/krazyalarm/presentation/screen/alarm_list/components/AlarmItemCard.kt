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
import com.aaditx23.krazyalarm.domain.util.AlarmScheduleFormatter
import com.aaditx23.krazyalarm.domain.util.AlarmTimeCalculator
import com.aaditx23.krazyalarm.presentation.screen.DetailsModal.components.TimePickerDialog

@SuppressLint("DefaultLocale")
@Composable
fun AlarmItemCard(
    alarm: Alarm,
    nowMillis: Long,
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

    // Trigger time is only recomputed when the alarm itself changes (data class equality).
    // The countdown then derives from (triggerTime - nowMillis) — pure subtraction, no coroutine.
    val triggerTime = remember(alarm) {
        if (alarm.enabled) AlarmTimeCalculator.getNextTriggerTime(alarm) else 0L
    }
    val remainingMillis = if (alarm.enabled) (triggerTime - nowMillis).coerceAtLeast(0L) else 0L

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
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.padding(vertical = 6.dp).padding(start=4.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Centralised schedule label
                        val displayText = AlarmScheduleFormatter.cardLabel(alarm)
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
                    // Countdown — only shown when enabled
                    if (alarm.enabled) {
                        val totalSeconds = remainingMillis / 1000
                        val days    = totalSeconds / 86400
                        val hours   = (totalSeconds % 86400) / 3600
                        val minutes = (totalSeconds % 3600) / 60
                        val seconds = totalSeconds % 60
                        Text(
                            text = String.format("%02d:%02d:%02d:%02d", days, hours, minutes, seconds),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.W700,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 8.dp)
                        )
                    }

                    // Time — clickable, opens time picker
                    val hour12 = if (alarm.hour == 0) 12 else if (alarm.hour > 12) alarm.hour - 12 else alarm.hour
                    val amPm = if (alarm.hour < 12) "AM" else "PM"

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0f),
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

                Switch(
                    checked = alarm.enabled,
                    onCheckedChange = onToggle,
                )
            }
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            initialHour = pickerHour,
            initialMinute = pickerMinute,
            onDismiss = { showTimePicker = false },
            onConfirm = { h, m ->
                showTimePicker = false
                onTimeChange(h, m)
            }
        )
    }
}
