package com.example.healthmate.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
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

/** "1 Day | 7 Days | 30 Days | 3 Months" chips of equal width. The selected one is filled blue. */
@Composable
fun PeriodFilterChips(
    selected: SummaryPeriod,
    onSelect: (SummaryPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        SummaryPeriod.entries.forEach { period ->
            val isSelected = period == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(period) },
                label = {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = period.label,
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                },
                shape = MaterialTheme.shapes.small,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                ),
                border = null,
                modifier = Modifier.weight(1f)
            )
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
