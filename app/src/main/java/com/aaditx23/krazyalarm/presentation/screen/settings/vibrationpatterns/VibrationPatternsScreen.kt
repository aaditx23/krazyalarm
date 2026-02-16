package com.aaditx23.krazyalarm.presentation.screen.settings.vibrationpatterns

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aaditx23.krazyalarm.domain.models.VibrationPattern
import com.aaditx23.krazyalarm.presentation.screen.settings.SettingsViewModel
import com.aaditx23.krazyalarm.presentation.screen.settings.vibrationpatterns.components.VibrationDurationDropdown
import com.aaditx23.krazyalarm.presentation.screen.settings.vibrationpatterns.components.VibrationPatternCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VibrationPatternsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    val selectedPattern = VibrationPattern.fromId(uiState.defaultVibrationPattern)
    var isPlaying by remember { mutableStateOf(false) }
    var previewDuration by remember { mutableStateOf(3) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vibration Patterns") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Select and preview vibration patterns for alarms",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Duration Dropdown
            VibrationDurationDropdown(
                selectedDuration = previewDuration,
                onDurationChange = { previewDuration = it }
            )

            // Pattern Cards
            VibrationPattern.getAll().forEach { pattern ->
                VibrationPatternCard(
                    pattern = pattern,
                    isSelected = selectedPattern.id == pattern.id,
                    isPlaying = isPlaying && selectedPattern.id == pattern.id,
                    onSelect = {
                        viewModel.updateVibrationPattern(pattern.id)
                    },
                    onPreview = {
                        if (!isPlaying) {
                            isPlaying = true
                            scope.launch {
                                playVibrationPattern(context, pattern, previewDuration)
                                isPlaying = false
                            }
                        }
                    },
                    enabled = !isPlaying
                )
            }
        }
    }
}

private suspend fun playVibrationPattern(context: Context, pattern: VibrationPattern, durationSeconds: Int) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    } ?: return

    try {
        val endTime = System.currentTimeMillis() + (durationSeconds * 1000L)

        when (pattern) {
            VibrationPattern.Continuous -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(durationSeconds * 1000L, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(durationSeconds * 1000L)
                }
            }
            VibrationPattern.Pulse -> {
                while (System.currentTimeMillis() < endTime) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(500)
                    }
                    delay(500)
                }
            }
            VibrationPattern.Escalating -> {
                var currentTime = System.currentTimeMillis()
                var duration = 200L
                while (currentTime < endTime) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(duration)
                    }
                    delay(duration + 200)
                    currentTime = System.currentTimeMillis()
                    duration = minOf(duration + 100, 800L)
                }
            }
            VibrationPattern.Heartbeat -> {
                while (System.currentTimeMillis() < endTime) {
                    // First beat
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(150)
                    }
                    delay(150)
                    // Second beat
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(150)
                    }
                    delay(800)
                }
            }
            VibrationPattern.Wave -> {
                while (System.currentTimeMillis() < endTime) {
                    // Crescendo
                    for (duration in listOf(100L, 150L, 200L, 250L, 200L, 150L, 100L)) {
                        if (System.currentTimeMillis() >= endTime) break
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator.vibrate(duration)
                        }
                        delay(duration + 50)
                    }
                    delay(300)
                }
            }
        }
        vibrator.cancel()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

