package com.aaditx23.krazyalarm.data.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.aaditx23.krazyalarm.domain.repository.AlarmRepository
import com.aaditx23.krazyalarm.domain.models.Alarm
import com.aaditx23.krazyalarm.domain.repository.AlarmScheduler
import com.aaditx23.krazyalarm.domain.util.AlarmTimeCalculator
import com.aaditx23.krazyalarm.presentation.widget.AlarmWidgetUpdater
import kotlinx.coroutines.flow.first

class AlarmScheduler(private val context: Context, private val alarmRepository: AlarmRepository) : AlarmScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override suspend fun scheduleAlarm(alarm: Alarm): Result<Unit> {
        return try {
            schedule(alarm)
            AlarmWidgetUpdater.updateAllWidgets(context)
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
            AlarmWidgetUpdater.updateAllWidgets(context)
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
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
        return AlarmTimeCalculator.getNextTriggerTime(alarm)
    }
}
