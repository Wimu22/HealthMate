package com.example.healthmate.domain

import java.time.LocalDate

/**
 * The filters chosen in the Filter bottom sheet (spec 4.6).
 * A missing date means "no limit on that side"; with no filters at all, every record matches.
 */
data class SearchFilter(
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val sortOrder: SortOrder = SortOrder.NEWEST_FIRST
) {
    /** "End date must be after start date", or null when the range is fine. */
    val dateRangeError: String?
        get() = RecordValidation.validateDateRange(startDate, endDate)
}
