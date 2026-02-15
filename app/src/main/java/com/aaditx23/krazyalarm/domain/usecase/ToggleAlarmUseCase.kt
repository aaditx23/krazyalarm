package com.aaditx23.krazyalarm.domain.usecase

import com.aaditx23.krazyalarm.domain.repository.AlarmRepository
import com.aaditx23.krazyalarm.domain.repository.AlarmScheduler

class ToggleAlarmUseCase(
    private val alarmRepository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke(id: Long, enabled: Boolean): Result<Unit> {
        return try {
            // Update the alarm's enabled state
            val result = alarmRepository.toggleAlarm(id, enabled)

            if (result.isSuccess) {
                if (enabled) {
                    // Schedule the alarm if enabled
                    val alarm = alarmRepository.getAlarm(id)
                    if (alarm != null) {
                        alarmScheduler.scheduleAlarm(alarm)
                    }
                } else {
                    // Cancel the alarm if disabled
                    alarmScheduler.cancelAlarm(id)
                }
            }

            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
