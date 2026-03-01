package com.aaditx23.krazyalarm.presentation.screen.alarm_list

import com.aaditx23.krazyalarm.domain.models.Alarm

data class UiState(
    val isLoading: Boolean = false,
    val alarms: List<Alarm> = emptyList(),
    val errorMessage: String? = null,
    val showSheet: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val isSelectMode: Boolean = false,
    val selectedAlarms: Set<Long> = emptySet(),
    val editingAlarmId: Long? = null, // null = create mode, non-null = edit mode
    val showCreateTimePicker: Boolean = false, // show time picker before opening the sheet
    val createInitialHour: Int = 0,
    val createInitialMinute: Int = 0
)
