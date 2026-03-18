package com.aaditx23.krazyalarm.presentation.screen.alarm_list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.aaditx23.krazyalarm.presentation.components.EmptyState
import com.aaditx23.krazyalarm.presentation.components.ErrorState
import com.aaditx23.krazyalarm.presentation.components.LoadingState
import com.aaditx23.krazyalarm.presentation.screen.DetailsModal.DetailsModalSheet
import com.aaditx23.krazyalarm.presentation.screen.DetailsModal.components.TimePickerDialog
import com.aaditx23.krazyalarm.presentation.screen.alarm_list.components.AlarmItemCard
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(
    viewModel: AlarmListViewModel = koinViewModel(),
    onNavigateToSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { true }
    )
    val uiEvents by viewModel.uiEvents.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Single shared ticker — one coroutine for the whole screen, all cards read from this
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            nowMillis = System.currentTimeMillis()
        }
    }

    LaunchedEffect(uiEvents) {
        uiEvents?.let { event ->
            when (event) {
                is UiEvent.Error -> snackbarHostState.showSnackbar(event.message)
                is UiEvent.Success -> { /* no snackbar for success */ }
                is UiEvent.AlarmDeleted -> {
                    val deletedAlarm = event.alarm
                    val result = snackbarHostState.showSnackbar(
                        message = "Alarm deleted",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoDelete(deletedAlarm)
                    }
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
                        IconButton(onClick = { viewModel.selectAllAlarms() }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select all")
                        }
                        IconButton(onClick = { viewModel.toggleSelectMode() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                        }
                        IconButton(onClick = { viewModel.showDeleteDialog(true) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete selected")
                        }
                    } else {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!uiState.isSelectMode) {
                FloatingActionButton(onClick = { viewModel.openCreateAlarmSheet() }) {
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
                uiState.isLoading -> LoadingState()
                uiState.errorMessage != null -> ErrorState(message = uiState.errorMessage!!)
                uiState.alarms.isEmpty() -> EmptyState(
                    title = "No alarms set",
                    message = "Tap the + button to create your first alarm"
                )
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
                                nowMillis = nowMillis,
                                isSelectMode = uiState.isSelectMode,
                                isSelected = uiState.selectedAlarms.contains(alarm.id),
                                modifier = Modifier.animateItem(),
                                onToggle = { enabled ->
                                    if (!uiState.isSelectMode) viewModel.toggleAlarm(alarm.id, enabled)
                                },
                                onEdit = {
                                    if (!uiState.isSelectMode) viewModel.openEditAlarmSheet(alarm.id)
                                },
                                onLongClick = {
                                    if (!uiState.isSelectMode) viewModel.toggleSelectModeAndSelect(alarm.id)
                                },
                                onSelect = {
                                    if (uiState.isSelectMode) viewModel.toggleAlarmSelection(alarm.id)
                                },
                                onDelete = { viewModel.deleteAlarm(alarm.id) },
                                onSwipeDelete = { viewModel.swipeDeleteAlarm(alarm) },
                                onTimeChange = { hour, minute ->
                                    viewModel.updateAlarmTime(alarm.id, hour, minute)
                                },
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
                TextButton(onClick = { viewModel.deleteSelectedAlarms() }) {
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

    if (uiState.showCreateTimePicker) {
        TimePickerDialog(
            initialHour = uiState.createInitialHour,
            initialMinute = uiState.createInitialMinute,
            onDismiss = { viewModel.dismissCreateTimePicker() },
            onConfirm = { h, m -> viewModel.confirmCreateTime(h, m) }
        )
    }

    if (uiState.showSheet) {
        DetailsModalSheet(
            sheetState = sheetState,
            editingAlarmId = uiState.editingAlarmId,
            initialHour = uiState.createInitialHour,
            initialMinute = uiState.createInitialMinute,
            onDismiss = { viewModel.showSheet(false) },
            onAlarmSaved = { message, enabled -> viewModel.handleAlarmSaved(message, enabled) }
        )
    }
}
