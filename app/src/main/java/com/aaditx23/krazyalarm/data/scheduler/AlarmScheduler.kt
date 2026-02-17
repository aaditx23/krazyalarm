package com.aaditx23.krazyalarm.data.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.aaditx23.krazyalarm.domain.repository.AlarmRepository
import com.aaditx23.krazyalarm.domain.models.Alarm
import com.aaditx23.krazyalarm.domain.repository.AlarmScheduler
import kotlinx.coroutines.flow.first
import java.util.Calendar

class AlarmScheduler(private val context: Context, private val alarmRepository: AlarmRepository) : AlarmScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override suspend fun scheduleAlarm(alarm: Alarm): Result<Unit> {
        return try {
            schedule(alarm)
            Result.success(Unit)
        } catch (e: SecurityException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cancelAlarm(alarmId: Long): Result<Unit> {
        return try {
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                alarmId.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun rescheduleAllAlarms(): Result<Unit> {
        return try {
            val alarms = alarmRepository.observeAlarms().first()
            alarms.forEach { alarm ->
                if (alarm.enabled) {
                    scheduleAlarm(alarm)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun schedule(alarm: Alarm) {
        if (!alarmManager.canScheduleExactAlarms()) {
            throw SecurityException("Cannot schedule exact alarms: permission not granted")
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarm_id", alarm.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = getNextAlarmTime(alarm)

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
    }

    private fun getNextAlarmTime(alarm: Alarm): Long {
        val calendar = Calendar.getInstance()

        if (alarm.days == 0) {
            // One time alarm
            if (alarm.scheduledDate != null) {
                // Use the scheduled date if it's set
                calendar.timeInMillis = alarm.scheduledDate
                // Ensure hour and minute are correct
                calendar.set(Calendar.HOUR_OF_DAY, alarm.hour)
                calendar.set(Calendar.MINUTE, alarm.minute)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                return calendar.timeInMillis
            } else {
                // No scheduled date, use today's time or tomorrow if already passed
                calendar.set(Calendar.HOUR_OF_DAY, alarm.hour)
                calendar.set(Calendar.MINUTE, alarm.minute)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                if (calendar.timeInMillis <= System.currentTimeMillis()) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
                return calendar.timeInMillis
            }
        } else {
            // Repeating alarm on specific days
            calendar.set(Calendar.HOUR_OF_DAY, alarm.hour)
            calendar.set(Calendar.MINUTE, alarm.minute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon, ..., 7=Sat
            for (i in 0..6) {
                val checkDay = ((currentDayOfWeek - 1 + i) % 7) + 1 // Cycle through days
                if ((alarm.days and (1 shl (checkDay - 1))) != 0) {
                    calendar.add(Calendar.DAY_OF_YEAR, i)
                    return calendar.timeInMillis
                }
            }
            // If no day found in the week, add 7 days (should not happen if days != 0)
            calendar.add(Calendar.DAY_OF_YEAR, 7)
            return calendar.timeInMillis
        }
    }
}
