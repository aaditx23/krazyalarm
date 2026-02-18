package com.aaditx23.krazyalarm.data.local.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "alarms",
    indices = [
        Index(value = ["enabled"])
    ]
)
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val days: Int, // bitmask for days of week (7 bits)
    val enabled: Boolean,
    val label: String? = null,
    val ringtoneUri: String? = null,
    val flashPatternId: String? = null,
    val vibrationPatternId: String? = null,
    val vibrationIntensity: String = "MEDIUM",
    val snoozeDurationMinutes: Int = 10,
    val alarmDurationMinutes: Int = 1,
    val scheduledDate: Long? = null, // Specific date in millis for one-time alarms
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
