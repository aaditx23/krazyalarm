package com.aaditx23.krazyalarm.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val hasSeenPermissionsScreen: Flow<Boolean>
    suspend fun setHasSeenPermissionsScreen(seen: Boolean)

    val snoozeDefaultMinutes: Flow<Int>
    suspend fun setSnoozeDefaultMinutes(minutes: Int)

    val darkMode: Flow<String>
    suspend fun setDarkMode(mode: String)

    val defaultFlashPattern: Flow<String>
    suspend fun setDefaultFlashPattern(patternId: String)

    val defaultVibrationPattern: Flow<String>
    suspend fun setDefaultVibrationPattern(patternId: String)

    val defaultVibrationIntensity: Flow<String>
    suspend fun setDefaultVibrationIntensity(intensity: String)

    val defaultVolume: Flow<Int>
    suspend fun setDefaultVolume(volume: Int)

    val alarmDurationMinutes: Flow<Int>
    suspend fun setAlarmDurationMinutes(minutes: Int)

    val buttonMotionSpeed: Flow<Int>
    suspend fun setButtonMotionSpeed(speed: Int)

    val buttonFlickerIntervalMs: Flow<Int>
    suspend fun setButtonFlickerIntervalMs(intervalMs: Int)
}
