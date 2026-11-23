package com.example.healthmate.data.local

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.healthmate.domain.SortOrder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [HealthRecordDao] (spec section 7).
 *
 * Each test gets a fresh in-memory database: it lives only in RAM, starts empty (no seed data,
 * because the seed callback is not added) and is thrown away after the test, so tests never
 * affect each other or the real app data. Flows are read with `first()` to get the current value.
 */
@RunWith(AndroidJUnit4::class)
class HealthRecordDaoTest {

    private lateinit var db: HealthMateDatabase
    private lateinit var dao: HealthRecordDao

    @Before
    fun createDb() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            HealthMateDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.healthRecordDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    private fun record(date: String, steps: Int = 5000, calories: Int = 300, water: Int = 2000) =
        HealthRecord(date = date, steps = steps, calories = calories, water = water)

    /** Three records with different values, inserted out of date order on purpose. */
    private suspend fun insertSample() {
        dao.insert(record("2026-11-10", steps = 8000, calories = 200, water = 3000))
        dao.insert(record("2026-11-05", steps = 12000, calories = 450, water = 1500))
        dao.insert(record("2026-11-20", steps = 4000, calories = 600, water = 2500))
    }

    private suspend fun searchDates(start: String?, end: String?, sort: SortOrder) =
        dao.search(start, end, sort.name).first().map { it.date }

    // ---------- Create / Read ----------

    @Test
    fun insert_returnsNewId_andRecordCanBeReadById() = runTest {
        val original = record("2026-11-15", steps = 7500, calories = 320, water = 2100)
            .copy(photoPath = "/data/photo.jpg", updatedAt = 1_700_000_000_000)

        val id = dao.insert(original)

        assertTrue(id > 0)
        assertEquals(original.copy(id = id), dao.getById(id))
        assertEquals(original.copy(id = id), dao.observeById(id).first())
    }

    @Test
    fun getById_unknownId_returnsNull() = runTest {
        assertNull(dao.getById(999))
    }

    @Test
    fun getByDate_returnsMatchingRecord() = runTest {
        insertSample()
        assertEquals(12000, dao.getByDate("2026-11-05")?.steps)
        assertNull(dao.getByDate("2026-11-06"))
    }

    @Test
    fun getAll_returnsAllRecordsNewestFirst() = runTest {
        insertSample()
        val dates = dao.getAll().first().map { it.date }
        assertEquals(listOf("2026-11-20", "2026-11-10", "2026-11-05"), dates)
    }

    @Test
    fun getAll_emptyDatabase_returnsEmptyList() = runTest {
        assertTrue(dao.getAll().first().isEmpty())
    }

    // ---------- Update ----------

    @Test
    fun update_changesStoredValues() = runTest {
        val id = dao.insert(record("2026-11-15"))
        val updated = record("2026-11-16", steps = 9999, calories = 555, water = 3100)
            .copy(id = id, updatedAt = 1_800_000_000_000)

        dao.update(updated)

        assertEquals(updated, dao.getById(id))
        assertEquals(1, dao.getAll().first().size)
    }

    // ---------- Delete ----------

    @Test
    fun delete_removesOnlyThatRecord() = runTest {
        val keepId = dao.insert(record("2026-11-01"))
        val deleteId = dao.insert(record("2026-11-02"))

        dao.delete(dao.getById(deleteId)!!)

        assertNull(dao.getById(deleteId))
        assertNotNull(dao.getById(keepId))
        assertEquals(listOf(keepId), dao.getAll().first().map { it.id })
    }

    // ---------- Unique date rule ----------

    @Test
    fun insert_duplicateDate_throwsAndKeepsOriginal() = runTest {
        dao.insert(record("2026-11-15", steps = 1111))
        try {
            dao.insert(record("2026-11-15", steps = 2222))
            fail("Inserting a second record for the same date should throw")
        } catch (e: SQLiteConstraintException) {
            // expected
        }
        val all = dao.getAll().first()
        assertEquals(1, all.size)
        assertEquals(1111, all.single().steps)
    }

    @Test
    fun update_toDateAlreadyUsed_throws() = runTest {
        dao.insert(record("2026-11-15"))
        val id = dao.insert(record("2026-11-16"))
        try {
            dao.update(dao.getById(id)!!.copy(date = "2026-11-15"))
            fail("Updating a record to a date that is already used should throw")
        } catch (e: SQLiteConstraintException) {
            // expected
        }
        assertEquals("2026-11-16", dao.getById(id)?.date)
    }

    // ---------- Date-range search ----------

    @Test
    fun getInRange_isInclusiveOfBothEnds() = runTest {
        insertSample()
        val dates = dao.getInRange("2026-11-05", "2026-11-10").first().map { it.date }
        assertEquals(listOf("2026-11-10", "2026-11-05"), dates)
    }

    @Test
    fun search_withDateRange_returnsOnlyRecordsInside() = runTest {
        insertSample()
        assertEquals(
            listOf("2026-11-10"),
            searchDates("2026-11-06", "2026-11-19", SortOrder.NEWEST_FIRST)
        )
    }

    @Test
    fun search_withOnlyStartOrOnlyEnd_isOpenEnded() = runTest {
        insertSample()
        assertEquals(
            listOf("2026-11-20", "2026-11-10"),
            searchDates("2026-11-10", null, SortOrder.NEWEST_FIRST)
        )
        assertEquals(
            listOf("2026-11-10", "2026-11-05"),
            searchDates(null, "2026-11-10", SortOrder.NEWEST_FIRST)
        )
    }

    @Test
    fun search_rangeWithNoRecords_returnsEmptyList() = runTest {
        insertSample()
        assertTrue(searchDates("2026-12-01", "2026-12-31", SortOrder.NEWEST_FIRST).isEmpty())
    }

    // ---------- Sort orders ----------

    @Test
    fun search_newestFirst() = runTest {
        insertSample()
        assertEquals(
            listOf("2026-11-20", "2026-11-10", "2026-11-05"),
            searchDates(null, null, SortOrder.NEWEST_FIRST)
        )
    }

    @Test
    fun search_oldestFirst() = runTest {
        insertSample()
        assertEquals(
            listOf("2026-11-05", "2026-11-10", "2026-11-20"),
            searchDates(null, null, SortOrder.OLDEST_FIRST)
        )
    }

    @Test
    fun search_highestSteps() = runTest {
        insertSample()
        val steps = dao.search(null, null, SortOrder.HIGHEST_STEPS.name).first().map { it.steps }
        assertEquals(listOf(12000, 8000, 4000), steps)
    }

    @Test
    fun search_highestCalories() = runTest {
        insertSample()
        val calories =
            dao.search(null, null, SortOrder.HIGHEST_CALORIES.name).first().map { it.calories }
        assertEquals(listOf(600, 450, 200), calories)
    }

    @Test
    fun search_highestWater() = runTest {
        insertSample()
        val water = dao.search(null, null, SortOrder.HIGHEST_WATER.name).first().map { it.water }
        assertEquals(listOf(3000, 2500, 1500), water)
    }

    @Test
    fun search_highestSteps_tiesAreBrokenByNewestDate() = runTest {
        dao.insert(record("2026-11-01", steps = 5000))
        dao.insert(record("2026-11-03", steps = 5000))
        dao.insert(record("2026-11-02", steps = 9000))
        assertEquals(
            listOf("2026-11-02", "2026-11-03", "2026-11-01"),
            searchDates(null, null, SortOrder.HIGHEST_STEPS)
        )
    }

    @Test
    fun search_sortIsAppliedWithinDateRange() = runTest {
        insertSample()
        assertEquals(
            listOf("2026-11-05", "2026-11-10"),
            searchDates("2026-11-01", "2026-11-15", SortOrder.HIGHEST_STEPS)
        )
    }
}
