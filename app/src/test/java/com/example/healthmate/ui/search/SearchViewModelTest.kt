package com.example.healthmate.ui.search

import com.example.healthmate.domain.RecordValidation
import com.example.healthmate.domain.SearchFilter
import com.example.healthmate.domain.SortOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Filter bottom sheet state (spec 4.6): date-range validation, sort order, Reset and Apply. */
class SearchViewModelTest {

    private val today = LocalDate.of(2026, 11, 22)
    private val nov1 = LocalDate.of(2026, 11, 1)
    private val nov15 = LocalDate.of(2026, 11, 15)

    private val viewModel = SearchViewModel(currentDate = { today })
    private val state get() = viewModel.uiState.value

    // ---------- Defaults ----------

    @Test
    fun initialState_noDates_newestFirst_sheetClosed() {
        assertEquals(SearchFilter(null, null, SortOrder.NEWEST_FIRST), state.filter)
        assertFalse(state.showFilterSheet)
        assertNull(state.dateRangeError)
        assertTrue(state.canApply)
        assertEquals(today, state.today)
    }

    @Test
    fun openAndDismiss_togglesSheet_keepsFilters() {
        viewModel.openFilters()
        assertTrue(state.showFilterSheet)

        viewModel.onSortOrderChange(SortOrder.HIGHEST_WATER)
        viewModel.dismissFilters()
        assertFalse(state.showFilterSheet)

        viewModel.openFilters()
        assertEquals(SortOrder.HIGHEST_WATER, state.filter.sortOrder)
    }

    // ---------- Date-range validation ----------

    @Test
    fun endAfterStart_isValid() {
        viewModel.onStartDateChange(nov1)
        viewModel.onEndDateChange(nov15)

        assertNull(state.dateRangeError)
        assertTrue(state.canApply)
    }

    @Test
    fun endSameAsStart_isValid() {
        viewModel.onStartDateChange(nov15)
        viewModel.onEndDateChange(nov15)

        assertNull(state.dateRangeError)
        assertTrue(state.canApply)
    }

    @Test
    fun endBeforeStart_showsError_andDisablesApply() {
        viewModel.onStartDateChange(nov15)
        viewModel.onEndDateChange(nov1)

        assertEquals("End date must be after start date", state.dateRangeError)
        assertFalse(state.canApply)
    }

    @Test
    fun movingStartAfterEnd_showsError() {
        viewModel.onEndDateChange(nov1)
        assertNull(state.dateRangeError)

        viewModel.onStartDateChange(nov15)
        assertEquals(RecordValidation.ERROR_DATE_RANGE, state.dateRangeError)
    }

    @Test
    fun fixingTheRange_clearsError() {
        viewModel.onStartDateChange(nov15)
        viewModel.onEndDateChange(nov1)
        viewModel.onEndDateChange(today)

        assertNull(state.dateRangeError)
        assertTrue(state.canApply)
    }

    @Test
    fun onlyOneDate_isValid() {
        viewModel.onStartDateChange(nov15)
        assertNull(state.dateRangeError)

        viewModel.resetFilters()
        viewModel.onEndDateChange(nov1)
        assertNull(state.dateRangeError)
    }

    // ---------- Sort order ----------

    @Test
    fun everySortOption_canBeSelected() {
        SortOrder.entries.forEach { option ->
            viewModel.onSortOrderChange(option)
            assertEquals(option, state.filter.sortOrder)
        }
    }

    @Test
    fun changingSort_keepsDates() {
        viewModel.onStartDateChange(nov1)
        viewModel.onEndDateChange(nov15)
        viewModel.onSortOrderChange(SortOrder.HIGHEST_CALORIES)

        assertEquals(SearchFilter(nov1, nov15, SortOrder.HIGHEST_CALORIES), state.filter)
    }

    @Test
    fun sortLabels_matchSpec() {
        assertEquals(
            listOf("Newest first", "Oldest first", "Highest steps", "Highest calories", "Highest water"),
            SortOrder.entries.map { it.label }
        )
    }

    // ---------- Reset ----------

    @Test
    fun reset_clearsDates_andRestoresNewestFirst() {
        viewModel.openFilters()
        viewModel.onStartDateChange(nov15)
        viewModel.onEndDateChange(nov1)
        viewModel.onSortOrderChange(SortOrder.OLDEST_FIRST)

        viewModel.resetFilters()

        assertEquals(SearchFilter(), state.filter)
        assertNull(state.dateRangeError)
        assertTrue("Reset keeps the sheet open", state.showFilterSheet)
    }

    // ---------- Apply ----------

    @Test
    fun apply_validFilters_returnsThem_andClosesSheet() {
        viewModel.openFilters()
        viewModel.onStartDateChange(nov1)
        viewModel.onEndDateChange(nov15)
        viewModel.onSortOrderChange(SortOrder.HIGHEST_STEPS)

        val applied = viewModel.applyFilters()

        assertEquals(SearchFilter(nov1, nov15, SortOrder.HIGHEST_STEPS), applied)
        assertFalse(state.showFilterSheet)
    }

    @Test
    fun apply_noFilters_returnsDefaults() {
        viewModel.openFilters()
        assertEquals(SearchFilter(), viewModel.applyFilters())
    }

    @Test
    fun apply_invalidRange_returnsNull_andKeepsSheetOpen() {
        viewModel.openFilters()
        viewModel.onStartDateChange(nov15)
        viewModel.onEndDateChange(nov1)

        assertNull(viewModel.applyFilters())
        assertTrue(state.showFilterSheet)
        assertEquals(RecordValidation.ERROR_DATE_RANGE, state.dateRangeError)
    }
}
