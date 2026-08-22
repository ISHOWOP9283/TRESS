package com.example.treemap.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class EntryCategory(
    val key: String,
    val label: String,
    val colorHex: Long,
    val description: String
) {
    THRIVING_GROWTH(
        key = "thriving_growth",
        label = "Thriving Growth",
        colorHex = 0xFF2E7D32,
        description = "Healthy mangrove canopy, dense prop roots, active fauna"
    ),
    FAIR_GROWTH(
        key = "fair_growth",
        label = "Fair Growth",
        colorHex = 0xFFD97706,
        description = "Moderate canopy coverage, stable salinity, regenerating shoots"
    ),
    AT_RISK_DYING(
        key = "at_risk_dying",
        label = "At Risk / Dying",
        colorHex = 0xFFDC2626,
        description = "Tidal blockage, hypersalinity, dieback or coastal erosion"
    );

    val composeColor: Color get() = Color(colorHex)

    val icon: ImageVector
        get() = when (this) {
            THRIVING_GROWTH -> Icons.Default.Spa
            FAIR_GROWTH -> Icons.Default.TrendingUp
            AT_RISK_DYING -> Icons.Default.WarningAmber
        }

    companion object {
        fun fromKey(key: String): EntryCategory {
            return entries.find { it.key.equals(key, ignoreCase = true) } ?: when {
                key.contains("thriv", true) || key.contains("plant", true) -> THRIVING_GROWTH
                key.contains("fair", true) || key.contains("trash", true) -> FAIR_GROWTH
                else -> AT_RISK_DYING
            }
        }
    }
}

