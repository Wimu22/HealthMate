package com.example.healthmate.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healthmate.data.local.HealthRecord
import com.example.healthmate.domain.HealthSummary
import com.example.healthmate.domain.SortOrder
import com.example.healthmate.domain.SummaryPeriod
import com.example.healthmate.ui.components.EmptyState
import com.example.healthmate.ui.components.HealthMateBrand
import com.example.healthmate.ui.components.HealthStat
import com.example.healthmate.ui.components.PeriodFilterChips
import com.example.healthmate.ui.components.PrimaryButton
import com.example.healthmate.ui.components.RecordCard
import com.example.healthmate.ui.components.SecondaryButton
import com.example.healthmate.ui.components.StatCard
import com.example.healthmate.ui.components.toLongDisplayDate
import com.example.healthmate.ui.theme.BorderWidth
import com.example.healthmate.ui.theme.HealthMateTheme
import com.example.healthmate.ui.theme.Spacing
import java.time.LocalDate

/**
 * Dashboard (Home) screen, spec 4.1.
 *
 * This stateful version gets the ViewModel and collects its state; all drawing happens in the
 * stateless [DashboardContent], which only takes plain data and callbacks (easy to preview/test).
 * Navigation is passed in as lambdas, so this screen never sees the NavController.
 */
@Composable
fun DashboardScreen(
    onAddRecord: () -> Unit,
    onAllRecords: () -> Unit,
    onOpenRecord: (Long) -> Unit,
    onSearch: (startDate: String?, endDate: String?, sortOrder: SortOrder) -> Unit,
    viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory)
) {
    // Collects only while the screen is visible, so DB queries pause when the app is in the background.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Placeholder until the Filter bottom sheet (spec 4.6) is built: a dialog that can only
    // "search all records". The sheet will replace it and call the same onSearch.
    var showFilters by rememberSaveable { mutableStateOf(false) }

    DashboardContent(
        state = uiState,
        onSelectPeriod = viewModel::selectPeriod,
        onAddRecord = onAddRecord,
        onAllRecords = onAllRecords,
        onOpenRecord = onOpenRecord,
        onOpenFilters = { showFilters = true }
    )

    if (showFilters) {
        AlertDialog(
            onDismissRequest = { showFilters = false },
            icon = { Icon(Icons.Outlined.Tune, contentDescription = null) },
            title = { Text("Filters") },
            text = { Text("Date range and sort filters are coming soon.") },
            confirmButton = {
                TextButton(onClick = {
                    showFilters = false
                    onSearch(null, null, SortOrder.NEWEST_FIRST)
                }) { Text("Show all records") }
            },
            dismissButton = {
                TextButton(onClick = { showFilters = false }) { Text("Close") }
            }
        )
    }
}

@Composable
fun DashboardContent(
    state: DashboardUiState,
    onSelectPeriod: (SummaryPeriod) -> Unit,
    onAddRecord: () -> Unit,
    onAllRecords: () -> Unit,
    onOpenRecord: (Long) -> Unit,
    onOpenFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        // One scrolling list for the whole screen, so it also fits small phones and landscape.
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = Spacing.screenHorizontal, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // ---------- Header ----------
            item(key = "header") {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    HealthMateBrand()
                    // IntrinsicSize.Min + fillMaxHeight: both buttons always share the same height,
                    // even if a large font size makes one label wrap.
                    Row(
                        modifier = Modifier.height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        PrimaryButton(
                            text = "Add New Record",
                            onClick = onAddRecord,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        SecondaryButton(
                            text = "All Records",
                            onClick = onAllRecords,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                    Text(
                        text = "Today – ${state.today.toLongDisplayDate()}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ---------- Summary ----------
            item(key = "summary_title") { SectionTitle("Summary") }
            item(key = "period_chips") {
                PeriodFilterChips(selected = state.selectedPeriod, onSelect = onSelectPeriod)
            }

            if (state.errorMessage != null) {
                item(key = "error") { ErrorCard(state.errorMessage) }
            } else {
                item(key = "stat_steps") {
                    StatCard(HealthStat.STEPS, state.summary.totalSteps, Modifier.fillMaxWidth(), label = "Total Steps")
                }
                item(key = "stat_calories") {
                    StatCard(HealthStat.CALORIES, state.summary.totalCalories, Modifier.fillMaxWidth(), label = "Total Calories Burned")
                }
                item(key = "stat_water") {
                    StatCard(HealthStat.WATER, state.summary.totalWater, Modifier.fillMaxWidth(), label = "Total Water Intake")
                }
                if (!state.isLoading && !state.hasTodayRecord) {
                    item(key = "today_prompt") { AddTodayPrompt(onAddRecord) }
                }
            }

            // ---------- Search ----------
            item(key = "search") { SearchBarRow(onOpenFilters) }

            // ---------- Recent Records ----------
            item(key = "recent_title") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionTitle("Recent Records", Modifier.weight(1f))
                    TextButton(onClick = onAllRecords) { Text("See all") }
                }
            }
            when {
                state.isLoading -> item(key = "loading") {
                    Box(Modifier.fillMaxWidth().padding(Spacing.lg), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.errorMessage == null && state.recentRecords.isEmpty() -> item(key = "empty") {
                    EmptyState(
                        title = "No records yet",
                        message = "Start tracking your steps, calories and water.",
                        actionLabel = "Add New Record",
                        actionIcon = Icons.Filled.Add,
                        onAction = onAddRecord
                    )
                }
                else -> items(state.recentRecords, key = { it.id }) { record ->
                    RecordCard(record = record, onClick = { onOpenRecord(record.id) })
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.semantics { heading() }
    )
}

/** Shown when there is no record for today (spec 4.1). Light yellow = secondary highlight colour. */
@Composable
private fun AddTodayPrompt(onAddRecord: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(start = Spacing.md, end = Spacing.sm, top = Spacing.sm, bottom = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "You haven't added today's record yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onAddRecord) { Text("Add now") }
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(Modifier.padding(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(Spacing.sm))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}

/**
 * Looks like a search field, but tapping it (or Filters) opens the filters, because HealthMate
 * searches by date range and sort order rather than by typed text (spec 4.6).
 */
@Composable
private fun SearchBarRow(onOpenFilters: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Surface(
            onClick = onOpenFilters,
            modifier = Modifier
                .weight(1f)
                .height(SearchRowHeight),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(BorderWidth, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    text = "Search records",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        SecondaryButton(
            text = "Filters",
            onClick = onOpenFilters,
            icon = Icons.Outlined.Tune,
            modifier = Modifier.height(SearchRowHeight)
        )
    }
}

private val SearchRowHeight = 52.dp

// ---------- Previews ----------

private val previewRecords = (0L until 5L).map { i ->
    HealthRecord(
        id = i + 1,
        date = LocalDate.of(2026, 11, 18).minusDays(i).toString(),
        steps = 8_000 + (i * 750).toInt(),
        calories = 420 + (i * 30).toInt(),
        water = 2_100 + (i * 150).toInt()
    )
}

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun DashboardContentPreview() {
    HealthMateTheme {
        DashboardContent(
            state = DashboardUiState(
                today = LocalDate.of(2026, 11, 19),
                selectedPeriod = SummaryPeriod.SEVEN_DAYS,
                summary = HealthSummary(totalSteps = 58_420, totalCalories = 2_870, totalWater = 15_400),
                recentRecords = previewRecords,
                hasTodayRecord = false,
                isLoading = false
            ),
            onSelectPeriod = {}, onAddRecord = {}, onAllRecords = {}, onOpenRecord = {}, onOpenFilters = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun DashboardContentEmptyPreview() {
    HealthMateTheme {
        DashboardContent(
            state = DashboardUiState(today = LocalDate.of(2026, 11, 19), isLoading = false),
            onSelectPeriod = {}, onAddRecord = {}, onAllRecords = {}, onOpenRecord = {}, onOpenFilters = {}
        )
    }
}
