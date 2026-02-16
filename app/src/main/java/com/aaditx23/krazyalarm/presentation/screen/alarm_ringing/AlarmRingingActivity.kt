package com.aaditx23.krazyalarm.presentation.screen.alarm_ringing

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.aaditx23.krazyalarm.data.scheduler.AlarmRingingService
import com.aaditx23.krazyalarm.domain.repository.SettingsRepository
import com.aaditx23.krazyalarm.presentation.theme.KrazyAlarmTheme
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

    companion object {
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

        // Show activity when screen is locked
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
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
