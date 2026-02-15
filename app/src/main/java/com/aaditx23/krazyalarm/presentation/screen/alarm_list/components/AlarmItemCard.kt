package com.aaditx23.krazyalarm.presentation.screen.alarm_list.components

import android.annotation.SuppressLint
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aaditx23.krazyalarm.domain.models.Alarm

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
    modifier: Modifier = Modifier
) {
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Row(
                        modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        // Days of week
                        val daysText = getDaysOfWeekText(alarm.days)
                        if (daysText.isNotEmpty()) {
                            Text(
                                text = daysText,
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
                    // Time
                    Text(
                        text = String.format("%02d:%02d", alarm.hour, alarm.minute),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Toggle switch
                Switch(
                    checked = alarm.enabled,
                    onCheckedChange = onToggle,

                )
            }
        }
    }
}

private fun getDaysOfWeekText(days: Int): String {
    val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val selectedDays = mutableListOf<String>()

    for (i in 0..6) {
        if ((days and (1 shl i)) != 0) {
            selectedDays.add(dayNames[i])
        }
    }

    return when {
        selectedDays.size == 7 -> "Every day"
        selectedDays.size == 5 && !selectedDays.contains("Sat") && !selectedDays.contains("Sun") -> "Weekdays"
        selectedDays.size == 2 && selectedDays.contains("Sat") && selectedDays.contains("Sun") -> "Weekends"
        selectedDays.isEmpty() -> "One time"
        else -> selectedDays.joinToString(", ")
    }
}
