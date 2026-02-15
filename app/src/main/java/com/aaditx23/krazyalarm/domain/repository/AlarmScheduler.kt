package com.aaditx23.krazyalarm.domain.repository

import com.aaditx23.krazyalarm.domain.models.Alarm

interface AlarmScheduler {
    suspend fun scheduleAlarm(alarm: Alarm): Result<Unit>
    suspend fun cancelAlarm(alarmId: Long): Result<Unit>
    suspend fun rescheduleAllAlarms(): Result<Unit>
}
