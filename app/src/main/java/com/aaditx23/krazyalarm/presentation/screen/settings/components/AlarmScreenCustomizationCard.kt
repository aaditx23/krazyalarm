package com.aaditx23.krazyalarm.presentation.screen.settings.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.runtime.Composable

@Composable
fun AlarmScreenCustomizationCard(
    onClick: () -> Unit
) {
    SettingsNavigationCard(
        title = "Alarm Screen Customization",
        subtitle = "Button motion, flicker effect and preview",
        icon = Icons.Default.PhoneAndroid,
        onClick = onClick
    )
}

