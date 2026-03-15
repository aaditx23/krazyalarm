package com.aaditx23.krazyalarm.domain.usecase

import com.aaditx23.krazyalarm.domain.repository.AlarmRepository
import com.aaditx23.krazyalarm.domain.repository.AlarmScheduler

class DeleteAlarmUseCase(
    private val alarmRepository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke(id: Long): Result<Unit> {
        return try {
            val deleteResult = alarmRepository.deleteAlarm(id)
            if (deleteResult.isSuccess) {
                alarmScheduler.cancelAlarm(id)
            } else {
                deleteResult
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
