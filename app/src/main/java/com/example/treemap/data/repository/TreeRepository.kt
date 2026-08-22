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
        try {
            if (treeDao.count() == 0) {
                val now = System.currentTimeMillis()

                val img1a = ImageStorageHelper.createSampleMangrovePhoto(context, "Nursery Quadrant A1", "Sapling Stage", "Rhizophora apiculata", "#1B4D3E") ?: ""
                val img1b = ImageStorageHelper.createSampleMangrovePhoto(context, "A1 Prop Root Detail", "Root Density", "Rhizophora apiculata", "#0D382E") ?: ""
                val img2 = ImageStorageHelper.createSampleMangrovePhoto(context, "Tidal Ridge A2", "Avicennia Stand", "Avicennia alba", "#245A48") ?: ""
                val img3 = ImageStorageHelper.createSampleMangrovePhoto(context, "Estuary Confluence A3", "Sediment Assessment", "Sonneratia alba", "#5C5220") ?: ""
                val img4 = ImageStorageHelper.createSampleMangrovePhoto(context, "Outer Marsh A4", "Chlorosis Condition", "Rhizophora mucronata", "#5A2020") ?: ""
                val img5 = ImageStorageHelper.createSampleMangrovePhoto(context, "Delta Shoal B1", "Mature Canopy", "Bruguiera gymnorhiza", "#1F4E3F") ?: ""
                val img6 = ImageStorageHelper.createSampleMangrovePhoto(context, "Breakwater C1", "Erosion Inspection", "Rhizophora mangle", "#632828") ?: ""

                val sampleData = listOf(
                    TreeEntry(
                        lat = 1.3548,
                        lng = 103.8182,
                        category = EntryCategory.THRIVING_GROWTH.key,
                        title = "Nursery Quadrant A1",
                        notes = "Dense Rhizophora apiculata saplings, 92% leaf retention with robust aerial stilt roots.",
                        reporter = "Dr. Maya Lin",
                        zoneId = "zone_a",
                        species = "Rhizophora apiculata",
                        imageUrls = if (img1a.isNotBlank()) "$img1a|||$img1b" else "",
                        date = now - 3600_000 * 2
                    ),
                    TreeEntry(
                        lat = 1.3562,
                        lng = 103.8218,
                        category = EntryCategory.THRIVING_GROWTH.key,
                        title = "Tidal Ridge Station A2",
                        notes = "Avicennia alba flourishing along intertidal crest, healthy crab burrows indicating good soil oxygen.",
                        reporter = "Alex Rivera",
                        zoneId = "zone_a",
                        species = "Avicennia alba",
                        imageUrls = img2,
                        date = now - 3600_000 * 6
                    ),
                    TreeEntry(
                        lat = 1.3528,
                        lng = 103.8205,
                        category = EntryCategory.FAIR_GROWTH.key,
                        title = "Estuary Confluence A3",
                        notes = "Moderate sedimentation buildup, pneumatophore density acceptable with slight algal film.",
                        reporter = "Elena Vance",
                        zoneId = "zone_a",
                        species = "Sonneratia alba",
                        imageUrls = img3,
                        date = now - 3600_000 * 18
                    ),
                    TreeEntry(
                        lat = 1.3512,
                        lng = 103.8175,
                        category = EntryCategory.AT_RISK_DYING.key,
                        title = "Outer Marsh Border A4",
                        notes = "Tidal blockage behind drainage culvert, chlorosis on outer foliage and hypersaline crust.",
                        reporter = "Marcus Chen",
                        zoneId = "zone_a",
                        species = "Rhizophora mucronata",
                        imageUrls = img4,
                        date = now - 3600_000 * 36
                    ),
                    TreeEntry(
                        lat = 1.3495,
                        lng = 103.8260,
                        category = EntryCategory.THRIVING_GROWTH.key,
                        title = "Delta Shoal B1",
                        notes = "Bruguiera gymnorhiza mature trees establishing broad canopy cover and nursery habitat.",
                        reporter = "Sarah Miller",
                        zoneId = "zone_b",
                        species = "Bruguiera gymnorhiza",
                        imageUrls = img5,
                        date = now - 3600_000 * 12
                    ),
                    TreeEntry(
                        lat = 1.3468,
                        lng = 103.8242,
                        category = EntryCategory.FAIR_GROWTH.key,
                        title = "Mid-Lagoon Channel B2",
                        notes = "Recent boat wake erosion stabilized with coir geotextile logs, steady sapling growth.",
                        reporter = "Dr. Maya Lin",
                        zoneId = "zone_b",
                        species = "Avicennia marina",
                        imageUrls = "",
                        date = now - 3600_000 * 24
                    ),
                    TreeEntry(
                        lat = 1.3445,
                        lng = 103.8152,
                        category = EntryCategory.AT_RISK_DYING.key,
                        title = "Marine Breakwater C1",
                        notes = "High-energy wave impact exposed roots, urgent rock sill reinforcement requested.",
                        reporter = "Alex Rivera",
                        zoneId = "zone_c",
                        species = "Rhizophora mangle",
                        imageUrls = img6,
                        date = now - 3600_000 * 48
                    )
                )
                treeDao.insertAll(sampleData)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

