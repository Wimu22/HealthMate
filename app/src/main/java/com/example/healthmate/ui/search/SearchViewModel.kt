package com.example.healthmate.ui.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.healthmate.data.local.HealthMateDatabase
import com.example.healthmate.data.local.HealthRecord
import com.example.healthmate.data.repository.HealthRecordRepository
import com.example.healthmate.domain.SearchFilter
import com.example.healthmate.domain.SortOrder
import com.example.healthmate.ui.navigation.Screen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Everything the Filter sheet and the Search Results screen show, in one immutable object. */
data class SearchUiState(
    /** What the Filter sheet is editing (not searched with until Apply). */
    val filter: SearchFilter = SearchFilter(),
    val showFilterSheet: Boolean = false,
    /** Latest date that can be picked: records can't be in the future. */
    val today: LocalDate = LocalDate.now(),

    // ---------- Search Results only ----------
    /** The filters the results were found with; null on the Dashboard, where nothing is searched. */
    val appliedFilter: SearchFilter? = null,
    val results: List<HealthRecord> = emptyList(),
    val isLoading: Boolean = false,
    val loadError: String? = null,
    /** Not null → the delete confirmation dialog is open for this record. */
    val recordToDelete: HealthRecord? = null,
    val isDeleting: Boolean = false,
    /** A one-off message for the Snackbar ("Record deleted" or a friendly error). */
    val message: String? = null
) {
    /** Shown under the end date: "End date must be after start date". */
    val dateRangeError: String? get() = filter.dateRangeError

    /** Apply is disabled while the date range is invalid. */
    val canApply: Boolean get() = dateRangeError == null

    /** True when the results are filtered by something other than the defaults (all dates, newest first). */
    val hasActiveFilters: Boolean get() = appliedFilter != null && appliedFilter != SearchFilter()
}

/**
 * Holds the search filters (spec 4.6) and, on Search Results, the matching records (spec 4.7).
 *
 * The same class serves two screens, each with its own instance:
 * - **Dashboard:** only the Filter sheet. [applyFilters] returns the filters, and the Dashboard
 *   opens Search Results with them. Nothing is searched here ([SearchUiState.appliedFilter] is null).
 * - **Search Results:** starts with the filters from the route ([initialFilter]) and searches the
 *   database with them. Applying new filters in the sheet, clearing or resetting them updates
 *   [SearchUiState.appliedFilter], and the results follow automatically.
 *
 * The results come from a Room Flow, so editing or deleting a record refreshes the list by itself.
 * [currentDate] is a parameter so tests can fix "today" to a known date.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val repository: HealthRecordRepository,
    initialFilter: SearchFilter? = null,
    private val currentDate: () -> LocalDate = { LocalDate.now() }
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SearchUiState(
            // The sheet opens showing the filters the results were found with.
            filter = initialFilter ?: SearchFilter(),
            today = currentDate(),
            appliedFilter = initialFilter,
            isLoading = initialFilter != null
        )
    )
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        // Every time the applied filters change, switch to a new database search (flatMapLatest
        // cancels the old one). On the Dashboard appliedFilter stays null, so nothing runs.
        viewModelScope.launch {
            _uiState
                .map { it.appliedFilter }
                .distinctUntilChanged()
                .filterNotNull()
                .flatMapLatest { filter ->
                    repository.searchRecords(
                        startDate = filter.startDate?.toString(), // yyyy-MM-dd, as stored
                        endDate = filter.endDate?.toString(),
                        sortOrder = filter.sortOrder
                    ).map { records -> SearchOutcome(filter, records, error = null) }
                        .catch { emit(SearchOutcome(filter, emptyList(), error = "Could not search your records. Please try again.")) }
                }
                .collect { outcome ->
                    _uiState.update {
                        // Ignore a late result for filters that were changed in the meantime.
                        if (it.appliedFilter != outcome.filter) it
                        else it.copy(results = outcome.records, isLoading = false, loadError = outcome.error)
                    }
                }
        }
    }

    private data class SearchOutcome(val filter: SearchFilter, val records: List<HealthRecord>, val error: String?)

    // ---------- Filter sheet ----------

    /** Filters button, search bar, or a filter chip tapped. */
    fun openFilters() = _uiState.update { it.copy(showFilterSheet = true, today = currentDate()) }

    /** Sheet closed by swiping down or tapping outside; the chosen filters are kept. */
    fun dismissFilters() = _uiState.update { it.copy(showFilterSheet = false) }

    fun onStartDateChange(date: LocalDate) = updateFilter { it.copy(startDate = date) }

    fun onEndDateChange(date: LocalDate) = updateFilter { it.copy(endDate = date) }

    fun onSortOrderChange(sortOrder: SortOrder) = updateFilter { it.copy(sortOrder = sortOrder) }

    /** Reset in the sheet: clears both dates and goes back to Newest first. */
    fun resetFilters() = updateFilter { SearchFilter() }

    /**
     * Apply tapped. Returns the filters to search with and closes the sheet,
     * or returns null (and keeps the sheet open with its error) if the date range is invalid.
     * On Search Results, the new filters are searched straight away.
     */
    fun applyFilters(): SearchFilter? {
        val filter = _uiState.value.filter
        if (filter.dateRangeError != null) return null
        _uiState.update {
            val searchNow = it.appliedFilter != null && it.appliedFilter != filter
            it.copy(
                showFilterSheet = false,
                appliedFilter = if (it.appliedFilter != null) filter else null,
                isLoading = it.isLoading || searchNow
            )
        }
        return filter
    }

    // ---------- Search Results ----------

    /**
     * "Clear" next to the filter chips, or "Reset filters" when nothing matched:
     * show every record, newest first. The sheet is reset too, so it matches.
     */
    fun clearFilters() = _uiState.update {
        val default = SearchFilter()
        it.copy(
            filter = default,
            appliedFilter = default,
            isLoading = it.isLoading || it.appliedFilter != default
        )
    }

    /** Delete icon on a card: ask for confirmation first. */
    fun onDeleteClick(record: HealthRecord) =
        _uiState.update { if (it.isDeleting) it else it.copy(recordToDelete = record) }

    fun onDismissDelete() = _uiState.update { it.copy(recordToDelete = null) }

    /** The user tapped Delete in the confirmation dialog. */
    fun confirmDelete() {
        val state = _uiState.value
        val record = state.recordToDelete ?: return
        if (state.isDeleting) return

        _uiState.update { it.copy(recordToDelete = null, isDeleting = true) }
        viewModelScope.launch {
            val message = try {
                repository.deleteRecord(record) // also deletes the selfie file, if any
                "Record deleted"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                "Could not delete the record. Please try again."
            }
            _uiState.update { it.copy(isDeleting = false, message = message) }
        }
    }

    /** The screen calls this after showing [SearchUiState.message]. */
    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    private fun updateFilter(change: (SearchFilter) -> SearchFilter) =
        _uiState.update { it.copy(filter = change(it.filter)) }

    companion object {
        /**
         * Builds the ViewModel (the app has no dependency-injection library).
         * On the Search Results route the filters are read from the route arguments;
         * on the Dashboard there are none, so nothing is searched.
         */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val dao = HealthMateDatabase.getInstance(app).healthRecordDao()
                SearchViewModel(
                    repository = HealthRecordRepository(dao),
                    initialFilter = createSavedStateHandle().toSearchFilter()
                )
            }
        }

        /** Reads `startDate`, `endDate` and `sort` from the Search Results route, or null if absent. */
        internal fun SavedStateHandle.toSearchFilter(): SearchFilter? {
            val sort = get<String>(Screen.ARG_SORT) ?: return null
            return SearchFilter(
                startDate = get<String>(Screen.ARG_START_DATE)?.toLocalDateOrNull(),
                endDate = get<String>(Screen.ARG_END_DATE)?.toLocalDateOrNull(),
                sortOrder = SortOrder.entries.firstOrNull { it.name == sort } ?: SortOrder.NEWEST_FIRST
            )
        }

        private fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this) }.getOrNull()
    }
}
