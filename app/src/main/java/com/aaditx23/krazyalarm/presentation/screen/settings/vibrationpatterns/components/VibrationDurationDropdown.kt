package com.aaditx23.krazyalarm.presentation.screen.settings.vibrationpatterns.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VibrationDurationDropdown(
    selectedDuration: Int,
    onDurationChange: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val durations = (1..10).toList()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Preview Duration",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "$selectedDuration second${if (selectedDuration > 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                    ) {
                        Text("$selectedDuration sec")
                    }

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        durations.forEach { duration ->
                            DropdownMenuItem(
                                text = { Text("$duration second${if (duration > 1) "s" else ""}") },
                                onClick = {
                                    onDurationChange(duration)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

