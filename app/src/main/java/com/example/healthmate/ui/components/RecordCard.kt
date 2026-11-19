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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.healthmate.data.local.HealthRecord
import com.example.healthmate.ui.theme.BorderWidth
import com.example.healthmate.ui.theme.HealthMateTheme
import com.example.healthmate.ui.theme.Spacing
import java.io.File

/**
 * A list item for one record: photo thumbnail (or a calendar icon), date, and the three values.
 * Tapping the card opens the record. Edit and Delete icons appear only when their callbacks are given,
 * so the same card works for Recent Records (no actions) and All Records / Search Results (with actions).
 */
@Composable
fun RecordCard(
    record: HealthRecord,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(BorderWidth, MaterialTheme.colorScheme.outline)
    ) {
        Column(Modifier.padding(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RecordThumbnail(record.photoPath)
                Spacer(Modifier.width(Spacing.md))
                Text(
                    text = formatRecordDate(record.date),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (onEdit != null) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = "Edit record",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Delete record",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            Spacer(Modifier.size(Spacing.md))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatValue(HealthStat.STEPS, record.steps)
                StatValue(HealthStat.CALORIES, record.calories)
                StatValue(HealthStat.WATER, record.water)
            }
        }
    }
}

@Composable
private fun RecordThumbnail(photoPath: String?) {
    val shape = MaterialTheme.shapes.small
    if (photoPath != null) {
        AsyncImage(
            model = File(photoPath),
            contentDescription = "Selfie",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
    } else {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, shape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.CalendarToday,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/** Small coloured icon + value + unit, e.g. "🔥 420 kcal". */
@Composable
private fun StatValue(stat: HealthStat, value: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(stat.icon, contentDescription = stat.label, tint = stat.color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(Spacing.xs))
        Text(
            text = formatNumber(value),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.width(2.dp))
        Text(
            text = stat.unit,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RecordCardPreview() {
    HealthMateTheme {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            RecordCard(
                record = HealthRecord(id = 1, date = "2026-11-18", steps = 9_840, calories = 455, water = 2_350),
                onClick = {}
            )
            RecordCard(
                record = HealthRecord(id = 2, date = "2026-11-17", steps = 12_310, calories = 610, water = 2_900),
                onClick = {},
                onEdit = {},
                onDelete = {}
            )
        }
    }
}
