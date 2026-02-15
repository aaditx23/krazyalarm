package com.aaditx23.krazyalarm.data.scheduler

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aaditx23.krazyalarm.domain.repository.AlarmRepository
import com.aaditx23.krazyalarm.domain.repository.AlarmScheduler
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SnoozeReceiver : BroadcastReceiver(), KoinComponent {

    private val alarmRepository: AlarmRepository by inject()
    private val alarmScheduler: AlarmScheduler by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra("alarm_id", -1L)
        if (alarmId != -1L) {
            runBlocking {
                val alarm = alarmRepository.getAlarm(alarmId)
                if (alarm != null) {
                    // Cancel the notification
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(alarmId.toInt())

                    // Stop the alarm ringing service
                    val serviceIntent = Intent(context, AlarmRingingService::class.java).apply {
                        action = AlarmRingingService.ACTION_SNOOZE
                        putExtra(AlarmRingingService.EXTRA_ALARM_ID, alarmId)
                    }
                    context.startService(serviceIntent)
                }
            }
        }
    }
}
