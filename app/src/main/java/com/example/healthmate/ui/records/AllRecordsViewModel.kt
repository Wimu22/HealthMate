package com.example.healthmate.ui.records

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.healthmate.data.local.HealthMateDatabase
import com.example.healthmate.data.local.HealthRecord
import com.example.healthmate.data.repository.HealthRecordRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Everything the All Records screen shows, in one immutable object. */
data class AllRecordsUiState(
    /** All records, newest first. */
    val records: List<HealthRecord> = emptyList(),
    val isLoading: Boolean = true,
    /** The list could not be loaded (database error). */
    val loadError: String? = null,
    /** Not null → the delete confirmation dialog is open for this record. */
    val recordToDelete: HealthRecord? = null,
    val isDeleting: Boolean = false,
    /** A one-off message for the Snackbar ("Record deleted" or a friendly error). */
    val message: String? = null
)

/**
 * ViewModel for All Records (spec 4.2).
 *
 * Two sources are combined into one UI state:
 * - the list itself, a Room Flow, so adding, editing or deleting anywhere refreshes it automatically;
 * - the screen's own state (which record the delete dialog is for, and the Snackbar message).
 *
 * Delete is two steps: [onDeleteClick] opens the confirmation dialog, and only [confirmDelete]
 * actually removes the record (the repository also deletes its photo file).
 */
class AllRecordsViewModel(
    private val repository: HealthRecordRepository
) : ViewModel() {

    /** The list part of the state: records, still loading, or a load error. */
    private data class ListState(
        val records: List<HealthRecord> = emptyList(),
        val isLoading: Boolean = true,
        val loadError: String? = null
    )

    /** The screen's own state; `records`/`isLoading`/`loadError` here are ignored. */
    private val screenState = MutableStateFlow(AllRecordsUiState())

    private val listState = repository.getAllRecords()
        .map { ListState(records = it, isLoading = false) }
        .catch { emit(ListState(isLoading = false, loadError = "Could not load your records. Please try again.")) }

    val uiState: StateFlow<AllRecordsUiState> = combine(listState, screenState) { list, screen ->
        screen.copy(records = list.records, isLoading = list.isLoading, loadError = list.loadError)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AllRecordsUiState()
    )

    /** Delete icon on a card: ask for confirmation first. */
    fun onDeleteClick(record: HealthRecord) =
        screenState.update { if (it.isDeleting) it else it.copy(recordToDelete = record) }

    fun onDismissDelete() = screenState.update { it.copy(recordToDelete = null) }

    /** The user tapped Delete in the confirmation dialog. */
    fun confirmDelete() {
        val state = screenState.value
        val record = state.recordToDelete ?: return
        if (state.isDeleting) return

        screenState.update { it.copy(recordToDelete = null, isDeleting = true) }
        viewModelScope.launch {
            val message = try {
                repository.deleteRecord(record) // also deletes the selfie file, if any
                "Record deleted"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                "Could not delete the record. Please try again."
            }
            screenState.update { it.copy(isDeleting = false, message = message) }
        }
    }

    /** The screen calls this after showing [AllRecordsUiState.message]. */
    fun onMessageShown() = screenState.update { it.copy(message = null) }

    companion object {
        /** Builds the ViewModel with its repository (the app has no dependency-injection library). */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val dao = HealthMateDatabase.getInstance(app).healthRecordDao()
                AllRecordsViewModel(HealthRecordRepository(dao))
            }
        }
    }
}
