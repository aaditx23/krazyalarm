package com.aaditx23.krazyalarm.presentation.screen.alarm_list
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aaditx23.krazyalarm.domain.models.Alarm
import com.aaditx23.krazyalarm.domain.models.AlarmInput
import com.aaditx23.krazyalarm.domain.usecase.CreateAlarmUseCase
import com.aaditx23.krazyalarm.domain.usecase.DeleteAlarmUseCase
import com.aaditx23.krazyalarm.domain.usecase.GetAlarmByIdUseCase
import com.aaditx23.krazyalarm.domain.usecase.GetAlarmsUseCase
import com.aaditx23.krazyalarm.domain.usecase.ToggleAlarmUseCase
import com.aaditx23.krazyalarm.domain.usecase.UpdateAlarmUseCase
import com.aaditx23.krazyalarm.domain.util.AlarmScheduleFormatter
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
    private val createAlarmUseCase: CreateAlarmUseCase,
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

    /**
     * Swipe-to-delete: immediately remove from DB but emit an [UiEvent.AlarmDeleted]
     * so the UI can offer an Undo snackbar action.
     */
    fun swipeDeleteAlarm(alarm: Alarm) {
        viewModelScope.launch {
            deleteAlarmUseCase(alarm.id)
                .onSuccess {
                    _uiEvents.value = UiEvent.AlarmDeleted(alarm)
                }
                .onFailure { exception ->
                    _uiEvents.value = UiEvent.Error("Failed to delete alarm: ${exception.message}")
                }
        }
    }

    /** Re-create the alarm that was just swipe-deleted (undo). */
    fun undoDelete(alarm: Alarm) {
        viewModelScope.launch {
            val input = AlarmInput(
                hour = alarm.hour,
                minute = alarm.minute,
                days = alarm.days,
                enabled = alarm.enabled,
                label = alarm.label,
                ringtoneUri = alarm.ringtoneUri,
                flashPatternId = alarm.flashPatternId,
                vibrationPatternId = alarm.vibrationPatternId,
                vibrationIntensity = alarm.vibrationIntensity,
                snoozeDurationMinutes = alarm.snoozeDurationMinutes,
                alarmDurationMinutes = alarm.alarmDurationMinutes,
                scheduledDate = alarm.scheduledDate
            )
            createAlarmUseCase(input)
                .onFailure {
                    _uiEvents.value = UiEvent.Error("Failed to restore alarm")
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
            val count = current.selectedAlarms.size
            var anyFailed = false
            current.selectedAlarms.forEach { alarmId ->
                deleteAlarmUseCase(alarmId)
                    .onFailure { anyFailed = true }
            }
            _uiState.value = current.copy(selectedAlarms = emptySet(), isSelectMode = false, showDeleteDialog = false)
            loadAlarms()
            if (anyFailed) {
                _uiEvents.value = UiEvent.Error("Failed to delete some alarms")
            }
        }
    }

    fun handleAlarmSaved(message: String, enabled: Boolean) {
        showSheet(false)
        loadAlarms()
    }

    fun showSheet(show: Boolean) {
        _uiState.value = _uiState.value.copy(showSheet = show)
    }

    fun openCreateAlarmSheet() {
        val currentTime = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.MINUTE, 1)
        }
        _uiState.value = _uiState.value.copy(
            showCreateTimePicker = true,
            createInitialHour = currentTime.get(java.util.Calendar.HOUR_OF_DAY),
            createInitialMinute = currentTime.get(java.util.Calendar.MINUTE)
        )
    }

    fun confirmCreateTime(hour: Int, minute: Int) {
        _uiState.value = _uiState.value.copy(
            showCreateTimePicker = false,
            showSheet = true,
            editingAlarmId = null,
            createInitialHour = hour,
            createInitialMinute = minute
        )
    }

    fun dismissCreateTimePicker() {
        _uiState.value = _uiState.value.copy(showCreateTimePicker = false)
    }

    fun openEditAlarmSheet(alarmId: Long) {
        _uiState.value = _uiState.value.copy(showSheet = true, editingAlarmId = alarmId)
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


    private fun formatAlarmScheduleMessage(alarm: com.aaditx23.krazyalarm.domain.models.Alarm): String =
        AlarmScheduleFormatter.snackbarMessage(alarm)
}
