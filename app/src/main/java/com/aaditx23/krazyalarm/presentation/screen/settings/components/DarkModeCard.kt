package com.aaditx23.krazyalarm.presentation.screen.settings.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable

@Composable
fun DarkModeCard(
    isDarkMode: Boolean,
    onToggle: (Boolean) -> Unit
) {
    SettingsCard(
        title = "Dark Mode",
        icon = Icons.Default.DarkMode
    ) {
        Switch(
            checked = isDarkMode,
            onCheckedChange = onToggle
        )
    }
}

