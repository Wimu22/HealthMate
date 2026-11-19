package com.example.healthmate.domain

/** Totals of steps, calories and water for a date range (shown on the Dashboard stat cards). */
data class HealthSummary(
    val totalSteps: Long = 0,
    val totalCalories: Long = 0,
    val totalWater: Long = 0
)
