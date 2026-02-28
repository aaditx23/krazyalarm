package com.aaditx23.krazyalarm.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        private val KEY_SNOOZE_DEFAULT_MINUTES = intPreferencesKey("snooze_default_minutes")
        private val KEY_DARK_MODE = stringPreferencesKey("dark_mode")
        private val KEY_DEFAULT_FLASH_PATTERN = stringPreferencesKey("default_flash_pattern")
        private val KEY_DEFAULT_VIBRATION_PATTERN = stringPreferencesKey("default_vibration_pattern")
        private val KEY_DEFAULT_VIBRATION_INTENSITY = stringPreferencesKey("default_vibration_intensity")
        private val KEY_DEFAULT_VOLUME = intPreferencesKey("default_volume")
        private val KEY_ALARM_DURATION_MINUTES = intPreferencesKey("alarm_duration_minutes")
        private val KEY_BUTTON_MOTION_SPEED = intPreferencesKey("button_motion_speed")
        private val KEY_BUTTON_FLICKER_INTERVAL_MS = intPreferencesKey("button_flicker_interval_ms")

        const val DARK_MODE_LIGHT = "LIGHT"
        const val DARK_MODE_DARK = "DARK"
        const val DARK_MODE_SYSTEM = "SYSTEM"
    }

    // Snooze duration
    val snoozeDefaultMinutes: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_SNOOZE_DEFAULT_MINUTES] ?: 10
        }

    suspend fun setSnoozeDefaultMinutes(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SNOOZE_DEFAULT_MINUTES] = minutes
        }
    }

    // Dark mode
    val darkMode: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_DARK_MODE] ?: DARK_MODE_SYSTEM
        }

    suspend fun setDarkMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DARK_MODE] = mode
        }
    }

    // Default flash pattern
    val defaultFlashPattern: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_DEFAULT_FLASH_PATTERN] ?: "NONE"
        }

    suspend fun setDefaultFlashPattern(patternId: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DEFAULT_FLASH_PATTERN] = patternId
        }
    }

    // Default vibration pattern
    val defaultVibrationPattern: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_DEFAULT_VIBRATION_PATTERN] ?: "PULSE"
        }

    suspend fun setDefaultVibrationPattern(patternId: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DEFAULT_VIBRATION_PATTERN] = patternId
        }
    }

    // Default vibration intensity
    val defaultVibrationIntensity: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_DEFAULT_VIBRATION_INTENSITY] ?: "MEDIUM"
        }

    suspend fun setDefaultVibrationIntensity(intensity: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DEFAULT_VIBRATION_INTENSITY] = intensity
        }
    }

    // Default volume (1-150 for overclock support)
    val defaultVolume: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_DEFAULT_VOLUME] ?: 100
        }

    suspend fun setDefaultVolume(volume: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DEFAULT_VOLUME] = volume.coerceIn(1, 150)
        }
    }

    // Alarm duration in minutes (1-5)
    val alarmDurationMinutes: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_ALARM_DURATION_MINUTES] ?: 1
        }

    suspend fun setAlarmDurationMinutes(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ALARM_DURATION_MINUTES] = minutes.coerceIn(1, 5)
        }
    }

    // Button motion speed (0-8, 0 = no motion, 4 = default)
    val buttonMotionSpeed: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_BUTTON_MOTION_SPEED] ?: 4
        }

    suspend fun setButtonMotionSpeed(speed: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_BUTTON_MOTION_SPEED] = speed.coerceIn(0, 8)
        }
    }

    // Button flicker interval in ms (0 = disabled)
    val buttonFlickerIntervalMs: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_BUTTON_FLICKER_INTERVAL_MS] ?: 0
        }

    suspend fun setButtonFlickerIntervalMs(intervalMs: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_BUTTON_FLICKER_INTERVAL_MS] = intervalMs.coerceAtLeast(0)
        }
    }
}
