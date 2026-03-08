package com.aaditx23.krazyalarm.presentation.screen.settings.alarmscreencustomization

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aaditx23.krazyalarm.presentation.screen.settings.SettingsViewModel
import com.aaditx23.krazyalarm.presentation.screen.settings.alarmscreencustomization.components.ButtonFlickerCard
import com.aaditx23.krazyalarm.presentation.screen.settings.alarmscreencustomization.components.ButtonMotionSpeedCard
import com.aaditx23.krazyalarm.presentation.screen.settings.components.SettingsNavigationCard
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
                title = { Text("Alarm Screen Customization") },
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
            SettingsNavigationCard(
                title = "Preview Alarm Screen",
                subtitle = "Preview the alarm ringing screen with current settings",
                icon = Icons.Default.Alarm,
                onClick = onTestAlarm
            )
        }
    }
}

