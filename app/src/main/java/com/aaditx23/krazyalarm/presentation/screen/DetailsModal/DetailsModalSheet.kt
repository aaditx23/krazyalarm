package com.aaditx23.krazyalarm.presentation.screen.DetailsModal

import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsModalSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onAlarmSaved: () -> Unit,
    viewModel: DetailsModalViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val editState by viewModel.editState.collectAsState()
    val editEvents by viewModel.editEvents.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }

    // Ringtone picker launcher
    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        android.util.Log.d("DetailsModalSheet", "Ringtone picker result received, resultCode: ${result.resultCode}")
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.let { intent ->
                IntentCompat.getParcelableExtra(
                    intent,
                    android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI,
                    android.net.Uri::class.java
                )
            }
            android.util.Log.d("DetailsModalSheet", "Selected ringtone URI: $uri")
            viewModel.updateRingtoneUri(uri?.toString())

            // Fetch ringtone name using ViewModel
            viewModel.fetchRingtoneName(context, uri?.toString())
        } else {
            android.util.Log.d("DetailsModalSheet", "Ringtone picker cancelled or failed")
        }
    }

    // Fetch ringtone name when modal opens
    LaunchedEffect(Unit) {
        android.util.Log.d("DetailsModalSheet", "=== Modal opened, triggering ringtone fetch ===")
        viewModel.fetchRingtoneName(context)
    }

    // Handle save success event
    LaunchedEffect(editEvents) {
        editEvents?.let { event ->
            when (event) {
                is AlarmEditEvent.SaveSuccess,
                is AlarmEditEvent.SaveSuccessWithTime,
                is AlarmEditEvent.SaveSuccessWithMessage -> {
                    onAlarmSaved()
                    viewModel.consumeEditEvent()
                }
                is AlarmEditEvent.SaveError -> {
                    // Error is shown in the UI
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .width(32.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                )
            }
        }
    ) {
        DetailsModalContent(
            viewModel = viewModel,
            onDismiss = onDismiss,
            onSoundClick = {
                val intent = Intent(android.media.RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                    putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TYPE, android.media.RingtoneManager.TYPE_ALARM)
                    putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Sound")
                    putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, editState.ringtoneUri?.let { android.net.Uri.parse(it) })
                    putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                    putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                }
                ringtonePickerLauncher.launch(intent)
            },
            onScheduleClick = { showDatePicker = true }
        )
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = editState.scheduledDate ?: System.currentTimeMillis()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedMillis ->
                        val calendar = java.util.Calendar.getInstance().apply {
                            timeInMillis = selectedMillis
                            set(java.util.Calendar.HOUR_OF_DAY, editState.hour)
                            set(java.util.Calendar.MINUTE, editState.minute)
                            set(java.util.Calendar.SECOND, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }

                        viewModel.updateScheduledDate(calendar.timeInMillis)
                        viewModel.updateDays(0)
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Auto-expand sheet when visible
    LaunchedEffect(sheetState.isVisible) {
        if (sheetState.isVisible) {
            sheetState.expand()
        }
    }
}
