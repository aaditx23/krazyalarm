package com.aaditx23.krazyalarm.presentation.screen.DetailsModal

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aaditx23.krazyalarm.domain.models.Alarm
import com.aaditx23.krazyalarm.domain.models.AlarmInput
import com.aaditx23.krazyalarm.domain.models.FlashPattern
import com.aaditx23.krazyalarm.domain.models.VibrationIntensity
import com.aaditx23.krazyalarm.domain.models.VibrationPattern
import com.aaditx23.krazyalarm.domain.repository.SettingsRepository
import com.aaditx23.krazyalarm.domain.usecase.CreateAlarmUseCase
import com.aaditx23.krazyalarm.domain.usecase.DeleteAlarmUseCase
import com.aaditx23.krazyalarm.domain.usecase.GetAlarmByIdUseCase
import com.aaditx23.krazyalarm.domain.usecase.UpdateAlarmUseCase
import com.aaditx23.krazyalarm.domain.util.AlarmScheduleFormatter
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
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _editState = MutableStateFlow(DetailsModalState())
    val editState: StateFlow<DetailsModalState> = _editState.asStateFlow()

    private val _editEvents = MutableStateFlow<AlarmEditEvent?>(null)
    val editEvents: StateFlow<AlarmEditEvent?> = _editEvents.asStateFlow()

    private var editingAlarmId: Long? = null

    fun startCreateAlarm() {
        editingAlarmId = null
        android.util.Log.d("DetailsModalViewModel", "startCreateAlarm called")

        // Set hour/minute synchronously so the time picker shows the correct time
        // immediately if it auto-opens before the coroutine finishes.
        val currentTime = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.MINUTE, 1)
        }
        _editState.value = DetailsModalState(
            hour = currentTime.get(java.util.Calendar.HOUR_OF_DAY),
            minute = currentTime.get(java.util.Calendar.MINUTE),
            isLoadingRingtone = true
        )

        viewModelScope.launch {
            // Load default patterns from settings (suspend calls)
            val defaultFlashPatternId = settingsRepository.defaultFlashPattern.first()
            val defaultVibrationPatternId = settingsRepository.defaultVibrationPattern.first()
            val defaultSnoozeDuration = settingsRepository.snoozeDefaultMinutes.first()
            val defaultAlarmDuration = settingsRepository.alarmDurationMinutes.first()

            _editState.value = _editState.value.copy(
                flashPattern = FlashPattern.fromId(defaultFlashPatternId),
                vibrationPattern = VibrationPattern.fromId(defaultVibrationPatternId),
                snoozeDurationMinutes = defaultSnoozeDuration,
                alarmDurationMinutes = defaultAlarmDuration,
                ringtoneName = "",
                isLoadingRingtone = true
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
                    _editState.value = DetailsModalState(
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
        _editState.value = _editState.value.copy(hour = hour)
    }

    fun updateMinute(minute: Int) {
        _editState.value = _editState.value.copy(minute = minute)
    }

    fun updateDays(days: Int) {
        android.util.Log.d("DetailsModalViewModel", "updateDays: $days, scheduledDate=${if (days != 0) null else _editState.value.scheduledDate}")
        _editState.value = _editState.value.copy(
            days = days,
            // Clear scheduled date when selecting recurring days
            scheduledDate = if (days != 0) null else _editState.value.scheduledDate
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
        android.util.Log.d("DetailsModalViewModel", "updateScheduledDate: $dateMillis")
        _editState.value = _editState.value.copy(scheduledDate = dateMillis)
    }

    fun saveAlarm() {
        viewModelScope.launch {
            android.util.Log.d("DetailsModalViewModel", "=== SAVE ALARM STARTED ===")
            android.util.Log.d("DetailsModalViewModel", "editingAlarmId = $editingAlarmId")
            android.util.Log.d("DetailsModalViewModel", "isEditMode = ${_editState.value.isEditMode}")

            // Set saving state
            _editState.value = _editState.value.copy(isSaving = true)

            val state = _editState.value

            android.util.Log.d("DetailsModalViewModel", "saveAlarm: hour=${state.hour}, minute=${state.minute}, days=${state.days}, scheduledDate=${state.scheduledDate}, editingAlarmId=$editingAlarmId")


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
                    android.util.Log.d("DetailsModalViewModel", "Calling UPDATE for alarm ID: $editingAlarmId")
                    updateAlarmUseCase(editingAlarmId!!, alarmInput)
                } else {
                    android.util.Log.d("DetailsModalViewModel", "Calling CREATE for new alarm")
                    createAlarmUseCase(alarmInput)
                }

                if (result.isSuccess) {
                    val alarm = result.getOrThrow()
                    android.util.Log.d("DetailsModalViewModel", "Save successful - alarm ID: ${alarm.id}, was editing: $editingAlarmId")
                    _editState.value = _editState.value.copy(isSaving = false)
                    val message = formatAlarmScheduleMessage(alarm)
                    _editEvents.value = AlarmEditEvent.SaveSuccessWithMessage(message)
                } else {
                    android.util.Log.e("DetailsModalViewModel", "Save failed: ${result.exceptionOrNull()?.message}")
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

    private fun formatAlarmScheduleMessage(alarm: Alarm): String =
        AlarmScheduleFormatter.snackbarMessage(alarm)
}
