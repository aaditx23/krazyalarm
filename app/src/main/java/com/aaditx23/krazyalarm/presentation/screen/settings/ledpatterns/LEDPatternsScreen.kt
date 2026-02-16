package com.aaditx23.krazyalarm.presentation.screen.settings.ledpatterns

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
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
import androidx.core.content.ContextCompat
import com.aaditx23.krazyalarm.domain.models.FlashPattern
import com.aaditx23.krazyalarm.presentation.screen.settings.SettingsViewModel
import com.aaditx23.krazyalarm.presentation.screen.settings.ledpatterns.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LEDPatternsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    val selectedPattern = FlashPattern.fromId(uiState.defaultFlashPattern)
    var isPlaying by remember { mutableStateOf(false) }
    var previewDuration by remember { mutableStateOf(3) }
    val hasCameraPermission = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LED Flash Patterns") },
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
                text = "Select and preview flash patterns for alarms",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Duration Dropdown
            DurationDropdown(
                selectedDuration = previewDuration,
                onDurationChange = { previewDuration = it }
            )

            // Pattern Cards
            FlashPattern.getAll().forEach { pattern ->
                FlashPatternCard(
                    pattern = pattern,
                    isSelected = selectedPattern.id == pattern.id,
                    isPlaying = isPlaying && selectedPattern.id == pattern.id,
                    onSelect = {
                        viewModel.updateFlashPattern(pattern.id)
                    },
                    onPreview = {
                        if (!isPlaying && hasCameraPermission) {
                            isPlaying = true
                            scope.launch {
                                playFlashPattern(context, pattern, previewDuration)
                                isPlaying = false
                            }
                        }
                    },
                    enabled = !isPlaying
                )
            }

            if (!hasCameraPermission) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "Camera permission is required to preview flash patterns",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

private suspend fun playFlashPattern(context: Context, pattern: FlashPattern, durationSeconds: Int) {
    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return

    try {
        val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return
        val endTime = System.currentTimeMillis() + (durationSeconds * 1000L)

        when (pattern) {
            FlashPattern.None -> { /* Do nothing */ }
            FlashPattern.AlwaysOn -> {
                cameraManager.setTorchMode(cameraId, true)
                delay(durationSeconds * 1000L)
                cameraManager.setTorchMode(cameraId, false)
            }
            FlashPattern.SosBlink -> {
                while (System.currentTimeMillis() < endTime) {
                    // S (3 short)
                    repeat(3) {
                        cameraManager.setTorchMode(cameraId, true)
                        delay(200)
                        cameraManager.setTorchMode(cameraId, false)
                        delay(200)
                    }
                    delay(400)
                    // O (3 long)
                    repeat(3) {
                        cameraManager.setTorchMode(cameraId, true)
                        delay(600)
                        cameraManager.setTorchMode(cameraId, false)
                        delay(200)
                    }
                    delay(400)
                    // S (3 short)
                    repeat(3) {
                        cameraManager.setTorchMode(cameraId, true)
                        delay(200)
                        cameraManager.setTorchMode(cameraId, false)
                        delay(200)
                    }
                    delay(1000)
                }
                cameraManager.setTorchMode(cameraId, false)
            }
            FlashPattern.Strobe -> {
                while (System.currentTimeMillis() < endTime) {
                    cameraManager.setTorchMode(cameraId, true)
                    delay(100)
                    cameraManager.setTorchMode(cameraId, false)
                    delay(100)
                }
            }
            FlashPattern.Pulse -> {
                while (System.currentTimeMillis() < endTime) {
                    cameraManager.setTorchMode(cameraId, true)
                    delay(500)
                    cameraManager.setTorchMode(cameraId, false)
                    delay(500)
                }
            }
            FlashPattern.Heartbeat -> {
                while (System.currentTimeMillis() < endTime) {
                    // First beat
                    cameraManager.setTorchMode(cameraId, true)
                    delay(150)
                    cameraManager.setTorchMode(cameraId, false)
                    delay(150)
                    // Second beat
                    cameraManager.setTorchMode(cameraId, true)
                    delay(150)
                    cameraManager.setTorchMode(cameraId, false)
                    delay(800)
                }
            }
        }
    } catch (e: CameraAccessException) {
        e.printStackTrace()
    }
}

