package com.aaditx23.krazyalarm.presentation.screen.settings.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun SnoozeDurationCard(
    snoozeDuration: Int,
    onUpdateDuration: (Int) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var tempValue by remember { mutableStateOf(snoozeDuration.toString()) }

    SettingsNavigationCard(
        title = "Snooze Duration",
        subtitle = "$snoozeDuration minutes",
        icon = Icons.Default.AccessTime,
        onClick = {
            tempValue = snoozeDuration.toString()
            showDialog = true
        }
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Snooze Duration", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
            text = {
                OutlinedTextField(
                    value = tempValue,
                    onValueChange = { newValue ->
                        // Only allow numbers
                        if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                            tempValue = newValue
                        }
                    },
                    label = { Text("Minutes") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    supportingText = { Text("Enter value between 1 and 60") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        tempValue.toIntOrNull()?.let { minutes ->
                            if (minutes in 1..60) {
                                onUpdateDuration(minutes)
                                showDialog = false
                            }
                        }
                    },
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDialog = false },
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

