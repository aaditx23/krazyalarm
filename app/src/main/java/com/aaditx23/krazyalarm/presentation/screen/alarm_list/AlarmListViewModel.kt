package com.aaditx23.krazyalarm.presentation.screen.alarm_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aaditx23.krazyalarm.domain.models.Alarm
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

sealed class UiState {
    object Loading : UiState()
    data class Success(val alarms: List<Alarm>) : UiState()
    data class Error(val message: String) : UiState()
}

class AlarmListViewModel(
    private val getAlarmsUseCase: GetAlarmsUseCase,
    private val toggleAlarmUseCase: ToggleAlarmUseCase,
    private val deleteAlarmUseCase: DeleteAlarmUseCase,
    private val getAlarmByIdUseCase: GetAlarmByIdUseCase,
    private val createAlarmUseCase: CreateAlarmUseCase,
    private val updateAlarmUseCase: UpdateAlarmUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _editState = MutableStateFlow(AlarmEditState())
    val editState: StateFlow<AlarmEditState> = _editState.asStateFlow()

    private val _editEvents = MutableStateFlow<AlarmEditEvent?>(null)
    val editEvents: StateFlow<AlarmEditEvent?> = _editEvents.asStateFlow()

    private var editingAlarmId: Long? = null

    init {
        loadAlarms()
    }

    fun loadAlarms() {
        getAlarmsUseCase()
            .onEach { alarms ->
                _uiState.value = UiState.Success(alarms)
            }
            .catch { exception ->
                _uiState.value = UiState.Error("Failed to load alarms: ${exception.message}")
            }
            .launchIn(viewModelScope)
    }

    fun toggleAlarm(alarmId: Long, enabled: Boolean) {
        viewModelScope.launch {
            toggleAlarmUseCase(alarmId, enabled)
                .onFailure { exception ->
                    // TODO: Show error message to user
                    // For now, just log the error
                    println("Failed to toggle alarm: ${exception.message}")
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
        _editState.value = AlarmEditState()
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
                        snoozeDurationMinutes = alarm.snoozeDurationMinutes
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
        _editState.value = _editState.value.copy(days = days)
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

    fun saveAlarm() {
        viewModelScope.launch {
            val state = _editState.value
            val alarmInput = AlarmInput(
                hour = state.hour,
                minute = state.minute,
                days = state.days,
                enabled = state.enabled,
                label = state.label,
                flashPatternId = state.flashPattern.id,
                vibrationPatternId = state.vibrationPattern.id,
                vibrationIntensity = state.vibrationIntensity,
                snoozeDurationMinutes = state.snoozeDurationMinutes
            )

            try {
                if (editingAlarmId != null) {
                    updateAlarmUseCase(editingAlarmId!!, alarmInput)
                } else {
                    createAlarmUseCase(alarmInput)
                }
                _editEvents.value = AlarmEditEvent.SaveSuccess
                loadAlarms() // Refresh list
            } catch (e: Exception) {
                _editEvents.value = AlarmEditEvent.SaveError("Failed to save alarm: ${e.message}")
            }
        }
    }

    fun consumeEditEvent() {
        _editEvents.value = null
    }
}
