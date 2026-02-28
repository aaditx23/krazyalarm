package com.aaditx23.krazyalarm.presentation.screen.settings.components

import android.content.Context
import android.media.RingtoneManager
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aaditx23.krazyalarm.util.AudioAmplifier
import kotlin.math.roundToInt

@Composable
fun VolumeCard(
    volume: Int,
    onVolumeChange: (Int) -> Unit
) {
    val context = LocalContext.current
    var sliderValue by remember(volume) { mutableFloatStateOf(volume.toFloat()) }
    val audioAmplifier = remember { AudioAmplifier(context) }
    var lastPlayedValue by remember { mutableStateOf(-1) }

    // Clean up audio amplifier when composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            audioAmplifier.stop()
        }
    }

    val volumeLabel = when {
        sliderValue < 50  -> "Low"
        sliderValue <= 100 -> "Normal"
        sliderValue <= 130 -> "High"
        sliderValue <= 160 -> "Overclocked"
        else               -> "Maximum"
    }

    val isOverclocked = sliderValue > 100

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    tint = if (isOverclocked) Color(0xFFFF6B35) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Volume",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "$volumeLabel (${sliderValue.roundToInt()}%)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isOverclocked) Color(0xFFFF6B35) else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isOverclocked) FontWeight.Bold else FontWeight.Normal
                        )
                        if (isOverclocked) {
                            Text(
                                text = "🔥",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                // Reset button
                TextButton(
                    onClick = {
                        sliderValue = 100f
                        onVolumeChange(100)
                        // Stop any playing sound
                        audioAmplifier.stop()
                        lastPlayedValue = -1
                    },
                    enabled = sliderValue.roundToInt() != 100
                ) {
                    Text(
                        text = "Reset",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Slider row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "1%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = sliderValue,
                    onValueChange = {
                        sliderValue = it
                        val currentValue = it.roundToInt()
                        if (currentValue != lastPlayedValue) {
                            lastPlayedValue = currentValue
                            audioAmplifier.stop()
                            playVolumePreview(context, audioAmplifier, currentValue)
                        }
                    },
                    onValueChangeFinished = {
                        val newVolume = sliderValue.roundToInt()
                        onVolumeChange(newVolume)
                        lastPlayedValue = -1
                    },
                    valueRange = 1f..200f,
                    steps = 198, // 200 discrete values (1–200)
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = if (isOverclocked) Color(0xFFFF6B35) else MaterialTheme.colorScheme.primary,
                        activeTrackColor = if (isOverclocked) Color(0xFFFF6B35) else MaterialTheme.colorScheme.primary
                    )
                )
                Text(
                    text = "200%",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (sliderValue > 100) Color(0xFFFF6B35) else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (sliderValue > 100) FontWeight.Bold else FontWeight.Normal
                )
            }

            // Overclock disclaimer
            if (isOverclocked) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFF6B35).copy(alpha = 0.12f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFF6B35),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "⚠️ Prolonged use above 100% can damage your speaker and battery.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFF6B35),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Test button
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    // Stop any current sound and play test
                    audioAmplifier.stop()
                    playVolumePreview(context, audioAmplifier, sliderValue.roundToInt())
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Test Volume (${sliderValue.roundToInt()}%)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun playVolumePreview(
    context: Context,
    audioAmplifier: AudioAmplifier,
    volume: Int
) {
    try {
        // Get the actual default alarm ringtone that the system uses
        val ringtoneUri = try {
            val uri = RingtoneManager.getActualDefaultRingtoneUri(
                context,
                RingtoneManager.TYPE_ALARM
            )
            uri ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        } catch (e: Exception) {
            android.util.Log.w("VolumeCard", "Could not get actual default alarm ringtone, using notification sound", e)
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }

        audioAmplifier.playWithAmplification(
            audioUri = ringtoneUri,
            volumePercent = volume,
            durationMs = 2000
        )
    } catch (e: Exception) {
        android.util.Log.e("VolumeCard", "Error playing volume preview", e)
    }
}
