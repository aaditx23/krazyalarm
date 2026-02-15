package com.aaditx23.krazyalarm.domain.usecase

import com.aaditx23.krazyalarm.domain.models.AlarmInput
import com.aaditx23.krazyalarm.domain.repository.AlarmRepository
import com.aaditx23.krazyalarm.domain.repository.AlarmScheduler

class CreateAlarmUseCase(
    private val alarmRepository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke(input: AlarmInput): Result<Long> {
        return try {
            // Validate input
            validateAlarmInput(input)

            // Create alarm in repository
            val result = alarmRepository.createAlarm(input)

            if (result.isSuccess) {
                val alarmId = result.getOrThrow()

                // Get the created alarm to schedule it
                val alarm = alarmRepository.getAlarm(alarmId)
                if (alarm != null && alarm.enabled) {
                    alarmScheduler.scheduleAlarm(alarm)
                }

                result
            } else {
                result
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun validateAlarmInput(input: AlarmInput) {
        require(input.hour in 0..23) { "Hour must be between 0 and 23" }
        require(input.minute in 0..59) { "Minute must be between 0 and 59" }
        require(input.days >= 0) { "Days bitmask must be non-negative" }
        require(input.snoozeDurationMinutes > 0) { "Snooze duration must be positive" }
        if (input.label != null) {
            require(input.label.length <= 50) { "Label must be 50 characters or less" }
        }
    }
}
