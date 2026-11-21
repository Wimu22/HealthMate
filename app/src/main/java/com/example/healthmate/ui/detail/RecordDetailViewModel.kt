package com.example.healthmate.ui.detail

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
import com.example.healthmate.ui.navigation.Screen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Everything the Record View screen shows, in one immutable object. */
data class RecordDetailUiState(
    val record: HealthRecord? = null,
    val isLoading: Boolean = true,
    /** The record could not be loaded (deleted elsewhere or a database error). */
    val loadError: String? = null,
    val showDeleteDialog: Boolean = false,
    val isDeleting: Boolean = false,
    /** A friendly message when deleting failed. */
    val errorMessage: String? = null,
    /** Set after a successful delete ("Record deleted"): show it and go back. */
    val deletedMessage: String? = null
)

/**
 * ViewModel for Record View (spec 4.5).
 *
 * It watches the record with a Room Flow, so after Edit → Update Record the screen shows the new
 * values by itself. Delete is two steps: [onDeleteClick] opens the confirmation dialog and only
 * [confirmDelete] actually removes the record (and its photo file, via the repository).
 */
class RecordDetailViewModel(
    private val repository: HealthRecordRepository,
    private val recordId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordDetailUiState())
    val uiState: StateFlow<RecordDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeRecord(recordId)
                .catch {
                    _uiState.update {
                        it.copy(isLoading = false, loadError = "Could not load the record. Please try again.")
                    }
                }
                .collect { record ->
                    _uiState.update {
                        when {
                            record != null -> it.copy(record = record, isLoading = false, loadError = null)
                            // Our own delete makes the Flow emit null: keep showing the record while leaving.
                            it.isDeleting || it.deletedMessage != null -> it
                            else -> it.copy(record = null, isLoading = false, loadError = "This record no longer exists.")
                        }
                    }
                }
        }
    }

    fun onDeleteClick() = _uiState.update { if (it.record != null) it.copy(showDeleteDialog = true) else it }

    fun onDismissDelete() = _uiState.update { it.copy(showDeleteDialog = false) }

    /** The user tapped Delete in the confirmation dialog. */
    fun confirmDelete() {
        val state = _uiState.value
        val record = state.record ?: return
        if (state.isDeleting || state.deletedMessage != null) return

        _uiState.update { it.copy(showDeleteDialog = false, isDeleting = true) }
        viewModelScope.launch {
            try {
                repository.deleteRecord(record) // also deletes the selfie file, if any
                _uiState.update { it.copy(isDeleting = false, deletedMessage = "Record deleted") }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isDeleting = false, errorMessage = "Could not delete the record. Please try again.")
                }
            }
        }
    }

    /** The screen calls this after showing [RecordDetailUiState.errorMessage]. */
    fun onErrorMessageShown() = _uiState.update { it.copy(errorMessage = null) }

    companion object {
        /** Builds the ViewModel; the record id comes from the route `record_view/{recordId}`. */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val recordId = checkNotNull(createSavedStateHandle().get<Long>(Screen.ARG_RECORD_ID))
                val dao = HealthMateDatabase.getInstance(app).healthRecordDao()
                RecordDetailViewModel(HealthRecordRepository(dao), recordId)
            }
        }
    }
}
