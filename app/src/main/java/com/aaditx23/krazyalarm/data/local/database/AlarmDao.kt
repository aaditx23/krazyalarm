package com.aaditx23.krazyalarm.data.local.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alarm: AlarmEntity): Long

    @Update
    suspend fun update(alarm: AlarmEntity)

    @Delete
    suspend fun delete(alarm: AlarmEntity)

    @Query("DELETE FROM alarms WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM alarms ORDER BY hour, minute")
    fun getAllAlarmsFlow(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getAlarmById(id: Long): AlarmEntity?

    @Query("SELECT * FROM alarms WHERE enabled = 1")
    suspend fun getEnabledAlarms(): List<AlarmEntity>

    @Query("UPDATE alarms SET snoozedUntilMillis = :snoozedUntilMillis, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSnoozedUntil(id: Long, snoozedUntilMillis: Long?, updatedAt: Long)

    @Query("SELECT * FROM alarms ORDER BY hour, minute")
    suspend fun getAllAlarms(): List<AlarmEntity>
}
