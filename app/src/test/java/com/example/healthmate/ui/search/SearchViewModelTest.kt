package com.example.healthmate.ui.search

import com.example.healthmate.data.local.HealthRecord
import com.example.healthmate.data.repository.HealthRecordRepository
import com.example.healthmate.domain.RecordValidation
import com.example.healthmate.domain.SearchFilter
import com.example.healthmate.domain.SortOrder
import com.example.healthmate.testutil.FakeHealthRecordDao
import com.example.healthmate.ui.components.UserMessage
import com.example.healthmate.testutil.MainDispatcherRule
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

/**
 * Filter bottom sheet state (spec 4.6): date-range validation, sort order, Reset and Apply;
 * and Search Results (spec 4.7): searching with the applied filters, Clear/Reset, and Delete.
 */
class SearchViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(dispatcher)

    private val today = LocalDate.of(2026, 11, 22)
    private val nov1 = LocalDate.of(2026, 11, 1)
    private val nov15 = LocalDate.of(2026, 11, 15)

    private val nov5 = record(1, "2026-11-05")
    private val nov10 = record(2, "2026-11-10")
    private val nov20 = record(3, "2026-11-20")
    private val dao = FakeHealthRecordDao(listOf(nov5, nov10, nov20))
    private val repository = HealthRecordRepository(dao, ioDispatcher = dispatcher)

    /** Dashboard instance: no applied filters, so nothing is searched. */
    private val viewModel = SearchViewModel(repository, initialFilter = null, currentDate = { today })
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

    // ---------- Dashboard instance doesn't search ----------

    @Test
    fun dashboardInstance_neverQueriesTheDatabase() {
        viewModel.openFilters()
        viewModel.onSortOrderChange(SortOrder.HIGHEST_STEPS)
        viewModel.applyFilters()

        assertTrue(dao.searchCalls.isEmpty())
        assertNull(state.appliedFilter)
        assertFalse(state.isLoading)
    }

    // ---------- Search Results ----------

    private fun resultsViewModel(filter: SearchFilter) =
        SearchViewModel(repository, initialFilter = filter, currentDate = { today })

    @Test
    fun results_searchWithRouteFilters_inDatabaseFormat() {
        val vm = resultsViewModel(SearchFilter(nov1, nov15, SortOrder.HIGHEST_STEPS))

        assertEquals(
            FakeHealthRecordDao.SearchCall("2026-11-01", "2026-11-15", "HIGHEST_STEPS"),
            dao.searchCalls.single()
        )
        assertEquals(listOf(nov10, nov5), vm.uiState.value.results)
        assertFalse(vm.uiState.value.isLoading)
        assertTrue(vm.uiState.value.hasActiveFilters)
    }

    @Test
    fun results_sheetStartsWithTheAppliedFilters() {
        val filter = SearchFilter(nov1, nov15, SortOrder.OLDEST_FIRST)
        val vm = resultsViewModel(filter)

        assertEquals(filter, vm.uiState.value.filter)
    }

    @Test
    fun results_noDates_searchesWithoutLimits() {
        val vm = resultsViewModel(SearchFilter())

        assertEquals(FakeHealthRecordDao.SearchCall(null, null, "NEWEST_FIRST"), dao.searchCalls.single())
        assertEquals(3, vm.uiState.value.results.size)
        assertFalse("Defaults are not active filters", vm.uiState.value.hasActiveFilters)
    }

    @Test
    fun results_nothingMatches_isEmpty() {
        val vm = resultsViewModel(SearchFilter(startDate = today, endDate = today))

        assertTrue(vm.uiState.value.results.isEmpty())
        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.loadError)
    }

    @Test
    fun results_applyNewFiltersInSheet_searchesAgainInPlace() {
        val vm = resultsViewModel(SearchFilter(nov1, nov15))
        vm.openFilters()
        vm.onStartDateChange(nov15)
        vm.onEndDateChange(today)
        vm.onSortOrderChange(SortOrder.HIGHEST_WATER)

        val applied = vm.applyFilters()

        assertEquals(SearchFilter(nov15, today, SortOrder.HIGHEST_WATER), applied)
        assertEquals(applied, vm.uiState.value.appliedFilter)
        assertEquals(
            FakeHealthRecordDao.SearchCall("2026-11-15", "2026-11-22", "HIGHEST_WATER"),
            dao.searchCalls.last()
        )
        assertEquals(listOf(nov20), vm.uiState.value.results)
        assertFalse(vm.uiState.value.showFilterSheet)
    }

    @Test
    fun results_dismissSheetWithoutApply_keepsResults() {
        val vm = resultsViewModel(SearchFilter(nov1, nov15))
        vm.openFilters()
        vm.onSortOrderChange(SortOrder.HIGHEST_STEPS)
        vm.dismissFilters()

        assertEquals(SearchFilter(nov1, nov15), vm.uiState.value.appliedFilter)
        assertEquals(1, dao.searchCalls.size)
    }

    @Test
    fun results_clearFilters_showsAllRecordsNewestFirst() {
        val vm = resultsViewModel(SearchFilter(nov1, nov15, SortOrder.HIGHEST_STEPS))

        vm.clearFilters()

        assertEquals(SearchFilter(), vm.uiState.value.appliedFilter)
        assertEquals(SearchFilter(), vm.uiState.value.filter)
        assertEquals(FakeHealthRecordDao.SearchCall(null, null, "NEWEST_FIRST"), dao.searchCalls.last())
        assertEquals(listOf(nov20, nov10, nov5), vm.uiState.value.results)
        assertFalse(vm.uiState.value.hasActiveFilters)
    }

    @Test
    fun results_databaseError_showsFriendlyMessage() {
        dao.failSearch = true
        val vm = resultsViewModel(SearchFilter())

        assertEquals("Could not search your records. Please try again.", vm.uiState.value.loadError)
        assertFalse(vm.uiState.value.isLoading)
    }

    // ---------- Delete from Search Results ----------

    @Test
    fun delete_asksFirst_thenRemovesRecord_andRefreshes() {
        val vm = resultsViewModel(SearchFilter(nov1, nov15))

        vm.onDeleteClick(nov5)
        assertEquals(nov5, vm.uiState.value.recordToDelete)
        assertEquals("Nothing deleted before confirming", 2, vm.uiState.value.results.size)

        vm.confirmDelete()

        assertNull(vm.uiState.value.recordToDelete)
        assertEquals(listOf(nov10), vm.uiState.value.results)
        assertEquals(UserMessage("Record deleted", isError = false), vm.uiState.value.message)

        vm.onMessageShown()
        assertNull(vm.uiState.value.message)
    }

    @Test
    fun delete_cancel_keepsRecord() {
        val vm = resultsViewModel(SearchFilter())
        vm.onDeleteClick(nov5)
        vm.onDismissDelete()

        assertNull(vm.uiState.value.recordToDelete)
        assertEquals(3, vm.uiState.value.results.size)
    }

    @Test
    fun delete_databaseError_showsFriendlyMessage_andKeepsRecord() {
        dao.failDelete = true
        val vm = resultsViewModel(SearchFilter())
        vm.onDeleteClick(nov5)
        vm.confirmDelete()

        assertEquals(UserMessage("Could not delete the record. Please try again.", isError = true), vm.uiState.value.message)
        assertFalse(vm.uiState.value.isDeleting)
        assertEquals(3, vm.uiState.value.results.size)
    }

    // ---------- Chip labels ----------

    @Test
    fun dateRangeLabel_allCases() {
        val uk = Locale.UK
        assertEquals("All dates", dateRangeLabel(null, null, uk))
        assertEquals("1 Nov – 15 Nov 2026", dateRangeLabel(nov1, nov15, uk))
        assertEquals(
            "28 Dec 2025 – 3 Jan 2026",
            dateRangeLabel(LocalDate.of(2025, 12, 28), LocalDate.of(2026, 1, 3), uk)
        )
        assertEquals("15 Nov 2026", dateRangeLabel(nov15, nov15, uk))
        assertEquals("From 1 Nov 2026", dateRangeLabel(nov1, null, uk))
        assertEquals("Until 15 Nov 2026", dateRangeLabel(null, nov15, uk))
    }

    private fun record(id: Long, date: String) =
        HealthRecord(id = id, date = date, steps = 8_000, calories = 400, water = 2_000)
}
