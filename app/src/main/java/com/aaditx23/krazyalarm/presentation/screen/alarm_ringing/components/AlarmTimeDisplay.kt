package com.aaditx23.krazyalarm.presentation.screen.alarm_ringing.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AlarmTimeDisplay(
    dateString: String,
    currentTime: String,
    scheduledTime: String,
    alarmStartTime: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Date
        Text(
            text = dateString,
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.titleMedium,
            fontSize = 18.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Current time (large display)
        Text(
            text = currentTime,
            color = Color.White,
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            fontSize = 72.sp,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Scheduled time
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Scheduled for: ",
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 15.sp
            )
            Text(
                text = scheduledTime,
                color = Color(0xFFFFD700), // Gold color
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Alarm trigger time
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Triggered at: ",
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 15.sp
            )
            Text(
                text = alarmStartTime,
                color = Color(0xFF4ECDC4), // Cyan/turquoise color
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }
    }
}
