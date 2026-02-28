package com.aaditx23.krazyalarm.presentation.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aaditx23.krazyalarm.domain.models.FlashPattern
import com.aaditx23.krazyalarm.domain.models.VibrationPattern
import com.aaditx23.krazyalarm.presentation.screen.settings.components.*
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToLEDPatterns: () -> Unit = {},
    onNavigateToVibrationPatterns: () -> Unit = {},
    onNavigateToAlarmScreenCustomization: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val isSystemInDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()

    val actualIsDarkMode = when (uiState.darkModeValue) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
            // Dark Mode Card
            DarkModeCard(
                isDarkMode = actualIsDarkMode,
                onToggle = { viewModel.toggleDarkMode(it) }
            )

            // Snooze Duration Card
            SnoozeDurationCard(
                snoozeDuration = uiState.snoozeDuration,
                onUpdateDuration = { viewModel.updateSnoozeDuration(it) }
            )

            // LED/Flash Patterns Card
            LEDPatternsCard(
                selectedPattern = FlashPattern.fromId(uiState.defaultFlashPattern).displayName,
                onClick = onNavigateToLEDPatterns
            )

            // Vibration Patterns Card
            VibrationPatternsCard(
                selectedPattern = VibrationPattern.fromId(uiState.defaultVibrationPattern).displayName,
                onClick = onNavigateToVibrationPatterns
            )

            // Alarm Screen Customization Card
            AlarmScreenCustomizationCard(
                onClick = onNavigateToAlarmScreenCustomization
            )

            // Alarm Duration Card
            AlarmDurationCard(
                durationMinutes = uiState.alarmDurationMinutes,
                onDurationChange = { viewModel.updateAlarmDuration(it) }
            )

            // Volume Slider
            VolumeCard(
                volume = uiState.defaultVolume,
                onVolumeChange = { viewModel.updateVolume(it) }
            )
        }
    }
}
