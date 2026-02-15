package com.aaditx23.krazyalarm.presentation.screen.alarm_list.DetailsModal.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DaySelectorSection(
    selectedDays: Int,
    onDaysChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val dayNames = listOf("M", "T", "W", "T", "F", "S", "S")
        dayNames.forEachIndexed { index, dayName ->
            val dayBit = 1 shl index
            val isSelected = (selectedDays and dayBit) != 0

            Box(
                modifier = Modifier
                    .weight(1f)
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)
                    )
                    .clickable {
                        val newDays = if (isSelected) {
                            selectedDays and dayBit.inv()
                        } else {
                            selectedDays or dayBit
                        }
                        onDaysChange(newDays)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dayName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Normal,
                        fontSize = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
