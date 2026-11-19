package com.example.healthmate.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.healthmate.domain.HealthSummary
import kotlinx.coroutines.flow.Flow

/**
 * All database operations for health records.
 *
 * Functions returning [Flow] are "live": Room emits a new value every time the table changes,
 * so screens refresh automatically. `suspend` functions run once and return a single result.
 * Dates are `yyyy-MM-dd` text, so comparing and sorting them as strings gives date order.
 */
@Dao
interface HealthRecordDao {

    // ---------- Create ----------

    /** Inserts a record and returns its new id. Throws if a record for that date already exists. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(record: HealthRecord): Long

    /** Inserts many records at once (used for seeding dummy data). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(records: List<HealthRecord>)

    // ---------- Read ----------

    @Query("SELECT * FROM health_records ORDER BY date DESC")
    fun getAll(): Flow<List<HealthRecord>>

    @Query("SELECT * FROM health_records ORDER BY date DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<HealthRecord>>

    @Query("SELECT * FROM health_records WHERE id = :id")
    fun observeById(id: Long): Flow<HealthRecord?>

    @Query("SELECT * FROM health_records WHERE id = :id")
    suspend fun getById(id: Long): HealthRecord?

    @Query("SELECT * FROM health_records WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): HealthRecord?

    @Query("SELECT * FROM health_records WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getInRange(startDate: String, endDate: String): Flow<List<HealthRecord>>

    // ---------- Update ----------

    @Update
    suspend fun update(record: HealthRecord)

    // ---------- Delete ----------

    @Delete
    suspend fun delete(record: HealthRecord)

    // ---------- Summary ----------

    /** Totals for a date range (inclusive). Returns zeros when there are no records. */
    @Query(
        """
        SELECT COALESCE(SUM(steps), 0)    AS totalSteps,
               COALESCE(SUM(calories), 0) AS totalCalories,
               COALESCE(SUM(water), 0)    AS totalWater
        FROM health_records
        WHERE date BETWEEN :startDate AND :endDate
        """
    )
    fun getSummary(startDate: String, endDate: String): Flow<HealthSummary>

    // ---------- Search ----------

    /**
     * Records between [startDate] and [endDate] (inclusive; either may be null for "no limit"),
     * sorted by [sortOrder], which must be a [com.example.healthmate.domain.SortOrder] name.
     *
     * Room cannot bind a column name into ORDER BY, so each sort option is a CASE expression.
     * Only the matching CASE produces values; the others are NULL and have no effect.
     * `date DESC` at the end is the default order and the tie-breaker.
     */
    @Query(
        """
        SELECT * FROM health_records
        WHERE (:startDate IS NULL OR date >= :startDate)
          AND (:endDate IS NULL OR date <= :endDate)
        ORDER BY
            CASE WHEN :sortOrder = 'OLDEST_FIRST' THEN date END ASC,
            CASE :sortOrder
                WHEN 'HIGHEST_STEPS' THEN steps
                WHEN 'HIGHEST_CALORIES' THEN calories
                WHEN 'HIGHEST_WATER' THEN water
            END DESC,
            date DESC
        """
    )
    fun search(startDate: String?, endDate: String?, sortOrder: String): Flow<List<HealthRecord>>
}
