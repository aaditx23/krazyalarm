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
import android.media.audiofx.LoudnessEnhancer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
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
import com.aaditx23.krazyalarm.presentation.screen.alarm_ringing.AlarmRingingActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import androidx.core.net.toUri

class AlarmRingingService : Service() {

    companion object {
        private const val TAG = "AlarmRingingService"
        private const val NOTIFICATION_CHANNEL_ID = "alarm_channel"
        private const val NOTIFICATION_ID = 1
        const val ACTION_DISMISS = "com.aaditx23.krazyalarm.ACTION_DISMISS"
        const val ACTION_SNOOZE = "com.aaditx23.krazyalarm.ACTION_SNOOZE"
        const val ACTION_AUTO_DISMISS = "com.aaditx23.krazyalarm.ACTION_AUTO_DISMISS"
        const val EXTRA_ALARM_ID = "alarm_id"

        // SharedFlow (replay=1) so Activity receives auto-dismiss even if it subscribes slightly late
        private val _autoDismissFlow = MutableSharedFlow<Long>(replay = 1)
        val autoDismissFlow: SharedFlow<Long> = _autoDismissFlow.asSharedFlow()

        // Tracks the alarm ID currently ringing (null when no alarm is active)
        private val _currentRingingAlarmId = MutableStateFlow<Long?>(null)
        val currentRingingAlarmId: StateFlow<Long?> = _currentRingingAlarmId.asStateFlow()
    }

    private val alarmRepository: AlarmRepository by inject()
    private val alarmScheduler: AlarmScheduler by inject()
    private val settingsRepository: com.aaditx23.krazyalarm.domain.repository.SettingsRepository by inject()

    private var currentAlarm: Alarm? = null
    private var mediaPlayer: MediaPlayer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var vibrator: Vibrator? = null
    private var cameraManager: CameraManager? = null
    private var flashJob: Job? = null
    private var autoDismissJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initializeHardware()

        // Acquire wakelock so the screen turns on immediately when the alarm fires
        val powerManager = getSystemService(PowerManager::class.java)
        @Suppress("DEPRECATION", "WakelockTimeout")
        wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or
            PowerManager.ACQUIRE_CAUSES_WAKEUP or
            PowerManager.ON_AFTER_RELEASE,
            "KrazyAlarm::ServiceWakeLock"
        ).also { it.acquire(10 * 60 * 1000L /* 10 min max */) }
        Log.d(TAG, "Service WakeLock acquired")
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

                Log.d(TAG, "Alarm loaded: id=${alarm.id}, duration=${alarm.alarmDurationMinutes}min")

                currentAlarm = alarm
                _currentRingingAlarmId.value = alarm.id

                // Start foreground service with notification (fullScreenIntent handles lock-screen on most devices)
                startForeground(NOTIFICATION_ID, createNotification(alarm))

                // Also directly start the Activity so it shows over lock screen / when screen is off
                // on devices where fullScreenIntent alone is not reliable
                val activityIntent = AlarmRingingActivity.createIntent(this@AlarmRingingService, alarm.id)
                startActivity(activityIntent)

                // Start alarm components
                startRingtone(alarm)
                startVibration(alarm)
                startFlash(alarm)

                // Schedule auto-dismiss after alarm duration
                scheduleAutoDismiss(alarm)

                Log.d(TAG, "Alarm ringing started for: ${alarm.label ?: "Alarm"}")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to start alarm ringing", e)
                stopSelf()
            }
        }
    }

    private fun scheduleAutoDismiss(alarm: Alarm) {
        val durationMillis = alarm.alarmDurationMinutes * 60 * 1000L
        autoDismissJob = CoroutineScope(Dispatchers.Main).launch {
            delay(durationMillis)
            Log.d(TAG, "Auto-dismissing alarm after ${alarm.alarmDurationMinutes} minutes")

            // Emit to SharedFlow so the Activity (if alive) finishes itself
            _autoDismissFlow.emit(alarm.id)
            Log.d(TAG, "Auto-dismiss emitted for alarm ${alarm.id}")

            // Small delay to let Activity react, then dismiss service
            delay(300)
            dismissAlarm()
        }
    }

    private fun createNotification(alarm: Alarm): Notification {
        val fullScreenIntent = AlarmRingingActivity.createIntent(this, alarm.id)

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
            .setSmallIcon(R.drawable.ic_notification_icon)
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
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Read volume from settings
                val volume = settingsRepository.defaultVolume.first()
                Log.d(TAG, "Using volume from settings: $volume%")

                // Set system alarm volume based on settings volume
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_ALARM)

                if (volume <= 100) {
                    // For 1-100%, set system volume proportionally
                    val systemVolume = ((volume / 100.0) * maxVolume).toInt().coerceIn(1, maxVolume)
                    audioManager.setStreamVolume(android.media.AudioManager.STREAM_ALARM, systemVolume, 0)
                    Log.d(TAG, "System volume: $systemVolume/$maxVolume ($volume%)")
                } else {
                    // For >100%, set system volume to maximum (we'll use enhancer for boost)
                    audioManager.setStreamVolume(android.media.AudioManager.STREAM_ALARM, maxVolume, 0)
                    Log.d(TAG, "System volume: MAX, applying boost via LoudnessEnhancer")
                }

                mediaPlayer = MediaPlayer().apply {
                // Try to use custom ringtone first, fallback to default if it fails
                val ringtoneUri = alarm.ringtoneUri?.toUri()
                var dataSourceSet = false

                if (ringtoneUri != null) {
                    try {
                        setDataSource(this@AlarmRingingService, ringtoneUri)
                        dataSourceSet = true
                        Log.d(TAG, "Using custom ringtone: $ringtoneUri")
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to load custom ringtone, falling back to default", e)
                        reset() // Reset MediaPlayer after failed setDataSource
                    }
                }

                // If custom ringtone failed or wasn't set, use actual default alarm ringtone
                if (!dataSourceSet) {
                    val defaultUri = try {
                        val uri = RingtoneManager.getActualDefaultRingtoneUri(
                            this@AlarmRingingService,
                            RingtoneManager.TYPE_ALARM
                        )
                        uri ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not get actual default alarm ringtone, using notification", e)
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    }

                    try {
                        setDataSource(this@AlarmRingingService, defaultUri)
                        Log.d(TAG, "Using actual default alarm ringtone: $defaultUri")
                    } catch (e: Exception) {
                        // If even that fails, try system notification sound as last resort
                        Log.e(TAG, "Default alarm ringtone failed, trying notification sound", e)
                        reset()
                        val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        setDataSource(this@AlarmRingingService, notificationUri)
                    }
                }

                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )

                prepare()

                // Use LoudnessEnhancer for real volume boost
                if (volume > 100) {
                    try {
                        loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                            // Calculate gain in millibels
                            val ratio = volume / 100.0
                            val decibels = 20 * kotlin.math.log10(ratio)
                            val millibels = (decibels * 100).toInt().coerceIn(0, 3000)

                            Log.d(TAG, "LoudnessEnhancer: $volume% = +${millibels}mB (+${millibels/100}dB)")
                            setTargetGain(millibels)
                            enabled = true
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to apply LoudnessEnhancer", e)
                    }
                }

                isLooping = true
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start ringtone completely", e)
            // Still stop the service if we absolutely can't play any sound
            stopSelf()
        }
        }
    }

    private fun startVibration(alarm: Alarm) {
        vibrator?.let {
            val pattern = VibrationPattern.fromId(alarm.vibrationPatternId)
            val intensity = alarm.vibrationIntensity

            when (pattern) {
                VibrationPattern.Off -> { /* Do nothing - no vibration */ }
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
        Log.d(TAG, "dismissAlarm() called, currentAlarm: ${currentAlarm?.id}")
        val alarmId = currentAlarm?.id

        if (alarmId == null) {
            Log.e(TAG, "dismissAlarm() called but currentAlarm is null!")
        }

        _currentRingingAlarmId.value = null
        stopAllAlarmComponents()

        // Notify queue manager that this alarm finished
        if (alarmId != null) {
            Log.d(TAG, "Notifying AlarmQueueManager that alarm $alarmId finished")
            AlarmQueueManager.onAlarmFinished(this, alarmId)
        }

        Log.d(TAG, "Stopping service")
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

        val alarmId = currentAlarm?.id
        _currentRingingAlarmId.value = null
        stopAllAlarmComponents()

        // Notify queue manager that this alarm finished
        if (alarmId != null) {
            AlarmQueueManager.onAlarmFinished(this, alarmId)
        }

        stopSelf()
    }

    private fun stopAllAlarmComponents() {
        // Cancel auto-dismiss job
        autoDismissJob?.cancel()
        autoDismissJob = null

        // Stop LoudnessEnhancer
        loudnessEnhancer?.enabled = false
        loudnessEnhancer?.release()
        loudnessEnhancer = null

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
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
                Log.d(TAG, "Service WakeLock released")
            }
        }
        wakeLock = null
        Log.d(TAG, "AlarmRingingService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
