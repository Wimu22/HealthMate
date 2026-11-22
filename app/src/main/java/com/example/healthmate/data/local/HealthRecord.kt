package com.example.healthmate.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One day of health data. Stored in the `health_records` table.
 *
 * [date] is saved as text in `yyyy-MM-dd` format so it sorts correctly as a string,
 * and it is unique: there can only be one record per date.
 */
@Entity(
    tableName = "health_records",
    indices = [Index(value = ["date"], unique = true)]
)
data class HealthRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,
    val steps: Int,
    val calories: Int,
    val water: Int,
    /** Optional selfie stored in the app's private storage (extra feature). */
    @ColumnInfo(name = "photo_path")
    val photoPath: String? = null,
    /**
     * When the record was last saved or updated, in milliseconds since 1970 (UTC).
     * Null for the seeded dummy records, so Record View only shows it "if available" (spec 4.5).
     */
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long? = null
)
