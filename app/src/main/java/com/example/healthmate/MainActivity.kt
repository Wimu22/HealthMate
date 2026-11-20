package com.example.healthmate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.healthmate.ui.components.AppTopBar
import com.example.healthmate.ui.components.EmptyState
import com.example.healthmate.ui.theme.HealthMateTheme

/**
 * The app's single activity and its entry point (the launcher activity in AndroidManifest.xml).
 * Every screen is a composable drawn inside this activity; Navigation Compose will switch
 * between them once the nav graph is added.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Draw behind the status and navigation bars; Scaffold adds the matching insets as padding.
        enableEdgeToEdge()
        setContent {
            HealthMateTheme {
                PlaceholderScreen()
            }
        }
    }
}

/** Temporary start screen, replaced by the navigation graph and Dashboard next. */
@Composable
private fun PlaceholderScreen() {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title = "HealthMate") }
    ) { innerPadding ->
        EmptyState(
            title = "Welcome to HealthMate",
            message = "Your dashboard is coming soon.",
            icon = Icons.Outlined.FavoriteBorder,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PlaceholderScreenPreview() {
    HealthMateTheme {
        PlaceholderScreen()
    }
}
