package com.example.treemap.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class PlaceSearchResult(
    val title: String,
    val subtitle: String,
    val lat: Double,
    val lng: Double,
    val isMangroveZone: Boolean = false
)

object LocationHelper {

    val POPULAR_MANGROVE_HUBS = listOf(
        PlaceSearchResult(
            title = "Panvel",
            subtitle = "Navi Mumbai, Maharashtra, India",
            lat = 18.9894,
            lng = 73.1175,
            isMangroveZone = true
        ),
        PlaceSearchResult(
            title = "Panvel Creek Mangrove Belt",
            subtitle = "Raigad District, Maharashtra (Intertidal Wetland)",
            lat = 18.9950,
            lng = 73.0850,
            isMangroveZone = true
        ),
        PlaceSearchResult(
            title = "Khandeshwar Mangrove Wetland",
            subtitle = "Panvel, Navi Mumbai, Maharashtra",
            lat = 18.9912,
            lng = 73.1023,
            isMangroveZone = true
        ),
        PlaceSearchResult(
            title = "Navi Mumbai Coastal Belt",
            subtitle = "Thane Creek Estuary, Maharashtra",
            lat = 19.0330,
            lng = 73.0297,
            isMangroveZone = true
        ),
        PlaceSearchResult(
            title = "Thane Creek Flamingo Sanctuary",
            subtitle = "Airoli / Bhandup, Mumbai Metropolitan Region",
            lat = 19.1670,
            lng = 72.9980,
            isMangroveZone = true
        ),
        PlaceSearchResult(
            title = "Uran Intertidal Mudflats",
            subtitle = "Navi Mumbai Coastal Region, Maharashtra",
            lat = 18.8841,
            lng = 72.9342,
            isMangroveZone = true
        ),
        PlaceSearchResult(
            title = "Mumbai Coastal Mangroves",
            subtitle = "Mahim Nature Park & Gorai Creek, Mumbai",
            lat = 19.0550,
            lng = 72.8450,
            isMangroveZone = true
        ),
        PlaceSearchResult(
            title = "Kharghar Hills & Creek",
            subtitle = "Navi Mumbai, Maharashtra",
            lat = 19.0473,
            lng = 73.0699,
            isMangroveZone = true
        ),
        PlaceSearchResult(
            title = "Alibaug Coastal Estuary",
            subtitle = "Raigad, Maharashtra",
            lat = 18.6414,
            lng = 72.8722,
            isMangroveZone = true
        ),
        PlaceSearchResult(
            title = "Ratnagiri Estuary Mangroves",
            subtitle = "Bhatye Estuary, Maharashtra",
            lat = 16.9902,
            lng = 73.3120,
            isMangroveZone = true
        ),
        PlaceSearchResult(
            title = "Sindhudurg Marine Zone",
            subtitle = "Malvan & Karli River Mangrove Belt",
            lat = 16.0384,
            lng = 73.5594,
            isMangroveZone = true
        ),
        PlaceSearchResult(
            title = "Sundarbans National Mangrove Biosphere",
            subtitle = "West Bengal, India",
            lat = 21.9497,
            lng = 89.1833,
            isMangroveZone = true
        ),
        PlaceSearchResult(
            title = "Pichavaram Mangrove Forest",
            subtitle = "Chidambaram, Tamil Nadu, India",
            lat = 11.4286,
            lng = 79.7820,
            isMangroveZone = true
        ),
        PlaceSearchResult(
            title = "Bhitarkanika National Park",
            subtitle = "Kendrapara, Odisha, India",
            lat = 20.7247,
            lng = 86.8661,
            isMangroveZone = true
        ),
        PlaceSearchResult(
            title = "Coringa Wildlife Sanctuary",
            subtitle = "Kakinada, Andhra Pradesh, India",
            lat = 16.8920,
            lng = 82.2858,
            isMangroveZone = true
        ),
        PlaceSearchResult(
            title = "Dr. Salim Ali Bird Sanctuary",
            subtitle = "Chorão Island, Mandovi River, Goa",
            lat = 15.5292,
            lng = 73.8644,
            isMangroveZone = true
        ),
        PlaceSearchResult(
            title = "Kochi Mangrove Forest Buffer",
            subtitle = "Vembanad Lake Estuary, Kerala",
            lat = 9.9312,
            lng = 76.2673,
            isMangroveZone = true
        ),
        PlaceSearchResult(
            title = "Port Blair Mangrove Marine Reserve",
            subtitle = "South Andaman Island, India",
            lat = 11.6234,
            lng = 92.7265,
            isMangroveZone = true
        ),
        PlaceSearchResult(
            title = "Central Tidal Nursery (Sector A)",
            subtitle = "Estuary Mangrove Research Base",
            lat = 1.3521,
            lng = 103.8198,
            isMangroveZone = true
        )
    )

    fun getSuggestions(query: String): List<PlaceSearchResult> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return emptyList()

        return POPULAR_MANGROVE_HUBS.filter {
            it.title.contains(trimmed, ignoreCase = true) ||
            it.subtitle.contains(trimmed, ignoreCase = true)
        }.take(5)
    }

    suspend fun resolveLocation(context: Context, query: String): PlaceSearchResult? = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext null

        // 1. Check if user typed coordinates like "18.9894, 73.1175"
        val coordParts = trimmed.split(",", " ").filter { it.isNotBlank() }
        if (coordParts.size == 2) {
            val lat = coordParts[0].toDoubleOrNull()
            val lng = coordParts[1].toDoubleOrNull()
            if (lat != null && lng != null && lat in -90.0..90.0 && lng in -180.0..180.0) {
                return@withContext PlaceSearchResult(
                    title = "Coordinates",
                    subtitle = "%.4f, %.4f".format(lat, lng),
                    lat = lat,
                    lng = lng
                )
            }
        }

        // 2. Direct Hub Match (Exact or substring, e.g. "Panvel", "panvel", "navi mumbai", "thane", etc.)
        val presetMatch = POPULAR_MANGROVE_HUBS.find {
            it.title.equals(trimmed, ignoreCase = true) ||
            it.title.contains(trimmed, ignoreCase = true) ||
            trimmed.contains(it.title, ignoreCase = true)
        }
        if (presetMatch != null) {
            return@withContext presetMatch
        }

        // 3. Android Native Geocoder Lookup (Supports any real city/place worldwide)
        try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses: List<Address>? = geocoder.getFromLocationName(trimmed, 3)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    val featureName = addr.featureName ?: addr.locality ?: trimmed
                    val subText = listOfNotNull(addr.locality, addr.adminArea, addr.countryName).joinToString(", ")
                    return@withContext PlaceSearchResult(
                        title = featureName,
                        subtitle = if (subText.isNotBlank()) subText else "Geocoded Location",
                        lat = addr.latitude,
                        lng = addr.longitude
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 4. Subtitle match fallback
        val subtitleMatch = POPULAR_MANGROVE_HUBS.find {
            it.subtitle.contains(trimmed, ignoreCase = true)
        }
        if (subtitleMatch != null) {
            return@withContext subtitleMatch
        }

        null
    }

    fun hasLocationPermission(context: Context): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocation || coarseLocation
    }

    @SuppressLint("MissingPermission")
    fun fetchLiveLocation(
        context: Context,
        onSuccess: (lat: Double, lng: Double) -> Unit,
        onError: (message: String) -> Unit = {}
    ) {
        if (!hasLocationPermission(context)) {
            onError("Location permission not granted.")
            return
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (locationManager == null) {
            onError("Location service unavailable.")
            return
        }

        // Try getting best last known location first
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )

        var bestLocation: Location? = null
        for (provider in providers) {
            try {
                if (locationManager.isProviderEnabled(provider)) {
                    val loc = locationManager.getLastKnownLocation(provider)
                    if (loc != null) {
                        if (bestLocation == null || loc.accuracy < bestLocation.accuracy || loc.time > bestLocation.time) {
                            bestLocation = loc
                        }
                    }
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (bestLocation != null) {
            onSuccess(bestLocation.latitude, bestLocation.longitude)
        }

        // Request live single update from GPS or Network
        val activeProvider = when {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> null
        }

        if (activeProvider != null) {
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    onSuccess(location.latitude, location.longitude)
                    try {
                        locationManager.removeUpdates(this)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            try {
                locationManager.requestSingleUpdate(activeProvider, listener, Looper.getMainLooper())
            } catch (e: Exception) {
                if (bestLocation == null) {
                    onError("Could not acquire GPS signal.")
                }
            }
        } else if (bestLocation == null) {
            onError("Location providers disabled. Please enable GPS in device settings.")
        }
    }
}
