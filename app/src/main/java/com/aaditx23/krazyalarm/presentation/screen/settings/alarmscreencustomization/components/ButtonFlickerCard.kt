package com.aaditx23.krazyalarm.presentation.screen.settings.alarmscreencustomization.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val FLICKER_OPTIONS = listOf(
    "Disabled"          to 0,
    "Very Slow (3s)"    to 3000,
    "Slow (2s)"         to 2000,
    "Medium (1s)"       to 1000,
    "Fast (500ms)"      to 500,
    "Very Fast (250ms)" to 250
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ButtonFlickerCard(
    flickerIntervalMs: Int,
    onFlickerIntervalChange: (Int) -> Unit
) {
    val selectedLabel = FLICKER_OPTIONS.firstOrNull { it.second == flickerIntervalMs }?.first
        ?: "Disabled"

    var expanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Button Flicker",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Buttons alternate visibility — only one is visible at a time",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    shape = RoundedCornerShape(16.dp),
                    value = selectedLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Flicker interval") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    FLICKER_OPTIONS.forEach { (label, ms) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                onFlickerIntervalChange(ms)
                                expanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }
        }
    }
}

