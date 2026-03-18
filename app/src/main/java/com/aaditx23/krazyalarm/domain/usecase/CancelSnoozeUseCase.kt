package com.aaditx23.krazyalarm.domain.usecase

import com.aaditx23.krazyalarm.domain.repository.AlarmRepository
import com.aaditx23.krazyalarm.domain.repository.AlarmScheduler

class CancelSnoozeUseCase(
    private val alarmRepository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke(id: Long): Result<Unit> {
        return try {
            val alarm = alarmRepository.getAlarm(id)
                ?: return Result.failure(IllegalArgumentException("Alarm with id $id not found"))

            alarmRepository.updateSnoozedUntil(id, null)

            if (alarm.enabled) {
                alarmScheduler.scheduleAlarm(alarm.copy(snoozedUntilMillis = null))
            } else {
                alarmScheduler.cancelAlarm(id)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

