package com.aaditx23.krazyalarm.data.repository

import com.aaditx23.krazyalarm.data.local.preferences.SettingsDataStore
import com.aaditx23.krazyalarm.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class SettingsRepositoryImpl(
    private val settingsDataStore: SettingsDataStore
) : SettingsRepository {

    override val snoozeDefaultMinutes: Flow<Int> = settingsDataStore.snoozeDefaultMinutes

    override suspend fun setSnoozeDefaultMinutes(minutes: Int) {
        settingsDataStore.setSnoozeDefaultMinutes(minutes)
    }

    override val darkMode: Flow<String> = settingsDataStore.darkMode

    override suspend fun setDarkMode(mode: String) {
        settingsDataStore.setDarkMode(mode)
    }

    override val defaultFlashPattern: Flow<String> = settingsDataStore.defaultFlashPattern

    override suspend fun setDefaultFlashPattern(patternId: String) {
        settingsDataStore.setDefaultFlashPattern(patternId)
    }

    override val defaultVibrationPattern: Flow<String> = settingsDataStore.defaultVibrationPattern

    override suspend fun setDefaultVibrationPattern(patternId: String) {
        settingsDataStore.setDefaultVibrationPattern(patternId)
    }

    override val defaultVibrationIntensity: Flow<String> = settingsDataStore.defaultVibrationIntensity

    override suspend fun setDefaultVibrationIntensity(intensity: String) {
        settingsDataStore.setDefaultVibrationIntensity(intensity)
    }

    override val defaultVolume: Flow<Int> = settingsDataStore.defaultVolume

    override suspend fun setDefaultVolume(volume: Int) {
        settingsDataStore.setDefaultVolume(volume)
    }
}
