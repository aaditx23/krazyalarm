package com.aaditx23.krazyalarm.domain.repository

import com.aaditx23.krazyalarm.domain.models.Alarm
import com.aaditx23.krazyalarm.domain.models.AlarmInput
import kotlinx.coroutines.flow.Flow

interface AlarmRepository {
    suspend fun createAlarm(input: AlarmInput): Result<Long>
    suspend fun updateAlarm(id: Long, input: AlarmInput): Result<Unit>
    suspend fun updateAlarmDirect(alarm: Alarm): Result<Unit>
    /** Restores a previously deleted alarm with all original data intact (id, schedule, etc.) */
    suspend fun restoreAlarm(alarm: Alarm): Result<Unit>
    suspend fun deleteAlarm(id: Long): Result<Unit>
    fun observeAlarms(): Flow<List<Alarm>>
    suspend fun getAlarm(id: Long): Alarm?
    suspend fun toggleAlarm(id: Long, enabled: Boolean): Result<Unit>
    suspend fun getEnabledAlarms(): List<Alarm>
}
