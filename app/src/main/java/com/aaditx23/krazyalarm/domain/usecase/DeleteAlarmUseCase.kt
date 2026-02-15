package com.aaditx23.krazyalarm.domain.usecase

import com.aaditx23.krazyalarm.domain.repository.AlarmRepository
import com.aaditx23.krazyalarm.domain.repository.AlarmScheduler

class DeleteAlarmUseCase(
    private val alarmRepository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke(id: Long): Result<Unit> {
        return try {
            // Cancel any scheduled alarm first
            alarmScheduler.cancelAlarm(id)

            // Delete from repository
            alarmRepository.deleteAlarm(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
