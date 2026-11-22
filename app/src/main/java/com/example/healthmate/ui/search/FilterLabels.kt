package com.example.healthmate.ui.search

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Text for the date-range chip on Search Results, e.g. "1 Nov – 15 Nov 2026".
 *
 * - no dates → "All dates"
 * - same year → the year is written once: "1 Nov – 15 Nov 2026"
 * - different years → "28 Dec 2025 – 3 Jan 2026"
 * - one day → "15 Nov 2026"
 * - only a start or end → "From 1 Nov 2026" / "Until 15 Nov 2026"
 */
fun dateRangeLabel(start: LocalDate?, end: LocalDate?, locale: Locale = Locale.getDefault()): String {
    val dayMonth = DateTimeFormatter.ofPattern("d MMM", locale)
    val full = DateTimeFormatter.ofPattern("d MMM yyyy", locale)
    return when {
        start == null && end == null -> "All dates"
        start == null -> "Until ${end!!.format(full)}"
        end == null -> "From ${start.format(full)}"
        start == end -> start.format(full)
        start.year == end.year -> "${start.format(dayMonth)} – ${end.format(full)}"
        else -> "${start.format(full)} – ${end.format(full)}"
    }
}
