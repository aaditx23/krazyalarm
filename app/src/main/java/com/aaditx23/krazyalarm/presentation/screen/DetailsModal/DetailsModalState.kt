package com.aaditx23.krazyalarm.presentation.screen.DetailsModal

import com.aaditx23.krazyalarm.domain.models.FlashPattern
import com.aaditx23.krazyalarm.domain.models.VibrationIntensity
import com.aaditx23.krazyalarm.domain.models.VibrationPattern

data class DetailsModalState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isLoadingRingtone: Boolean = false,
    val isEditMode: Boolean = false,
    val hour: Int = 0,
    val minute: Int = 0,
    val days: Int = 0, // bitmask for days
    val enabled: Boolean = true,
    val label: String = "",
    val flashPattern: FlashPattern = FlashPattern.None,
    val vibrationPattern: VibrationPattern = VibrationPattern.Continuous,
    val vibrationIntensity: VibrationIntensity = VibrationIntensity.MEDIUM,
    val snoozeDurationMinutes: Int = 10,
    val alarmDurationMinutes: Int = 1,
    val ringtoneUri: String? = null,
    val ringtoneName: String = "",
    val scheduledDate: Long? = null, // Specific date in millis for one-time alarms (when days == 0)
    val snoozedUntilMillis: Long? = null
)

sealed class AlarmEditEvent {
    object SaveSuccess : AlarmEditEvent()
    data class SaveSuccessWithTime(val hours: Int, val minutes: Int) : AlarmEditEvent()
    data class SaveSuccessWithMessage(val message: String, val enabled: Boolean) : AlarmEditEvent()
    data class SaveError(val message: String) : AlarmEditEvent()
}
