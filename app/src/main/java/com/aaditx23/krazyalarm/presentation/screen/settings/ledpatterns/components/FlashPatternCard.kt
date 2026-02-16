package com.aaditx23.krazyalarm.presentation.screen.settings.ledpatterns.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aaditx23.krazyalarm.domain.models.FlashPattern

@Composable
fun FlashPatternCard(
    pattern: FlashPattern,
    isSelected: Boolean,
    isPlaying: Boolean,
    onSelect: () -> Unit,
    onPreview: () -> Unit,
    enabled: Boolean = true
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onSelect,
        enabled = enabled,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            CardDefaults.outlinedCardBorder().copy(
                width = 2.dp
            )
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.FlashOn,
                    contentDescription = null,
                    tint = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                Column {
                    Text(
                        text = pattern.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Text(
                        text = getPatternDescription(pattern),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            if (pattern != FlashPattern.None) {
                FilledTonalIconButton(
                    onClick = onPreview,
                    enabled = enabled && !isPlaying
                ) {
                    if (isPlaying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Preview"
                        )
                    }
                }
            }
        }
    }
}

private fun getPatternDescription(pattern: FlashPattern): String {
    return when (pattern) {
        FlashPattern.None -> "No flash during alarm"
        FlashPattern.AlwaysOn -> "Constant flash light"
        FlashPattern.SosBlink -> "Emergency SOS pattern"
        FlashPattern.Strobe -> "Rapid flashing"
        FlashPattern.Pulse -> "Slow pulsing"
        FlashPattern.Heartbeat -> "Double beat pattern"
    }
}

