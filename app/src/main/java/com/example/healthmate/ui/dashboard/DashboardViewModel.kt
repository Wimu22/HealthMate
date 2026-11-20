package com.example.healthmate.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.healthmate.data.local.HealthMateDatabase
import com.example.healthmate.data.local.HealthRecord
import com.example.healthmate.data.repository.HealthRecordRepository
import com.example.healthmate.domain.HealthSummary
import com.example.healthmate.domain.SummaryPeriod
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

/** Everything the Dashboard shows, in one immutable object. */
data class DashboardUiState(
    val today: LocalDate = LocalDate.now(),
    val selectedPeriod: SummaryPeriod = SummaryPeriod.ONE_DAY,
    val summary: HealthSummary = HealthSummary(),
    val recentRecords: List<HealthRecord> = emptyList(),
    /** False → the Dashboard shows a friendly prompt to add today's record. */
    val hasTodayRecord: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

/**
 * ViewModel for the Dashboard (spec 4.1).
 *
 * The only input is [selectPeriod]. Every time the period changes, `flatMapLatest` switches to new
 * repository Flows for that period's date range. Because Room Flows emit again whenever the table
 * changes, adding, editing or deleting a record updates the totals and recent list automatically.
 *
 * [currentDate] is a parameter so tests can fix "today" to a known date.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val repository: HealthRecordRepository,
    private val currentDate: () -> LocalDate = { LocalDate.now() }
) : ViewModel() {

    private val selectedPeriod = MutableStateFlow(SummaryPeriod.ONE_DAY)

    val uiState: StateFlow<DashboardUiState> = selectedPeriod
        .flatMapLatest { period ->
            // Read "today" on each period change, so it stays correct if the app is open past midnight.
            val today = currentDate()
            val range = period.dateRange(today)
            combine(
                repository.getSummary(range.startText, range.endText),
                repository.getRecentRecords(RECENT_RECORDS_LIMIT),
                repository.getRecordsInRange(today.toString(), today.toString())
            ) { summary, recent, todayRecords ->
                DashboardUiState(
                    today = today,
                    selectedPeriod = period,
                    summary = summary,
                    recentRecords = recent,
                    hasTodayRecord = todayRecords.isNotEmpty(),
                    isLoading = false
                )
            }.catch {
                // A database error shows a friendly message instead of crashing the app.
                emit(
                    DashboardUiState(
                        today = today,
                        selectedPeriod = period,
                        isLoading = false,
                        errorMessage = "Could not load your health data. Please try again."
                    )
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            // Keep collecting for 5 s after the screen stops (e.g. rotation), then pause the DB queries.
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardUiState(today = currentDate())
        )

    /** Called when the user taps a period chip (1 Day / 7 Days / 30 Days / 3 Months). */
    fun selectPeriod(period: SummaryPeriod) {
        selectedPeriod.value = period
    }

    companion object {
        const val RECENT_RECORDS_LIMIT = 5

        /** Builds the ViewModel with its repository (the app has no dependency-injection library). */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val dao = HealthMateDatabase.getInstance(app).healthRecordDao()
                DashboardViewModel(HealthRecordRepository(dao))
            }
        }
    }
}
