package com.aaditx23.krazyalarm.presentation.screen.alarm_list

import UiEvent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aaditx23.krazyalarm.domain.models.AlarmInput
import com.aaditx23.krazyalarm.domain.usecase.DeleteAlarmUseCase
import com.aaditx23.krazyalarm.domain.usecase.GetAlarmByIdUseCase
import com.aaditx23.krazyalarm.domain.usecase.GetAlarmsUseCase
import com.aaditx23.krazyalarm.domain.usecase.ToggleAlarmUseCase
import com.aaditx23.krazyalarm.domain.usecase.UpdateAlarmUseCase
import com.aaditx23.krazyalarm.domain.util.AlarmTimeCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class AlarmListViewModel(
    private val getAlarmsUseCase: GetAlarmsUseCase,
    private val toggleAlarmUseCase: ToggleAlarmUseCase,
    private val deleteAlarmUseCase: DeleteAlarmUseCase,
    private val getAlarmByIdUseCase: GetAlarmByIdUseCase,
    private val updateAlarmUseCase: UpdateAlarmUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState(isLoading = true))
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _uiEvents = MutableStateFlow<UiEvent?>(null)
    val uiEvents: StateFlow<UiEvent?> = _uiEvents.asStateFlow()


    init {
        loadAlarms()
    }

    fun loadAlarms() {
        getAlarmsUseCase()
            .onEach { alarms ->
                _uiState.value = _uiState.value.copy(isLoading = false, alarms = alarms, errorMessage = null)
            }
            .catch { exception ->
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Failed to load alarms: ${exception.message}")
            }
            .launchIn(viewModelScope)
    }

    fun toggleAlarm(alarmId: Long, enabled: Boolean) {
        viewModelScope.launch {
            toggleAlarmUseCase(alarmId, enabled)
                .onSuccess {
                    if (enabled) {
                        // Get the alarm details to show schedule message
                        val alarm = getAlarmByIdUseCase(alarmId)
                        if (alarm != null) {
                            val message = formatAlarmScheduleMessage(alarm)
                            _uiEvents.value = UiEvent.Success(message)
                        }
                    }
                }
                .onFailure {
                    _uiEvents.value = UiEvent.Error("Failed to ${if (enabled) "enable" else "disable"} alarm")
                }
        }
    }

    fun deleteAlarm(alarmId: Long) {
        viewModelScope.launch {
            deleteAlarmUseCase(alarmId)
                .onFailure { exception ->
                    _uiEvents.value = UiEvent.Error("Failed to delete alarm: ${exception.message}")
                }
        }
    }

    fun updateAlarmTime(alarmId: Long, hour: Int, minute: Int) {
        viewModelScope.launch {
            val existing = getAlarmByIdUseCase(alarmId) ?: return@launch
            val input = AlarmInput(
                hour = hour,
                minute = minute,
                days = existing.days,
                enabled = true,
                label = existing.label,
                ringtoneUri = existing.ringtoneUri,
                flashPatternId = existing.flashPatternId,
                vibrationPatternId = existing.vibrationPatternId,
                vibrationIntensity = existing.vibrationIntensity,
                snoozeDurationMinutes = existing.snoozeDurationMinutes,
                alarmDurationMinutes = existing.alarmDurationMinutes,
                scheduledDate = existing.scheduledDate
            )
            updateAlarmUseCase(alarmId, input)
                .onSuccess { alarm ->
                    val message = formatAlarmScheduleMessage(alarm)
                    _uiEvents.value = UiEvent.Success(message)
                }
                .onFailure {
                    _uiEvents.value = UiEvent.Error("Failed to update alarm time")
                }
        }
    }


    fun toggleSelectMode() {
        val current = _uiState.value
        _uiState.value = current.copy(
            isSelectMode = !current.isSelectMode,
            selectedAlarms = if (!current.isSelectMode) emptySet() else current.selectedAlarms
        )
    }

    fun toggleAlarmSelection(alarmId: Long) {
        val current = _uiState.value
        val newSelected = if (current.selectedAlarms.contains(alarmId)) {
            current.selectedAlarms - alarmId
        } else {
            current.selectedAlarms + alarmId
        }
        _uiState.value = current.copy(selectedAlarms = newSelected)
    }

    fun selectAllAlarms() {
        val current = _uiState.value
        val allAlarmIds = current.alarms.map { it.id }.toSet()
        _uiState.value = current.copy(selectedAlarms = allAlarmIds)
    }

    fun deleteSelectedAlarms() {
        viewModelScope.launch {
            val current = _uiState.value
            current.selectedAlarms.forEach { alarmId ->
                deleteAlarmUseCase(alarmId)
                    .onFailure {
                        _uiEvents.value = UiEvent.Error("Failed to delete some alarms")
                    }
            }
            _uiState.value = current.copy(selectedAlarms = emptySet(), isSelectMode = false, showDeleteDialog = false)
            loadAlarms()
        }
    }

    fun showSheet(show: Boolean) {
        _uiState.value = _uiState.value.copy(showSheet = show)
    }

    fun openCreateAlarmSheet() {
        _uiState.value = _uiState.value.copy(showSheet = true, editingAlarmId = null, autoOpenTimePicker = true)
    }

    fun openEditAlarmSheet(alarmId: Long) {
        _uiState.value = _uiState.value.copy(showSheet = true, editingAlarmId = alarmId, autoOpenTimePicker = false)
    }

    fun showDeleteDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showDeleteDialog = show)
    }

    fun toggleSelectModeAndSelect(alarmId: Long) {
        _uiState.value = _uiState.value.copy(
            isSelectMode = true,
            selectedAlarms = setOf(alarmId)
        )
    }


    fun consumeUiEvent() {
        _uiEvents.value = null
    }

    fun consumeAutoOpenTimePicker() {
        _uiState.value = _uiState.value.copy(autoOpenTimePicker = false)
    }

    private fun formatAlarmScheduleMessage(alarm: com.aaditx23.krazyalarm.domain.models.Alarm): String {
        val now = System.currentTimeMillis()
        val triggerTime = AlarmTimeCalculator.getNextTriggerTime(alarm)
        val diffMillis = triggerTime - now

        // Round up to next minute if there are remaining seconds
        val totalMinutes = kotlin.math.ceil(diffMillis / 60000.0).toLong()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        // Check if it's today or tomorrow
        val triggerCalendar = java.util.Calendar.getInstance().apply {
            timeInMillis = triggerTime
        }
        val todayCalendar = java.util.Calendar.getInstance()

        val isSameDay = triggerCalendar.get(java.util.Calendar.YEAR) == todayCalendar.get(java.util.Calendar.YEAR) &&
                triggerCalendar.get(java.util.Calendar.DAY_OF_YEAR) == todayCalendar.get(java.util.Calendar.DAY_OF_YEAR)

        val tomorrowCalendar = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        val isTomorrow = triggerCalendar.get(java.util.Calendar.YEAR) == tomorrowCalendar.get(java.util.Calendar.YEAR) &&
                triggerCalendar.get(java.util.Calendar.DAY_OF_YEAR) == tomorrowCalendar.get(java.util.Calendar.DAY_OF_YEAR)

        return when {
            isSameDay -> {
                if (hours > 0) {
                    "Alarm scheduled in $hours hour${if (hours != 1L) "s" else ""} and $minutes minute${if (minutes != 1L) "s" else ""}"
                } else {
                    "Alarm scheduled in $minutes minute${if (minutes != 1L) "s" else ""}"
                }
            }
            isTomorrow -> {
                val timeStr = String.format("%02d:%02d", alarm.hour, alarm.minute)
                "Alarm scheduled for tomorrow at $timeStr"
            }
            else -> {
                val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", java.util.Locale.getDefault())
                "Alarm scheduled for ${dateFormat.format(triggerTime)}"
            }
        }
    }
}
