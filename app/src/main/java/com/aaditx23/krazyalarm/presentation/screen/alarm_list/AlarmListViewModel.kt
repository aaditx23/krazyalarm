package com.aaditx23.krazyalarm.presentation.screen.alarm_list

import UiEvent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aaditx23.krazyalarm.domain.models.AlarmInput
import com.aaditx23.krazyalarm.domain.models.FlashPattern
import com.aaditx23.krazyalarm.domain.models.VibrationIntensity
import com.aaditx23.krazyalarm.domain.models.VibrationPattern
import com.aaditx23.krazyalarm.domain.usecase.CreateAlarmUseCase
import com.aaditx23.krazyalarm.domain.usecase.DeleteAlarmUseCase
import com.aaditx23.krazyalarm.domain.usecase.GetAlarmByIdUseCase
import com.aaditx23.krazyalarm.domain.usecase.GetAlarmsUseCase
import com.aaditx23.krazyalarm.domain.usecase.ToggleAlarmUseCase
import com.aaditx23.krazyalarm.domain.usecase.UpdateAlarmUseCase
import com.aaditx23.krazyalarm.presentation.screen.alarm_list.DetailsModal.AlarmEditEvent
import com.aaditx23.krazyalarm.presentation.screen.alarm_list.DetailsModal.AlarmEditState
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
    private val createAlarmUseCase: CreateAlarmUseCase,
    private val updateAlarmUseCase: UpdateAlarmUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState(isLoading = true))
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _editState = MutableStateFlow(AlarmEditState())
    val editState: StateFlow<AlarmEditState> = _editState.asStateFlow()

    private val _editEvents = MutableStateFlow<AlarmEditEvent?>(null)
    val editEvents: StateFlow<AlarmEditEvent?> = _editEvents.asStateFlow()

    private val _uiEvents = MutableStateFlow<UiEvent?>(null)
    val uiEvents: StateFlow<UiEvent?> = _uiEvents.asStateFlow()

    private var editingAlarmId: Long? = null

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
                    // TODO: Show error message to user
                    println("Failed to delete alarm: ${exception.message}")
                }
        }
    }

    fun startCreateAlarm() {
        editingAlarmId = null
        val currentTime = java.util.Calendar.getInstance()
        // Add 1 minute to current time
        currentTime.add(java.util.Calendar.MINUTE, 1)

        _editState.value = AlarmEditState(
            hour = currentTime.get(java.util.Calendar.HOUR_OF_DAY),
            minute = currentTime.get(java.util.Calendar.MINUTE)
        )
    }

    fun startEditAlarm(alarmId: Long) {
        editingAlarmId = alarmId
        loadAlarm(alarmId)
    }

    private fun loadAlarm(id: Long) {
        viewModelScope.launch {
            _editState.value = _editState.value.copy(isLoading = true)

            try {
                val alarm = getAlarmByIdUseCase(id)
                if (alarm != null) {
                    _editState.value = AlarmEditState(
                        isLoading = false,
                        isEditMode = true,
                        hour = alarm.hour,
                        minute = alarm.minute,
                        days = alarm.days,
                        enabled = alarm.enabled,
                        label = alarm.label ?: "",
                        flashPattern = FlashPattern.fromId(alarm.flashPatternId),
                        vibrationPattern = VibrationPattern.fromId(alarm.vibrationPatternId),
                        vibrationIntensity = alarm.vibrationIntensity,
                        snoozeDurationMinutes = alarm.snoozeDurationMinutes,
                        ringtoneUri = alarm.ringtoneUri,
                        scheduledDate = alarm.scheduledDate
                    )
                } else {
                    _editEvents.value = AlarmEditEvent.SaveError("Alarm not found")
                }
            } catch (e: Exception) {
                _editEvents.value = AlarmEditEvent.SaveError("Failed to load alarm: ${e.message}")
            }
        }
    }

    fun updateHour(hour: Int) {
        _editState.value = _editState.value.copy(hour = hour)
    }

    fun updateMinute(minute: Int) {
        _editState.value = _editState.value.copy(minute = minute)
    }

    fun updateDays(days: Int) {
        _editState.value = _editState.value.copy(
            days = days,
            // Clear scheduled date when selecting recurring days
            scheduledDate = if (days != 0) null else _editState.value.scheduledDate
        )
    }

    fun updateEnabled(enabled: Boolean) {
        _editState.value = _editState.value.copy(enabled = enabled)
    }

    fun updateLabel(label: String) {
        _editState.value = _editState.value.copy(label = label)
    }

    fun updateFlashPattern(flashPattern: FlashPattern) {
        _editState.value = _editState.value.copy(flashPattern = flashPattern)
    }

    fun updateVibrationPattern(vibrationPattern: VibrationPattern) {
        _editState.value = _editState.value.copy(vibrationPattern = vibrationPattern)
    }

    fun updateVibrationIntensity(vibrationIntensity: VibrationIntensity) {
        _editState.value = _editState.value.copy(vibrationIntensity = vibrationIntensity)
    }

    fun updateSnoozeDuration(snoozeDurationMinutes: Int) {
        _editState.value = _editState.value.copy(snoozeDurationMinutes = snoozeDurationMinutes)
    }

    fun updateRingtoneUri(ringtoneUri: String?) {
        _editState.value = _editState.value.copy(ringtoneUri = ringtoneUri)
    }

    fun updateRingtoneName(ringtoneName: String) {
        _editState.value = _editState.value.copy(ringtoneName = ringtoneName)
    }

    fun updateScheduledDate(dateMillis: Long?) {
        _editState.value = _editState.value.copy(scheduledDate = dateMillis)
    }

    fun saveAlarm() {
        viewModelScope.launch {
            val state = _editState.value
            val alarmInput = AlarmInput(
                hour = state.hour,
                minute = state.minute,
                days = state.days,
                enabled = state.enabled,
                label = state.label,
                ringtoneUri = state.ringtoneUri,
                flashPatternId = state.flashPattern.id,
                vibrationPatternId = state.vibrationPattern.id,
                vibrationIntensity = state.vibrationIntensity,
                snoozeDurationMinutes = state.snoozeDurationMinutes,
                scheduledDate = state.scheduledDate
            )

            try {
                val result = if (editingAlarmId != null) {
                    updateAlarmUseCase(editingAlarmId!!, alarmInput)
                } else {
                    createAlarmUseCase(alarmInput)
                }

                if (result.isSuccess) {
                    val alarm = result.getOrThrow()
                    val nextTime = calculateNextAlarmTime(alarm)
                    val hours = nextTime / 3600000
                    val minutes = (nextTime % 3600000) / 60000
                    _editEvents.value = AlarmEditEvent.SaveSuccessWithTime(hours.toInt(), minutes.toInt())
                    loadAlarms() // Refresh list
                } else {
                    _editEvents.value = AlarmEditEvent.SaveError(result.exceptionOrNull()?.message ?: "Unknown error")
                }
            } catch (e: Exception) {
                _editEvents.value = AlarmEditEvent.SaveError("Failed to save alarm: ${e.message}")
            }
        }
    }

    fun consumeEditEvent() {
        _editEvents.value = null
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

    fun showDeleteDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showDeleteDialog = show)
    }

    fun toggleSelectModeAndSelect(alarmId: Long) {
        _uiState.value = _uiState.value.copy(
            isSelectMode = true,
            selectedAlarms = setOf(alarmId)
        )
    }

    fun deleteCurrentAlarm() {
        editingAlarmId?.let { deleteAlarm(it) }
        _uiState.value = _uiState.value.copy(showSheet = false)
        loadAlarms()
    }

    fun showDatePicker(show: Boolean) {
        _uiState.value = _uiState.value.copy(showDatePicker = show)
    }

    fun consumeUiEvent() {
        _uiEvents.value = null
    }

    private fun calculateNextAlarmTime(alarm: com.aaditx23.krazyalarm.domain.models.Alarm): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, alarm.hour)
        calendar.set(java.util.Calendar.MINUTE, alarm.minute)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)

        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }

        return calendar.timeInMillis - System.currentTimeMillis()
    }
}
