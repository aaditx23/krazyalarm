package com.aaditx23.krazyalarm.presentation.screen.alarm_ringing.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun PulsingAlarmIcon(
    scale: Float,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .size(120.dp)
            .scale(scale),
        shape = CircleShape,
        color = Color(0xFFFF6B35).copy(alpha = 0.2f)
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Alarm,
                contentDescription = "Alarm",
                tint = Color(0xFFFF6B35),
                modifier = Modifier.size(64.dp)
            )
        }
    }
}
