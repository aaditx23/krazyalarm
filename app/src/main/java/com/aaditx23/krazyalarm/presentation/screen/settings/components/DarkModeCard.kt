package com.aaditx23.krazyalarm.presentation.screen.settings.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DarkModeCard(
    isDarkMode: Boolean,
    onToggle: (Boolean) -> Unit
) {
    SettingsCard(
        title = "Dark Mode",
        subtitle = "Toggle Dark mode on or off",
        icon = Icons.Default.DarkMode
    ) {
        Switch(
            checked = isDarkMode,
            onCheckedChange = onToggle,

        )
    }
}

