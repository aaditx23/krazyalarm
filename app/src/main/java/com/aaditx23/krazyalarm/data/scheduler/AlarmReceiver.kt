package com.aaditx23.krazyalarm.data.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_ALARM_ID = "alarm_id"
        private const val TAG = "AlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)

        if (alarmId == -1L) {
            Log.e(TAG, "Received alarm broadcast without alarm ID")
            return
        }

        Log.d(TAG, "Alarm triggered for alarm ID: $alarmId")

        // Start the alarm ringing service
        val serviceIntent = Intent(context, AlarmRingingService::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
        }

        context.startForegroundService(serviceIntent)
    }
}
