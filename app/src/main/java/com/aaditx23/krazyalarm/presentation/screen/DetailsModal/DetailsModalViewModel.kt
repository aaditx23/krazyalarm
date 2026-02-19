package com.aaditx23.krazyalarm.presentation.screen.DetailsModal

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aaditx23.krazyalarm.domain.models.AlarmInput
import com.aaditx23.krazyalarm.domain.models.FlashPattern
import com.aaditx23.krazyalarm.domain.models.VibrationIntensity
import com.aaditx23.krazyalarm.domain.models.VibrationPattern
import com.aaditx23.krazyalarm.domain.repository.SettingsRepository
import com.aaditx23.krazyalarm.domain.usecase.CheckDuplicateAlarmUseCase
import com.aaditx23.krazyalarm.domain.usecase.CreateAlarmUseCase
import com.aaditx23.krazyalarm.domain.usecase.DeleteAlarmUseCase
import com.aaditx23.krazyalarm.domain.usecase.GetAlarmByIdUseCase
import com.aaditx23.krazyalarm.domain.usecase.UpdateAlarmUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DetailsModalViewModel(
    private val getAlarmByIdUseCase: GetAlarmByIdUseCase,
    private val createAlarmUseCase: CreateAlarmUseCase,
    private val updateAlarmUseCase: UpdateAlarmUseCase,
    private val deleteAlarmUseCase: DeleteAlarmUseCase,
    private val checkDuplicateAlarmUseCase: CheckDuplicateAlarmUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _editState = MutableStateFlow(AlarmEditState())
    val editState: StateFlow<AlarmEditState> = _editState.asStateFlow()

    private val _editEvents = MutableStateFlow<AlarmEditEvent?>(null)
    val editEvents: StateFlow<AlarmEditEvent?> = _editEvents.asStateFlow()

    private var editingAlarmId: Long? = null

    fun startCreateAlarm() {
        editingAlarmId = null
        android.util.Log.d("DetailsModalViewModel", "startCreateAlarm called")
        viewModelScope.launch {
            val currentTime = java.util.Calendar.getInstance()
            // Add 1 minute to current time
            currentTime.add(java.util.Calendar.MINUTE, 1)

            // Load default patterns from settings
            val defaultFlashPatternId = settingsRepository.defaultFlashPattern.first()
            val defaultVibrationPatternId = settingsRepository.defaultVibrationPattern.first()
            val defaultSnoozeDuration = settingsRepository.snoozeDefaultMinutes.first()
            val defaultAlarmDuration = settingsRepository.alarmDurationMinutes.first()

            _editState.value = AlarmEditState(
                hour = currentTime.get(java.util.Calendar.HOUR_OF_DAY),
                minute = currentTime.get(java.util.Calendar.MINUTE),
                flashPattern = FlashPattern.fromId(defaultFlashPatternId),
                vibrationPattern = VibrationPattern.fromId(defaultVibrationPatternId),
                snoozeDurationMinutes = defaultSnoozeDuration,
                alarmDurationMinutes = defaultAlarmDuration,
                ringtoneName = "",
                isLoadingRingtone = true // Set to true so UI shows loading
            )
            android.util.Log.d("DetailsModalViewModel", "State initialized - loading: ${_editState.value.isLoadingRingtone}")
        }
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
                        alarmDurationMinutes = alarm.alarmDurationMinutes,
                        ringtoneUri = alarm.ringtoneUri,
                        scheduledDate = alarm.scheduledDate,
                        isLoadingRingtone = true // Set to true so UI shows loading
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
        android.util.Log.d("DetailsModalViewModel", "updateHour: $hour, clearing duplicateError")
        _editState.value = _editState.value.copy(hour = hour, duplicateError = null)
    }

    fun updateMinute(minute: Int) {
        android.util.Log.d("DetailsModalViewModel", "updateMinute: $minute, clearing duplicateError")
        _editState.value = _editState.value.copy(minute = minute, duplicateError = null)
    }

    fun updateDays(days: Int) {
        android.util.Log.d("DetailsModalViewModel", "updateDays: $days, clearing duplicateError and scheduledDate=${if (days != 0) null else _editState.value.scheduledDate}")
        _editState.value = _editState.value.copy(
            days = days,
            // Clear scheduled date when selecting recurring days
            scheduledDate = if (days != 0) null else _editState.value.scheduledDate,
            duplicateError = null
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
        android.util.Log.d("DetailsModalViewModel", "updateRingtoneName: '$ringtoneName'")
        _editState.value = _editState.value.copy(ringtoneName = ringtoneName)
    }

    fun fetchRingtoneName(context: Context, ringtoneUri: String? = null) {
        android.util.Log.d("DetailsModalViewModel", "=== fetchRingtoneName called ===")
        android.util.Log.d("DetailsModalViewModel", "ringtoneUri parameter: $ringtoneUri")
        android.util.Log.d("DetailsModalViewModel", "Current state ringtoneUri: ${_editState.value.ringtoneUri}")

        viewModelScope.launch {
            _editState.value = _editState.value.copy(isLoadingRingtone = true)
            android.util.Log.d("DetailsModalViewModel", "Set isLoadingRingtone = true")

            try {
                val uriToUse = ringtoneUri ?: _editState.value.ringtoneUri
                val uri = if (uriToUse != null) {
                    android.util.Log.d("DetailsModalViewModel", "Using custom ringtone URI: $uriToUse")
                    android.net.Uri.parse(uriToUse)
                } else {
                    android.util.Log.d("DetailsModalViewModel", "No custom ringtone, getting default alarm URI...")
                    val defaultUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                    android.util.Log.d("DetailsModalViewModel", "Default alarm URI: $defaultUri")
                    defaultUri
                }

                android.util.Log.d("DetailsModalViewModel", "Getting ringtone object for URI: $uri")
                val ringtone = android.media.RingtoneManager.getRingtone(context, uri)
                android.util.Log.d("DetailsModalViewModel", "Ringtone object obtained: ${ringtone != null}")

                val title = ringtone?.getTitle(context) ?: ""
                android.util.Log.d("DetailsModalViewModel", "Ringtone title: '$title'")

                _editState.value = _editState.value.copy(
                    ringtoneName = title,
                    isLoadingRingtone = false
                )
                android.util.Log.d("DetailsModalViewModel", "=== Ringtone fetch complete, updated state ===")
            } catch (e: Exception) {
                android.util.Log.e("DetailsModalViewModel", "Exception during ringtone fetch", e)
                android.util.Log.e("DetailsModalViewModel", "Exception message: ${e.message}")
                _editState.value = _editState.value.copy(
                    ringtoneName = "",
                    isLoadingRingtone = false
                )
            }
        }
    }

    fun updateScheduledDate(dateMillis: Long?) {
        android.util.Log.d("DetailsModalViewModel", "updateScheduledDate: $dateMillis, clearing duplicateError")
        _editState.value = _editState.value.copy(scheduledDate = dateMillis, duplicateError = null)
    }

    fun saveAlarm() {
        viewModelScope.launch {
            // Set saving state
            _editState.value = _editState.value.copy(isSaving = true, duplicateError = null)

            val state = _editState.value

            android.util.Log.d("DetailsModalViewModel", "saveAlarm: hour=${state.hour}, minute=${state.minute}, days=${state.days}, scheduledDate=${state.scheduledDate}, editingAlarmId=$editingAlarmId")

            // Check for duplicate alarm using use case
            val isDuplicate = checkDuplicateAlarmUseCase(
                hour = state.hour,
                minute = state.minute,
                days = state.days,
                scheduledDate = state.scheduledDate,
                excludeId = editingAlarmId
            )

            android.util.Log.d("DetailsModalViewModel", "isDuplicate=$isDuplicate")

            if (isDuplicate) {
                _editState.value = _editState.value.copy(
                    isSaving = false,
                    duplicateError = "An alarm with the same time and date already exists"
                )
                return@launch
            }

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
                alarmDurationMinutes = state.alarmDurationMinutes,
                scheduledDate = state.scheduledDate
            )

            try {
                val result = if (editingAlarmId != null) {
                    updateAlarmUseCase(editingAlarmId!!, alarmInput)
                } else {
                    createAlarmUseCase(alarmInput)
                }

                if (result.isSuccess) {
                    _editState.value = _editState.value.copy(isSaving = false)
                    val alarm = result.getOrThrow()
                    val message = formatAlarmScheduleMessage(alarm)
                    _editEvents.value = AlarmEditEvent.SaveSuccessWithMessage(message)
                } else {
                    _editState.value = _editState.value.copy(isSaving = false)
                    _editEvents.value = AlarmEditEvent.SaveError(result.exceptionOrNull()?.message ?: "Unknown error")
                }
            } catch (e: Exception) {
                _editState.value = _editState.value.copy(isSaving = false)
                _editEvents.value = AlarmEditEvent.SaveError("Failed to save alarm: ${e.message}")
            }
        }
    }

    fun deleteAlarm() {
        viewModelScope.launch {
            editingAlarmId?.let { id ->
                deleteAlarmUseCase(id)
                    .onSuccess {
                        _editEvents.value = AlarmEditEvent.SaveSuccess
                    }
                    .onFailure { exception ->
                        _editEvents.value = AlarmEditEvent.SaveError("Failed to delete alarm: ${exception.message}")
                    }
            }
        }
    }

    fun consumeEditEvent() {
        _editEvents.value = null
    }

    private fun formatAlarmScheduleMessage(alarm: com.aaditx23.krazyalarm.domain.models.Alarm): String {
        val now = System.currentTimeMillis()

        // Calculate the actual trigger time using the same logic as AlarmScheduler
        val triggerTime = calculateTriggerTime(alarm)
        val diffMillis = triggerTime - now

        // Round up to next minute if there are remaining seconds
        val totalMinutes = kotlin.math.ceil(diffMillis / 60000.0).toLong()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        // Check if it's today or tomorrow
        val triggerCalendar = java.util.Calendar.getInstance().apply {
            timeInMillis = triggerTime
        }
        val todayCalendar = java.util.Calendar.getInstance()

        val isSameDay = triggerCalendar.get(java.util.Calendar.YEAR) == todayCalendar.get(java.util.Calendar.YEAR) &&
                triggerCalendar.get(java.util.Calendar.DAY_OF_YEAR) == todayCalendar.get(java.util.Calendar.DAY_OF_YEAR)

        val tomorrowCalendar = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        val isTomorrow = triggerCalendar.get(java.util.Calendar.YEAR) == tomorrowCalendar.get(java.util.Calendar.YEAR) &&
                triggerCalendar.get(java.util.Calendar.DAY_OF_YEAR) == tomorrowCalendar.get(java.util.Calendar.DAY_OF_YEAR)

        return when {
            isSameDay -> {
                if (hours > 0) {
                    "Alarm scheduled in $hours hour${if (hours != 1L) "s" else ""} and $minutes minute${if (minutes != 1L) "s" else ""}"
                } else {
                    "Alarm scheduled in $minutes minute${if (minutes != 1L) "s" else ""}"
                }
            }
            isTomorrow -> {
                val timeStr = String.format("%02d:%02d", alarm.hour, alarm.minute)
                "Alarm scheduled for tomorrow at $timeStr"
            }
            else -> {
                val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", java.util.Locale.getDefault())
                "Alarm scheduled for ${dateFormat.format(triggerTime)}"
            }
        }
    }

    private fun calculateTriggerTime(alarm: com.aaditx23.krazyalarm.domain.models.Alarm): Long {
        val calendar = java.util.Calendar.getInstance()

        if (alarm.days == 0) {
            // One time alarm
            if (alarm.scheduledDate != null) {
                // Use the scheduled date if it's set
                calendar.timeInMillis = alarm.scheduledDate
                calendar.set(java.util.Calendar.HOUR_OF_DAY, alarm.hour)
                calendar.set(java.util.Calendar.MINUTE, alarm.minute)
                calendar.set(java.util.Calendar.SECOND, 0)
                calendar.set(java.util.Calendar.MILLISECOND, 0)
                return calendar.timeInMillis
            } else {
                // No scheduled date, use today's time or tomorrow if already passed
                calendar.set(java.util.Calendar.HOUR_OF_DAY, alarm.hour)
                calendar.set(java.util.Calendar.MINUTE, alarm.minute)
                calendar.set(java.util.Calendar.SECOND, 0)
                calendar.set(java.util.Calendar.MILLISECOND, 0)
                if (calendar.timeInMillis <= System.currentTimeMillis()) {
                    calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
                }
                return calendar.timeInMillis
            }
        } else {
            // Repeating alarm on specific days
            calendar.set(java.util.Calendar.HOUR_OF_DAY, alarm.hour)
            calendar.set(java.util.Calendar.MINUTE, alarm.minute)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)

            val currentDayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
            for (i in 0..6) {
                val checkDay = ((currentDayOfWeek - 1 + i) % 7) + 1
                if ((alarm.days and (1 shl (checkDay - 1))) != 0) {
                    calendar.add(java.util.Calendar.DAY_OF_YEAR, i)
                    return calendar.timeInMillis
                }
            }
            calendar.add(java.util.Calendar.DAY_OF_YEAR, 7)
            return calendar.timeInMillis
        }
    }

    fun reset() {
        android.util.Log.d("DetailsModalViewModel", "=== RESET called - clearing all state ===")
        editingAlarmId = null
        _editState.value = AlarmEditState()
        _editEvents.value = null
    }
}
