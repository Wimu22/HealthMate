package com.example.healthmate.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.random.Random

class SeedDataTest {

    private val endDate = LocalDate.of(2026, 11, 18)
    private val records = SeedData.generate(endDate = endDate, random = Random(42))

    @Test
    fun generates90UniqueConsecutiveDaysEndingOnEndDate() {
        assertEquals(90, records.size)
        assertEquals(90, records.map { it.date }.toSet().size)
        assertEquals("2026-11-18", records.first().date)
        assertEquals(endDate.minusDays(89).toString(), records.last().date)
    }

    @Test
    fun valuesAreWithinRealisticRanges() {
        records.forEach {
            assertTrue("steps ${it.steps}", it.steps in 3_000..14_000)
            assertTrue("calories ${it.calories}", it.calories in 150..700)
            assertTrue("water ${it.water}", it.water in 1_200..3_200)
            assertNull(it.photoPath)
        }
    }
}
