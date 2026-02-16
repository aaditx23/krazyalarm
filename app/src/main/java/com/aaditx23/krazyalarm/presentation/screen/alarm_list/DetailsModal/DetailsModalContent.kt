package com.aaditx23.krazyalarm.presentation.screen.alarm_list.DetailsModal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aaditx23.krazyalarm.domain.models.FlashPattern
import com.aaditx23.krazyalarm.domain.models.VibrationIntensity
import com.aaditx23.krazyalarm.domain.models.VibrationPattern
import com.aaditx23.krazyalarm.presentation.screen.alarm_list.DetailsModal.components.ActionButtons
import com.aaditx23.krazyalarm.presentation.screen.alarm_list.DetailsModal.components.AlarmNameCard
import com.aaditx23.krazyalarm.presentation.screen.alarm_list.DetailsModal.components.DaySelectorSection
import com.aaditx23.krazyalarm.presentation.screen.alarm_list.DetailsModal.components.FlashPatternCard
import com.aaditx23.krazyalarm.presentation.screen.alarm_list.DetailsModal.components.FlashPatternSelectionDialog
import com.aaditx23.krazyalarm.presentation.screen.alarm_list.DetailsModal.components.ScheduleAlarmButton
import com.aaditx23.krazyalarm.presentation.screen.alarm_list.DetailsModal.components.SoundCard
import com.aaditx23.krazyalarm.presentation.screen.alarm_list.DetailsModal.components.TimeDisplaySection
import com.aaditx23.krazyalarm.presentation.screen.alarm_list.DetailsModal.components.TimePickerDialog
import com.aaditx23.krazyalarm.presentation.screen.alarm_list.DetailsModal.components.UpcomingAlarmSection
import com.aaditx23.krazyalarm.presentation.screen.alarm_list.DetailsModal.components.VibrationPatternCard
import com.aaditx23.krazyalarm.presentation.screen.alarm_list.DetailsModal.components.VibrationPatternSelectionDialog

@Composable
fun DetailsModalContent(
    onSave: () -> Unit,
    state: AlarmEditState,
    events: AlarmEditEvent?,
    onConsumeEvent: () -> Unit,
    onUpdateHour: (Int) -> Unit,
    onUpdateMinute: (Int) -> Unit,
    onUpdateDays: (Int) -> Unit,
    onUpdateEnabled: (Boolean) -> Unit,
    onUpdateLabel: (String) -> Unit,
    onUpdateFlashPattern: (FlashPattern) -> Unit,
    onUpdateVibrationPattern: (VibrationPattern) -> Unit,
    onUpdateVibrationIntensity: (VibrationIntensity) -> Unit,
    onUpdateSnoozeDuration: (Int) -> Unit,
    onSaveAlarm: () -> Unit,
    modifier: Modifier = Modifier,
    onDelete: () -> Unit = {},
    onDismiss: () -> Unit = {},
    onSoundClick: () -> Unit = {},
    onScheduleClick: () -> Unit = {},
    onUpdateRingtoneUri: (String?) -> Unit,
) {
    var showTimePicker by remember { mutableStateOf(false) }
    var showFlashPatternDialog by remember { mutableStateOf(false) }
    var showVibrationPatternDialog by remember { mutableStateOf(false) }

    // Handle events
    events?.let { event ->
        when (event) {
            AlarmEditEvent.SaveSuccess -> onSave()
            is AlarmEditEvent.SaveSuccessWithTime -> onSave()
            is AlarmEditEvent.SaveError -> {
                // TODO: Show error message
            }
        }
        onConsumeEvent()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .padding(horizontal = 0.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Top spacing
        Spacer(modifier = Modifier.height(24.dp))

        // Time Display Section with Edit Button
        TimeDisplaySection(
            hour = state.hour,
            minute = state.minute,
            onTimeClick = { showTimePicker = true },
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Day Selector
        DaySelectorSection(
            selectedDays = state.days,
            onDaysChange = onUpdateDays,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Upcoming Alarm Info and Schedule Alarm
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            UpcomingAlarmSection(
                hour = state.hour,
                minute = state.minute,
                days = state.days,
                scheduledDate = state.scheduledDate
            )

            ScheduleAlarmButton(
                onScheduleClick = onScheduleClick,
                hasScheduledDate = state.scheduledDate != null
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Alarm Name Card
        AlarmNameCard(
            label = state.label,
            onLabelChange = onUpdateLabel,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Sound Card
        SoundCard(
            soundName = state.ringtoneName,
            onSoundClick = onSoundClick,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Flash Pattern Card
        FlashPatternCard(
            patternName = state.flashPattern.displayName,
            onPatternClick = { showFlashPatternDialog = true },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Vibration Pattern Card
        VibrationPatternCard(
            patternName = state.vibrationPattern.displayName,
            onPatternClick = { showVibrationPatternDialog = true },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Action Buttons (Delete and Save)
        ActionButtons(
            onDelete = onDelete,
            onSave = onSaveAlarm,
            isEditMode = state.isEditMode,
            isLoading = state.isLoading,
            onDismiss = onDismiss,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))
    }

    // Time Picker Dialog
    if (showTimePicker) {
        TimePickerDialog(
            initialHour = state.hour,
            initialMinute = state.minute,
            onHourChange = { onUpdateHour(it) },
            onMinuteChange = { onUpdateMinute(it) },
            onDismiss = { showTimePicker = false }
        )
    }

    // Flash Pattern Selection Dialog
    if (showFlashPatternDialog) {
        FlashPatternSelectionDialog(
            selectedPattern = state.flashPattern,
            onPatternSelected = onUpdateFlashPattern,
            onDismiss = { showFlashPatternDialog = false }
        )
    }

    // Vibration Pattern Selection Dialog
    if (showVibrationPatternDialog) {
        VibrationPatternSelectionDialog(
            selectedPattern = state.vibrationPattern,
            onPatternSelected = onUpdateVibrationPattern,
            onDismiss = { showVibrationPatternDialog = false }
        )
    }
}
