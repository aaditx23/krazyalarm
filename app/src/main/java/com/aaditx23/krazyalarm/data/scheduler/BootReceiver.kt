package com.aaditx23.krazyalarm.data.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.aaditx23.krazyalarm.domain.repository.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class BootReceiver : BroadcastReceiver(), KoinComponent {

    private val alarmScheduler: AlarmScheduler by inject()

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                Log.d(TAG, "Device boot or timezone change detected, rescheduling alarms")

                // Reschedule all enabled alarms
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        alarmScheduler.rescheduleAllAlarms()
                        Log.d(TAG, "Rescheduled all alarms")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to reschedule alarms", e)
                    }
                }
            }
        }
    }
}
