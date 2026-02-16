package com.aaditx23.krazyalarm.presentation.screen.settings.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.runtime.Composable

@Composable
fun VibrationPatternsCard(
    selectedPattern: String = "Continuous",
    onClick: () -> Unit
) {
    SettingsNavigationCard(
        title = "Vibration Patterns",
        subtitle = selectedPattern,
        icon = Icons.Default.Vibration,
        onClick = onClick
    )
}
