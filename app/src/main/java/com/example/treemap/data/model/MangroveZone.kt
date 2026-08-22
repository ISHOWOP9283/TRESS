package com.example.treemap.data.model

import androidx.compose.ui.graphics.Color

data class MangroveZone(
    val id: String,
    val name: String,
    val sectorCode: String,
    val description: String,
    val totalAreaHectares: Int,
    val thrivingPercent: Int,
    val fairPercent: Int,
    val atRiskPercent: Int,
    val canopyTrend: List<Float>, // 6 monthly readings
    val centerLat: Double,
    val centerLng: Double,
    val polygonOffsets: List<Pair<Double, Double>>, // lat, lng vertices
    val fillColor: Color,
    val strokeColor: Color
) {
    companion object {
        val SAMPLE_ZONES = listOf(
            MangroveZone(
                id = "zone_a",
                name = "Zone A: Sector Overview",
                sectorCode = "Sector A",
                description = "North Estuary Tidal Basin & Nursery Shoreline",
                totalAreaHectares = 125,
                thrivingPercent = 65,
                fairPercent = 20,
                atRiskPercent = 15,
                canopyTrend = listOf(45f, 52f, 50f, 60f, 63f, 72f),
                centerLat = 1.3521,
                centerLng = 103.8198,
                polygonOffsets = listOf(
                    Pair(1.3565, 103.8160),
                    Pair(1.3580, 103.8210),
                    Pair(1.3540, 103.8245),
                    Pair(1.3505, 103.8215),
                    Pair(1.3510, 103.8155)
                ),
                fillColor = Color(0x332E7D32),
                strokeColor = Color(0xFF2E7D32)
            ),
            MangroveZone(
                id = "zone_b",
                name = "Zone B: Estuary Delta",
                sectorCode = "Sector B",
                description = "Central Delta Mangrove & Mudflat Channels",
                totalAreaHectares = 98,
                thrivingPercent = 45,
                fairPercent = 35,
                atRiskPercent = 20,
                canopyTrend = listOf(40f, 44f, 48f, 46f, 51f, 54f),
                centerLat = 1.3480,
                centerLng = 103.8250,
                polygonOffsets = listOf(
                    Pair(1.3540, 103.8245),
                    Pair(1.3520, 103.8300),
                    Pair(1.3450, 103.8280),
                    Pair(1.3460, 103.8220),
                    Pair(1.3505, 103.8215)
                ),
                fillColor = Color(0x33D97706),
                strokeColor = Color(0xFFD97706)
            ),
            MangroveZone(
                id = "zone_c",
                name = "Zone C: Coastal Buffer",
                sectorCode = "Sector C",
                description = "South-West Marine Fringe & Wave Break Zone",
                totalAreaHectares = 84,
                thrivingPercent = 30,
                fairPercent = 30,
                atRiskPercent = 40,
                canopyTrend = listOf(55f, 52f, 47f, 42f, 38f, 35f),
                centerLat = 1.3460,
                centerLng = 103.8150,
                polygonOffsets = listOf(
                    Pair(1.3510, 103.8155),
                    Pair(1.3460, 103.8220),
                    Pair(1.3410, 103.8180),
                    Pair(1.3420, 103.8110),
                    Pair(1.3480, 103.8115)
                ),
                fillColor = Color(0x33DC2626),
                strokeColor = Color(0xFFDC2626)
            )
        )
    }
}
