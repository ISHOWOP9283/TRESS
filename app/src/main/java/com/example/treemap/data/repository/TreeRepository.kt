package com.example.treemap.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.treemap.data.local.TreeDao
import com.example.treemap.data.model.EntryCategory
import com.example.treemap.data.model.EntryStats
import com.example.treemap.data.model.TreeEntry
import com.example.treemap.util.ImageStorageHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class TreeRepository(
    private val treeDao: TreeDao,
    private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("mangrove_mapper_prefs", Context.MODE_PRIVATE)

    val allEntries: Flow<List<TreeEntry>> = treeDao.getAllEntries()
        .catch { e ->
            e.printStackTrace()
            emit(emptyList())
        }

    val stats: Flow<EntryStats> = allEntries.map { list ->
        val thriving = list.count { it.category == EntryCategory.THRIVING_GROWTH.key }
        val fair = list.count { it.category == EntryCategory.FAIR_GROWTH.key }
        val atRisk = list.count { it.category == EntryCategory.AT_RISK_DYING.key }
        EntryStats(
            total = list.size,
            thrivingCount = thriving,
            fairCount = fair,
            atRiskCount = atRisk
        )
    }.catch { e ->
        e.printStackTrace()
        emit(EntryStats())
    }

    suspend fun insert(entry: TreeEntry): Long = treeDao.insertEntry(entry)

    suspend fun delete(id: Long) = treeDao.deleteEntryById(id)

    suspend fun clearAll() = treeDao.clearAll()

    fun getSavedReporter(): String {
        return prefs.getString("saved_reporter_name", "Alex Rivera") ?: "Alex Rivera"
    }

    fun saveReporter(name: String) {
        prefs.edit().putString("saved_reporter_name", name).apply()
    }

    suspend fun seedSampleDataIfEmpty() {
        // No random mock reports are seeded. Only real reports submitted by users/admins are stored.
    }
}

