package com.example.treemap.ui.components

import android.Manifest
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.treemap.data.model.EntryCategory
import com.example.treemap.data.model.MangroveZone
import com.example.treemap.data.model.TreeEntry
import com.example.treemap.ui.theme.MangroveTealPrimary
import com.example.treemap.util.ImageStorageHelper
import com.example.treemap.util.LocationHelper
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEntryDialog(
    lat: Double,
    lng: Double,
    zone: MangroveZone,
    initialReporter: String,
    onDismiss: () -> Unit,
    onSave: (TreeEntry) -> Unit
) {
    val context = LocalContext.current
    var currentLat by remember { mutableStateOf(lat) }
    var currentLng by remember { mutableStateOf(lng) }
    var isGpsSynced by remember { mutableStateOf(false) }
    
    // Default to At Risk / Severe issue or Thriving
    var selectedCategory by remember { mutableStateOf(EntryCategory.AT_RISK_DYING) }
    
    // Specific Co-Location / Landmark detail (e.g. Near the temple, near the road)
    var coLocationDetail by remember { mutableStateOf("") }
    
    // Comment / What is happening
    var comments by remember { mutableStateOf("") }
    var commentError by remember { mutableStateOf(false) }

    // Automatic Reporter from logged in session
    val activeReporterName = remember(initialReporter) {
        if (initialReporter.isNotBlank()) initialReporter else "Gaurav (Field User)"
    }

    // Multi-Image List (Captured via live camera)
    val attachedImages = remember { mutableStateListOf<String>() }

    // Pulsing light transition for glowing action buttons
    val infiniteTransition = rememberInfiniteTransition(label = "pulseGlow")
    val pulseGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlowAlpha"
    )

    // Quick landmark chips for co-location
    val landmarkPresets = listOf(
        "Near the Temple",
        "Near Main Road",
        "Near River / Bridge",
        "Near Forest Entry Gate",
        "Coastal Shoreline",
        "Behind Mangrove Jetty"
    )

    // Quick common issue descriptions (Display full, clear descriptions)
    val commonIssueSuggestions = listOf(
        "🚨 Tree cutting is happening in large amounts, forest area damaged" to "Tree cutting is happening in large amounts, forest area damaged",
        "🍂 Tree is drying and canopy withering" to "Tree is drying and canopy withering",
        "🪵 Illegal deforestation & wood transport" to "Illegal deforestation & wood transport",
        "💧 Tidal water blockage & hypersalinity" to "Tidal water blockage & hypersalinity",
        "🌱 Healthy mangrove trees and new shoots" to "Healthy mangrove trees and new shoots"
    )

    // Live Location Launcher
    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            LocationHelper.fetchLiveLocation(
                context = context,
                onSuccess = { newLat, newLng ->
                    currentLat = newLat
                    currentLng = newLng
                    isGpsSynced = true
                    Toast.makeText(context, "Location synced to live GPS", Toast.LENGTH_SHORT).show()
                },
                onError = { msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            Toast.makeText(context, "Permission required for live GPS", Toast.LENGTH_SHORT).show()
        }
    }

    // High-Resolution Live Camera Launcher
    var pendingPhotoPath by remember { mutableStateOf<String?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && pendingPhotoPath != null) {
            attachedImages.add(pendingPhotoPath!!)
            Toast.makeText(context, "Full HD field photo captured", Toast.LENGTH_SHORT).show()
        }
    }

    // High-Resolution Gallery Photo Picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            val savedPath = ImageStorageHelper.saveUriToInternalStorage(context, uri)
            if (savedPath != null) {
                attachedImages.add(savedPath)
            }
        }
        if (uris.isNotEmpty()) {
            Toast.makeText(context, "${uris.size} HD photo(s) selected", Toast.LENGTH_SHORT).show()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // 1. Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Report Field Issue",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFF00E676)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${zone.name} • %.5f, %.5f".format(currentLat, currentLng),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Automatic Logged-in Reporter Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Reporter: $activeReporterName",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Auto-submitted to Admin Panel",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = Color(0xFF10B981)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.20f)
                        ) {
                            Text(
                                text = "VERIFIED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981),
                                    fontSize = 9.5.sp
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. EYE-CATCHING LUMINOUS ACTION ROW: "Sync GPS" & "Take Photo"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // A) SYNC GPS BUTTON (WITH AMBIENT LIGHT & GLOW BORDER)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.Transparent,
                        border = BorderStroke(
                            width = 1.5.dp,
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF00E676).copy(alpha = pulseGlowAlpha),
                                    Color(0xFF00B0FF).copy(alpha = pulseGlowAlpha),
                                    Color(0xFF10B981).copy(alpha = pulseGlowAlpha)
                                )
                            )
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF00E676).copy(alpha = 0.15f),
                                        Color(0xFF00B0FF).copy(alpha = 0.08f),
                                        Color(0xFF11221D)
                                    )
                                )
                            )
                            .clickable {
                                if (LocationHelper.hasLocationPermission(context)) {
                                    LocationHelper.fetchLiveLocation(
                                        context = context,
                                        onSuccess = { newLat, newLng ->
                                            currentLat = newLat
                                            currentLng = newLng
                                            isGpsSynced = true
                                            Toast.makeText(context, "Location synced to live GPS", Toast.LENGTH_SHORT).show()
                                        },
                                        onError = { msg ->
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                } else {
                                    locationLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            }
                            .testTag("sync_gps_action_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            // Glowing Icon Backdrop
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFF00E676).copy(alpha = 0.40f),
                                                Color(0xFF00E676).copy(alpha = 0.10f)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isGpsSynced) Icons.Default.Check else Icons.Default.MyLocation,
                                    contentDescription = "Sync GPS",
                                    tint = if (isGpsSynced) Color(0xFF00E676) else Color(0xFF69F0AE),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (isGpsSynced) "GPS Synced" else "Sync GPS",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    ),
                                    color = Color(0xFFE8F5E9)
                                )
                                Text(
                                    text = if (isGpsSynced) "Live lock active" else "Tap to fetch live",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = Color(0xFF00E676)
                                )
                            }
                        }
                    }

                    // B) TAKE PHOTO BUTTON (WITH LUMINOUS SHINE & CAMERA GLOW)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.Transparent,
                        border = BorderStroke(
                            width = 1.5.dp,
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF34D399).copy(alpha = pulseGlowAlpha),
                                    Color(0xFF10B981).copy(alpha = pulseGlowAlpha),
                                    Color(0xFF86EFAC).copy(alpha = pulseGlowAlpha)
                                )
                            )
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF10B981).copy(alpha = 0.18f),
                                        Color(0xFF34D399).copy(alpha = 0.08f),
                                        Color(0xFF132720)
                                    )
                                )
                            )
                            .clickable {
                                val photoPair = ImageStorageHelper.createImageUriForCapture(context)
                                if (photoPair != null) {
                                    pendingPhotoPath = photoPair.second
                                    cameraLauncher.launch(photoPair.first)
                                } else {
                                    Toast.makeText(context, "Error initializing HD camera storage", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .testTag("live_camera_capture_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            // Glowing Camera Icon Backdrop
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFF34D399).copy(alpha = 0.40f),
                                                Color(0xFF34D399).copy(alpha = 0.10f)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Capture HD photo",
                                    tint = Color(0xFF6EE7B7),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (attachedImages.isEmpty()) "Take HD Photo" else "Photo (${attachedImages.size})",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    ),
                                    color = Color(0xFFE8F5E9)
                                )
                                Text(
                                    text = if (attachedImages.isEmpty()) "Full sensor quality" else "Tap to add more",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = Color(0xFF34D399)
                                )
                            }
                        }
                    }
                }

                // Secondary Gallery Pick Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { galleryLauncher.launch("image/*") }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Or select from Gallery",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    color = Color(0xFFE8F5E9)
                                )
                            )
                        }
                    }
                }

                // Thumbnail Strip for Captured Photos (With HD Badge)
                if (attachedImages.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(attachedImages) { imagePath ->
                            val imageModel = remember(imagePath) {
                                if (imagePath.startsWith("/")) File(imagePath) else imagePath
                            }
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(2.dp, Color(0xFF10B981), RoundedCornerShape(10.dp))
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(imageModel)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Captured photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // HD Quality Badge
                                Surface(
                                    shape = RoundedCornerShape(topEnd = 6.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.90f),
                                    modifier = Modifier.align(Alignment.BottomStart)
                                ) {
                                    Text(
                                        text = "HD",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Black,
                                            fontSize = 9.sp,
                                            color = Color.Black
                                        ),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = Color.Black.copy(alpha = 0.75f),
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(20.dp)
                                        .clickable { attachedImages.remove(imagePath) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove photo",
                                        tint = Color.White,
                                        modifier = Modifier
                                            .padding(3.dp)
                                            .fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4. Issue / Health Category Selection
                Text(
                    text = "SELECT ISSUE / HEALTH STATUS",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val statusOptions = listOf(
                        Triple(
                            EntryCategory.AT_RISK_DYING,
                            "Tree Cutting / Severe Forest Damage",
                            "Tree cutting in large amounts, severe damage or dieback"
                        ),
                        Triple(
                            EntryCategory.FAIR_GROWTH,
                            "Tree Drying / Dying Issues",
                            "Tree is drying, withering leaves, or tidal salinity issues"
                        ),
                        Triple(
                            EntryCategory.THRIVING_GROWTH,
                            "Thriving Growth / Good Condition",
                            "Dense green mangrove canopy, healthy roots and shoots"
                        )
                    )

                    statusOptions.forEach { (category, title, sub) ->
                        val isSelected = selectedCategory == category
                        val color = category.composeColor

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .background(
                                    if (isSelected) color.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface
                                )
                                .clickable { selectedCategory = category }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(color),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = category.icon,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    ),
                                    color = if (isSelected) color else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                // FULL, PROPER DESCRIPTION (No single-line truncation)
                                Text(
                                    text = sub,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 5. Co-Location / Specific Nearby Location
                Text(
                    text = "CO-LOCATION / NEARBY LOCATION",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = coLocationDetail,
                    onValueChange = { coLocationDetail = it },
                    label = { Text("Specific Location / Landmark") },
                    placeholder = { Text("e.g., Near the temple, near the road, coastal bridge…") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = Color(0xFF10B981)
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF10B981),
                        focusedLabelColor = Color(0xFF10B981)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Co-Location Shortcut Chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    landmarkPresets.forEach { preset ->
                        val isMatched = coLocationDetail.contains(preset)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isMatched) Color(0xFF10B981).copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            border = BorderStroke(
                                1.dp,
                                if (isMatched) Color(0xFF10B981) else Color.Transparent
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    coLocationDetail = if (coLocationDetail.isBlank()) preset else "$coLocationDetail, $preset"
                                }
                        ) {
                            Text(
                                text = "+ $preset",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp,
                                    color = if (isMatched) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 6. Issue Details & Comment
                Text(
                    text = "ISSUE DETAILS / COMMENT",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = comments,
                    onValueChange = {
                        comments = it
                        if (it.isNotBlank()) commentError = false
                    },
                    label = { Text("Describe the issue / observations") },
                    placeholder = { Text("e.g., Tree cutting is happening in large amounts and the forest area is damaged…") },
                    minLines = 3,
                    maxLines = 5,
                    isError = commentError,
                    supportingText = {
                        if (commentError) {
                            Text("Please enter a short comment describing the issue", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF10B981),
                        focusedLabelColor = Color(0xFF10B981)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Issue Text Suggestions - DISPLAYED IN FULL, PROPER FORMAT (NO TRUNCATION)
                Text(
                    text = "Quick Issue Templates (Tap to fill):",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    commonIssueSuggestions.forEach { (displayTitle, textToAppend) ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    comments = if (comments.isBlank()) textToAppend else "$comments. $textToAppend"
                                    commentError = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = displayTitle,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 7. Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (comments.trim().isEmpty()) {
                                commentError = true
                            } else {
                                val resolvedTitle = if (coLocationDetail.isNotBlank()) {
                                    coLocationDetail.trim()
                                } else {
                                    "${zone.sectorCode} Observation"
                                }

                                val resolvedSpecies = when (selectedCategory) {
                                    EntryCategory.AT_RISK_DYING -> "Tree Cutting / Severe Damage"
                                    EntryCategory.FAIR_GROWTH -> "Tree Drying / Dying Issue"
                                    EntryCategory.THRIVING_GROWTH -> "Healthy Mangrove Forest"
                                }

                                onSave(
                                    TreeEntry(
                                        lat = currentLat,
                                        lng = currentLng,
                                        category = selectedCategory.key,
                                        title = resolvedTitle,
                                        species = resolvedSpecies,
                                        zoneId = zone.id,
                                        imageUrls = attachedImages.joinToString("|||"),
                                        notes = comments.trim(),
                                        reporter = activeReporterName
                                    )
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("save_observation_submit_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MangroveTealPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Submit Report", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
