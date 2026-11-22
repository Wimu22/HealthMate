package com.example.healthmate.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale

/** The "Last updated" time on Record View (spec 4.5). */
class FormattingTest {

    /** 19 Nov 2026, 14:05:00 UTC. */
    private val millis = 1_795_097_100_000

    @Test
    fun formatUpdatedAt_utc() {
        assertEquals("19 Nov 2026, 14:05", formatUpdatedAt(millis, ZoneOffset.UTC, Locale.UK))
    }

    @Test
    fun formatUpdatedAt_usesThePhonesTimeZone() {
        // Sri Lanka is UTC+5:30.
        assertEquals("19 Nov 2026, 19:35", formatUpdatedAt(millis, ZoneId.of("Asia/Colombo"), Locale.UK))
    }

    @Test
    fun formatUpdatedAt_crossesMidnight() {
        // UTC+10 turns 14:05 into 00:05 the next day.
        assertEquals("20 Nov 2026, 00:05", formatUpdatedAt(millis, ZoneOffset.ofHours(10), Locale.UK))
    }
}
