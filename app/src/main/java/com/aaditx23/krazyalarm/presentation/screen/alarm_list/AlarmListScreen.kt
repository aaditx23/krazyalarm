package com.aaditx23.krazyalarm.presentation.screen.alarm_list

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.aaditx23.krazyalarm.presentation.components.EmptyState
import com.aaditx23.krazyalarm.presentation.components.ErrorState
import com.aaditx23.krazyalarm.presentation.components.LoadingState
import com.aaditx23.krazyalarm.presentation.screen.alarm_list.DetailsModal.AlarmEditEvent
import com.aaditx23.krazyalarm.presentation.screen.alarm_list.DetailsModal.DetailsModalContent
import com.aaditx23.krazyalarm.presentation.screen.alarm_list.components.AlarmItemCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(
    viewModel: AlarmListViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val editState by viewModel.editState.collectAsState()
    val editEvents by viewModel.editEvents.collectAsState()
    val uiEvents by viewModel.uiEvents.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<android.net.Uri>(android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            viewModel.updateRingtoneUri(uri?.toString())
        }
    }

    LaunchedEffect(editEvents) {
        editEvents?.let { event ->
            when (event) {
                is AlarmEditEvent.SaveSuccessWithTime -> {
                    snackbarHostState.showSnackbar("Alarm set for ${event.hours} hours and ${event.minutes} minutes from now")
                }
                is AlarmEditEvent.SaveError -> {
                    snackbarHostState.showSnackbar("Error: ${event.message}")
                }
                AlarmEditEvent.SaveSuccess -> {
                    // Handle old event if needed
                }
            }
            viewModel.consumeEditEvent()
        }
    }

    LaunchedEffect(uiEvents) {
        uiEvents?.let { event ->
            when (event) {
                is UiEvent.Error -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
            viewModel.consumeUiEvent()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isSelectMode) "${uiState.selectedAlarms.size} selected" else "Alarms") },
                actions = {
                    if (uiState.isSelectMode) {
                        IconButton(onClick = { viewModel.toggleSelectMode() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                        }
                        IconButton(onClick = { viewModel.showDeleteDialog(true) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete selected")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!uiState.isSelectMode) {
                FloatingActionButton(onClick = {
                    viewModel.startCreateAlarm()
                    viewModel.showSheet(true)
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Alarm")
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingState()
                }
                uiState.errorMessage != null -> {
                    ErrorState(message = uiState.errorMessage!!)
                }
                uiState.alarms.isEmpty() -> {
                    EmptyState(
                        title = "No alarms set",
                        message = "Tap the + button to create your first alarm"
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = uiState.alarms,
                            key = { it.id }
                        ) { alarm ->
                            AlarmItemCard(
                                alarm = alarm,
                                isSelectMode = uiState.isSelectMode,
                                isSelected = uiState.selectedAlarms.contains(alarm.id),
                                onToggle = { enabled ->
                                    if (!uiState.isSelectMode) {
                                        viewModel.toggleAlarm(alarm.id, enabled)
                                    }
                                },
                                onEdit = {
                                    if (!uiState.isSelectMode) {
                                        viewModel.startEditAlarm(alarm.id)
                                        viewModel.showSheet(true)
                                    }
                                },
                                onLongClick = {
                                    if (!uiState.isSelectMode) {
                                        viewModel.toggleSelectModeAndSelect(alarm.id)
                                    }
                                },
                                onSelect = {
                                    if (uiState.isSelectMode) {
                                        viewModel.toggleAlarmSelection(alarm.id)
                                    }
                                },
                                onDelete = { viewModel.deleteAlarm(alarm.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showDeleteDialog(false) },
            title = { Text("Delete Alarms") },
            text = { Text("Are you sure you want to delete ${uiState.selectedAlarms.size} alarm(s)?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSelectedAlarms()
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Deleted ${uiState.selectedAlarms.size} alarm(s)")
                    }
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showDeleteDialog(false) }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (uiState.showSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                viewModel.showSheet(false)
            },
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
                onSave = {
                    viewModel.showSheet(false)
                    viewModel.loadAlarms() // Refresh the list
                },
                state = editState,
                events = editEvents,
                onConsumeEvent = viewModel::consumeEditEvent,
                onUpdateHour = viewModel::updateHour,
                onUpdateMinute = viewModel::updateMinute,
                onUpdateDays = viewModel::updateDays,
                onUpdateEnabled = viewModel::updateEnabled,
                onUpdateLabel = viewModel::updateLabel,
                onUpdateFlashPattern = viewModel::updateFlashPattern,
                onUpdateVibrationPattern = viewModel::updateVibrationPattern,
                onUpdateVibrationIntensity = viewModel::updateVibrationIntensity,
                onUpdateSnoozeDuration = viewModel::updateSnoozeDuration,
                onSaveAlarm = viewModel::saveAlarm,
                modifier = Modifier,
                onDelete = viewModel::deleteCurrentAlarm,
                onDismiss = { viewModel.showSheet(false) },
                onSoundClick = {
                    val intent = android.content.Intent(android.media.RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TYPE, android.media.RingtoneManager.TYPE_ALARM)
                        putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Sound")
                        putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, editState.ringtoneUri?.let { android.net.Uri.parse(it) })
                        putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                        putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                    }
                    ringtonePickerLauncher.launch(intent)
                },
                onScheduleClick = { viewModel.showDatePicker(true) },
                onUpdateRingtoneUri = viewModel::updateRingtoneUri,
            )
        }

        LaunchedEffect(sheetState.isVisible) {
            if (sheetState.isVisible) {
                sheetState.expand()
            }
        }
    }

    if (uiState.showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { viewModel.showDatePicker(false) },
            confirmButton = {
                TextButton(onClick = { viewModel.showDatePicker(false) }) {
                    Text("OK")
                }
            }
        ) {
            DatePicker(state = rememberDatePickerState())
        }
    }
}
