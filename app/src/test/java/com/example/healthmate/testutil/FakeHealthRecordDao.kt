package com.example.healthmate.testutil

import com.example.healthmate.data.local.HealthRecord
import com.example.healthmate.data.local.HealthRecordDao
import com.example.healthmate.domain.HealthSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * An in-memory stand-in for the Room DAO, so ViewModels can be unit tested without a device.
 * Like Room, its Flows emit again whenever the records change.
 *
 * The real sorting is SQL in [HealthRecordDao.search]; this fake only filters by date and keeps
 * newest first, and records the arguments it was called with so tests can check them.
 * [failSearch] / [failDelete] simulate database errors.
 */
class FakeHealthRecordDao(initial: List<HealthRecord> = emptyList()) : HealthRecordDao {

    val records = MutableStateFlow(initial)

    data class SearchCall(val startDate: String?, val endDate: String?, val sortOrder: String)

    val searchCalls = mutableListOf<SearchCall>()
    var failSearch = false
    var failDelete = false

    override suspend fun insert(record: HealthRecord): Long {
        val id = (records.value.maxOfOrNull { it.id } ?: 0) + 1
        records.update { it + record.copy(id = id) }
        return id
    }

    override suspend fun insertAll(records: List<HealthRecord>) {
        records.forEach { insert(it) }
    }

    override fun getAll(): Flow<List<HealthRecord>> = records.map { it.sortedByDescending(HealthRecord::date) }

    override fun getRecent(limit: Int): Flow<List<HealthRecord>> = getAll().map { it.take(limit) }

    override fun observeById(id: Long): Flow<HealthRecord?> = records.map { list -> list.find { it.id == id } }

    override suspend fun getById(id: Long): HealthRecord? = records.value.find { it.id == id }

    override suspend fun getByDate(date: String): HealthRecord? = records.value.find { it.date == date }

    override fun getInRange(startDate: String, endDate: String): Flow<List<HealthRecord>> =
        getAll().map { list -> list.filter { it.date in startDate..endDate } }

    override suspend fun update(record: HealthRecord) {
        records.update { list -> list.map { if (it.id == record.id) record else it } }
    }

    override suspend fun delete(record: HealthRecord) {
        if (failDelete) throw IllegalStateException("Simulated database error")
        records.update { list -> list.filterNot { it.id == record.id } }
    }

    override fun getSummary(startDate: String, endDate: String): Flow<HealthSummary> =
        getInRange(startDate, endDate).map { list ->
            HealthSummary(
                totalSteps = list.sumOf { it.steps.toLong() },
                totalCalories = list.sumOf { it.calories.toLong() },
                totalWater = list.sumOf { it.water.toLong() }
            )
        }

    override fun search(startDate: String?, endDate: String?, sortOrder: String): Flow<List<HealthRecord>> {
        searchCalls += SearchCall(startDate, endDate, sortOrder)
        if (failSearch) return kotlinx.coroutines.flow.flow { throw IllegalStateException("Simulated database error") }
        return getAll().map { list ->
            list.filter { (startDate == null || it.date >= startDate) && (endDate == null || it.date <= endDate) }
        }
    }
}
