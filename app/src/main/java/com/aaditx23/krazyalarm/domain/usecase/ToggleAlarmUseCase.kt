package com.aaditx23.krazyalarm.domain.usecase

import android.util.Log
import com.aaditx23.krazyalarm.domain.repository.AlarmRepository
import com.aaditx23.krazyalarm.domain.repository.AlarmScheduler
import com.aaditx23.krazyalarm.domain.util.AlarmTimeCalculator

class ToggleAlarmUseCase(
    private val alarmRepository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler
) {
    private companion object {
        const val TAG = "ToggleAlarmUseCase"
    }

    suspend operator fun invoke(id: Long, enabled: Boolean): Result<Unit> {
        return try {
            if (enabled) {
                val alarm = alarmRepository.getAlarm(id)
                if (alarm != null && alarm.days == 0) {
                    // One-time alarm: always recalculate scheduledDate to the next valid future time
                    val nextTrigger = AlarmTimeCalculator.getNextOneTimeTrigger(alarm.hour, alarm.minute)

                    Log.d(TAG, "Re-enabling one-time alarm ID: ${alarm.id}, hour=${alarm.hour}, minute=${alarm.minute}")
                    Log.d(TAG, "Old scheduledDate: ${alarm.scheduledDate}, new scheduledDate: $nextTrigger")

                    val updatedAlarm = alarm.copy(
                        scheduledDate = nextTrigger,
                        enabled = true
                    )
                    alarmRepository.updateAlarmDirect(updatedAlarm)
                    alarmScheduler.scheduleAlarm(updatedAlarm)
                } else if (alarm != null) {
                    // Repeating alarm - just toggle and schedule
                    alarmRepository.toggleAlarm(id, true)
                    alarmScheduler.scheduleAlarm(alarm.copy(enabled = true))
                }
            } else {
                alarmRepository.toggleAlarm(id, false)
                alarmScheduler.cancelAlarm(id)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling alarm $id", e)
            Result.failure(e)
        }
    }
}
