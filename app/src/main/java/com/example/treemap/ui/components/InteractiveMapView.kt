package com.example.treemap.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.treemap.data.model.EntryCategory
import com.example.treemap.data.model.MangroveZone
import com.example.treemap.data.model.TreeEntry
import com.example.treemap.ui.theme.MangroveTealPrimary
import com.example.treemap.ui.theme.StatusAtRisk
import com.example.treemap.ui.theme.StatusFair
import com.example.treemap.ui.theme.StatusThriving

@Composable
fun InteractiveMapView(
    entries: List<TreeEntry>,
    zones: List<MangroveZone>,
    activeZone: MangroveZone,
    activeCategory: EntryCategory?,
    temporaryPin: Pair<Double, Double>?,
    userLocation: Pair<Double, Double>?,
    centerLat: Double,
    centerLng: Double,
    zoomLevel: Float,
    isFetchingLocation: Boolean = false,
    onMapTapped: (Double, Double) -> Unit,
    onEntrySelected: (TreeEntry) -> Unit,
    onZoneSelected: (MangroveZone) -> Unit,
    onAddPointClick: () -> Unit,
    onRecenter: () -> Unit,
    onRequestLiveLocation: () -> Unit,
    onPan: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var isSatelliteMode by remember { mutableStateOf(false) }

    val filteredEntries = remember(entries, activeCategory) {
        if (activeCategory == null) entries else entries.filter { it.category == activeCategory.key }
    }

    val textMeasurer = rememberTextMeasurer()

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(if (isSatelliteMode) Color(0xFF1B2822) else Color(0xFFE2EBE2))
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        // Coordinate projection to canvas pixels
        val scale = zoomLevel * 3800f

        fun latLngToScreen(lat: Double, lng: Double): Offset {
            val x = widthPx / 2f + ((lng - centerLng) * scale).toFloat()
            val y = heightPx / 2f - ((lat - centerLat) * scale).toFloat()
            return Offset(x, y)
        }

        fun screenToLatLng(x: Float, y: Float): Pair<Double, Double> {
            val lng = centerLng + (x - widthPx / 2f) / scale
            val lat = centerLat - (y - heightPx / 2f) / scale
            return Pair(lat, lng)
        }

        // Main Map Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("interactive_map_canvas")
                .pointerInput(centerLat, centerLng, zoomLevel) {
                    detectTransformGestures { _, pan, _, _ ->
                        if (pan != Offset.Zero) {
                            val dLng = -pan.x / scale
                            val dLat = pan.y / scale
                            onPan(dLat.toFloat(), dLng.toFloat())
                        }
                    }
                }
                .pointerInput(centerLat, centerLng, zoomLevel, filteredEntries, zones) {
                    detectTapGestures { tapOffset ->
                        // Check if tapped near an existing pin (within 28dp)
                        val tapRadius = 28.dp.toPx()
                        val hitEntry = filteredEntries.firstOrNull { entry ->
                            val markerScreen = latLngToScreen(entry.lat, entry.lng)
                            (tapOffset - markerScreen).getDistance() <= tapRadius
                        }

                        if (hitEntry != null) {
                            onEntrySelected(hitEntry)
                            return@detectTapGestures
                        }

                        // Check if tapped inside any sector zone
                        val hitZone = zones.firstOrNull { zone ->
                            val zoneCenter = latLngToScreen(zone.centerLat, zone.centerLng)
                            (tapOffset - zoneCenter).getDistance() <= 60.dp.toPx()
                        }

                        if (hitZone != null) {
                            onZoneSelected(hitZone)
                        } else {
                            val (lat, lng) = screenToLatLng(tapOffset.x, tapOffset.y)
                            onMapTapped(lat, lng)
                        }
                    }
                }
        ) {
            // 1. Draw Estuary Waterways & Shoreline Landscape
            drawCoastalTerrain(widthPx, heightPx, centerLat, centerLng, scale, isSatelliteMode)

            // 2. Draw Zone Sector Polygons & Outlines
            zones.forEach { zone ->
                val polygonScreenPoints = zone.polygonOffsets.map { (lat, lng) ->
                    latLngToScreen(lat, lng)
                }

                if (polygonScreenPoints.size >= 3) {
                    val path = Path().apply {
                        moveTo(polygonScreenPoints.first().x, polygonScreenPoints.first().y)
                        for (i in 1 until polygonScreenPoints.size) {
                            lineTo(polygonScreenPoints[i].x, polygonScreenPoints[i].y)
                        }
                        close()
                    }

                    // Translucent sector fill
                    val isCurrent = zone.id == activeZone.id
                    val alphaFill = if (isCurrent) 0.35f else 0.20f
                    drawPath(
                        path = path,
                        color = zone.strokeColor.copy(alpha = alphaFill),
                        style = Fill
                    )

                    // Sector stroke boundary
                    drawPath(
                        path = path,
                        color = zone.strokeColor,
                        style = Stroke(
                            width = if (isCurrent) 3.dp.toPx() else 1.8.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // Draw Sector Label
                    val centerScreen = latLngToScreen(zone.centerLat, zone.centerLng)
                    if (centerScreen.x in 0f..widthPx && centerScreen.y in 0f..heightPx) {
                        val textResult = textMeasurer.measure(
                            text = zone.sectorCode,
                            style = TextStyle(
                                color = zone.strokeColor,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp
                            )
                        )
                        drawText(
                            textLayoutResult = textResult,
                            topLeft = Offset(
                                centerScreen.x - textResult.size.width / 2f,
                                centerScreen.y - textResult.size.height - 22.dp.toPx()
                            )
                        )
                    }
                }
            }

            // 3. Draw Mangrove Status Pins (Green Leaf, Yellow Sprout, Red Warning)
            filteredEntries.forEach { entry ->
                val pos = latLngToScreen(entry.lat, entry.lng)
                if (pos.x in -60f..(widthPx + 60f) && pos.y in -60f..(heightPx + 60f)) {
                    drawMangroveMarker(pos, entry.categoryEnum)
                }
            }

            // 4. Draw Live User Location Marker (Pulsing Blue GPS Dot)
            userLocation?.let { (uLat, uLng) ->
                val uPos = latLngToScreen(uLat, uLng)
                if (uPos.x in -60f..(widthPx + 60f) && uPos.y in -60f..(heightPx + 60f)) {
                    drawLiveUserLocation(uPos, pulseScale)
                }
            }

            // 5. Draw Temporary Drop Pin
            temporaryPin?.let { (tLat, tLng) ->
                val tPos = latLngToScreen(tLat, tLng)
                drawTemporaryDropPin(tPos, pulseScale)
            }
        }

        // Top-Right Floating Map Controls (GPS / Live Location, Compass, Satellite Layer)
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Live GPS Location Button
            Surface(
                shape = CircleShape,
                color = if (userLocation != null) MangroveTealPrimary else Color.White,
                shadowElevation = 6.dp,
                modifier = Modifier.size(44.dp)
            ) {
                IconButton(
                    onClick = onRequestLiveLocation,
                    modifier = Modifier.fillMaxSize().testTag("live_gps_button")
                ) {
                    if (isFetchingLocation) {
                        CircularProgressIndicator(
                            strokeWidth = 2.5.dp,
                            color = if (userLocation != null) Color.White else MangroveTealPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            imageVector = if (userLocation != null) Icons.Default.GpsFixed else Icons.Default.MyLocation,
                            contentDescription = "Fetch Live GPS Location",
                            tint = if (userLocation != null) Color.White else MangroveTealPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.size(8.dp))

            // Recenter to Default Sector Map View
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 5.dp,
                modifier = Modifier.size(42.dp)
            ) {
                IconButton(
                    onClick = onRecenter,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "Recenter Map",
                        tint = MangroveTealPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.size(8.dp))

            // Satellite Layer Toggle
            Surface(
                shape = CircleShape,
                color = if (isSatelliteMode) MangroveTealPrimary else Color.White,
                shadowElevation = 5.dp,
                modifier = Modifier.size(42.dp)
            ) {
                IconButton(
                    onClick = { isSatelliteMode = !isSatelliteMode },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "Toggle satellite mode",
                        tint = if (isSatelliteMode) Color.White else MangroveTealPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawLiveUserLocation(pos: Offset, pulseScale: Float) {
    val baseRadius = 9.dp.toPx()

    // Outer radar wave pulse
    drawCircle(
        color = Color(0x332196F3),
        radius = baseRadius * pulseScale * 2.8f,
        center = pos
    )

    // Mid blue halo ring
    drawCircle(
        color = Color(0x4D1976D2),
        radius = baseRadius * 1.6f,
        center = pos
    )

    // Crisp white border
    drawCircle(
        color = Color.White,
        radius = baseRadius + 2.5.dp.toPx(),
        center = pos
    )

    // Solid blue GPS center
    drawCircle(
        color = Color(0xFF1E88E5),
        radius = baseRadius,
        center = pos
    )

    // Inner bright center dot
    drawCircle(
        color = Color.White,
        radius = baseRadius * 0.35f,
        center = pos
    )
}

private fun DrawScope.drawCoastalTerrain(
    width: Float,
    height: Float,
    centerLat: Double,
    centerLng: Double,
    scale: Float,
    isSatellite: Boolean
) {
    // Water basin background
    val waterColor = if (isSatellite) Color(0xFF1E3A3A) else Color(0xFFBFE0E9)
    val landColor = if (isSatellite) Color(0xFF23362A) else Color(0xFFD4E3CE)
    val roadColor = if (isSatellite) Color(0x33FFFFFF) else Color(0xFFF2EFE9)

    // Base land fill
    drawRect(color = landColor, size = Size(width, height))

    // Curved estuary water channel running through right & bottom
    val waterPath = Path().apply {
        moveTo(width * 0.72f, 0f)
        cubicTo(
            width * 0.65f, height * 0.3f,
            width * 0.85f, height * 0.6f,
            width * 0.55f, height * 0.82f
        )
        cubicTo(
            width * 0.35f, height * 0.95f,
            width * 0.1f, height * 0.88f,
            0f, height * 0.95f
        )
        lineTo(0f, height)
        lineTo(width, height)
        lineTo(width, 0f)
        close()
    }

    drawPath(path = waterPath, color = waterColor, style = Fill)

    // Secondary tidal creek
    val creekPath = Path().apply {
        moveTo(width * 0.45f, 0f)
        cubicTo(
            width * 0.48f, height * 0.25f,
            width * 0.38f, height * 0.45f,
            width * 0.42f, height * 0.6f
        )
    }
    drawPath(
        path = creekPath,
        color = waterColor.copy(alpha = 0.85f),
        style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Faint grid / road layout lines in northwest
    val gridColor = roadColor.copy(alpha = 0.6f)
    for (i in 1..4) {
        val y = height * 0.08f * i
        drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(width * 0.4f, y + 20f),
            strokeWidth = 2.5f
        )
    }
    for (j in 1..4) {
        val x = width * 0.09f * j
        drawLine(
            color = gridColor,
            start = Offset(x, 0f),
            end = Offset(x + 15f, height * 0.35f),
            strokeWidth = 2.5f
        )
    }
}

private fun DrawScope.drawMangroveMarker(
    pos: Offset,
    category: EntryCategory
) {
    val radius = 17.dp.toPx()
    val badgeColor = category.composeColor

    // Soft drop shadow
    drawCircle(
        color = Color(0x33000000),
        radius = radius + 3.dp.toPx(),
        center = pos + Offset(0f, 2.5.dp.toPx())
    )

    // Outer crisp white ring
    drawCircle(
        color = Color.White,
        radius = radius + 2.5.dp.toPx(),
        center = pos
    )

    // Colored badge circle
    drawCircle(
        color = badgeColor,
        radius = radius,
        center = pos
    )

    // Custom inner icon paths matching mockup
    when (category) {
        EntryCategory.THRIVING_GROWTH -> {
            // 3-leaf mangrove sprout (White)
            val centerLeaf = Path().apply {
                moveTo(pos.x, pos.y - radius * 0.58f)
                cubicTo(
                    pos.x + radius * 0.25f, pos.y - radius * 0.1f,
                    pos.x, pos.y + radius * 0.25f,
                    pos.x, pos.y + radius * 0.25f
                )
                cubicTo(
                    pos.x, pos.y + radius * 0.25f,
                    pos.x - radius * 0.25f, pos.y - radius * 0.1f,
                    pos.x, pos.y - radius * 0.58f
                )
                close()
            }
            drawPath(centerLeaf, Color.White, style = Fill)

            // Left leaf
            val leftLeaf = Path().apply {
                moveTo(pos.x - radius * 0.1f, pos.y + radius * 0.05f)
                cubicTo(
                    pos.x - radius * 0.55f, pos.y - radius * 0.15f,
                    pos.x - radius * 0.5f, pos.y + radius * 0.25f,
                    pos.x, pos.y + radius * 0.32f
                )
                close()
            }
            drawPath(leftLeaf, Color.White, style = Fill)

            // Right leaf
            val rightLeaf = Path().apply {
                moveTo(pos.x + radius * 0.1f, pos.y + radius * 0.05f)
                cubicTo(
                    pos.x + radius * 0.55f, pos.y - radius * 0.15f,
                    pos.x + radius * 0.5f, pos.y + radius * 0.25f,
                    pos.x, pos.y + radius * 0.32f
                )
                close()
            }
            drawPath(rightLeaf, Color.White, style = Fill)

            // Roots / stem base
            drawLine(
                color = Color.White,
                start = Offset(pos.x, pos.y + radius * 0.25f),
                end = Offset(pos.x, pos.y + radius * 0.52f),
                strokeWidth = 2.dp.toPx()
            )
        }

        EntryCategory.FAIR_GROWTH -> {
            // Upward Growth Trend Arrow with Sprout
            val arrowPath = Path().apply {
                moveTo(pos.x - radius * 0.45f, pos.y + radius * 0.35f)
                lineTo(pos.x - radius * 0.1f, pos.y)
                lineTo(pos.x + radius * 0.15f, pos.y + radius * 0.15f)
                lineTo(pos.x + radius * 0.45f, pos.y - radius * 0.35f)
            }
            drawPath(
                path = arrowPath,
                color = Color.White,
                style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Arrow head
            val arrowHead = Path().apply {
                moveTo(pos.x + radius * 0.15f, pos.y - radius * 0.35f)
                lineTo(pos.x + radius * 0.45f, pos.y - radius * 0.35f)
                lineTo(pos.x + radius * 0.45f, pos.y - radius * 0.05f)
            }
            drawPath(
                path = arrowHead,
                color = Color.White,
                style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }

        EntryCategory.AT_RISK_DYING -> {
            // Dying / Defoliated Leaf Warning Symbol
            val leafPath = Path().apply {
                moveTo(pos.x, pos.y - radius * 0.55f)
                cubicTo(
                    pos.x + radius * 0.5f, pos.y - radius * 0.15f,
                    pos.x + radius * 0.25f, pos.y + radius * 0.45f,
                    pos.x, pos.y + radius * 0.55f
                )
                cubicTo(
                    pos.x - radius * 0.25f, pos.y + radius * 0.45f,
                    pos.x - radius * 0.5f, pos.y - radius * 0.15f,
                    pos.x, pos.y - radius * 0.55f
                )
                close()
            }
            drawPath(leafPath, Color.White, style = Fill)

            // Inner alert vein cut
            drawLine(
                color = badgeColor,
                start = Offset(pos.x, pos.y - radius * 0.35f),
                end = Offset(pos.x, pos.y + radius * 0.4f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = badgeColor,
                start = Offset(pos.x - radius * 0.2f, pos.y - radius * 0.05f),
                end = Offset(pos.x, pos.y + radius * 0.1f),
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = badgeColor,
                start = Offset(pos.x + radius * 0.2f, pos.y - radius * 0.05f),
                end = Offset(pos.x, pos.y + radius * 0.1f),
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

private fun DrawScope.drawTemporaryDropPin(pos: Offset, pulseScale: Float) {
    val baseRadius = 20.dp.toPx()

    // Pulse wave
    drawCircle(
        color = MangroveTealPrimary.copy(alpha = 0.25f),
        radius = baseRadius * pulseScale * 1.4f,
        center = pos
    )

    // Inner glowing ring
    drawCircle(
        color = MangroveTealPrimary,
        radius = baseRadius,
        center = pos
    )

    drawCircle(
        color = Color.White,
        radius = baseRadius,
        center = pos,
        style = Stroke(width = 3.dp.toPx())
    )

    // Plus icon in center
    val arm = 7.dp.toPx()
    drawLine(
        color = Color.White,
        start = Offset(pos.x - arm, pos.y),
        end = Offset(pos.x + arm, pos.y),
        strokeWidth = 3.dp.toPx()
    )
    drawLine(
        color = Color.White,
        start = Offset(pos.x, pos.y - arm),
        end = Offset(pos.x, pos.y + arm),
        strokeWidth = 3.dp.toPx()
    )
}
