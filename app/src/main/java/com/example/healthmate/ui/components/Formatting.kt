package com.example.healthmate.ui.components

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

// Display helpers shared by the components and screens.

private val DisplayDateFormatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy")

/** 2026-11-19 → "Thu, 19 Nov 2026". */
fun LocalDate.toDisplayDate(): String = format(DisplayDateFormatter)

private val LongDateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")

/** 2026-11-19 → "19 November 2026" (Dashboard date line). */
fun LocalDate.toLongDisplayDate(): String = format(LongDateFormatter)

/** Database date text "2026-11-19" → "Thu, 19 Nov 2026". Falls back to the raw text if invalid. */
fun formatRecordDate(isoDate: String): String = try {
    LocalDate.parse(isoDate).toDisplayDate()
} catch (e: DateTimeParseException) {
    isoDate
}

/** 12450 → "12,450" (thousands separator from the phone's language). */
fun formatNumber(value: Number): String = NumberFormat.getIntegerInstance().format(value)
