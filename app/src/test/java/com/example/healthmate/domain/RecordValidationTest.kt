package com.example.healthmate.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Form validation rules from spec section 5, including boundary values and exact messages. */
class RecordValidationTest {

    private val today = LocalDate.of(2026, 11, 21)

    // ---------- Date: required, not in the future ----------

    @Test
    fun date_missing_showsRequiredError() {
        assertEquals("Please choose a date", RecordValidation.validateDate(null, today))
    }

    @Test
    fun date_today_isValid() {
        assertNull(RecordValidation.validateDate(today, today))
    }

    @Test
    fun date_inThePast_isValid() {
        assertNull(RecordValidation.validateDate(today.minusDays(1), today))
        assertNull(RecordValidation.validateDate(LocalDate.of(2020, 1, 1), today))
    }

    @Test
    fun date_tomorrow_showsFutureError() {
        assertEquals("Date can't be in the future", RecordValidation.validateDate(today.plusDays(1), today))
    }

    @Test
    fun date_nextYear_showsFutureError() {
        assertEquals(RecordValidation.ERROR_DATE_IN_FUTURE, RecordValidation.validateDate(today.plusYears(1), today))
    }

    // ---------- Date: one record per date ----------

    @Test
    fun uniqueDate_noExistingRecord_isValid() {
        assertNull(RecordValidation.validateUniqueDate(existingRecordId = null, currentRecordId = null))
    }

    @Test
    fun uniqueDate_addingOnUsedDate_showsDuplicateError() {
        assertEquals(
            "A record for this date already exists",
            RecordValidation.validateUniqueDate(existingRecordId = 5, currentRecordId = null)
        )
    }

    @Test
    fun uniqueDate_editingAndKeepingOwnDate_isValid() {
        assertNull(RecordValidation.validateUniqueDate(existingRecordId = 5, currentRecordId = 5))
    }

    @Test
    fun uniqueDate_editingToAnotherRecordsDate_showsDuplicateError() {
        assertEquals(
            RecordValidation.ERROR_DATE_TAKEN,
            RecordValidation.validateUniqueDate(existingRecordId = 7, currentRecordId = 5)
        )
    }

    @Test
    fun uniqueDate_editingToFreeDate_isValid() {
        assertNull(RecordValidation.validateUniqueDate(existingRecordId = null, currentRecordId = 5))
    }

    // ---------- Steps: whole number 0–100,000 ----------

    @Test
    fun steps_boundaries_areValid() {
        assertNull(RecordValidation.validateSteps("0"))
        assertNull(RecordValidation.validateSteps("100000"))
    }

    @Test
    fun steps_typicalValue_isValid() {
        assertNull(RecordValidation.validateSteps("8450"))
    }

    @Test
    fun steps_aboveMaximum_showsError() {
        assertEquals("Enter steps between 0 and 100,000", RecordValidation.validateSteps("100001"))
    }

    @Test
    fun steps_empty_showsError() {
        assertEquals(RecordValidation.ERROR_STEPS, RecordValidation.validateSteps(""))
        assertEquals(RecordValidation.ERROR_STEPS, RecordValidation.validateSteps("   "))
    }

    @Test
    fun steps_negative_showsError() {
        assertEquals(RecordValidation.ERROR_STEPS, RecordValidation.validateSteps("-1"))
    }

    @Test
    fun steps_decimal_showsError() {
        assertEquals(RecordValidation.ERROR_STEPS, RecordValidation.validateSteps("5000.5"))
    }

    @Test
    fun steps_notANumber_showsError() {
        assertEquals(RecordValidation.ERROR_STEPS, RecordValidation.validateSteps("abc"))
        assertEquals(RecordValidation.ERROR_STEPS, RecordValidation.validateSteps("12a"))
    }

    // ---------- Calories: whole number 0–10,000 ----------

    @Test
    fun calories_boundaries_areValid() {
        assertNull(RecordValidation.validateCalories("0"))
        assertNull(RecordValidation.validateCalories("10000"))
    }

    @Test
    fun calories_aboveMaximum_showsError() {
        assertEquals("Enter calories between 0 and 10,000", RecordValidation.validateCalories("10001"))
    }

    @Test
    fun calories_emptyNegativeOrDecimal_showsError() {
        listOf("", "-50", "320.7", "kcal").forEach { input ->
            assertEquals(input, RecordValidation.ERROR_CALORIES, RecordValidation.validateCalories(input))
        }
    }

    // ---------- Water: whole number 0–10,000 ml ----------

    @Test
    fun water_boundaries_areValid() {
        assertNull(RecordValidation.validateWater("0"))
        assertNull(RecordValidation.validateWater("10000"))
    }

    @Test
    fun water_aboveMaximum_showsError() {
        assertEquals("Enter water between 0 and 10,000 ml", RecordValidation.validateWater("10001"))
    }

    @Test
    fun water_emptyNegativeOrDecimal_showsError() {
        listOf("", "-1", "1.5", "2L").forEach { input ->
            assertEquals(input, RecordValidation.ERROR_WATER, RecordValidation.validateWater(input))
        }
    }

    // ---------- Whole-number parsing ----------

    @Test
    fun parse_digits_returnsNumber() {
        assertEquals(1234, RecordValidation.parseWholeNumber("1234"))
        assertEquals(0, RecordValidation.parseWholeNumber("0"))
    }

    @Test
    fun parse_ignoresSurroundingSpacesAndLeadingZeros() {
        assertEquals(42, RecordValidation.parseWholeNumber("  42 "))
        assertEquals(7, RecordValidation.parseWholeNumber("007"))
    }

    @Test
    fun parse_rejectsAnythingThatIsNotAWholeNumber() {
        listOf("", " ", "-5", "+5", "1.0", "1,000", "1e3", "12a", "1 000").forEach { input ->
            assertNull(input, RecordValidation.parseWholeNumber(input))
        }
    }

    @Test
    fun parse_tooLargeForInt_returnsNullAndFailsValidation() {
        assertNull(RecordValidation.parseWholeNumber("99999999999"))
        assertEquals(RecordValidation.ERROR_STEPS, RecordValidation.validateSteps("99999999999"))
    }

    // ---------- Date range (Filter sheet) ----------

    @Test
    fun dateRange_endAfterStart_isValid() {
        assertNull(RecordValidation.validateDateRange(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 15)))
    }

    @Test
    fun dateRange_sameDay_isValid() {
        assertNull(RecordValidation.validateDateRange(today, today))
    }

    @Test
    fun dateRange_endBeforeStart_showsError() {
        assertEquals(
            "End date must be after start date",
            RecordValidation.validateDateRange(LocalDate.of(2026, 11, 15), LocalDate.of(2026, 11, 1))
        )
    }

    @Test
    fun dateRange_missingStartOrEnd_isValid() {
        assertNull(RecordValidation.validateDateRange(null, today))
        assertNull(RecordValidation.validateDateRange(today, null))
        assertNull(RecordValidation.validateDateRange(null, null))
    }

    // ---------- Whole form ----------

    @Test
    fun form_allValid_returnsParsedValues() {
        val result = RecordValidation.validateForm(today, "8450", "320", "2100", today)
        assertEquals(RecordFormResult.Valid(today, 8450, 320, 2100), result)
    }

    @Test
    fun form_allInvalid_returnsAnErrorForEveryField() {
        val result = RecordValidation.validateForm(null, "", "10001", "-1", today)
        assertTrue(result is RecordFormResult.Invalid)
        val errors = (result as RecordFormResult.Invalid).errors
        assertEquals(RecordValidation.ERROR_DATE_REQUIRED, errors.date)
        assertEquals(RecordValidation.ERROR_STEPS, errors.steps)
        assertEquals(RecordValidation.ERROR_CALORIES, errors.calories)
        assertEquals(RecordValidation.ERROR_WATER, errors.water)
        assertTrue(errors.hasErrors)
    }

    @Test
    fun form_oneInvalidField_onlyThatFieldHasAnError() {
        val result = RecordValidation.validateForm(today.plusDays(1), "8450", "320", "2100", today)
        val errors = (result as RecordFormResult.Invalid).errors
        assertEquals(RecordFormErrors(date = RecordValidation.ERROR_DATE_IN_FUTURE), errors)
    }

    @Test
    fun formErrors_empty_hasNoErrors() {
        assertFalse(RecordFormErrors().hasErrors)
    }
}
