package com.example.treemap.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "entries")
data class TreeEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val lat: Double,
    val lng: Double,
    val category: String, // "thriving_growth", "fair_growth", "at_risk_dying"
    val notes: String?,
    val reporter: String,
    val zoneId: String = "zone_a",
    val title: String = "Monitoring Station",
    val species: String = "Rhizophora mangle",
    val imageUrls: String = "", // Comma-separated list of image file paths/URIs
    val date: Long = System.currentTimeMillis()
) {
    val categoryEnum: EntryCategory
        get() = EntryCategory.fromKey(category)

    val imageList: List<String>
        get() = if (imageUrls.isBlank()) emptyList() else imageUrls.split("|||").filter { it.isNotBlank() }
}

