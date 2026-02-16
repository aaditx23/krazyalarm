package com.aaditx23.krazyalarm.presentation.screen.settings.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.runtime.Composable

@Composable
fun LEDPatternsCard(
    selectedPattern: String = "No Flash",
    onClick: () -> Unit
) {
    SettingsNavigationCard(
        title = "LED Patterns",
        subtitle = selectedPattern,
        icon = Icons.Default.FlashOn,
        onClick = onClick
    )
}
