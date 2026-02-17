package com.aaditx23.krazyalarm.presentation.screen.alarm_ringing.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun AlarmInfo(
    label: String?,
    hour: Int,
    minute: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Alarm label
        Text(
            text = label?.takeIf { it.isNotBlank() } ?: "Alarm",
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            fontSize = 28.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Alarm time
        val alarmTimeText = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
        Text(
            text = "Scheduled for $alarmTimeText",
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodyLarge,
            fontSize = 16.sp
        )
    }
}
