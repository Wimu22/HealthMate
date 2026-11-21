package com.example.healthmate.ui.addedit

import android.database.sqlite.SQLiteConstraintException
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.healthmate.data.local.HealthMateDatabase
import com.example.healthmate.data.local.HealthRecord
import com.example.healthmate.data.local.LocalPhotoStorage
import com.example.healthmate.data.local.PhotoStorage
import com.example.healthmate.data.repository.HealthRecordRepository
import com.example.healthmate.domain.RecordFormErrors
import com.example.healthmate.domain.RecordFormResult
import com.example.healthmate.domain.RecordValidation
import com.example.healthmate.ui.navigation.Screen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Everything the Add / Edit Record form shows, in one immutable object. */
data class AddEditUiState(
    /** null → adding a new record; otherwise the id of the record being edited. */
    val recordId: Long? = null,
    /** Latest date the picker allows (future dates are not allowed). */
    val today: LocalDate = LocalDate.now(),
    val date: LocalDate? = null,
    // Number fields are kept as the text the user typed, so half-typed or empty values can be shown.
    val steps: String = "",
    val calories: String = "",
    val water: String = "",
    /** Selfie file in app-private storage, or null for no photo (the photo is optional). */
    val photoPath: String? = null,
    /** True while a picked photo is being copied into app storage. */
    val isProcessingPhoto: Boolean = false,
    val errors: RecordFormErrors = RecordFormErrors(),
    /** Set when the chosen date already has a record, so the screen can offer to edit that record. */
    val duplicateRecordId: Long? = null,
    /** True while an existing record is being loaded for editing. */
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    /** A friendly message for a database problem (load or save failed, record missing). */
    val errorMessage: String? = null,
    /** Set after a successful save ("Record saved" / "Record updated"): show it and go back. */
    val savedMessage: String? = null
) {
    val isEditMode: Boolean get() = recordId != null
    val title: String get() = if (isEditMode) "Edit Record" else "Add New Record"
    val saveButtonLabel: String get() = if (isEditMode) "Update Record" else "Save Record"
    val canSave: Boolean get() = !isLoading && !isSaving && !isProcessingPhoto && savedMessage == null
}

/**
 * ViewModel for both Add New Record (spec 4.3) and Edit Record (spec 4.4).
 *
 * - Adding: [recordId] is null and the date starts as today.
 * - Editing: [recordId] comes from the route `edit_record/{recordId}`; the record is loaded from
 *   the database and the form is pre-filled with it.
 *
 * Validation (spec 5): errors are shown when Save is tapped. After that first attempt, every field
 * is re-checked as the user types, so an error disappears as soon as it is fixed. The
 * one-record-per-date rule is also checked as soon as a date is picked, so the user finds out early.
 *
 * Selfie (spec 4.3): a picked photo is copied into app storage straight away, so the preview shows
 * the app's own copy. Copies that never get saved (replaced, removed, or the user leaves without
 * saving) are deleted again, so no unused files pile up. The photo the record already had is only
 * deleted by the repository once the change is actually saved.
 *
 * [currentDate] is a parameter so tests can fix "today" to a known date.
 */
class AddEditRecordViewModel(
    private val repository: HealthRecordRepository,
    private val photoStorage: PhotoStorage,
    private val recordId: Long?,
    private val currentDate: () -> LocalDate = { LocalDate.now() }
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AddEditUiState(
            recordId = recordId,
            today = currentDate(),
            date = if (recordId == null) currentDate() else null,
            isLoading = recordId != null
        )
    )
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()

    /** Becomes true on the first Save tap; from then on, errors update live while typing. */
    private var hasTriedToSave = false
    private var duplicateCheckJob: Job? = null

    /** A photo copied during this visit that is not saved in the database yet (null if none). */
    private var unsavedPhotoPath: String? = null

    init {
        if (recordId != null) {
            loadRecord(recordId)
        } else {
            checkDateIsFree(currentDate())
        }
    }

    // ---------- Events from the screen ----------

    fun onDateSelected(date: LocalDate) {
        _uiState.update {
            it.copy(
                date = date,
                duplicateRecordId = null,
                errors = it.errors.copy(date = if (hasTriedToSave) validateDate(date) else null)
            )
        }
        checkDateIsFree(date)
    }

    fun onStepsChange(value: String) = _uiState.update {
        it.copy(steps = value, errors = it.errors.copy(steps = liveError { RecordValidation.validateSteps(value) }))
    }

    fun onCaloriesChange(value: String) = _uiState.update {
        it.copy(
            calories = value,
            errors = it.errors.copy(calories = liveError { RecordValidation.validateCalories(value) })
        )
    }

    fun onWaterChange(value: String) = _uiState.update {
        it.copy(water = value, errors = it.errors.copy(water = liveError { RecordValidation.validateWater(value) }))
    }

    /** The user picked an image with the Photo Picker: copy it into app storage and preview it. */
    fun onPhotoPicked(uri: Uri) {
        if (_uiState.value.isProcessingPhoto) return
        _uiState.update { it.copy(isProcessingPhoto = true) }
        viewModelScope.launch {
            try {
                val newPath = photoStorage.savePhoto(uri)
                discardUnsavedPhoto() // replacing a photo picked earlier in this visit
                unsavedPhotoPath = newPath
                _uiState.update { it.copy(photoPath = newPath, isProcessingPhoto = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isProcessingPhoto = false, errorMessage = "Could not add this photo. Please try another one.")
                }
            }
        }
    }

    /** Removes the selfie from the form. The record's saved photo is only deleted when saving. */
    fun onPhotoRemoved() {
        discardUnsavedPhoto()
        _uiState.update { it.copy(photoPath = null) }
    }

    /** The screen calls this after showing [AddEditUiState.errorMessage]. */
    fun onErrorMessageShown() = _uiState.update { it.copy(errorMessage = null) }

    /** Validates the whole form and, if everything is valid, adds or updates the record. */
    fun saveRecord() {
        val state = _uiState.value
        if (!state.canSave) return
        hasTriedToSave = true

        val result = RecordValidation.validateForm(
            date = state.date,
            steps = state.steps,
            calories = state.calories,
            water = state.water,
            today = currentDate()
        )
        if (result is RecordFormResult.Invalid) {
            // Keep a duplicate-date error found earlier if the date itself is otherwise valid.
            _uiState.update { it.copy(errors = result.errors.copy(date = result.errors.date ?: it.errors.date)) }
            return
        }
        val valid = result as RecordFormResult.Valid
        if (state.duplicateRecordId != null) return // date error is already showing

        duplicateCheckJob?.cancel()
        _uiState.update { it.copy(isSaving = true, errors = RecordFormErrors()) }
        viewModelScope.launch {
            try {
                // Check again just before saving: the database is the final word on duplicates.
                val dateText = valid.date.toString()
                val existingId = repository.getRecordByDate(dateText)?.id
                if (RecordValidation.validateUniqueDate(existingId, recordId) != null) {
                    showDuplicateDate(existingId)
                    return@launch
                }

                val record = HealthRecord(
                    id = recordId ?: 0,
                    date = dateText,
                    steps = valid.steps,
                    calories = valid.calories,
                    water = valid.water,
                    photoPath = state.photoPath
                )
                if (recordId == null) repository.addRecord(record) else repository.updateRecord(record)
                unsavedPhotoPath = null // the photo now belongs to the saved record

                _uiState.update {
                    it.copy(isSaving = false, savedMessage = if (recordId == null) "Record saved" else "Record updated")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: SQLiteConstraintException) {
                // The unique index on `date` rejected it (another save got there first).
                showDuplicateDate(runCatching { repository.getRecordByDate(valid.date.toString())?.id }.getOrNull())
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = "Could not save the record. Please try again.")
                }
            }
        }
    }

    /** Leaving the screen without saving: delete the photo copied for nothing. */
    override fun onCleared() {
        // Mid-save the record may still be written with this photo, so keep the file then.
        if (!_uiState.value.isSaving) discardUnsavedPhoto()
    }

    // ---------- Helpers ----------

    private fun discardUnsavedPhoto() {
        unsavedPhotoPath?.let { runCatching { photoStorage.deletePhoto(it) } }
        unsavedPhotoPath = null
    }

    private fun loadRecord(id: Long) {
        viewModelScope.launch {
            try {
                val record = repository.getRecord(id)
                _uiState.update {
                    if (record == null) {
                        it.copy(isLoading = false, errorMessage = "This record no longer exists.")
                    } else {
                        it.copy(
                            isLoading = false,
                            date = LocalDate.parse(record.date),
                            steps = record.steps.toString(),
                            calories = record.calories.toString(),
                            water = record.water.toString(),
                            photoPath = record.photoPath
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Could not load the record. Please try again.")
                }
            }
        }
    }

    /** Looks up [date] in the database and shows the duplicate-date error if another record uses it. */
    private fun checkDateIsFree(date: LocalDate) {
        duplicateCheckJob?.cancel()
        duplicateCheckJob = viewModelScope.launch {
            val existingId = try {
                repository.getRecordByDate(date.toString())?.id
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null // not critical: saveRecord() checks again
            }
            if (RecordValidation.validateUniqueDate(existingId, recordId) != null && _uiState.value.date == date) {
                _uiState.update {
                    it.copy(
                        duplicateRecordId = existingId,
                        errors = it.errors.copy(date = RecordValidation.ERROR_DATE_TAKEN)
                    )
                }
            }
        }
    }

    private fun showDuplicateDate(existingId: Long?) = _uiState.update {
        it.copy(
            isSaving = false,
            duplicateRecordId = existingId,
            errors = it.errors.copy(date = RecordValidation.ERROR_DATE_TAKEN)
        )
    }

    private fun validateDate(date: LocalDate?) = RecordValidation.validateDate(date, currentDate())

    /** Before the first Save tap no errors are shown, so the user isn't warned while still typing. */
    private inline fun liveError(check: () -> String?): String? = if (hasTriedToSave) check() else null

    companion object {
        /**
         * Builds the ViewModel with its repository. The record id is read from the navigation
         * route: `edit_record/{recordId}` has one, `add_record` does not (→ null → add mode).
         */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val recordId = createSavedStateHandle().get<Long>(Screen.ARG_RECORD_ID)
                val dao = HealthMateDatabase.getInstance(app).healthRecordDao()
                AddEditRecordViewModel(HealthRecordRepository(dao), LocalPhotoStorage(app), recordId)
            }
        }
    }
}
