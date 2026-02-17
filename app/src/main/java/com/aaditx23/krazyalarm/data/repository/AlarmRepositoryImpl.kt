package com.aaditx23.krazyalarm.data.repository

import com.aaditx23.krazyalarm.data.local.database.AlarmDao
import com.aaditx23.krazyalarm.data.local.database.AlarmEntity
import com.aaditx23.krazyalarm.domain.models.Alarm
import com.aaditx23.krazyalarm.domain.models.AlarmInput
import com.aaditx23.krazyalarm.domain.models.FlashPattern
import com.aaditx23.krazyalarm.domain.models.VibrationIntensity
import com.aaditx23.krazyalarm.domain.models.VibrationPattern
import com.aaditx23.krazyalarm.domain.repository.AlarmRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AlarmRepositoryImpl(
    private val alarmDao: AlarmDao
) : AlarmRepository {

    override suspend fun createAlarm(input: AlarmInput): Result<Long> {
        return try {
            val entity = input.toEntity()
            val id = alarmDao.insert(entity)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateAlarm(id: Long, input: AlarmInput): Result<Unit> {
        return try {
            val existing = alarmDao.getAlarmById(id)
            if (existing != null) {
                val updatedEntity = input.toEntity(id = id, createdAt = existing.createdAt)
                alarmDao.update(updatedEntity)
                Result.success(Unit)
            } else {
                Result.failure(IllegalArgumentException("Alarm with id $id not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteAlarm(id: Long): Result<Unit> {
        return try {
            alarmDao.deleteById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeAlarms(): Flow<List<Alarm>> {
        return alarmDao.getAllAlarmsFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getAlarm(id: Long): Alarm? {
        return try {
            alarmDao.getAlarmById(id)?.toDomain()
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun toggleAlarm(id: Long, enabled: Boolean): Result<Unit> {
        return try {
            val existing = alarmDao.getAlarmById(id)
            if (existing != null) {
                val updatedEntity = existing.copy(
                    enabled = enabled,
                    updatedAt = System.currentTimeMillis()
                )
                alarmDao.update(updatedEntity)
                Result.success(Unit)
            } else {
                Result.failure(IllegalArgumentException("Alarm with id $id not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getEnabledAlarms(): List<Alarm> {
        return alarmDao.getEnabledAlarms().map { it.toDomain() }
    }

    private fun AlarmInput.toEntity(id: Long = 0, createdAt: Long = System.currentTimeMillis()): AlarmEntity {
        return AlarmEntity(
            id = id,
            hour = hour,
            minute = minute,
            days = days,
            enabled = enabled,
            label = label,
            ringtoneUri = ringtoneUri,
            flashPatternId = flashPatternId,
            vibrationPatternId = vibrationPatternId,
            vibrationIntensity = vibrationIntensity.name,
            snoozeDurationMinutes = snoozeDurationMinutes,
            alarmDurationMinutes = alarmDurationMinutes,
            scheduledDate = scheduledDate,
            createdAt = createdAt,
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun AlarmEntity.toDomain(): Alarm {
        return Alarm(
            id = id,
            hour = hour,
            minute = minute,
            days = days,
            enabled = enabled,
            label = label,
            ringtoneUri = ringtoneUri,
            flashPatternId = flashPatternId,
            vibrationPatternId = vibrationPatternId,
            vibrationIntensity = VibrationIntensity.valueOf(vibrationIntensity),
            volume = volume,
            snoozeDurationMinutes = snoozeDurationMinutes,
            alarmDurationMinutes = alarmDurationMinutes,
            scheduledDate = scheduledDate,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
