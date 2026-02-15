package com.aaditx23.krazyalarm.presentation.screen.alarm_list

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.aaditx23.krazyalarm.presentation.components.EmptyState
import com.aaditx23.krazyalarm.presentation.components.ErrorState
import com.aaditx23.krazyalarm.presentation.components.LoadingState
import com.aaditx23.krazyalarm.presentation.screen.alarm_list.DetailsModal.DetailsModalContent
import com.aaditx23.krazyalarm.presentation.screen.alarm_list.components.AlarmItemCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(
    viewModel: AlarmListViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val editState by viewModel.editState.collectAsState()
    val editEvents by viewModel.editEvents.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alarms") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                viewModel.startCreateAlarm()
                showSheet = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Alarm")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is UiState.Loading -> {
                    LoadingState()
                }
                is UiState.Success -> {
                    if (state.alarms.isEmpty()) {
                        EmptyState(
                            title = "No alarms set",
                            message = "Tap the + button to create your first alarm"
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = state.alarms,
                                key = { it.id }
                            ) { alarm ->
                                AlarmItemCard(
                                    alarm = alarm,
                                    onToggle = { enabled ->
                                        viewModel.toggleAlarm(alarm.id, enabled)
                                    },
                                    onEdit = {
                                        viewModel.startEditAlarm(alarm.id)
                                        showSheet = true
                                    },
                                    onDelete = { viewModel.deleteAlarm(alarm.id) }
                                )
                            }
                        }
                    }
                }
                is UiState.Error -> {
                    ErrorState(message = state.message)
                }
            }
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showSheet = false
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
                    showSheet = false
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
                onDelete = { /* TODO: handle delete */ }
            )
        }

        LaunchedEffect(sheetState.isVisible) {
            if (sheetState.isVisible) {
                sheetState.expand()
            }
        }
    }
}
