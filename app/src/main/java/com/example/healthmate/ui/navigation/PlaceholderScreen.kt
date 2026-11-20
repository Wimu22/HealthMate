package com.example.healthmate.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.healthmate.ui.components.AppTopBar
import com.example.healthmate.ui.components.PrimaryButton
import com.example.healthmate.ui.components.SecondaryButton
import com.example.healthmate.ui.theme.HealthMateTheme
import com.example.healthmate.ui.theme.Spacing

/** One button on a placeholder screen: a label and where it leads. */
data class PlaceholderAction(val label: String, val onClick: () -> Unit)

/**
 * Temporary stand-in for a screen that is not built yet. It shows the screen name, any
 * navigation arguments it received, and buttons for the moves section 6 of the spec allows,
 * so the whole navigation graph can be tried out before the real screens exist.
 * Each one is replaced by the real screen as that feature is built.
 */
@Composable
fun PlaceholderScreen(
    title: String,
    modifier: Modifier = Modifier,
    details: String? = null,
    onBack: (() -> Unit)? = null,
    actions: List<PlaceholderAction> = emptyList()
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title = title, onBack = onBack) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Text(
                text = "$title screen – coming soon",
                style = MaterialTheme.typography.titleMedium
            )
            if (details != null) {
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            actions.forEachIndexed { index, action ->
                // First action is the main one (primary style), the rest are secondary.
                if (index == 0) {
                    PrimaryButton(action.label, action.onClick, Modifier.fillMaxWidth())
                } else {
                    SecondaryButton(action.label, action.onClick, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaceholderScreenPreview() {
    HealthMateTheme {
        PlaceholderScreen(
            title = "Record View",
            details = "Record id: 1",
            onBack = {},
            actions = listOf(
                PlaceholderAction("Add New") {},
                PlaceholderAction("Edit") {},
                PlaceholderAction("Delete") {}
            )
        )
    }
}
