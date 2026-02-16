package com.aaditx23.krazyalarm.presentation.screen.settings.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.runtime.Composable

@Composable
fun VibrationPatternsCard(
    onClick: () -> Unit
) {
    SettingsNavigationCard(
        title = "Vibration Patterns",
        subtitle = "Configure vibration patterns",
        icon = Icons.Default.Vibration,
        onClick = onClick
    )
}

