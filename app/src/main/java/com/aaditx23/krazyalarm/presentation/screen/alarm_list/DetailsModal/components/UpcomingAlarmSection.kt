package com.aaditx23.krazyalarm.presentation.screen.alarm_list.DetailsModal.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun UpcomingAlarmSection(
    hour: Int,
    minute: Int,
    days: Int
) {
    Column {
        Text(
            text = "Upcoming alarm",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))

        // Calculate next alarm time
        val upcomingText = if (days == 0) {
            "Tomorrow"
        } else {
            // Simple logic for now - just show "Tomorrow" or day name
            "Tomorrow" // TODO: Calculate actual next alarm day
        }

        Text(
            text = upcomingText,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
