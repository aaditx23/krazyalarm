package com.aaditx23.krazyalarm.presentation.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aaditx23.krazyalarm.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            combine(
                settingsRepository.darkMode,
                settingsRepository.snoozeDefaultMinutes,
                settingsRepository.defaultFlashPattern,
                settingsRepository.defaultVibrationPattern,
                settingsRepository.defaultVibrationIntensity,
                settingsRepository.defaultVolume,
                settingsRepository.alarmDurationMinutes,
                settingsRepository.buttonMotionSpeed
            ) { flows: Array<Any?> ->
                SettingsUiState(
                    darkModeValue = flows[0] as String,
                    snoozeDuration = flows[1] as Int,
                    defaultFlashPattern = flows[2] as String,
                    defaultVibrationPattern = flows[3] as String,
                    defaultVibrationIntensity = flows[4] as String,
                    defaultVolume = flows[5] as Int,
                    alarmDurationMinutes = flows[6] as Int,
                    buttonMotionSpeed = flows[7] as Int
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDarkMode(if (enabled) "dark" else "light")
        }
    }

    fun updateSnoozeDuration(minutes: Int) {
        viewModelScope.launch {
            if (minutes in 1..60) {
                settingsRepository.setSnoozeDefaultMinutes(minutes)
            }
        }
    }

    fun updateFlashPattern(patternId: String) {
        viewModelScope.launch {
            settingsRepository.setDefaultFlashPattern(patternId)
        }
    }

    fun updateVibrationPattern(patternId: String) {
        viewModelScope.launch {
            settingsRepository.setDefaultVibrationPattern(patternId)
        }
    }

    fun updateVibrationIntensity(intensity: String) {
        viewModelScope.launch {
            settingsRepository.setDefaultVibrationIntensity(intensity)
        }
    }

    fun updateVolume(volume: Int) {
        viewModelScope.launch {
            settingsRepository.setDefaultVolume(volume)
        }
    }

    fun updateAlarmDuration(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.setAlarmDurationMinutes(minutes)
        }
    }

    fun updateButtonMotionSpeed(speed: Int) {
        viewModelScope.launch {
            settingsRepository.setButtonMotionSpeed(speed)
        }
    }
}

data class SettingsUiState(
    val darkModeValue: String = "system",
    val snoozeDuration: Int = 10,
    val defaultFlashPattern: String = "NONE",
    val defaultVibrationPattern: String = "CONTINUOUS",
    val defaultVibrationIntensity: String = "MEDIUM",
    val defaultVolume: Int = 100,
    val alarmDurationMinutes: Int = 1,
    val buttonMotionSpeed: Int = 4
)

