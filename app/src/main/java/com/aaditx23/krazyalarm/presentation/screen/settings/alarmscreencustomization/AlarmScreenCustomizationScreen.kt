package com.aaditx23.krazyalarm.presentation.screen.settings.alarmscreencustomization

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aaditx23.krazyalarm.presentation.screen.settings.SettingsViewModel
import com.aaditx23.krazyalarm.presentation.screen.settings.alarmscreencustomization.components.ButtonFlickerCard
import com.aaditx23.krazyalarm.presentation.screen.settings.alarmscreencustomization.components.ButtonMotionSpeedCard
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmScreenCustomizationScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
    onTestAlarm: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alarm Screen") },
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
            // Button Motion Speed
            ButtonMotionSpeedCard(
                speed = uiState.buttonMotionSpeed,
                onSpeedChange = { viewModel.updateButtonMotionSpeed(it) }
            )

            // Button Flicker
            ButtonFlickerCard(
                flickerIntervalMs = uiState.buttonFlickerIntervalMs,
                onFlickerIntervalChange = { viewModel.updateButtonFlickerInterval(it) }
            )

            // Preview Alarm Screen
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = onTestAlarm
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Preview Alarm Screen",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Preview the alarm ringing screen with current settings",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = "Preview",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

