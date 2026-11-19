package com.example.healthmate.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

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
                ).build().also { instance = it }
            }
    }
}
