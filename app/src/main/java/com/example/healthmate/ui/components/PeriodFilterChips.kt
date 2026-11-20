package com.example.healthmate.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.healthmate.domain.SummaryPeriod
import com.example.healthmate.ui.theme.HealthMateTheme
import com.example.healthmate.ui.theme.Spacing

val SummaryPeriod.label: String
    get() = when (this) {
        SummaryPeriod.ONE_DAY -> "1 Day"
        SummaryPeriod.SEVEN_DAYS -> "7 Days"
        SummaryPeriod.THIRTY_DAYS -> "30 Days"
        SummaryPeriod.THREE_MONTHS -> "3 Months"
    }

/**
 * "1 Day | 7 Days | 30 Days | 3 Months" chips of equal width. The selected one is filled blue.
 *
 * Drawn with a selectable Surface rather than Material's FilterChip: FilterChip's fixed inner
 * padding left too little room in a quarter of the screen, and "3 Months" was cut to "3".
 * Labels never wrap; on very narrow screens or large font sizes they end with "…" instead.
 */
@Composable
fun PeriodFilterChips(
    selected: SummaryPeriod,
    onSelect: (SummaryPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        SummaryPeriod.entries.forEach { period ->
            val isSelected = period == selected
            Surface(
                selected = isSelected,
                onClick = { onSelect(period) },
                shape = MaterialTheme.shapes.small,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
                    .semantics { role = Role.Tab }
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.sm),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = period.label,
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PeriodFilterChipsPreview() {
    HealthMateTheme {
        var selected by remember { mutableStateOf(SummaryPeriod.SEVEN_DAYS) }
        PeriodFilterChips(selected, onSelect = { selected = it }, modifier = Modifier.padding(Spacing.md))
    }
}
