package com.example.healthmate.data.repository

import com.example.healthmate.data.local.HealthRecord
import com.example.healthmate.data.local.HealthRecordDao
import com.example.healthmate.domain.HealthSummary
import com.example.healthmate.domain.SortOrder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The single place ViewModels get health data from (Screen → ViewModel → Repository → DAO).
 *
 * Besides passing calls to the DAO, it keeps selfie files in sync with the database:
 * when a record is deleted, or its photo is replaced/removed, the old image file is deleted too.
 */
class HealthRecordRepository(
    private val dao: HealthRecordDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    fun getAllRecords(): Flow<List<HealthRecord>> = dao.getAll()

    fun getRecentRecords(limit: Int): Flow<List<HealthRecord>> = dao.getRecent(limit)

    fun observeRecord(id: Long): Flow<HealthRecord?> = dao.observeById(id)

    suspend fun getRecord(id: Long): HealthRecord? = dao.getById(id)

    suspend fun getRecordByDate(date: String): HealthRecord? = dao.getByDate(date)

    fun getRecordsInRange(startDate: String, endDate: String): Flow<List<HealthRecord>> =
        dao.getInRange(startDate, endDate)

    fun getSummary(startDate: String, endDate: String): Flow<HealthSummary> =
        dao.getSummary(startDate, endDate)

    fun searchRecords(startDate: String?, endDate: String?, sortOrder: SortOrder): Flow<List<HealthRecord>> =
        dao.search(startDate, endDate, sortOrder.name)

    /** Returns the new record's id. Throws `SQLiteConstraintException` if the date is already used. */
    suspend fun addRecord(record: HealthRecord): Long = dao.insert(record)

    /** Updates a record and deletes its previous photo file if the photo was replaced or removed. */
    suspend fun updateRecord(record: HealthRecord) {
        val oldPhotoPath = dao.getById(record.id)?.photoPath
        dao.update(record)
        if (oldPhotoPath != null && oldPhotoPath != record.photoPath) {
            deletePhotoFile(oldPhotoPath)
        }
    }

    /** Deletes a record and its photo file, if it has one. */
    suspend fun deleteRecord(record: HealthRecord) {
        dao.delete(record)
        record.photoPath?.let { deletePhotoFile(it) }
    }

    private suspend fun deletePhotoFile(path: String) = withContext(ioDispatcher) {
        File(path).delete()
    }
}
