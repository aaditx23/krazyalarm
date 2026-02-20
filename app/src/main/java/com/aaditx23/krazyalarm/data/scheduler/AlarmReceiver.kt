package com.aaditx23.krazyalarm.data.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aaditx23.krazyalarm.domain.repository.AlarmRepository
import com.aaditx23.krazyalarm.domain.repository.AlarmScheduler
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AlarmReceiver : BroadcastReceiver(), KoinComponent {

    private val alarmRepository: AlarmRepository by inject()
    private val alarmScheduler: AlarmScheduler by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra("alarm_id", -1L)
        if (alarmId != -1L) {
            runBlocking {
                val alarm = alarmRepository.getAlarm(alarmId)
                if (alarm != null) {
                    // Add alarm to queue instead of starting immediately
                    AlarmQueueManager.enqueueAlarm(context, alarmId)

                    // Reschedule if repeating
                    if (alarm.days != 0) {
                        alarmScheduler.scheduleAlarm(alarm)
                    } else {
                        // One-time alarm - disable it after it rings
                        alarmRepository.toggleAlarm(alarmId, false)
                    }
                }
            }
        }
    }
}
