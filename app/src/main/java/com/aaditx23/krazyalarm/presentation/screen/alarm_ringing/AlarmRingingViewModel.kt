package com.aaditx23.krazyalarm.presentation.screen.alarm_ringing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aaditx23.krazyalarm.domain.models.Alarm
import com.aaditx23.krazyalarm.domain.repository.AlarmRepository
import com.aaditx23.krazyalarm.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AlarmRingingViewModel(
    private val alarmId: Long,
    private val alarmRepository: AlarmRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AlarmRingingUiState>(AlarmRingingUiState.Loading)
    val uiState: StateFlow<AlarmRingingUiState> = _uiState.asStateFlow()

    init {
        loadAlarm()
    }

    private fun loadAlarm() {
        viewModelScope.launch {
            try {
                // Get button motion speed from settings
                var buttonMotionSpeed = 4
                settingsRepository.buttonMotionSpeed.collect { speed ->
                    buttonMotionSpeed = speed

                    // Handle test alarm scenario
                    if (alarmId == -1L) {
                        val currentCalendar = Calendar.getInstance()
                        val testAlarm = Alarm(
                            id = -1L,
                            hour = currentCalendar.get(Calendar.HOUR_OF_DAY),
                            minute = currentCalendar.get(Calendar.MINUTE),
                            days = 0,
                            enabled = true,
                            label = "Test Alarm",
                            ringtoneUri = null,
                            flashPatternId = "NONE",
                            vibrationPatternId = "CONTINUOUS",
                            vibrationIntensity = com.aaditx23.krazyalarm.domain.models.VibrationIntensity.MEDIUM,
                            snoozeDurationMinutes = 5,
                            alarmDurationMinutes = 1,
                            scheduledDate = null
                        )
                        _uiState.value = AlarmRingingUiState.Ringing(
                            alarm = testAlarm,
                            currentTime = getCurrentTimeString(),
                            buttonMotionSpeed = buttonMotionSpeed
                        )
                        return@collect
                    }

                    val alarm = alarmRepository.getAlarm(alarmId)
                    if (alarm != null) {
                        _uiState.value = AlarmRingingUiState.Ringing(
                            alarm = alarm,
                            currentTime = getCurrentTimeString(),
                            buttonMotionSpeed = buttonMotionSpeed
                        )
                    } else {
                        _uiState.value = AlarmRingingUiState.Error("Alarm not found")
                    }
                }
            } catch (e: Exception) {
                _uiState.value = AlarmRingingUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun getCurrentTimeString(): String {
        val calendar = Calendar.getInstance()
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        return timeFormat.format(calendar.time)
    }

    fun getCurrentDateString(): String {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }
}

sealed class AlarmRingingUiState {
    object Loading : AlarmRingingUiState()
    data class Ringing(
        val alarm: Alarm,
        val currentTime: String,
        val buttonMotionSpeed: Int,
        val alarmStartTime: String = getCurrentStartTimeString()
    ) : AlarmRingingUiState()
    data class Error(val message: String) : AlarmRingingUiState()
}

private fun getCurrentStartTimeString(): String {
    val calendar = Calendar.getInstance()
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    return timeFormat.format(calendar.time)
}

