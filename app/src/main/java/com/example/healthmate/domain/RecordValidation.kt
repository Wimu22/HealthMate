package com.example.healthmate.domain

import java.time.LocalDate

/**
 * The form validation rules from spec section 5, with the exact error messages.
 *
 * Every check returns the error message to show under the field, or `null` when the value is valid.
 * The rules are plain functions with no Android or database code, so they are easy to unit test.
 * "Today" is always passed in, so tests can fix it to a known date.
 */
object RecordValidation {

    const val MIN_VALUE = 0
    const val MAX_STEPS = 100_000
    const val MAX_CALORIES = 10_000
    const val MAX_WATER = 10_000

    const val ERROR_DATE_REQUIRED = "Please choose a date"
    const val ERROR_DATE_IN_FUTURE = "Date can't be in the future"
    const val ERROR_DATE_TAKEN = "A record for this date already exists"
    const val ERROR_STEPS = "Enter steps between 0 and 100,000"
    const val ERROR_CALORIES = "Enter calories between 0 and 10,000"
    const val ERROR_WATER = "Enter water between 0 and 10,000 ml"
    const val ERROR_DATE_RANGE = "End date must be after start date"

    /** Date is required and cannot be after [today]. */
    fun validateDate(date: LocalDate?, today: LocalDate): String? = when {
        date == null -> ERROR_DATE_REQUIRED
        date.isAfter(today) -> ERROR_DATE_IN_FUTURE
        else -> null
    }

    /**
     * Only one record per date.
     *
     * [existingRecordId] is the id of the record already saved for the chosen date (null if none).
     * [currentRecordId] is the record being edited (null when adding). When editing, finding the
     * record itself is fine, so keeping the same date does not count as a duplicate.
     */
    fun validateUniqueDate(existingRecordId: Long?, currentRecordId: Long?): String? =
        if (existingRecordId != null && existingRecordId != currentRecordId) ERROR_DATE_TAKEN else null

    fun validateSteps(input: String): String? = validateWholeNumber(input, MAX_STEPS, ERROR_STEPS)

    fun validateCalories(input: String): String? = validateWholeNumber(input, MAX_CALORIES, ERROR_CALORIES)

    fun validateWater(input: String): String? = validateWholeNumber(input, MAX_WATER, ERROR_WATER)

    /** For the Filter sheet: the end date may equal the start date, but not be before it. */
    fun validateDateRange(start: LocalDate?, end: LocalDate?): String? =
        if (start != null && end != null && end.isBefore(start)) ERROR_DATE_RANGE else null

    /**
     * Reads [input] as a whole number, or returns null if it isn't one.
     * Only digits are accepted (surrounding spaces are ignored): "", "12.5", "-3", "1,000" and
     * "12a" are all null. Numbers too large for an Int are null as well.
     */
    fun parseWholeNumber(input: String): Int? {
        val text = input.trim()
        if (text.isEmpty() || !text.all { it in '0'..'9' }) return null
        return text.toIntOrNull()
    }

    /**
     * Checks the whole Add/Edit form at once (everything except the one-record-per-date rule,
     * which needs the database and is checked by the ViewModel).
     * Returns the parsed values when everything is valid, or the error for each field.
     */
    fun validateForm(
        date: LocalDate?,
        steps: String,
        calories: String,
        water: String,
        today: LocalDate
    ): RecordFormResult {
        val errors = RecordFormErrors(
            date = validateDate(date, today),
            steps = validateSteps(steps),
            calories = validateCalories(calories),
            water = validateWater(water)
        )
        if (errors.hasErrors || date == null) return RecordFormResult.Invalid(errors)
        return RecordFormResult.Valid(
            date = date,
            steps = checkNotNull(parseWholeNumber(steps)),
            calories = checkNotNull(parseWholeNumber(calories)),
            water = checkNotNull(parseWholeNumber(water))
        )
    }

    private fun validateWholeNumber(input: String, max: Int, error: String): String? {
        val value = parseWholeNumber(input) ?: return error
        return if (value in MIN_VALUE..max) null else error
    }
}

/** The error message for each form field; `null` means that field is fine. */
data class RecordFormErrors(
    val date: String? = null,
    val steps: String? = null,
    val calories: String? = null,
    val water: String? = null
) {
    val hasErrors: Boolean
        get() = date != null || steps != null || calories != null || water != null
}

/** Result of [RecordValidation.validateForm]. */
sealed interface RecordFormResult {
    /** All fields are valid; the numbers are already converted from text. */
    data class Valid(val date: LocalDate, val steps: Int, val calories: Int, val water: Int) : RecordFormResult

    data class Invalid(val errors: RecordFormErrors) : RecordFormResult
}
