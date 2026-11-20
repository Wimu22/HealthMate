
package com.example.healthmate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.healthmate.ui.navigation.HealthMateNavHost
import com.example.healthmate.ui.theme.HealthMateTheme

/**
 * The app's single activity and its entry point (the launcher activity in AndroidManifest.xml).
 * Every screen is a composable drawn inside this activity; [HealthMateNavHost] switches
 * between them.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Draw behind the status and navigation bars; each screen's Scaffold adds the insets as padding.
        enableEdgeToEdge()
        setContent {
            HealthMateTheme {
                HealthMateNavHost()
            }
        }
    }
}
