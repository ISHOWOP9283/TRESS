package com.example.treemap.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.treemap.data.model.EntryCategory
import com.example.treemap.data.model.MangroveZone
import com.example.treemap.data.model.TreeEntry
import com.example.treemap.ui.theme.MangroveTealPrimary
import org.json.JSONArray
import org.json.JSONObject

enum class MapLayerType {
    DEFAULT,
    SATELLITE,
    TERRAIN
}

class MapBridge(
    private val onMarkerTap: (Long) -> Unit,
    private val onZoneTap: (String) -> Unit,
    private val onMapTap: (Double, Double) -> Unit,
    private val onMapMove: (Double, Double, Float) -> Unit
) {
    @JavascriptInterface
    fun onMarkerClicked(id: Long) {
        onMarkerTap(id)
    }

    @JavascriptInterface
    fun onZoneClicked(zoneId: String) {
        onZoneTap(zoneId)
    }

    @JavascriptInterface
    fun onMapClicked(lat: Double, lng: Double) {
        onMapTap(lat, lng)
    }

    @JavascriptInterface
    fun onMapMoved(lat: Double, lng: Double, zoom: Float) {
        onMapMove(lat, lng, zoom)
    }
}

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
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
    onMapMoved: (Double, Double, Float) -> Unit = { _, _, _ -> },
    onZoomIn: () -> Unit = {},
    onZoomOut: () -> Unit = {},
    onZoomDelta: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var mapLayer by remember { mutableStateOf(MapLayerType.DEFAULT) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isMapLoaded by remember { mutableStateOf(false) }

    val filteredEntries = remember(entries, activeCategory) {
        if (activeCategory == null) entries else entries.filter { it.category == activeCategory.key }
    }

    val mapBridge = remember {
        MapBridge(
            onMarkerTap = { id ->
                val entry = entries.firstOrNull { it.id == id }
                if (entry != null) onEntrySelected(entry)
            },
            onZoneTap = { zId ->
                val zone = zones.firstOrNull { it.id == zId }
                if (zone != null) onZoneSelected(zone)
            },
            onMapTap = { lat, lng ->
                onMapTapped(lat, lng)
            },
            onMapMove = { lat, lng, zoom ->
                onMapMoved(lat, lng, zoom)
            }
        )
    }

    // Push state updates to Leaflet when markers / layer / zones change
    LaunchedEffect(isMapLoaded, filteredEntries, temporaryPin, userLocation, mapLayer, activeZone.id) {
        if (isMapLoaded) {
            webViewRef?.let { wv ->
                // Update Layer
                val layerName = when (mapLayer) {
                    MapLayerType.DEFAULT -> "roadmap"
                    MapLayerType.SATELLITE -> "satellite"
                    MapLayerType.TERRAIN -> "terrain"
                }
                wv.evaluateJavascript("setMapLayer('$layerName');", null)

                // Update Markers JSON
                val markersArray = JSONArray()
                filteredEntries.forEach { entry ->
                    val obj = JSONObject().apply {
                        put("id", entry.id)
                        put("title", entry.title)
                        put("species", entry.species)
                        put("category", entry.category)
                        put("color", when (entry.categoryEnum) {
                            EntryCategory.THRIVING_GROWTH -> "#10B981"
                            EntryCategory.FAIR_GROWTH -> "#F59E0B"
                            EntryCategory.AT_RISK_DYING -> "#EF4444"
                            else -> "#00897B"
                        })
                        put("lat", entry.lat)
                        put("lng", entry.lng)
                    }
                    markersArray.put(obj)
                }
                wv.evaluateJavascript("updateMarkers(${markersArray});", null)

                // Update Zones JSON
                val zonesArray = JSONArray()
                zones.forEach { z ->
                    val zObj = JSONObject().apply {
                        put("id", z.id)
                        put("name", z.name)
                        put("sectorCode", z.sectorCode)
                        put("centerLat", z.centerLat)
                        put("centerLng", z.centerLng)
                        put("color", when (z.id) {
                            "zone_a" -> "#10B981"
                            "zone_b" -> "#F59E0B"
                            "zone_c" -> "#EF4444"
                            else -> "#00897B"
                        })
                        val coordsArray = JSONArray()
                        z.polygonOffsets.forEach { (lat, lng) ->
                            val pt = JSONArray().apply {
                                put(lat)
                                put(lng)
                            }
                            coordsArray.put(pt)
                        }
                        put("coordinates", coordsArray)
                    }
                    zonesArray.put(zObj)
                }
                wv.evaluateJavascript("updateZones(${zonesArray}, '${activeZone.id}');", null)

                // Update Temp Pin
                if (temporaryPin != null) {
                    wv.evaluateJavascript("setTempPin(${temporaryPin.first}, ${temporaryPin.second});", null)
                } else {
                    wv.evaluateJavascript("clearTempPin();", null)
                }

                // Update User GPS Location
                if (userLocation != null) {
                    wv.evaluateJavascript("setUserLocation(${userLocation.first}, ${userLocation.second});", null)
                }
            }
        }
    }

    // Animate map camera smoothly when centerLat or centerLng is updated (e.g. from location search like Panvel)
    LaunchedEffect(isMapLoaded, centerLat, centerLng) {
        if (isMapLoaded) {
            val targetZoom = (zoomLevel * 10f).toInt().coerceIn(12, 18)
            webViewRef?.evaluateJavascript("flyToLocation($centerLat, $centerLng, $targetZoom);", null)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Full Real World Interactive Map (Leaflet with Official Google Maps tiles and gesture pass-through)
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.databaseEnabled = true
                    settings.setSupportZoom(true)
                    settings.builtInZoomControls = false
                    settings.displayZoomControls = false

                    // Ensure Android does not intercept horizontal/vertical drag events from the map
                    setOnTouchListener { v, event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                                v.parent?.requestDisallowInterceptTouchEvent(true)
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                v.parent?.requestDisallowInterceptTouchEvent(false)
                            }
                        }
                        false
                    }

                    addJavascriptInterface(mapBridge, "AndroidBridge")

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isMapLoaded = true
                        }
                    }

                    webChromeClient = WebChromeClient()

                    val htmlContent = getMapHtml(
                        initialLat = centerLat,
                        initialLng = centerLng,
                        initialZoom = (zoomLevel * 10f).toInt().coerceIn(11, 19)
                    )
                    loadDataWithBaseURL("https://appassets.androidplatform.net/", htmlContent, "text/html", "UTF-8", null)
                    webViewRef = this
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("interactive_map_canvas")
        )

        // 2. Google Maps Attribution & Live Coordinates Tag (Bottom Left)
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.White.copy(alpha = 0.94f),
            shadowElevation = 3.dp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 14.dp, bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Google",
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4285F4)
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(12.dp)
                        .background(Color(0xFFD1D5DB))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "%.4f°, %.4f°".format(centerLat, centerLng),
                    style = TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF374151)
                    )
                )
            }
        }

        // 3. Floating Google Maps Control Stack (Right Side: Layers, Compass, GPS, Directional Pan & Zoom)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Layer Switcher (Roadmap, Satellite, Terrain)
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 5.dp,
                modifier = Modifier.size(44.dp)
            ) {
                IconButton(
                    onClick = {
                        mapLayer = when (mapLayer) {
                            MapLayerType.DEFAULT -> MapLayerType.SATELLITE
                            MapLayerType.SATELLITE -> MapLayerType.TERRAIN
                            MapLayerType.TERRAIN -> MapLayerType.DEFAULT
                        }
                    },
                    modifier = Modifier.fillMaxSize().testTag("map_layer_toggle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "Map layer",
                        tint = MangroveTealPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Compass / Reset North & Recenter
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 5.dp,
                modifier = Modifier.size(44.dp)
            ) {
                IconButton(
                    onClick = {
                        onRecenter()
                        webViewRef?.evaluateJavascript("panToCoordinates(${activeZone.centerLat}, ${activeZone.centerLng});", null)
                    },
                    modifier = Modifier.fillMaxSize().testTag("compass_recenter_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = "Reset North & Recenter",
                        tint = Color(0xFFE53935),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Live GPS "My Location" Button
            Surface(
                shape = CircleShape,
                color = if (userLocation != null) MangroveTealPrimary else Color.White,
                shadowElevation = 6.dp,
                modifier = Modifier.size(46.dp)
            ) {
                IconButton(
                    onClick = {
                        onRequestLiveLocation()
                        userLocation?.let { (lat, lng) ->
                            webViewRef?.evaluateJavascript("flyToLocation($lat, $lng, 16);", null)
                        }
                    },
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
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Quick Pan Left / Right Buttons Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                shadowElevation = 5.dp,
                modifier = Modifier.width(44.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    IconButton(
                        onClick = {
                            webViewRef?.evaluateJavascript("panByOffset(-150, 0);", null)
                        },
                        modifier = Modifier.size(40.dp).testTag("pan_left_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Pan Left",
                            tint = Color(0xFF374151),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.width(28.dp),
                        thickness = 1.dp,
                        color = Color(0xFFE5E7EB)
                    )

                    IconButton(
                        onClick = {
                            webViewRef?.evaluateJavascript("panByOffset(150, 0);", null)
                        },
                        modifier = Modifier.size(40.dp).testTag("pan_right_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Pan Right",
                            tint = Color(0xFF374151),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Google Maps Style Zoom + / Zoom - Controls Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                shadowElevation = 5.dp,
                modifier = Modifier.width(44.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    IconButton(
                        onClick = {
                            onZoomIn()
                            webViewRef?.evaluateJavascript("map.zoomIn();", null)
                        },
                        modifier = Modifier.size(40.dp).testTag("zoom_in_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Zoom In",
                            tint = Color(0xFF374151),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.width(28.dp),
                        thickness = 1.dp,
                        color = Color(0xFFE5E7EB)
                    )

                    IconButton(
                        onClick = {
                            onZoomOut()
                            webViewRef?.evaluateJavascript("map.zoomOut();", null)
                        },
                        modifier = Modifier.size(40.dp).testTag("zoom_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Zoom Out",
                            tint = Color(0xFF374151),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Leaflet Real World Map HTML & JS Template
// -------------------------------------------------------------

private fun getMapHtml(initialLat: Double, initialLng: Double, initialZoom: Int): String {
    return """
<!DOCTYPE html>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
    <style>
        html, body, #map {
            width: 100%;
            height: 100%;
            margin: 0;
            padding: 0;
            overflow: hidden;
            background: #e5e3df;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            touch-action: pan-x pan-y !important;
            -webkit-user-select: none;
            user-select: none;
        }
        .leaflet-container {
            width: 100% !important;
            height: 100% !important;
            cursor: grab;
        }
        .leaflet-container:active {
            cursor: grabbing;
        }
        .leaflet-control-attribution, .leaflet-control-zoom {
            display: none !important;
        }
        .custom-google-marker {
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            cursor: pointer;
        }
        .custom-pin-head {
            width: 30px;
            height: 30px;
            border-radius: 50% 50% 50% 0;
            transform: rotate(-45deg);
            display: flex;
            align-items: center;
            justify-content: center;
            box-shadow: 0 3px 6px rgba(0,0,0,0.35);
            border: 2px solid #ffffff;
        }
        .custom-pin-core {
            width: 11px;
            height: 11px;
            background: #ffffff;
            border-radius: 50%;
            transform: rotate(45deg);
        }
        .custom-pin-label {
            margin-top: 4px;
            background: rgba(255,255,255,0.92);
            color: #111827;
            font-size: 10px;
            font-weight: 700;
            padding: 2px 6px;
            border-radius: 4px;
            box-shadow: 0 1px 3px rgba(0,0,0,0.2);
            white-space: nowrap;
            pointer-events: none;
            border: 1px solid #e5e7eb;
        }
        .user-pulse-marker {
            width: 22px;
            height: 22px;
            border-radius: 50%;
            background: #1A73E8;
            border: 3px solid #ffffff;
            box-shadow: 0 0 10px rgba(26, 115, 232, 0.7);
            position: relative;
        }
        .user-pulse-marker::after {
            content: '';
            position: absolute;
            top: -12px;
            left: -12px;
            right: -12px;
            bottom: -12px;
            border-radius: 50%;
            background: rgba(26, 115, 232, 0.25);
            animation: pulse-ring 1.8s cubic-bezier(0.215, 0.61, 0.355, 1) infinite;
        }
        @keyframes pulse-ring {
            0% { transform: scale(0.6); opacity: 1; }
            100% { transform: scale(1.6); opacity: 0; }
        }
        .temp-drop-pin {
            width: 24px;
            height: 24px;
            border-radius: 50%;
            background: #00897B;
            border: 3px solid #ffffff;
            box-shadow: 0 2px 8px rgba(0,0,0,0.4);
            display: flex;
            align-items: center;
            justify-content: center;
            color: white;
            font-size: 14px;
            font-weight: bold;
        }
        .sector-badge-label {
            background: rgba(0, 137, 123, 0.9);
            color: white;
            font-weight: 800;
            font-size: 11px;
            padding: 3px 8px;
            border-radius: 12px;
            border: 1.5px solid #ffffff;
            box-shadow: 0 2px 4px rgba(0,0,0,0.25);
            white-space: nowrap;
        }
    </style>
</head>
<body>
    <div id="map"></div>

    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
    <script>
        var map = L.map('map', {
            center: [$initialLat, $initialLng],
            zoom: $initialZoom,
            zoomControl: false,
            attributionControl: false,
            dragging: true,
            touchZoom: true,
            scrollWheelZoom: true,
            doubleClickZoom: true,
            boxZoom: true,
            tapHold: false,
            inertia: true,
            inertiaDeceleration: 3000,
            inertiaMaxSpeed: 1500
        });

        // Official Google Maps Tile Layers
        var googleRoadmapLayer = L.tileLayer('https://mt{s}.google.com/vt/lyrs=m&x={x}&y={y}&z={z}', {
            maxZoom: 21,
            maxNativeZoom: 20,
            subdomains: ['0', '1', '2', '3']
        }).addTo(map);

        var googleSatelliteLayer = L.tileLayer('https://mt{s}.google.com/vt/lyrs=y&x={x}&y={y}&z={z}', {
            maxZoom: 21,
            maxNativeZoom: 20,
            subdomains: ['0', '1', '2', '3']
        });

        var googleTerrainLayer = L.tileLayer('https://mt{s}.google.com/vt/lyrs=p&x={x}&y={y}&z={z}', {
            maxZoom: 21,
            maxNativeZoom: 20,
            subdomains: ['0', '1', '2', '3']
        });

        var currentTileLayer = googleRoadmapLayer;

        function setMapLayer(name) {
            map.removeLayer(currentTileLayer);
            if (name === 'satellite') {
                currentTileLayer = googleSatelliteLayer;
            } else if (name === 'terrain') {
                currentTileLayer = googleTerrainLayer;
            } else {
                currentTileLayer = googleRoadmapLayer;
            }
            currentTileLayer.addTo(map);
        }

        var markersGroup = L.layerGroup().addTo(map);
        var zonesGroup = L.layerGroup().addTo(map);
        var tempPinMarker = null;
        var userLocationMarker = null;

        function updateMarkers(markers) {
            markersGroup.clearLayers();
            markers.forEach(function(m) {
                var iconHtml = '<div class="custom-google-marker">' +
                    '<div class="custom-pin-head" style="background:' + m.color + ';">' +
                        '<div class="custom-pin-core"></div>' +
                    '</div>' +
                    '<div class="custom-pin-label">' + m.title + '</div>' +
                '</div>';

                var customIcon = L.divIcon({
                    html: iconHtml,
                    className: '',
                    iconSize: [80, 48],
                    iconAnchor: [40, 30]
                });

                var marker = L.marker([m.lat, m.lng], { icon: customIcon }).addTo(markersGroup);
                marker.on('click', function(e) {
                    L.DomEvent.stopPropagation(e);
                    if (window.AndroidBridge) {
                        window.AndroidBridge.onMarkerClicked(m.id);
                    }
                });
            });
        }

        function updateZones(zones, activeZoneId) {
            zonesGroup.clearLayers();
            zones.forEach(function(z) {
                var isActive = (z.id === activeZoneId);
                var polygon = L.polygon(z.coordinates, {
                    color: z.color,
                    fillColor: z.color,
                    fillOpacity: isActive ? 0.35 : 0.15,
                    weight: isActive ? 3 : 1.8,
                    dashArray: '8, 6'
                }).addTo(zonesGroup);

                polygon.on('click', function(e) {
                    L.DomEvent.stopPropagation(e);
                    if (window.AndroidBridge) {
                        window.AndroidBridge.onZoneClicked(z.id);
                    }
                });

                // Sector Badge Marker at center
                var badgeIcon = L.divIcon({
                    html: '<div class="sector-badge-label" style="background:' + z.color + ';">' + z.sectorCode + '</div>',
                    className: '',
                    iconSize: [60, 24],
                    iconAnchor: [30, 12]
                });
                var badgeMarker = L.marker([z.centerLat, z.centerLng], { icon: badgeIcon }).addTo(zonesGroup);
                badgeMarker.on('click', function(e) {
                    L.DomEvent.stopPropagation(e);
                    if (window.AndroidBridge) {
                        window.AndroidBridge.onZoneClicked(z.id);
                    }
                });
            });
        }

        function setTempPin(lat, lng) {
            if (tempPinMarker) {
                map.removeLayer(tempPinMarker);
            }
            var pinIcon = L.divIcon({
                html: '<div class="temp-drop-pin">+</div>',
                className: '',
                iconSize: [24, 24],
                iconAnchor: [12, 12]
            });
            tempPinMarker = L.marker([lat, lng], { icon: pinIcon }).addTo(map);
        }

        function clearTempPin() {
            if (tempPinMarker) {
                map.removeLayer(tempPinMarker);
                tempPinMarker = null;
            }
        }

        function setUserLocation(lat, lng) {
            if (userLocationMarker) {
                userLocationMarker.setLatLng([lat, lng]);
            } else {
                var userIcon = L.divIcon({
                    html: '<div class="user-pulse-marker"></div>',
                    className: '',
                    iconSize: [22, 22],
                    iconAnchor: [11, 11]
                });
                userLocationMarker = L.marker([lat, lng], { icon: userIcon }).addTo(map);
            }
        }

        function panToCoordinates(lat, lng) {
            map.panTo([lat, lng], { animate: true, duration: 0.5 });
        }

        function panByOffset(dx, dy) {
            map.panBy([dx, dy], { animate: true, duration: 0.35 });
        }

        function flyToLocation(lat, lng, zoom) {
            map.flyTo([lat, lng], zoom || 15, { animate: true, duration: 1.0 });
        }

        map.on('click', function(e) {
            if (window.AndroidBridge) {
                window.AndroidBridge.onMapClicked(e.latlng.lat, e.latlng.lng);
            }
        });

        map.on('moveend', function() {
            var c = map.getCenter();
            if (window.AndroidBridge) {
                window.AndroidBridge.onMapMoved(c.lat, c.lng, map.getZoom());
            }
        });
    </script>
</body>
</html>
    """.trimIndent()
}
