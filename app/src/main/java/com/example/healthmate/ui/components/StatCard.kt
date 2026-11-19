package com.example.healthmate.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.healthmate.ui.theme.BorderWidth
import com.example.healthmate.ui.theme.HealthMateTheme
import com.example.healthmate.ui.theme.Spacing

/** The three things HealthMate tracks, with the icon, unit and colour each one always uses. */
enum class HealthStat(val label: String, val unit: String, val icon: ImageVector) {
    STEPS("Steps", "steps", Icons.AutoMirrored.Filled.DirectionsWalk),
    CALORIES("Calories Burned", "kcal", Icons.Filled.LocalFireDepartment),
    WATER("Water Intake", "ml", Icons.Filled.WaterDrop);

    val color: Color
        @Composable get() = when (this) {
            STEPS -> HealthMateTheme.extraColors.steps
            CALORIES -> HealthMateTheme.extraColors.calories
            WATER -> HealthMateTheme.extraColors.water
        }
}

/**
 * A card showing one statistic: coloured icon, label and a large bold number with its unit.
 * Used for the Dashboard totals and on the Record View screen.
 */
@Composable
fun StatCard(
    label: String,
    value: String,
    unit: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(BorderWidth, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(Spacing.md))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
        }
    }
}

/** Shortcut for the standard stats: `StatCard(HealthStat.STEPS, 8450)`. */
@Composable
fun StatCard(
    stat: HealthStat,
    value: Number,
    modifier: Modifier = Modifier,
    label: String = stat.label
) {
    StatCard(
        label = label,
        value = formatNumber(value),
        unit = stat.unit,
        icon = stat.icon,
        color = stat.color,
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun StatCardPreview() {
    HealthMateTheme {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            StatCard(HealthStat.STEPS, 58_420, Modifier.fillMaxWidth(), label = "Total Steps")
            StatCard(HealthStat.CALORIES, 2_870, Modifier.fillMaxWidth(), label = "Total Calories Burned")
            StatCard(HealthStat.WATER, 15_350, Modifier.fillMaxWidth(), label = "Total Water Intake")
        }
    }
}
