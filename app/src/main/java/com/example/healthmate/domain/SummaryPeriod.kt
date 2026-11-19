package com.example.healthmate.domain

import java.time.LocalDate

/** Periods for the Dashboard summary. Each one covers [days] days ending today (inclusive). */
enum class SummaryPeriod(val days: Int) {
    ONE_DAY(1),
    SEVEN_DAYS(7),
    THIRTY_DAYS(30),
    THREE_MONTHS(90);

    /** First day of the period, e.g. 7 Days ending 19 Nov → 13 Nov. */
    fun startDate(today: LocalDate): LocalDate = today.minusDays((days - 1).toLong())
}
