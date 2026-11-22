package com.example.healthmate.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healthmate.data.local.HealthRecord
import com.example.healthmate.domain.SearchFilter
import com.example.healthmate.domain.SortOrder
import com.example.healthmate.ui.components.AppTopBar
import com.example.healthmate.ui.components.ConfirmDeleteDialog
import com.example.healthmate.ui.components.EmptyState
import com.example.healthmate.ui.components.RecordCard
import com.example.healthmate.ui.theme.HealthMateTheme
import com.example.healthmate.ui.theme.Spacing
import java.time.LocalDate

/**
 * Search Results screen, spec 4.7.
 *
 * This stateful version gets its own [SearchViewModel] (which reads the filters from the route),
 * passes on Snackbar messages and shows the Filter sheet and delete dialog. All drawing of the
 * chips and list happens in the stateless [SearchResultsContent].
 */
@Composable
fun SearchResultsScreen(
    onBack: () -> Unit,
    onOpenRecord: (recordId: Long) -> Unit,
    onEditRecord: (recordId: Long) -> Unit,
    onShowMessage: (message: String) -> Unit,
    viewModel: SearchViewModel = viewModel(factory = SearchViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // "Record deleted" (or a friendly error): hand it to the app-wide Snackbar once, then clear it.
    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            onShowMessage(it)
            viewModel.onMessageShown()
        }
    }

    SearchResultsContent(
        state = uiState,
        onBack = onBack,
        onChangeFilters = viewModel::openFilters,
        onClearFilters = viewModel::clearFilters,
        onOpenRecord = onOpenRecord,
        onEditRecord = onEditRecord,
        onDeleteRecord = viewModel::onDeleteClick
    )

    if (uiState.showFilterSheet) {
        FilterBottomSheet(
            state = uiState,
            onDismiss = viewModel::dismissFilters,
            onStartDateChange = viewModel::onStartDateChange,
            onEndDateChange = viewModel::onEndDateChange,
            onSortOrderChange = viewModel::onSortOrderChange,
            onReset = viewModel::resetFilters,
            // Already on Search Results: the ViewModel searches again in place, no navigation.
            onApply = { viewModel.applyFilters() }
        )
    }

    uiState.recordToDelete?.let { record ->
        ConfirmDeleteDialog(
            onConfirm = viewModel::confirmDelete,
            onDismiss = viewModel::onDismissDelete,
            recordDate = record.date
        )
    }
}

@Composable
fun SearchResultsContent(
    state: SearchUiState,
    onBack: () -> Unit,
    onChangeFilters: () -> Unit,
    onClearFilters: () -> Unit,
    onOpenRecord: (Long) -> Unit,
    onEditRecord: (Long) -> Unit,
    onDeleteRecord: (HealthRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val applied = state.appliedFilter ?: SearchFilter()
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(title = "Search Results", onBack = onBack, actions = {
                IconButton(onClick = onChangeFilters) {
                    Icon(Icons.Outlined.Tune, contentDescription = "Change filters")
                }
            })
        }
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            AppliedFilterChips(
                filter = applied,
                showClear = state.hasActiveFilters,
                onChange = onChangeFilters,
                onClear = onClearFilters
            )

            val contentModifier = Modifier.fillMaxSize()
            when {
                state.isLoading -> Box(contentModifier, contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                state.loadError != null -> EmptyState(
                    title = state.loadError,
                    modifier = contentModifier,
                    actionLabel = "Reset filters",
                    onAction = onClearFilters
                )

                state.results.isEmpty() -> EmptyState(
                    title = "No records found.",
                    message = if (state.hasActiveFilters) "Try a different date range." else null,
                    icon = Icons.Outlined.SearchOff,
                    modifier = contentModifier,
                    // Resetting only helps when something is filtered out.
                    actionLabel = if (state.hasActiveFilters) "Reset filters" else null,
                    onAction = onClearFilters
                )

                else -> LazyColumn(
                    modifier = contentModifier,
                    contentPadding = PaddingValues(
                        start = Spacing.screenHorizontal,
                        end = Spacing.screenHorizontal,
                        bottom = Spacing.md
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    item(key = "count") {
                        Text(
                            text = if (state.results.size == 1) "1 record" else "${state.results.size} records",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(state.results, key = { it.id }) { record ->
                        RecordCard(
                            record = record,
                            onClick = { onOpenRecord(record.id) },
                            onEdit = { onEditRecord(record.id) },
                            onDelete = { onDeleteRecord(record) } // ignored by the ViewModel while a delete runs
                        )
                    }
                }
            }
        }
    }
}

/**
 * The applied filters as chips, e.g. [📅 1 Nov – 15 Nov 2026] [⇅ Highest steps]  Clear.
 * Tapping a chip opens the Filter sheet to change it; Clear goes back to all records, newest first.
 * FlowRow wraps the chips onto a second line on narrow screens.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppliedFilterChips(
    filter: SearchFilter,
    showClear: Boolean,
    onChange: () -> Unit,
    onClear: () -> Unit
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            label = dateRangeLabel(filter.startDate, filter.endDate),
            icon = Icons.Outlined.CalendarToday,
            onClick = onChange,
            description = "Date range. Tap to change filters."
        )
        FilterChip(
            label = filter.sortOrder.label,
            icon = Icons.AutoMirrored.Outlined.Sort,
            onClick = onChange,
            description = "Sort order. Tap to change filters."
        )
        if (showClear) {
            TextButton(onClick = onClear) { Text("Clear") }
        }
    }
}

/** Light-yellow chip (the spec's secondary colour for chips). */
@Composable
private fun FilterChip(label: String, icon: ImageVector, onClick: () -> Unit, description: String) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = description, modifier = Modifier.size(AssistChipDefaults.IconSize)) },
        shape = MaterialTheme.shapes.small,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
            leadingIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        border = null
    )
}

// ---------- Previews ----------

private val previewFilter = SearchFilter(
    startDate = LocalDate.of(2026, 11, 1),
    endDate = LocalDate.of(2026, 11, 15),
    sortOrder = SortOrder.HIGHEST_STEPS
)

@Preview(showBackground = true, heightDp = 640)
@Composable
private fun SearchResultsPreview() {
    HealthMateTheme {
        SearchResultsContent(
            state = SearchUiState(
                appliedFilter = previewFilter,
                results = listOf(
                    HealthRecord(id = 3, date = "2026-11-12", steps = 13_840, calories = 655, water = 2_950),
                    HealthRecord(id = 2, date = "2026-11-04", steps = 12_310, calories = 610, water = 2_900),
                    HealthRecord(id = 1, date = "2026-11-09", steps = 9_120, calories = 380, water = 1_800)
                )
            ),
            onBack = {}, onChangeFilters = {}, onClearFilters = {},
            onOpenRecord = {}, onEditRecord = {}, onDeleteRecord = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 640)
@Composable
private fun SearchResultsEmptyPreview() {
    HealthMateTheme {
        SearchResultsContent(
            state = SearchUiState(appliedFilter = previewFilter),
            onBack = {}, onChangeFilters = {}, onClearFilters = {},
            onOpenRecord = {}, onEditRecord = {}, onDeleteRecord = {}
        )
    }
}
