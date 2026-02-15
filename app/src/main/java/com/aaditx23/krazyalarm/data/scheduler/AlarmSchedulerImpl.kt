package com.aaditx23.krazyalarm.data.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.aaditx23.krazyalarm.domain.models.Alarm
import com.aaditx23.krazyalarm.domain.repository.AlarmScheduler
import com.aaditx23.krazyalarm.domain.usecase.CalculateNextAlarmTimeUseCase

class AlarmSchedulerImpl(
    private val context: Context,
    private val calculateNextAlarmTimeUseCase: CalculateNextAlarmTimeUseCase
) : AlarmScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override suspend fun scheduleAlarm(alarm: Alarm): Result<Unit> {
        return try {
            if (!alarm.enabled) {
                cancelAlarm(alarm.id)
                return Result.success(Unit)
            }

            val nextTriggerTime = calculateNextAlarmTimeUseCase(
                hour = alarm.hour,
                minute = alarm.minute,
                days = alarm.days
            )

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                alarm.id.toInt(), // Use alarm ID as request code
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Use setExactAndAllowWhileIdle for API 23+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextTriggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    nextTriggerTime,
                    pendingIntent
                )
            }

            Result.success(Unit)
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
            pendingIntent.cancel()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun rescheduleAllAlarms(): Result<Unit> {
        return try {
            // This will be called from BootReceiver
            // The actual rescheduling will be handled by the use cases
            // that get all enabled alarms and schedule them
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
