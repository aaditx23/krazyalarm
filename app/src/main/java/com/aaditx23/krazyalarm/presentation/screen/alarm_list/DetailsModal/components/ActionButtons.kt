package com.aaditx23.krazyalarm.presentation.screen.alarm_list.DetailsModal.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ActionButtons(
    onDelete: () -> Unit,
    onSave: () -> Unit,
    isEditMode: Boolean,
    isLoading: Boolean,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left Button: Delete in edit mode, Cancel otherwise
        OutlinedButton(
            onClick = if (isEditMode) onDelete else onDismiss,
            shape = RoundedCornerShape(24.dp),
            colors = if (isEditMode) ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ) else ButtonDefaults.outlinedButtonColors()
        ) {
            Text(
                text = if (isEditMode) "Delete" else "Cancel",
            )
        }

        // Save Button
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.BottomEnd
        ){
            Button(
                onClick = onSave,
                enabled = !isLoading,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Text(
                    text = "Save",
                )
            }
        }
    }
}
