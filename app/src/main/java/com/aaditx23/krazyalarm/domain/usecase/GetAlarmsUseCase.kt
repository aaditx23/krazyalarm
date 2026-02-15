package com.aaditx23.krazyalarm.domain.usecase

import com.aaditx23.krazyalarm.domain.models.Alarm
import com.aaditx23.krazyalarm.domain.repository.AlarmRepository
import kotlinx.coroutines.flow.Flow

class GetAlarmsUseCase(
    private val alarmRepository: AlarmRepository
) {
    operator fun invoke(): Flow<List<Alarm>> {
        return alarmRepository.observeAlarms()
    }
}
