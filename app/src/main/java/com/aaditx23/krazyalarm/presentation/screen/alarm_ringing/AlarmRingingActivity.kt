package com.aaditx23.krazyalarm.presentation.screen.alarm_ringing

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.aaditx23.krazyalarm.data.scheduler.AlarmRingingService
import com.aaditx23.krazyalarm.domain.repository.SettingsRepository
import com.aaditx23.krazyalarm.presentation.theme.KrazyAlarmTheme
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class AlarmRingingActivity : ComponentActivity() {

    private val settingsRepository: SettingsRepository by inject()
    private val alarmId: Long by lazy {
        intent.getLongExtra(EXTRA_ALARM_ID, -1L)
    }

    private val viewModel: AlarmRingingViewModel by viewModel {
        parametersOf(alarmId)
    }

    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        private const val TAG = "AlarmRingingActivity"
        const val EXTRA_ALARM_ID = "alarm_id"

        fun createIntent(context: Context, alarmId: Long): Intent {
            return Intent(context, AlarmRingingActivity::class.java).apply {
                putExtra(EXTRA_ALARM_ID, alarmId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                       Intent.FLAG_ACTIVITY_CLEAR_TOP or
                       Intent.FLAG_ACTIVITY_NO_USER_ACTION
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Force screen on and show over lockscreen (no keyguard dismissal — that would prompt for password)
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        // Acquire a wakelock so the CPU + screen stay on while the alarm is ringing
        val powerManager = getSystemService(PowerManager::class.java)
        @Suppress("DEPRECATION", "WakelockTimeout")
        wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or
            PowerManager.ACQUIRE_CAUSES_WAKEUP or
            PowerManager.ON_AFTER_RELEASE,
            "KrazyAlarm::AlarmWakeLock"
        ).also { it.acquire(10 * 60 * 1000L /* 10 min max */) }

        Log.d(TAG, "WakeLock acquired for alarm $alarmId")

        // Collect auto-dismiss events from the service SharedFlow.
        // replay=1 means we get the last event even if we subscribe after it was emitted.
        lifecycleScope.launch {
            AlarmRingingService.autoDismissFlow.collect { dismissedAlarmId ->
                if (dismissedAlarmId == alarmId) {
                    Log.d(TAG, "Auto-dismiss received for alarm $alarmId, closing activity")
                    finish()
                }
            }
        }

        setContent {
            val darkMode by settingsRepository.darkMode.collectAsState(initial = "system")
            val isDarkTheme = when (darkMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            KrazyAlarmTheme(darkTheme = isDarkTheme) {
                AlarmRingingScreen(
                    viewModel = viewModel,
                    onDismiss = { handleDismiss() },
                    onSnooze = { handleSnooze() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // If the alarm is no longer ringing (e.g. auto-dismissed while screen was off),
        // close this activity immediately.
        val ringingId = AlarmRingingService.currentRingingAlarmId.value
        if (ringingId != alarmId) {
            Log.d(TAG, "onResume: alarm $alarmId is no longer ringing (current=$ringingId), finishing")
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release wakelock if still held
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
                Log.d(TAG, "WakeLock released")
            }
        }
        wakeLock = null
    }

    private fun handleDismiss() {
        // Send dismiss action to service
        val dismissIntent = Intent(this, AlarmRingingService::class.java).apply {
            action = AlarmRingingService.ACTION_DISMISS
            putExtra(AlarmRingingService.EXTRA_ALARM_ID, alarmId)
        }
        startService(dismissIntent)
        finish()
    }

    private fun handleSnooze() {
        // Send snooze action to service
        val snoozeIntent = Intent(this, AlarmRingingService::class.java).apply {
            action = AlarmRingingService.ACTION_SNOOZE
            putExtra(AlarmRingingService.EXTRA_ALARM_ID, alarmId)
        }
        startService(snoozeIntent)
        finish()
    }
}
