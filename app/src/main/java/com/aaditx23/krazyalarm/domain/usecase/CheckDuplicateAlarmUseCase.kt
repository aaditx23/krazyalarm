package com.aaditx23.krazyalarm.domain.usecase

import com.aaditx23.krazyalarm.domain.repository.AlarmRepository

class CheckDuplicateAlarmUseCase(
    private val alarmRepository: AlarmRepository
) {
    suspend operator fun invoke(
        hour: Int,
        minute: Int,
        scheduledDate: Long?,
        excludeId: Long? = null
    ): Boolean {
        return alarmRepository.checkDuplicateAlarm(hour, minute, scheduledDate, excludeId)
    }
}
