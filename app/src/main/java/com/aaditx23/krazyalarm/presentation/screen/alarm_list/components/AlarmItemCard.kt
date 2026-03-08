package com.aaditx23.krazyalarm.presentation.screen.alarm_list.components

import android.annotation.SuppressLint
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aaditx23.krazyalarm.domain.models.Alarm
import com.aaditx23.krazyalarm.domain.util.AlarmScheduleFormatter
import com.aaditx23.krazyalarm.domain.util.AlarmTimeCalculator
import com.aaditx23.krazyalarm.presentation.screen.DetailsModal.components.TimePickerDialog

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmItemCard(
    alarm: Alarm,
    nowMillis: Long,
    isSelectMode: Boolean,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onLongClick: () -> Unit,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onSwipeDelete: () -> Unit = onDelete,
    onTimeChange: (hour: Int, minute: Int) -> Unit,
) {
    var showTimePicker by remember { mutableStateOf(false) }
    var pickerHour by remember(alarm.hour) { mutableIntStateOf(alarm.hour) }
    var pickerMinute by remember(alarm.minute) { mutableIntStateOf(alarm.minute) }

    val triggerTime = remember(alarm) {
        if (alarm.enabled) AlarmTimeCalculator.getNextTriggerTime(alarm) else 0L
    }
    val remainingMillis = if (alarm.enabled) (triggerTime - nowMillis).coerceAtLeast(0L) else 0L

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (!isSelectMode &&
                (value == SwipeToDismissBoxValue.EndToStart ||
                 value == SwipeToDismissBoxValue.StartToEnd)
            ) {
                onSwipeDelete()
                true
            } else {
                false
            }
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.35f }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = !isSelectMode,
        enableDismissFromEndToStart = !isSelectMode,
        backgroundContent = {
            val progress = dismissState.progress
            val isEndToStart = dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart
            val triggered =
                dismissState.targetValue == SwipeToDismissBoxValue.EndToStart ||
                dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd

            val bgColor by animateColorAsState(
                targetValue = if (triggered)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                label = "swipeBg"
            )
            val iconScale by animateFloatAsState(
                targetValue = if (triggered) 1.2f else 0.8f + (progress * 0.4f),
                label = "iconScale"
            )

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val totalWidth = maxWidth
                val balloonWidth = (totalWidth * progress).coerceAtLeast(0.dp)

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (isEndToStart) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .width(balloonWidth)
                            .fillMaxHeight()
                            .background(
                                color = bgColor,
                                shape = RoundedCornerShape(24.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (progress > 0.05f) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete alarm",
                                tint = MaterialTheme.colorScheme.onError,
                                modifier = Modifier
                                    .size(24.dp)
                                    .scale(iconScale)
                            )
                        }
                    }
                }
            }
        }
    ) {
        // — Card content —
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
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
                        // Schedule label + user label
                        Row(
                            modifier = Modifier
                                .padding(vertical = 6.dp)
                                .padding(start = 4.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val displayText = AlarmScheduleFormatter.cardLabel(alarm)
                            if (displayText.isNotEmpty()) {
                                Text(
                                    text = displayText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Normal
                                )
                            }
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

                        // Time display — clickable, opens time picker
                        val hour12 = if (alarm.hour == 0) 12 else if (alarm.hour > 12) alarm.hour - 12 else alarm.hour
                        val amPm = if (alarm.hour < 12) "AM" else "PM"

                        Surface(
                            shape = RoundedCornerShape(24.dp),
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
                    } // end Column

                    Switch(
                        checked = alarm.enabled,
                        onCheckedChange = onToggle,
                    )
                } // end Row
            } // end Card
        } // end Box
    } // end SwipeToDismissBox content

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
