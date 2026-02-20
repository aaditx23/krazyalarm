package com.aaditx23.krazyalarm.domain.usecase

import com.aaditx23.krazyalarm.domain.repository.AlarmRepository
import com.aaditx23.krazyalarm.domain.repository.AlarmScheduler
import java.util.Calendar

class ToggleAlarmUseCase(
    private val alarmRepository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke(id: Long, enabled: Boolean): Result<Unit> {
        return try {
            if (enabled) {
                // Get the alarm before enabling
                val alarm = alarmRepository.getAlarm(id)
                if (alarm != null && alarm.days == 0) {
                    // One-time alarm: update scheduledDate to today/tomorrow
                    val calendar = Calendar.getInstance()
                    calendar.set(Calendar.HOUR_OF_DAY, alarm.hour)
                    calendar.set(Calendar.MINUTE, alarm.minute)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)

                    // If the time has already passed today, schedule for tomorrow
                    if (calendar.timeInMillis <= System.currentTimeMillis()) {
                        calendar.add(Calendar.DAY_OF_YEAR, 1)
                    }

                    // Update the alarm with new scheduled date
                    val updatedAlarm = alarm.copy(
                        scheduledDate = calendar.timeInMillis,
                        enabled = true
                    )
                    alarmRepository.updateAlarmDirect(updatedAlarm)

                    // Schedule the updated alarm
                    alarmScheduler.scheduleAlarm(updatedAlarm)
                } else {
                    // Repeating alarm or null alarm - just toggle
                    val result = alarmRepository.toggleAlarm(id, true)

                    if (result.isSuccess && alarm != null) {
                        alarmScheduler.scheduleAlarm(alarm)
                    }
                }
            } else {
                // Disabling alarm - cancel it
                alarmRepository.toggleAlarm(id, false)
                alarmScheduler.cancelAlarm(id)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
