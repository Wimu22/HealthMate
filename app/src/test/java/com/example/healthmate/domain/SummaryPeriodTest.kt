package com.example.healthmate.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Date-range calculation for the Dashboard period chips (1 Day / 7 Days / 30 Days / 3 Months). */
class SummaryPeriodTest {

    private val today = LocalDate.of(2026, 11, 19)

    @Test
    fun oneDay_coversOnlyToday() {
        val range = SummaryPeriod.ONE_DAY.dateRange(today)
        assertEquals(today, range.start)
        assertEquals(today, range.end)
    }

    @Test
    fun sevenDays_startsSixDaysBeforeToday() {
        val range = SummaryPeriod.SEVEN_DAYS.dateRange(today)
        assertEquals(LocalDate.of(2026, 11, 13), range.start)
        assertEquals(today, range.end)
    }

    @Test
    fun thirtyDays_crossesIntoPreviousMonth() {
        val range = SummaryPeriod.THIRTY_DAYS.dateRange(today)
        assertEquals(LocalDate.of(2026, 10, 21), range.start)
        assertEquals(today, range.end)
    }

    @Test
    fun threeMonths_covers90Days() {
        val range = SummaryPeriod.THREE_MONTHS.dateRange(today)
        assertEquals(LocalDate.of(2026, 8, 22), range.start)
        assertEquals(today, range.end)
    }

    @Test
    fun everyPeriod_includesExactlyItsNumberOfDays() {
        SummaryPeriod.entries.forEach { period ->
            val range = period.dateRange(today)
            val daysIncluded = ChronoUnit.DAYS.between(range.start, range.end) + 1
            assertEquals(period.name, period.days.toLong(), daysIncluded)
        }
    }

    @Test
    fun sevenDays_acrossYearBoundary() {
        val range = SummaryPeriod.SEVEN_DAYS.dateRange(LocalDate.of(2027, 1, 2))
        assertEquals(LocalDate.of(2026, 12, 27), range.start)
    }

    @Test
    fun sevenDays_acrossLeapDay() {
        val range = SummaryPeriod.SEVEN_DAYS.dateRange(LocalDate.of(2028, 3, 3))
        assertEquals(LocalDate.of(2028, 2, 26), range.start)
    }

    @Test
    fun dateRange_textMatchesDatabaseFormat() {
        val range = SummaryPeriod.SEVEN_DAYS.dateRange(today)
        assertEquals("2026-11-13", range.startText)
        assertEquals("2026-11-19", range.endText)
    }

    @Test
    fun dateRange_textIsZeroPadded() {
        val range = SummaryPeriod.ONE_DAY.dateRange(LocalDate.of(2026, 3, 5))
        assertEquals("2026-03-05", range.startText)
    }
}
