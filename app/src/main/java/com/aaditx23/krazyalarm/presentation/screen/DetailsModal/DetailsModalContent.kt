package com.aaditx23.krazyalarm.presentation.screen.DetailsModal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aaditx23.krazyalarm.presentation.screen.DetailsModal.components.ActionButtons
import com.aaditx23.krazyalarm.presentation.screen.DetailsModal.components.AlarmNameCard
import com.aaditx23.krazyalarm.presentation.screen.DetailsModal.components.DaySelectorSection
import com.aaditx23.krazyalarm.presentation.screen.DetailsModal.components.FlashPatternCard
import com.aaditx23.krazyalarm.presentation.screen.DetailsModal.components.FlashPatternSelectionDialog
import com.aaditx23.krazyalarm.presentation.screen.DetailsModal.components.ScheduleAlarmButton
import com.aaditx23.krazyalarm.presentation.screen.DetailsModal.components.SoundCard
import com.aaditx23.krazyalarm.presentation.screen.DetailsModal.components.TimeDisplaySection
import com.aaditx23.krazyalarm.presentation.screen.DetailsModal.components.UpcomingAlarmSection
import com.aaditx23.krazyalarm.presentation.screen.DetailsModal.components.VibrationPatternCard
import com.aaditx23.krazyalarm.presentation.screen.DetailsModal.components.VibrationPatternSelectionDialog

@Composable
fun DetailsModalContent(
    viewModel: DetailsModalViewModel,
    onDismiss: () -> Unit,
    onSoundClick: () -> Unit,
    onScheduleClick: () -> Unit,
    onTimeClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.editState.collectAsState()
    val events by viewModel.editEvents.collectAsState()

    var showFlashPatternDialog by remember { mutableStateOf(false) }
    var showVibrationPatternDialog by remember { mutableStateOf(false) }

    // Handle events - Don't consume or close for success events, let the parent handle it
    events?.let { event ->
        when (event) {
            AlarmEditEvent.SaveSuccess -> {
                // Let parent handle
            }
            is AlarmEditEvent.SaveSuccessWithTime -> {
                // Let parent handle
            }
            is AlarmEditEvent.SaveSuccessWithMessage -> {
                // Let parent handle
            }
            is AlarmEditEvent.SaveError -> {
                // Error is already handled by showing in UI state
                // Could add SnackBar here if needed
            }
        }
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

        // Time Display Section with Toggle
        TimeDisplaySection(
            hour = state.hour,
            minute = state.minute,
            enabled = state.enabled,
            onEnabledChange = viewModel::updateEnabled,
            onTimeClick = onTimeClick,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Day Selector
        DaySelectorSection(
            selectedDays = state.days,
            onDaysChange = viewModel::updateDays,
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
            onLabelChange = viewModel::updateLabel,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Sound Card
        SoundCard(
            soundName = state.ringtoneName,
            isLoading = state.isLoadingRingtone,
            onSoundClick = onSoundClick,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Flash Pattern Card
        FlashPatternCard(
            patternName = state.flashPattern.displayName,
            onPatternClick = { showFlashPatternDialog = true },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Vibration Pattern Card
        VibrationPatternCard(
            patternName = state.vibrationPattern.displayName,
            onPatternClick = { showVibrationPatternDialog = true },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))


        // Action Buttons (Delete and Save)
        ActionButtons(
            onDelete = {
                viewModel.deleteAlarm()
                onDismiss()
            },
            onSave = viewModel::saveAlarm,
            isEditMode = state.isEditMode,
            isLoading = state.isLoading,
            isSaving = state.isSaving,
            onDismiss = onDismiss,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))
    }


    // Flash Pattern Selection Dialog
    if (showFlashPatternDialog) {
        FlashPatternSelectionDialog(
            selectedPattern = state.flashPattern,
            onPatternSelected = viewModel::updateFlashPattern,
            onDismiss = { showFlashPatternDialog = false }
        )
    }

    // Vibration Pattern Selection Dialog
    if (showVibrationPatternDialog) {
        VibrationPatternSelectionDialog(
            selectedPattern = state.vibrationPattern,
            onPatternSelected = viewModel::updateVibrationPattern,
            onDismiss = { showVibrationPatternDialog = false }
        )
    }
}
