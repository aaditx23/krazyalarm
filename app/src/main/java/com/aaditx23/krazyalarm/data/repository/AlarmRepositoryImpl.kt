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
            android.util.Log.d("AlarmRepository", "=== CREATE ALARM called ===")
            android.util.Log.d("AlarmRepository", "Input: hour=${input.hour}, minute=${input.minute}, days=${input.days}, scheduledDate=${input.scheduledDate}")
            val entity = input.toEntity()
            android.util.Log.d("AlarmRepository", "Entity created with ID: ${entity.id}")
            val id = alarmDao.insert(entity)
            android.util.Log.d("AlarmRepository", "Inserted with ID: $id")
            Result.success(id)
        } catch (e: Exception) {
            android.util.Log.e("AlarmRepository", "Create alarm failed", e)
            Result.failure(e)
        }
    }

    override suspend fun updateAlarm(id: Long, input: AlarmInput): Result<Unit> {
        return try {
            android.util.Log.d("AlarmRepository", "=== UPDATE ALARM called ===")
            android.util.Log.d("AlarmRepository", "Updating alarm ID: $id")
            android.util.Log.d("AlarmRepository", "Input: hour=${input.hour}, minute=${input.minute}, days=${input.days}, scheduledDate=${input.scheduledDate}")
            val existing = alarmDao.getAlarmById(id)
            if (existing != null) {
                android.util.Log.d("AlarmRepository", "Existing alarm found: ID=${existing.id}")
                val updatedEntity = input.toEntity(
                    id = id,
                    createdAt = existing.createdAt,
                    snoozedUntilMillis = existing.snoozedUntilMillis
                )
                android.util.Log.d("AlarmRepository", "Updated entity: ID=${updatedEntity.id}")
                alarmDao.update(updatedEntity)
                android.util.Log.d("AlarmRepository", "Update successful")
                Result.success(Unit)
            } else {
                android.util.Log.e("AlarmRepository", "Alarm with id $id not found")
                Result.failure(IllegalArgumentException("Alarm with id $id not found"))
            }
        } catch (e: Exception) {
            android.util.Log.e("AlarmRepository", "Update alarm failed", e)
            Result.failure(e)
        }
    }

    override suspend fun updateAlarmDirect(alarm: Alarm): Result<Unit> {
        return try {
            val entity = alarm.toEntity()
            alarmDao.update(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun restoreAlarm(alarm: Alarm): Result<Unit> {
        return try {
            // Insert with the original id — REPLACE strategy means it's an exact restore
            val entity = alarm.toEntity()
            alarmDao.insert(entity)
            Result.success(Unit)
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

    override suspend fun updateSnoozedUntil(id: Long, snoozedUntilMillis: Long?): Result<Unit> {
        return try {
            alarmDao.updateSnoozedUntil(
                id = id,
                snoozedUntilMillis = snoozedUntilMillis,
                updatedAt = System.currentTimeMillis()
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getEnabledAlarms(): List<Alarm> {
        return alarmDao.getEnabledAlarms().map { it.toDomain() }
    }


    private fun AlarmInput.toEntity(
        id: Long = 0,
        createdAt: Long = System.currentTimeMillis(),
        snoozedUntilMillis: Long? = null
    ): AlarmEntity {
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
            snoozedUntilMillis = snoozedUntilMillis,
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
            snoozeDurationMinutes = snoozeDurationMinutes,
            alarmDurationMinutes = alarmDurationMinutes,
            scheduledDate = scheduledDate,
            snoozedUntilMillis = snoozedUntilMillis,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun Alarm.toEntity(): AlarmEntity {
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
            snoozedUntilMillis = snoozedUntilMillis,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
