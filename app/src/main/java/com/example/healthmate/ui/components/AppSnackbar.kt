package com.example.healthmate.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.healthmate.ui.theme.HealthMateTheme
import com.example.healthmate.ui.theme.Spacing

/**
 * A message for the Snackbar: a success ("Record saved", "Record updated", "Record deleted")
 * or a friendly error ("Could not delete the record. Please try again.").
 */
data class UserMessage(val text: String, val isError: Boolean = false)

/** Shows [message] in an [AppSnackbarHost] with the success or error style. */
suspend fun SnackbarHostState.showMessage(message: UserMessage) {
    showSnackbar(AppSnackbarVisuals(message))
}

private class AppSnackbarVisuals(val userMessage: UserMessage) : SnackbarVisuals {
    override val message: String get() = userMessage.text
    override val actionLabel: String? get() = null
    override val withDismissAction: Boolean get() = false
    override val duration: SnackbarDuration get() = SnackbarDuration.Short
}

/**
 * The app's Snackbar, in the spec's colours: near-black with white text (readable everywhere),
 * with a green check for success (`#12B76A`) or a red icon for errors (`#D92D20`).
 * The colour sits on the icon rather than the background because white text on that green
 * would be too low in contrast to read comfortably.
 */
@Composable
fun AppSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(hostState, modifier) { data ->
        val message = (data.visuals as? AppSnackbarVisuals)?.userMessage ?: UserMessage(data.visuals.message)
        AppSnackbar(message)
    }
}

@Composable
private fun AppSnackbar(message: UserMessage) {
    Snackbar(
        modifier = Modifier.padding(Spacing.sm),
        shape = MaterialTheme.shapes.small,
        containerColor = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (message.isError) Icons.Outlined.ErrorOutline else Icons.Outlined.CheckCircle,
                contentDescription = null, // the text says it all
                tint = if (message.isError) MaterialTheme.colorScheme.error else HealthMateTheme.extraColors.success,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(message.text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Preview
@Composable
private fun AppSnackbarPreview() {
    HealthMateTheme {
        Column {
            AppSnackbar(UserMessage("Record saved"))
            AppSnackbar(UserMessage("Could not delete the record. Please try again.", isError = true))
        }
    }
}
