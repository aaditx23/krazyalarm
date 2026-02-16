package com.aaditx23.krazyalarm.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.util.Log
import kotlin.math.log10
import kotlin.math.roundToInt

/**
 * AudioAmplifier - Provides real audio amplification using LoudnessEnhancer
 * This allows volume boost beyond system limits using Android's official audio effects API
 */
class AudioAmplifier(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    /**
     * Play audio with amplification using LoudnessEnhancer
     * @param audioUri URI of the audio file to play
     * @param volumePercent Volume percentage (1-150, where 100 = normal, 150 = max boost)
     * @param durationMs Maximum playback duration in milliseconds
     */
    fun playWithAmplification(
        audioUri: Uri,
        volumePercent: Int,
        durationMs: Long = 2000
    ) {
        try {
            stop()

            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_ALARM)

            // Set system volume to maximum for best quality
            audioManager.setStreamVolume(android.media.AudioManager.STREAM_ALARM, maxVolume, 0)

            // Determine which URI to use (with fallback to default)
            val finalUri = when {
                // Replace settings pointer with actual default ringtone
                audioUri.toString() == "content://settings/system/alarm_alert" -> {
                    Log.d(TAG, "Replacing settings pointer with actual default ringtone")
                    try {
                        val uri = android.media.RingtoneManager.getActualDefaultRingtoneUri(
                            context,
                            android.media.RingtoneManager.TYPE_ALARM
                        )
                        uri ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not get actual default, using notification", e)
                        android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                    }
                }
                // Media URIs can be used directly
                audioUri.toString().startsWith("content://media/") -> {
                    Log.d(TAG, "Using media URI directly: $audioUri")
                    audioUri
                }
                // For other URIs, validate before using
                else -> {
                    try {
                        context.contentResolver.openAssetFileDescriptor(audioUri, "r")?.close()
                        Log.d(TAG, "Using validated custom audio URI: $audioUri")
                        audioUri
                    } catch (e: Exception) {
                        Log.w(TAG, "Custom audio URI not accessible, using default alarm sound", e)
                        try {
                            val uri = android.media.RingtoneManager.getActualDefaultRingtoneUri(
                                context,
                                android.media.RingtoneManager.TYPE_ALARM
                            )
                            uri ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                        } catch (ex: Exception) {
                            Log.e(TAG, "Could not get default alarm ringtone", ex)
                            android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                        }
                    }
                }
            }

            val player = MediaPlayer().apply {
                var dataSourceSet = false

                // Get actual default alarm ringtone URI (not settings pointer, not first in list)
                val defaultUri = try {
                    val uri = android.media.RingtoneManager.getActualDefaultRingtoneUri(
                        context,
                        android.media.RingtoneManager.TYPE_ALARM
                    )
                    uri ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                } catch (e: Exception) {
                    Log.w(TAG, "Could not get actual default alarm ringtone, using notification", e)
                    android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                }

                // Try the finalUri first
                try {
                    setDataSource(context, finalUri)
                    dataSourceSet = true
                    Log.i(TAG, "✓ Audio source set: $finalUri")
                } catch (e: Exception) {
                    Log.w(TAG, "✗ Primary audio source failed (will try fallback): ${e.message}")
                }

                // If that failed and finalUri is not already the default, try default ringtone
                if (!dataSourceSet && finalUri.toString() != defaultUri.toString()) {
                    try {
                        reset()
                        setDataSource(context, defaultUri)
                        dataSourceSet = true
                        Log.i(TAG, "✓ Fallback audio source set: $defaultUri")
                    } catch (e: Exception) {
                        Log.e(TAG, "✗ Default alarm ringtone failed: ${e.message}")
                    }
                }

                // If still failed, try notification sound as last resort
                if (!dataSourceSet) {
                    try {
                        reset()
                        val notificationUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                        setDataSource(context, notificationUri)
                        dataSourceSet = true
                        Log.i(TAG, "✓ Last resort audio source set: notification sound")
                    } catch (e: Exception) {
                        Log.e(TAG, "✗ All audio sources failed", e)
                        throw Exception("Unable to set any audio source", e)
                    }
                }

                if (!dataSourceSet) {
                    throw Exception("Failed to set any valid audio source")
                }

                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )

                prepare()

                // Create LoudnessEnhancer with this player's audio session
                loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                    // Calculate gain in millibels
                    val gainMillibels = calculateGainMillibels(volumePercent)

                    Log.d(TAG, "Volume: $volumePercent%, Gain: $gainMillibels mB (${gainMillibels/100}dB)")

                    setTargetGain(gainMillibels)
                    enabled = true
                }

                start()
            }

            mediaPlayer = player

            // Auto-stop after duration - safely handle MediaPlayer state
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    // Cleanup LoudnessEnhancer first
                    try {
                        loudnessEnhancer?.enabled = false
                        loudnessEnhancer?.release()
                        loudnessEnhancer = null
                    } catch (e: Exception) {
                        Log.e(TAG, "Error releasing LoudnessEnhancer", e)
                    }

                    // Stop and release MediaPlayer - don't check isPlaying as it can throw exception
                    try {
                        player.stop()
                    } catch (e: Exception) {
                        // Player might already be stopped or released - that's fine
                        Log.d(TAG, "MediaPlayer stop called on already stopped/released player")
                    }

                    try {
                        player.release()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error releasing MediaPlayer", e)
                    }

                    // Clear our reference if it's still the same player
                    if (mediaPlayer == player) {
                        mediaPlayer = null
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in auto-stop handler", e)
                }
            }, durationMs)

        } catch (e: Exception) {
            Log.e(TAG, "Error playing amplified audio", e)
            stop()
        }
    }

    /**
     * Calculate gain in millibels for given volume percentage
     *
     * Formula: mB = 2000 * log10(volume / 100)
     * - 100% = 0 mB (no boost)
     * - 125% = ~1938 mB (~6 dB boost)
     * - 150% = ~3521 mB (~11.76 dB boost)
     */
    private fun calculateGainMillibels(volumePercent: Int): Int {
        return when {
            volumePercent <= 100 -> 0 // No boost needed
            else -> {
                val ratio = volumePercent / 100.0
                val decibels = 20 * log10(ratio) // 20*log10 for amplitude ratio
                val millibels = (decibels * 100).roundToInt()
                millibels.coerceIn(0, 3000) // Cap at +30dB to prevent extreme distortion
            }
        }
    }

    fun stop() {
        try {
            // Stop LoudnessEnhancer first
            try {
                loudnessEnhancer?.enabled = false
                loudnessEnhancer?.release()
            } catch (e: Exception) {
                Log.d(TAG, "Error releasing LoudnessEnhancer in stop()", e)
            }
            loudnessEnhancer = null

            // Stop MediaPlayer - don't check isPlaying to avoid IllegalStateException
            mediaPlayer?.let { player ->
                try {
                    player.stop()
                } catch (e: Exception) {
                    // Already stopped or in invalid state - that's okay
                    Log.d(TAG, "MediaPlayer already stopped")
                }

                try {
                    player.release()
                } catch (e: Exception) {
                    Log.e(TAG, "Error releasing MediaPlayer in stop()", e)
                }
            }
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio", e)
        }
    }

    companion object {
        private const val TAG = "AudioAmplifier"
    }
}
