package com.example.treemap.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.treemap.data.local.TreeDao
import com.example.treemap.data.model.EntryCategory
import com.example.treemap.data.model.EntryStats
import com.example.treemap.data.model.TreeEntry
import com.example.treemap.data.remote.SupabaseConfig
import com.example.treemap.data.remote.SupabaseService
import com.example.treemap.util.ImageStorageHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Collections

class TreeRepository(
    private val treeDao: TreeDao,
    private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("mangrove_mapper_prefs", Context.MODE_PRIVATE)
    private val supabaseService = SupabaseService(context)
    private val scope = CoroutineScope(Dispatchers.IO)
    private val tag = "TreeRepository"
    private val deletedIds = Collections.synchronizedSet(mutableSetOf<Long>())

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

    /**
     * Inserts an entry locally immediately for instant snappy UI (< 5ms),
     * and asynchronously uploads any attached photos to Supabase Storage
     * and syncs the observation with the Supabase database.
     */
    suspend fun insert(entry: TreeEntry): Long = withContext(Dispatchers.IO) {
        val uniqueId = if (entry.id != 0L) entry.id else System.currentTimeMillis()
        deletedIds.remove(uniqueId)
        val immediateEntry = entry.copy(id = uniqueId)

        // 1. Immediately save into local Room database for instantaneous UI feedback
        val rowId = treeDao.insertEntry(immediateEntry)

        // 2. Launch background sync to Supabase without blocking the UI thread
        if (supabaseService.isConfigured()) {
            scope.launch {
                try {
                    // Upload images to Supabase storage or encode as portable base64
                    val processedImageUrls = if (immediateEntry.imageUrls.isNotBlank()) {
                        val urls = immediateEntry.imageList.map { path ->
                            if (path.startsWith("/") || path.startsWith("file://")) {
                                val file = File(path.removePrefix("file://"))
                                if (file.exists() && file.length() > 0) {
                                    val uploadResult = supabaseService.uploadImage(file)
                                    if (uploadResult.isSuccess) {
                                        uploadResult.getOrThrow()
                                    } else {
                                        // Portable fallback: compress & base64 encode
                                        val b64 = ImageStorageHelper.encodeImageToBase64(file, 600, 70)
                                        if (b64 != null) "data:image/jpeg;base64,$b64" else path
                                    }
                                } else {
                                    path
                                }
                            } else {
                                path
                            }
                        }
                        urls.joinToString("|||")
                    } else {
                        immediateEntry.imageUrls
                    }

                    val syncedEntry = immediateEntry.copy(imageUrls = processedImageUrls)

                    // Update Room if image URLs changed to public URL / base64
                    if (syncedEntry.imageUrls != immediateEntry.imageUrls) {
                        treeDao.insertEntry(syncedEntry)
                    }

                    // Upsert entry to Supabase cloud table
                    val cloudResult = supabaseService.upsertEntry(syncedEntry)
                    if (cloudResult.isSuccess) {
                        Log.d(tag, "Observation successfully synced to Supabase: ${syncedEntry.title} (ID: $uniqueId)")
                        SupabaseConfig.setLastSyncTime(context, System.currentTimeMillis())
                    } else {
                        Log.w(tag, "Supabase upsert failed: ${cloudResult.exceptionOrNull()?.message}")
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Failed to sync entry to Supabase in background", e)
                }
            }
        }

        rowId
    }

    suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        deletedIds.add(id)
        treeDao.deleteEntryById(id)
        if (supabaseService.isConfigured()) {
            try {
                val delResult = supabaseService.deleteEntry(id)
                Log.d(tag, "Remote delete result for $id: ${delResult.isSuccess}")
            } catch (e: Exception) {
                Log.e(tag, "Failed to delete entry from Supabase", e)
            }
        }
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        treeDao.clearAll()
    }

    /**
     * Synchronizes all entries between local Room SQLite and Supabase cloud database.
     * Accurately removes remote deleted entries so they never resurrect.
     */
    suspend fun syncWithCloud(): Result<Int> = withContext(Dispatchers.IO) {
        if (!supabaseService.isConfigured()) {
            return@withContext Result.failure(Exception("Supabase is not configured yet."))
        }

        try {
            // 1. Fetch remote entries from Supabase
            val remoteResult = supabaseService.fetchAllEntries()
            if (remoteResult.isFailure) {
                return@withContext Result.failure(remoteResult.exceptionOrNull() ?: Exception("Unknown error fetching remote entries"))
            }

            val remoteEntries = remoteResult.getOrDefault(emptyList())
            val remoteIds = remoteEntries.map { it.id }.toSet()

            // 2. Check local Room entries
            val localEntries = treeDao.getAllEntriesList()
            val now = System.currentTimeMillis()

            // 3. Remove local entries that were deleted on remote
            for (local in localEntries) {
                if (!remoteIds.contains(local.id)) {
                    val isRecentOfflineCreation = (now - local.date) < 20_000L && !deletedIds.contains(local.id)
                    if (isRecentOfflineCreation) {
                        supabaseService.upsertEntry(local)
                    } else {
                        treeDao.deleteEntryById(local.id)
                    }
                }
            }

            // 4. Update/Insert all active remote entries into Room
            val activeRemoteEntries = remoteEntries.filter { !deletedIds.contains(it.id) }
            if (activeRemoteEntries.isNotEmpty()) {
                treeDao.insertAll(activeRemoteEntries)
            }

            SupabaseConfig.setLastSyncTime(context, System.currentTimeMillis())
            Result.success(activeRemoteEntries.size)
        } catch (e: Exception) {
            Log.e(tag, "Error during cloud sync", e)
            Result.failure(e)
        }
    }

    fun getSavedReporter(): String {
        return prefs.getString("saved_reporter_name", "Alex Rivera") ?: "Alex Rivera"
    }

    fun saveReporter(name: String) {
        prefs.edit().putString("saved_reporter_name", name).apply()
    }

    suspend fun seedSampleDataIfEmpty() {
        // Automatically perform cloud sync on launch if Supabase is configured
        if (supabaseService.isConfigured()) {
            try {
                syncWithCloud()
            } catch (e: Exception) {
                Log.w(tag, "Auto-sync on startup skipped: ${e.message}")
            }
        }
    }
}

