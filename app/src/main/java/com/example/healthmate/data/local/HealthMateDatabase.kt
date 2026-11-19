package com.example.healthmate.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/** The app's Room (SQLite) database. One instance is shared by the whole app. */
@Database(entities = [HealthRecord::class], version = 1, exportSchema = false)
abstract class HealthMateDatabase : RoomDatabase() {

    abstract fun healthRecordDao(): HealthRecordDao

    companion object {
        private const val DATABASE_NAME = "healthmate.db"

        @Volatile
        private var instance: HealthMateDatabase? = null

        fun getInstance(context: Context): HealthMateDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    HealthMateDatabase::class.java,
                    DATABASE_NAME
                )
                    .addCallback(SeedCallback)
                    .build()
                    .also { instance = it }
            }
    }

    /**
     * Fills the database with dummy data.
     *
     * Room calls `onCreate` only once: when the database file is first created (first launch,
     * or after the app's data is cleared). It is not called on later launches, so the seed
     * data is never inserted twice. Inserting here, inside the creation step, also means the
     * data is already there when the first screen reads from the database.
     */
    private object SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            SeedData.generate().forEach { record ->
                val values = ContentValues().apply {
                    put("date", record.date)
                    put("steps", record.steps)
                    put("calories", record.calories)
                    put("water", record.water)
                }
                db.insert("health_records", SQLiteDatabase.CONFLICT_IGNORE, values)
            }
        }
    }
}
