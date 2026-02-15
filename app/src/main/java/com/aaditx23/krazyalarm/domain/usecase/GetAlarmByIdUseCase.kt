package com.aaditx23.krazyalarm.domain.usecase

import com.aaditx23.krazyalarm.domain.models.Alarm
import com.aaditx23.krazyalarm.domain.repository.AlarmRepository

class GetAlarmByIdUseCase(
    private val alarmRepository: AlarmRepository
) {
    suspend operator fun invoke(id: Long): Alarm? {
        return alarmRepository.getAlarm(id)
    }
}
