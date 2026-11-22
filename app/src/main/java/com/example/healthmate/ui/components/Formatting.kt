package com.example.healthmate.ui.components

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

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

/**
 * "Last updated" time on Record View: epoch milliseconds → "22 Nov 2026, 14:05"
 * in the phone's time zone (24-hour clock).
 */
fun formatUpdatedAt(
    epochMillis: Long,
    zone: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault()
): String = Instant.ofEpochMilli(epochMillis)
    .atZone(zone)
    .format(DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", locale))

/** 12450 → "12,450" (thousands separator from the phone's language). */
fun formatNumber(value: Number): String = NumberFormat.getIntegerInstance().format(value)
