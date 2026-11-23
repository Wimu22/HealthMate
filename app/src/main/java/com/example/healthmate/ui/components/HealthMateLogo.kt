package com.example.healthmate.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.healthmate.R
import com.example.healthmate.ui.theme.HealthMateTheme
import com.example.healthmate.ui.theme.Spacing

/**
 * The HealthMate logo: the same mark as the launcher icon (white heart with a heartbeat line and a
 * yellow plus, from `R.drawable.ic_healthmate_mark`) on a rounded primary-blue square.
 */
@Composable
fun HealthMateLogo(modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Box(
        modifier = modifier
            .size(size)
            .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium),
        contentAlignment = Alignment.Center
    ) {
        // Image, not Icon: Icon would tint the whole mark one colour and lose the blue and yellow.
        Image(
            painter = painterResource(R.drawable.ic_healthmate_mark),
            contentDescription = null, // decorative: the app name is always shown next to it
            modifier = Modifier.size(size * 0.75f)
        )
    }
}

/** Logo followed by the app name, used as the Dashboard header. */
@Composable
fun HealthMateBrand(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        HealthMateLogo()
        Text(
            text = "HealthMate",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HealthMateBrandPreview() {
    HealthMateTheme {
        HealthMateBrand(Modifier.padding(Spacing.md))
    }
}
