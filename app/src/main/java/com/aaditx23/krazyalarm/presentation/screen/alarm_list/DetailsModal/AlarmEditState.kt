package com.aaditx23.krazyalarm.presentation.screen.alarm_list.DetailsModal

import com.aaditx23.krazyalarm.domain.models.FlashPattern
import com.aaditx23.krazyalarm.domain.models.VibrationIntensity
import com.aaditx23.krazyalarm.domain.models.VibrationPattern

data class AlarmEditState(
    val isLoading: Boolean = false,
    val isEditMode: Boolean = false,
    val hour: Int = 8,
    val minute: Int = 0,
    val days: Int = 0, // bitmask for days
    val enabled: Boolean = true,
    val label: String = "",
    val flashPattern: FlashPattern = FlashPattern.None,
    val vibrationPattern: VibrationPattern = VibrationPattern.Continuous,
    val vibrationIntensity: VibrationIntensity = VibrationIntensity.MEDIUM,
    val snoozeDurationMinutes: Int = 10
)

sealed class AlarmEditEvent {
    object SaveSuccess : AlarmEditEvent()
    data class SaveError(val message: String) : AlarmEditEvent()
}
