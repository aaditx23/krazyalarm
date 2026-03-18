package com.aaditx23.krazyalarm.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [AlarmEntity::class],
    version = 5,
    exportSchema = true
)
abstract class AlarmDatabase : RoomDatabase() {

    abstract fun alarmDao(): AlarmDao

    companion object {
        private const val DATABASE_NAME = "krazyalarm.db"

        @Volatile
        private var INSTANCE: AlarmDatabase? = null

        // Migration from version 3 to 4: Remove volume column
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create new table without volume column
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS alarms_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        hour INTEGER NOT NULL,
                        minute INTEGER NOT NULL,
                        days INTEGER NOT NULL,
                        enabled INTEGER NOT NULL,
                        label TEXT,
                        ringtoneUri TEXT,
                        flashPatternId TEXT,
                        vibrationPatternId TEXT,
                        vibrationIntensity TEXT NOT NULL DEFAULT 'MEDIUM',
                        snoozeDurationMinutes INTEGER NOT NULL DEFAULT 10,
                        alarmDurationMinutes INTEGER NOT NULL DEFAULT 1,
                        scheduledDate INTEGER,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())

                // Copy data from old table to new table (excluding volume)
                db.execSQL("""
                    INSERT INTO alarms_new (id, hour, minute, days, enabled, label, ringtoneUri, 
                        flashPatternId, vibrationPatternId, vibrationIntensity, snoozeDurationMinutes, 
                        alarmDurationMinutes, scheduledDate, createdAt, updatedAt)
                    SELECT id, hour, minute, days, enabled, label, ringtoneUri, 
                        flashPatternId, vibrationPatternId, vibrationIntensity, snoozeDurationMinutes, 
                        alarmDurationMinutes, scheduledDate, createdAt, updatedAt
                    FROM alarms
                """.trimIndent())

                // Drop old table
                db.execSQL("DROP TABLE alarms")

                // Rename new table
                db.execSQL("ALTER TABLE alarms_new RENAME TO alarms")

                // Recreate index
                db.execSQL("CREATE INDEX IF NOT EXISTS index_alarms_enabled ON alarms(enabled)")
            }
        }

        // Migration from version 4 to 5: Add snoozedUntilMillis for pending snooze tracking
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN snoozedUntilMillis INTEGER")
            }
        }

        fun getInstance(context: Context): AlarmDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AlarmDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
