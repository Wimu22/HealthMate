package com.example.healthmate.data.local

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.random.Random

/**
 * Generates realistic dummy records so the app has data to show on first launch.
 *
 * - One record per day for [days] days, ending on [endDate] (yesterday by default).
 * - Steps 3,000–14,000, a little higher at weekends.
 * - Calories 150–700, linked to steps (more walking → more calories) plus some variation.
 * - Water 1,200–3,200 ml, rounded to the nearest 50 ml.
 * - No photos.
 */
object SeedData {

    const val DAYS = 90

    fun generate(
        endDate: LocalDate = LocalDate.now().minusDays(1),
        days: Int = DAYS,
        random: Random = Random.Default
    ): List<HealthRecord> = (0 until days).map { offset ->
        val date = endDate.minusDays(offset.toLong())
        val isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY

        val steps = random.nextInt(if (isWeekend) 5_000 else 3_000, 14_001)
        val calories = (100 + steps * 0.035 + random.nextInt(-60, 61))
            .toInt()
            .coerceIn(150, 700)
        val water = random.nextInt(1_200 / 50, 3_200 / 50 + 1) * 50

        HealthRecord(
            date = date.format(DateTimeFormatter.ISO_LOCAL_DATE), // yyyy-MM-dd
            steps = steps,
            calories = calories,
            water = water
        )
    }
}
