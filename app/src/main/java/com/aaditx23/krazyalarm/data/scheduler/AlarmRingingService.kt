package com.aaditx23.krazyalarm.data.scheduler

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.aaditx23.krazyalarm.MainActivity
import com.aaditx23.krazyalarm.R
import com.aaditx23.krazyalarm.domain.models.Alarm
import com.aaditx23.krazyalarm.domain.models.FlashPattern
import com.aaditx23.krazyalarm.domain.models.VibrationIntensity
import com.aaditx23.krazyalarm.domain.models.VibrationPattern
import com.aaditx23.krazyalarm.domain.repository.AlarmRepository
import com.aaditx23.krazyalarm.domain.repository.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.io.IOException

class AlarmRingingService : Service() {

    companion object {
        private const val TAG = "AlarmRingingService"
        private const val NOTIFICATION_CHANNEL_ID = "alarm_channel"
        private const val NOTIFICATION_ID = 1
        const val ACTION_DISMISS = "com.aaditx23.krazyalarm.ACTION_DISMISS"
        const val ACTION_SNOOZE = "com.aaditx23.krazyalarm.ACTION_SNOOZE"
        const val EXTRA_ALARM_ID = "alarm_id"
    }

    private val alarmRepository: AlarmRepository by inject()
    private val alarmScheduler: AlarmScheduler by inject()

    private var currentAlarm: Alarm? = null
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var cameraManager: CameraManager? = null
    private var flashJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initializeHardware()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getLongExtra(EXTRA_ALARM_ID, -1L) ?: -1L

        if (alarmId == -1L) {
            Log.e(TAG, "No alarm ID provided")
            stopSelf()
            return START_NOT_STICKY
        }

        when (intent?.action) {
            ACTION_DISMISS -> {
                dismissAlarm()
                return START_NOT_STICKY
            }
            ACTION_SNOOZE -> {
                snoozeAlarm()
                return START_NOT_STICKY
            }
            else -> {
                startAlarmRinging(alarmId)
            }
        }

        return START_STICKY
    }

    private fun startAlarmRinging(alarmId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val alarm = alarmRepository.getAlarm(alarmId)
                if (alarm == null) {
                    Log.e(TAG, "Alarm not found: $alarmId")
                    stopSelf()
                    return@launch
                }

                currentAlarm = alarm

                // Start foreground service with notification
                startForeground(NOTIFICATION_ID, createNotification(alarm))

                // Start alarm components
                startRingtone(alarm)
                startVibration(alarm)
                startFlash(alarm)

                Log.d(TAG, "Alarm ringing started for: ${alarm.label ?: "Alarm"}")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to start alarm ringing", e)
                stopSelf()
            }
        }
    }

    private fun createNotification(alarm: Alarm): Notification {
        val fullScreenIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ALARM_ID, alarm.id)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            0,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(this, AlarmRingingService::class.java).apply {
            action = ACTION_DISMISS
            putExtra(EXTRA_ALARM_ID, alarm.id)
        }

        val dismissPendingIntent = PendingIntent.getService(
            this,
            1,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(this, AlarmRingingService::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_ALARM_ID, alarm.id)
        }

        val snoozePendingIntent = PendingIntent.getService(
            this,
            2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(alarm.label ?: "Alarm")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(R.drawable.ic_launcher_foreground, "Dismiss", dismissPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Snooze", snoozePendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun startRingtone(alarm: Alarm) {
        try {
            mediaPlayer = MediaPlayer().apply {
                val ringtoneUri = alarm.ringtoneUri?.let { android.net.Uri.parse(it) }
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

                setDataSource(this@AlarmRingingService, ringtoneUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to start ringtone", e)
        }
    }

    private fun startVibration(alarm: Alarm) {
        vibrator?.let {
            val pattern = VibrationPattern.fromId(alarm.vibrationPatternId)
            val intensity = alarm.vibrationIntensity

            when (pattern) {
                VibrationPattern.Continuous -> vibrateContinuous(intensity)
                VibrationPattern.Pulse -> vibratePulse(intensity)
                VibrationPattern.Escalating -> vibrateEscalating(intensity)
                VibrationPattern.Heartbeat -> vibrateHeartbeat(intensity)
                VibrationPattern.Wave -> vibrateWave(intensity)
            }
        }
    }

    private fun startFlash(alarm: Alarm) {
        cameraManager?.let { cameraManager ->
            val pattern = FlashPattern.fromId(alarm.flashPatternId)

            if (pattern != FlashPattern.None) {
                flashJob = CoroutineScope(Dispatchers.Default).launch {
                    try {
                        val cameraId = cameraManager.cameraIdList[0] // Use first camera

                        when (pattern) {
                            FlashPattern.AlwaysOn -> flashAlwaysOn(cameraManager, cameraId)
                            FlashPattern.SosBlink -> flashSos(cameraManager, cameraId)
                            FlashPattern.Strobe -> flashStrobe(cameraManager, cameraId)
                            FlashPattern.Pulse -> flashPulse(cameraManager, cameraId)
                            FlashPattern.Heartbeat -> flashHeartbeat(cameraManager, cameraId)
                            FlashPattern.None -> { /* Do nothing */ }
                        }
                    } catch (e: CameraAccessException) {
                        Log.e(TAG, "Camera access error", e)
                    }
                }
            }
        }
    }

    private fun vibrateContinuous(intensity: VibrationIntensity) {
        val amplitude = getVibrationAmplitude(intensity)
        // Use a repeating pattern for continuous vibration
        val pattern = longArrayOf(0, 1000) // Vibrate continuously in 1-second chunks
        val amplitudes = intArrayOf(0, amplitude)

        vibrator?.vibrate(
            VibrationEffect.createWaveform(pattern, amplitudes, 0) // 0 = repeat from index 0
        )
    }

    private fun vibratePulse(intensity: VibrationIntensity) {
        val amplitude = getVibrationAmplitude(intensity)
        val pattern = longArrayOf(0, 500, 500) // on, off, repeat
        val amplitudes = intArrayOf(0, amplitude, 0)

        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, 0))
    }

    private fun vibrateEscalating(intensity: VibrationIntensity) {
        val pattern = longArrayOf(0, 200, 200, 200, 200, 200, 200)
        val amplitudes = intArrayOf(0, 50, 100, 150, 200, 255, 0)

        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, 0))
    }

    private fun vibrateHeartbeat(intensity: VibrationIntensity) {
        val amplitude = getVibrationAmplitude(intensity)
        val pattern = longArrayOf(0, 100, 100, 100, 400) // lub, pause, dub, long pause
        val amplitudes = intArrayOf(0, amplitude, 0, amplitude, 0)

        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, 0))
    }

    private fun vibrateWave(intensity: VibrationIntensity) {
        val amplitude = getVibrationAmplitude(intensity)
        val pattern = longArrayOf(0, 300, 200, 500, 200, 300, 200)
        val amplitudes = intArrayOf(0, amplitude, 0, amplitude, 0, amplitude, 0)

        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, 0))
    }

    private fun getVibrationAmplitude(intensity: VibrationIntensity): Int {
        return when (intensity) {
            VibrationIntensity.LIGHT -> 100
            VibrationIntensity.MEDIUM -> 180
            VibrationIntensity.STRONG -> 255
        }
    }

    private suspend fun flashAlwaysOn(cameraManager: CameraManager, cameraId: String) {
        try {
            cameraManager.setTorchMode(cameraId, true)
            // Keep it on until cancelled
            while (flashJob?.isActive == true) {
                delay(100)
            }
        } finally {
            cameraManager.setTorchMode(cameraId, false)
        }
    }

    private suspend fun flashSos(cameraManager: CameraManager, cameraId: String) {
        val sosPattern = listOf(100L, 100L, 100L, 300L, 300L, 300L, 100L, 100L, 100L) // S O S
        while (flashJob?.isActive == true) {
            for (duration in sosPattern) {
                cameraManager.setTorchMode(cameraId, true)
                delay(duration)
                cameraManager.setTorchMode(cameraId, false)
                delay(100)
            }
            delay(1000) // Pause between SOS sequences
        }
    }

    private suspend fun flashStrobe(cameraManager: CameraManager, cameraId: String) {
        while (flashJob?.isActive == true) {
            cameraManager.setTorchMode(cameraId, true)
            delay(100)
            cameraManager.setTorchMode(cameraId, false)
            delay(100)
        }
    }

    private suspend fun flashPulse(cameraManager: CameraManager, cameraId: String) {
        while (flashJob?.isActive == true) {
            // Fade in
            for (i in 1..10) {
                cameraManager.setTorchMode(cameraId, true)
                delay(50)
                cameraManager.setTorchMode(cameraId, false)
                delay(50)
            }
            // Fade out
            for (i in 10 downTo 1) {
                cameraManager.setTorchMode(cameraId, true)
                delay(50)
                cameraManager.setTorchMode(cameraId, false)
                delay(50)
            }
        }
    }

    private suspend fun flashHeartbeat(cameraManager: CameraManager, cameraId: String) {
        while (flashJob?.isActive == true) {
            cameraManager.setTorchMode(cameraId, true)
            delay(100)
            cameraManager.setTorchMode(cameraId, false)
            delay(100)
            cameraManager.setTorchMode(cameraId, true)
            delay(100)
            cameraManager.setTorchMode(cameraId, false)
            delay(600) // Longer pause
        }
    }

    private fun dismissAlarm() {
        Log.d(TAG, "Dismissing alarm")
        stopAllAlarmComponents()
        stopSelf()
    }

    private fun snoozeAlarm() {
        currentAlarm?.let { alarm ->
            Log.d(TAG, "Snoozing alarm for ${alarm.snoozeDurationMinutes} minutes")

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

                    // Check if we can schedule exact alarms
                    if (!alarmManager.canScheduleExactAlarms()) {
                        Log.e(TAG, "Cannot schedule exact alarms: permission not granted")
                        return@launch
                    }

                    // Schedule snooze alarm
                    val snoozeTime = System.currentTimeMillis() + (alarm.snoozeDurationMinutes * 60 * 1000)
                    val snoozeIntent = Intent(this@AlarmRingingService, AlarmReceiver::class.java).apply {
                        putExtra("alarm_id", alarm.id)
                    }

                    val pendingIntent = PendingIntent.getBroadcast(
                        this@AlarmRingingService,
                        (alarm.id + 1000).toInt(), // Different request code for snooze
                        snoozeIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        snoozeTime,
                        pendingIntent
                    )
                } catch (e: SecurityException) {
                    Log.e(TAG, "SecurityException when scheduling snooze", e)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to schedule snooze", e)
                }
            }
        }

        stopAllAlarmComponents()
        stopSelf()
    }

    private fun stopAllAlarmComponents() {
        // Stop media player
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null

        // Stop vibration
        vibrator?.cancel()

        // Stop flash
        flashJob?.cancel()
        flashJob = null
        try {
            cameraManager?.cameraIdList?.get(0)?.let { cameraId ->
                cameraManager?.setTorchMode(cameraId, false)
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to turn off flash", e)
        }
    }

    private fun initializeHardware() {
        // Initialize vibrator
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        // Initialize camera manager for flash
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Alarm Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for alarm alerts"
                setSound(null, null) // Don't play sound through notification
                enableVibration(false) // Don't vibrate through notification
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAllAlarmComponents()
        Log.d(TAG, "AlarmRingingService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
