package com.aaditx23.krazyalarm.domain.usecase

import com.aaditx23.krazyalarm.domain.repository.AlarmRepository

class CheckDuplicateAlarmUseCase(
    private val alarmRepository: AlarmRepository
) {
    suspend operator fun invoke(
        hour: Int,
        minute: Int,
        days: Int,
        scheduledDate: Long?,
        label: String?,
        ringtoneUri: String?,
        flashPatternId: String?,
        vibrationPatternId: String?,
        excludeId: Long? = null
    ): Boolean {
        return alarmRepository.checkDuplicateAlarm(
            hour,
            minute,
            days,
            scheduledDate,
            label,
            ringtoneUri,
            flashPatternId,
            vibrationPatternId,
            excludeId
        )
    }
}
