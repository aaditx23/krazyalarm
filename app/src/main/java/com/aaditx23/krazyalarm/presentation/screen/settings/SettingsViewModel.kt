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
                settingsRepository.defaultVibrationIntensity
            ) { darkMode, snoozeDuration, flashPattern, vibrationPattern, vibrationIntensity ->
                SettingsUiState(
                    isDarkMode = darkMode == "dark",
                    snoozeDuration = snoozeDuration,
                    defaultFlashPattern = flashPattern,
                    defaultVibrationPattern = vibrationPattern,
                    defaultVibrationIntensity = vibrationIntensity
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
}

data class SettingsUiState(
    val isDarkMode: Boolean = false,
    val snoozeDuration: Int = 10,
    val defaultFlashPattern: String = "NONE",
    val defaultVibrationPattern: String = "CONTINUOUS",
    val defaultVibrationIntensity: String = "MEDIUM"
)

