package com.example.healthmate.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.healthmate.domain.SearchFilter
import com.example.healthmate.domain.SortOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate

/** Everything the Filter bottom sheet shows, in one immutable object. */
data class SearchUiState(
    val filter: SearchFilter = SearchFilter(),
    val showFilterSheet: Boolean = false,
    /** Latest date that can be picked: records can't be in the future. */
    val today: LocalDate = LocalDate.now()
) {
    /** Shown under the end date: "End date must be after start date". */
    val dateRangeError: String? get() = filter.dateRangeError

    /** Apply is disabled while the date range is invalid. */
    val canApply: Boolean get() = dateRangeError == null
}

/**
 * Holds the search filters (spec 4.6): start date, end date and sort order.
 *
 * The filters live here, not in the sheet, so they survive screen rotation and are still filled in
 * when the user opens the sheet again. Every change re-checks the date range, so the error appears
 * (and Apply is disabled) as soon as the end date is before the start date.
 *
 * [currentDate] is a parameter so tests can fix "today" to a known date.
 */
class SearchViewModel(
    private val currentDate: () -> LocalDate = { LocalDate.now() }
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState(today = currentDate()))
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    /** Filters button or search bar tapped. */
    fun openFilters() = _uiState.update { it.copy(showFilterSheet = true, today = currentDate()) }

    /** Sheet closed by swiping down or tapping outside; the chosen filters are kept. */
    fun dismissFilters() = _uiState.update { it.copy(showFilterSheet = false) }

    fun onStartDateChange(date: LocalDate) = updateFilter { it.copy(startDate = date) }

    fun onEndDateChange(date: LocalDate) = updateFilter { it.copy(endDate = date) }

    fun onSortOrderChange(sortOrder: SortOrder) = updateFilter { it.copy(sortOrder = sortOrder) }

    /** Reset: clears both dates and goes back to Newest first. */
    fun resetFilters() = updateFilter { SearchFilter() }

    /**
     * Apply tapped. Returns the filters to search with and closes the sheet,
     * or returns null (and keeps the sheet open with its error) if the date range is invalid.
     */
    fun applyFilters(): SearchFilter? {
        val filter = _uiState.value.filter
        if (filter.dateRangeError != null) return null
        _uiState.update { it.copy(showFilterSheet = false) }
        return filter
    }

    private fun updateFilter(change: (SearchFilter) -> SearchFilter) =
        _uiState.update { it.copy(filter = change(it.filter)) }

    companion object {
        /** Builds the ViewModel (the app has no dependency-injection library). */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { SearchViewModel() }
        }
    }
}
