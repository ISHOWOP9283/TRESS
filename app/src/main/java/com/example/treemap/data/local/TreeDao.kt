package com.example.treemap.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.treemap.data.model.TreeEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface TreeDao {
    @Query("SELECT * FROM entries ORDER BY date DESC")
    fun getAllEntries(): Flow<List<TreeEntry>>

    @Query("SELECT * FROM entries WHERE id = :id LIMIT 1")
    suspend fun getEntryById(id: Long): TreeEntry?

    @Query("SELECT * FROM entries WHERE category = :category ORDER BY date DESC")
    fun getEntriesByCategory(category: String): Flow<List<TreeEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: TreeEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<TreeEntry>)

    @Query("DELETE FROM entries WHERE id = :id")
    suspend fun deleteEntryById(id: Long)

    @Query("DELETE FROM entries")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM entries")
    suspend fun count(): Int
}
