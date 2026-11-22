package com.example.healthmate.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.healthmate.domain.SearchFilter
import com.example.healthmate.domain.SortOrder
import com.example.healthmate.ui.components.DatePickerField
import com.example.healthmate.ui.components.PrimaryButton
import com.example.healthmate.ui.components.SecondaryButton
import com.example.healthmate.ui.theme.HealthMateTheme
import com.example.healthmate.ui.theme.Spacing
import java.time.LocalDate

/** Text for each sort option, as shown in the sheet (and later in the Search Results chips). */
val SortOrder.label: String
    get() = when (this) {
        SortOrder.NEWEST_FIRST -> "Newest first"
        SortOrder.OLDEST_FIRST -> "Oldest first"
        SortOrder.HIGHEST_STEPS -> "Highest steps"
        SortOrder.HIGHEST_CALORIES -> "Highest calories"
        SortOrder.HIGHEST_WATER -> "Highest water"
    }

/**
 * Filter bottom sheet, spec 4.6: date range, sort order, Reset and Apply.
 *
 * Stateless: it shows [state] and reports every change through callbacks; the filters themselves
 * are kept in [SearchViewModel]. Swiping down or tapping outside calls [onDismiss].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    state: SearchUiState,
    onDismiss: () -> Unit,
    onStartDateChange: (LocalDate) -> Unit,
    onEndDateChange: (LocalDate) -> Unit,
    onSortOrderChange: (SortOrder) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        // Open fully: the whole form is visible at once instead of a half-height sheet.
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        FilterSheetContent(
            state = state,
            onStartDateChange = onStartDateChange,
            onEndDateChange = onEndDateChange,
            onSortOrderChange = onSortOrderChange,
            onReset = onReset,
            onApply = onApply
        )
    }
}

/** The inside of the sheet, separate so it can be previewed. */
@Composable
fun FilterSheetContent(
    state: SearchUiState,
    onStartDateChange: (LocalDate) -> Unit,
    onEndDateChange: (LocalDate) -> Unit,
    onSortOrderChange: (SortOrder) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filter = state.filter
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()) // fits small phones and landscape
            .padding(horizontal = Spacing.screenHorizontal)
            .padding(bottom = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Text(
            text = "Filters",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() }
        )

        // ---------- Date range ----------
        SectionLabel("Date range")
        DatePickerField(
            value = filter.startDate,
            onDateSelected = onStartDateChange,
            label = "Start date",
            placeholder = "Any",
            maxDate = state.today
        )
        DatePickerField(
            value = filter.endDate,
            onDateSelected = onEndDateChange,
            label = "End date",
            placeholder = "Any",
            // The calendar greys out days before the start date; the error below still covers
            // the case where the start date is moved after an end date that was already chosen.
            minDate = filter.startDate?.takeIf { !it.isAfter(state.today) },
            maxDate = state.today,
            error = state.dateRangeError
        )

        // ---------- Sort by ----------
        SectionLabel("Sort by")
        Column(Modifier.selectableGroup()) {
            SortOrder.entries.forEach { option ->
                SortOptionRow(
                    option = option,
                    selected = option == filter.sortOrder,
                    onSelect = { onSortOrderChange(option) }
                )
            }
        }

        // ---------- Buttons ----------
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            SecondaryButton(text = "Reset", onClick = onReset, modifier = Modifier.weight(1f))
            PrimaryButton(
                text = "Apply",
                onClick = onApply,
                enabled = state.canApply,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** One radio option; the whole row is tappable, not just the small circle. */
@Composable
private fun SortOptionRow(option: SortOrder, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null) // the row handles the tap
        Spacer(Modifier.width(Spacing.sm))
        Text(option.label, style = MaterialTheme.typography.bodyLarge)
    }
}

// ---------- Previews ----------

@Preview(showBackground = true, heightDp = 760)
@Composable
private fun FilterSheetPreview() {
    HealthMateTheme {
        FilterSheetContent(
            state = SearchUiState(
                filter = SearchFilter(sortOrder = SortOrder.HIGHEST_STEPS),
                today = LocalDate.of(2026, 11, 22)
            ),
            onStartDateChange = {}, onEndDateChange = {}, onSortOrderChange = {}, onReset = {}, onApply = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 760)
@Composable
private fun FilterSheetInvalidRangePreview() {
    HealthMateTheme {
        FilterSheetContent(
            state = SearchUiState(
                filter = SearchFilter(startDate = LocalDate.of(2026, 11, 15), endDate = LocalDate.of(2026, 11, 1)),
                today = LocalDate.of(2026, 11, 22)
            ),
            onStartDateChange = {}, onEndDateChange = {}, onSortOrderChange = {}, onReset = {}, onApply = {}
        )
    }
}
