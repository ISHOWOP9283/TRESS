package com.example.treemap.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.treemap.data.model.EntryCategory
import com.example.treemap.data.model.MangroveZone
import com.example.treemap.ui.admin.AdminDashboardScreen
import com.example.treemap.ui.auth.LoginScreen
import com.example.treemap.ui.components.AddEntryDialog
import com.example.treemap.ui.components.EntryDetailSheet
import com.example.treemap.ui.components.InteractiveMapView
import com.example.treemap.ui.components.TopMangroveAppBar
import com.example.treemap.ui.components.UserAnalysisScreen
import com.example.treemap.ui.components.UserPlacesImpactScreen
import com.example.treemap.ui.theme.MangroveDeepTeal
import com.example.treemap.ui.theme.MangroveTealPrimary
import com.example.treemap.util.LocationHelper
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }

    var showApiKeyInfoDialog by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            viewModel.setFetchingLocation(true)
            LocationHelper.fetchLiveLocation(
                context = context,
                onSuccess = { lat, lng ->
                    viewModel.updateUserLiveLocation(lat, lng)
                },
                onError = { msg ->
                    viewModel.setFetchingLocation(false)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(msg)
                    }
                }
            )
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Location permission required for live GPS sync.")
            }
        }
    }

    fun requestLiveLocation() {
        if (LocationHelper.hasLocationPermission(context)) {
            viewModel.setFetchingLocation(true)
            LocationHelper.fetchLiveLocation(
                context = context,
                onSuccess = { lat, lng ->
                    viewModel.updateUserLiveLocation(lat, lng)
                },
                onError = { msg ->
                    viewModel.setFetchingLocation(false)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(msg)
                    }
                }
            )
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            viewModel.clearToastMessage()
        }
    }

    // Gate screen behind Auth if no user is signed in
    if (currentUser == null) {
        LoginScreen(
            onLogin = { emailOrUser, pass ->
                viewModel.login(emailOrUser, pass)
            },
            errorMessage = uiState.loginErrorMessage
        )
        return
    }

    val user = currentUser!!

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(300.dp)
            ) {
                // Drawer Header with User Profile Details
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MangroveDeepTeal)
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            com.example.treemap.ui.components.MapTreeLogoBadge(
                                size = 42.dp,
                                showText = false
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "MapTree",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = "Project Tomorrow",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFFA7F3D0),
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (user.isAdmin) Color(0xFFE57373) else Color(0xFF81C784)
                        ) {
                            Text(
                                text = user.roleLabel.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = user.displayName,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Navigation Items
                if (user.isAdmin) {
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                Icons.Default.SupervisorAccount,
                                contentDescription = null,
                                tint = MangroveTealPrimary
                            )
                        },
                        label = { Text("Admin Control & Database", fontWeight = FontWeight.Bold) },
                        selected = uiState.currentTab == AppTab.ADMIN_PANEL,
                        onClick = {
                            viewModel.setTab(AppTab.ADMIN_PANEL)
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MangroveTealPrimary.copy(alpha = 0.12f),
                            selectedTextColor = MangroveTealPrimary
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )
                }

                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = null,
                            tint = MangroveTealPrimary
                        )
                    },
                    label = { Text("Map View") },
                    selected = uiState.currentTab == AppTab.MAP,
                    onClick = {
                        viewModel.setTab(AppTab.MAP)
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            tint = MangroveTealPrimary
                        )
                    },
                    label = { Text("My Analysis & Accuracy") },
                    selected = uiState.currentTab == AppTab.ANALYSIS,
                    onClick = {
                        viewModel.setTab(AppTab.ANALYSIS)
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = MangroveTealPrimary
                        )
                    },
                    label = { Text("My Uploaded Places") },
                    selected = uiState.currentTab == AppTab.MY_PLACES,
                    onClick = {
                        viewModel.setTab(AppTab.MY_PLACES)
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                // Only show monitoring sectors to Admin users
                if (user.isAdmin) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "MONITORING SECTORS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )

                    viewModel.zones.forEach { zone ->
                        val isSelected = uiState.selectedZone.id == zone.id && uiState.currentTab == AppTab.MAP
                        NavigationDrawerItem(
                            icon = {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(zone.strokeColor)
                                )
                            },
                            label = {
                                Text(
                                    text = zone.name,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            },
                            selected = isSelected,
                            onClick = {
                                viewModel.setTab(AppTab.MAP)
                                viewModel.selectZone(zone)
                                coroutineScope.launch { drawerState.close() }
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = MangroveTealPrimary.copy(alpha = 0.12f),
                                selectedTextColor = MangroveTealPrimary
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 1.dp)
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                )

                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = MangroveTealPrimary
                        )
                    },
                    label = { Text("Google Maps Status") },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        showApiKeyInfoDialog = true
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    label = { Text("Log Out (${user.username})", color = MaterialTheme.colorScheme.error) },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        viewModel.logout()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                // Bottom Brand Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    com.example.treemap.ui.components.MapTreeLogoBadge(
                        size = 32.dp,
                        showText = false
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "MapTree v2.4",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = "Project Tomorrow",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (uiState.currentTab != AppTab.MAP) {
                    TopMangroveAppBar(
                        searchQuery = uiState.searchQuery,
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        onProfileClick = {
                            coroutineScope.launch { drawerState.open() }
                        },
                        onSearchSubmit = { viewModel.searchAndNavigate(context, it) },
                        onSelectPlaceResult = { viewModel.selectPlaceSearchResult(it) },
                        activeCategory = uiState.selectedCategory,
                        onCategorySelected = { viewModel.setCategoryFilter(it) }
                    )
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    // Tab 1: Map View
                    NavigationBarItem(
                        selected = uiState.currentTab == AppTab.MAP,
                        onClick = { viewModel.setTab(AppTab.MAP) },
                        icon = {
                            Icon(
                                imageVector = if (uiState.currentTab == AppTab.MAP) Icons.Filled.Map else Icons.Outlined.Map,
                                contentDescription = "Map"
                            )
                        },
                        label = { Text("Map View") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MangroveTealPrimary,
                            selectedTextColor = MangroveTealPrimary,
                            indicatorColor = MangroveTealPrimary.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier.testTag("nav_tab_map")
                    )

                    // Tab 2: User Analysis (Replaces Field Journey)
                    NavigationBarItem(
                        selected = uiState.currentTab == AppTab.ANALYSIS,
                        onClick = { viewModel.setTab(AppTab.ANALYSIS) },
                        icon = {
                            Icon(
                                imageVector = if (uiState.currentTab == AppTab.ANALYSIS) Icons.Filled.Analytics else Icons.Outlined.Analytics,
                                contentDescription = "Analysis"
                            )
                        },
                        label = { Text("Analysis") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MangroveTealPrimary,
                            selectedTextColor = MangroveTealPrimary,
                            indicatorColor = MangroveTealPrimary.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier.testTag("nav_tab_analysis")
                    )

                    // Tab 3: User's Uploaded Places & Impact (Shows user's places data)
                    NavigationBarItem(
                        selected = uiState.currentTab == AppTab.MY_PLACES,
                        onClick = { viewModel.setTab(AppTab.MY_PLACES) },
                        icon = {
                            Icon(
                                imageVector = if (uiState.currentTab == AppTab.MY_PLACES) Icons.Filled.Place else Icons.Outlined.Place,
                                contentDescription = "Places"
                            )
                        },
                        label = { Text("Impact & Status") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MangroveTealPrimary,
                            selectedTextColor = MangroveTealPrimary,
                            indicatorColor = MangroveTealPrimary.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier.testTag("nav_tab_places")
                    )

                    // Tab 4: Admin Panel (Only for Admin users)
                    if (user.isAdmin) {
                        NavigationBarItem(
                            selected = uiState.currentTab == AppTab.ADMIN_PANEL,
                            onClick = { viewModel.setTab(AppTab.ADMIN_PANEL) },
                            icon = {
                                Icon(
                                    imageVector = if (uiState.currentTab == AppTab.ADMIN_PANEL) Icons.Filled.AdminPanelSettings else Icons.Outlined.AdminPanelSettings,
                                    contentDescription = "Admin"
                                )
                            },
                            label = { Text("Admin") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MangroveTealPrimary,
                                selectedTextColor = MangroveTealPrimary,
                                indicatorColor = MangroveTealPrimary.copy(alpha = 0.12f)
                            ),
                            modifier = Modifier.testTag("nav_tab_admin")
                        )
                    }
                }
            },
            modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (uiState.currentTab == AppTab.MAP) androidx.compose.foundation.layout.PaddingValues(0.dp) else innerPadding)
            ) {
                when (uiState.currentTab) {
                    AppTab.MAP -> {
                        val filteredMapEntries = remember(entries, uiState.searchQuery) {
                            if (uiState.searchQuery.isBlank()) {
                                entries
                            } else {
                                val query = uiState.searchQuery.trim().lowercase()
                                entries.filter {
                                    it.title.lowercase().contains(query) ||
                                    it.species.lowercase().contains(query) ||
                                    (it.notes?.lowercase()?.contains(query) == true) ||
                                    it.zoneId.lowercase().contains(query)
                                }
                            }
                        }

                        Box(modifier = Modifier.fillMaxSize()) {
                            // 1. Google Maps Fullscreen Clean Interactive Map View (No swipe down card)
                            InteractiveMapView(
                                entries = filteredMapEntries,
                                zones = viewModel.zones,
                                activeZone = uiState.selectedZone,
                                activeCategory = uiState.selectedCategory,
                                temporaryPin = uiState.temporaryPin,
                                userLocation = uiState.userLiveLocation,
                                centerLat = uiState.centerLat,
                                centerLng = uiState.centerLng,
                                zoomLevel = uiState.zoomLevel,
                                isFetchingLocation = uiState.isFetchingLocation,
                                onMapTapped = { lat, lng -> viewModel.onMapTapped(lat, lng) },
                                onEntrySelected = { entry -> viewModel.selectEntry(entry) },
                                onZoneSelected = { zone -> viewModel.selectZone(zone) },
                                onAddPointClick = { viewModel.openAddDialogAtCurrentCenter() },
                                onRecenter = { viewModel.recenter() },
                                onRequestLiveLocation = { requestLiveLocation() },
                                onPan = { dLat, dLng -> viewModel.pan(dLat, dLng) },
                                onMapMoved = { lat, lng, zoom -> viewModel.updateMapCenter(lat, lng, zoom) },
                                onZoomIn = { viewModel.zoomIn() },
                                onZoomOut = { viewModel.zoomOut() },
                                onZoomDelta = { viewModel.adjustZoom(it) },
                                modifier = Modifier.fillMaxSize()
                            )

                            // 2. Floating Google Maps Search Pill (with Profile button, no separate menu button)
                            TopMangroveAppBar(
                                searchQuery = uiState.searchQuery,
                                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                onProfileClick = {
                                    coroutineScope.launch { drawerState.open() }
                                },
                                onSearchSubmit = { viewModel.searchAndNavigate(context, it) },
                                onSelectPlaceResult = { viewModel.selectPlaceSearchResult(it) },
                                activeCategory = uiState.selectedCategory,
                                onCategorySelected = { viewModel.setCategoryFilter(it) },
                                modifier = Modifier.align(Alignment.TopCenter)
                            )

                            // 3. Floating Quick Action Button for Logging Observations directly on full map
                            ExtendedFloatingActionButton(
                                onClick = { viewModel.openAddDialogAtCurrentCenter() },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.AddLocation,
                                        contentDescription = "Report Place"
                                    )
                                },
                                text = { Text("Report Issue", fontWeight = FontWeight.Bold) },
                                containerColor = MangroveTealPrimary,
                                contentColor = Color.White,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(end = 16.dp, bottom = innerPadding.calculateBottomPadding() + 16.dp)
                                    .testTag("fab_report_issue_button")
                            )
                        }
                    }

                    AppTab.ANALYSIS -> {
                        UserAnalysisScreen(
                            currentUser = user,
                            allEntries = entries,
                            onReportNewClick = {
                                viewModel.setTab(AppTab.MAP)
                                viewModel.openAddDialogAtCurrentCenter()
                            },
                            onViewMyPlacesClick = {
                                viewModel.setTab(AppTab.MY_PLACES)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    AppTab.MY_PLACES -> {
                        UserPlacesImpactScreen(
                            currentUser = user,
                            allEntries = entries,
                            onSelectEntry = { entry -> viewModel.focusOnEntry(entry) },
                            onDeleteEntry = { id -> viewModel.deleteEntry(id) },
                            onReportNewClick = {
                                viewModel.setTab(AppTab.MAP)
                                viewModel.openAddDialogAtCurrentCenter()
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    AppTab.ADMIN_PANEL -> {
                        AdminDashboardScreen(
                            entries = entries,
                            users = allUsers,
                            zones = viewModel.zones,
                            stats = stats,
                            onDeleteEntry = { id -> viewModel.deleteEntry(id) },
                            onGrantAccess = { email, name, role, pass ->
                                viewModel.grantAccessToEmail(email, name, role, pass)
                            },
                            onDeleteUser = { userId ->
                                viewModel.deleteUser(userId)
                            },
                            onToggleUserActive = { userAccount ->
                                viewModel.toggleUserActive(userAccount)
                            },
                            onNavigateToMap = { entry ->
                                viewModel.focusOnEntry(entry)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    // Dialogs & Sheets
    if (uiState.isAddDialogOpen && uiState.temporaryPin != null) {
        val (lat, lng) = uiState.temporaryPin!!
        AddEntryDialog(
            lat = lat,
            lng = lng,
            zone = uiState.selectedZone,
            initialReporter = user.displayName,
            onDismiss = { viewModel.closeAddDialog() },
            onSave = { entry -> viewModel.saveEntry(entry) }
        )
    }

    uiState.selectedEntry?.let { entry ->
        EntryDetailSheet(
            entry = entry,
            onDismiss = { viewModel.selectEntry(null) },
            onDelete = { id -> viewModel.deleteEntry(id) }
        )
    }

    if (showApiKeyInfoDialog) {
        AlertDialog(
            onDismissRequest = { showApiKeyInfoDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.VpnKey,
                    contentDescription = null,
                    tint = MangroveTealPrimary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Google Maps API & Access System",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Current User: ${user.displayName} (${user.email})\nRole: ${user.roleLabel}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Admin access can be managed directly in the Admin Panel tab to add new volunteer or officer emails with instant access.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showApiKeyInfoDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MangroveTealPrimary)
                ) {
                    Text("Done")
                }
            }
        )
    }
}
