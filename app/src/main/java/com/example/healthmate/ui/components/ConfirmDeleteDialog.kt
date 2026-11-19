package com.example.healthmate.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.example.healthmate.ui.theme.HealthMateTheme

/**
 * Asks before deleting. "Delete" (red) permanently removes the record; "Cancel" or tapping
 * outside the dialog closes it without changes.
 */
@Composable
fun ConfirmDeleteDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    recordDate: String? = null
) {
    val message = if (recordDate != null) {
        "The record for ${formatRecordDate(recordDate)} will be permanently removed. This can't be undone."
    } else {
        "This record will be permanently removed. This can't be undone."
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("Delete record?") },
        text = { Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large
    )
}

@Preview(showBackground = true)
@Composable
private fun ConfirmDeleteDialogPreview() {
    HealthMateTheme {
        ConfirmDeleteDialog(onConfirm = {}, onDismiss = {}, recordDate = "2026-11-18")
    }
}
