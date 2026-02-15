package com.aaditx23.krazyalarm.presentation.screen.alarm_list.DetailsModal.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DayOfWeekSelector(
    selectedDays: Int, // bitmask
    onDaysChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val dayNames = listOf("M", "T", "W", "T", "F", "S", "S")

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        dayNames.forEachIndexed { index, dayName ->
            val dayBit = 1 shl index
            val isSelected = (selectedDays and dayBit) != 0

            FilterChip(
                selected = isSelected,
                onClick = {
                    val newDays = if (isSelected) {
                        selectedDays and dayBit.inv() // Remove the bit
                    } else {
                        selectedDays or dayBit // Add the bit
                    }
                    onDaysChange(newDays)
                },
                label = { Text(dayName) }
            )
        }
    }
}
