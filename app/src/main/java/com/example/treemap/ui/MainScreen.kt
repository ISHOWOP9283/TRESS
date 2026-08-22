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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.material3.TextButton
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
import com.example.treemap.ui.components.JournalListView
import com.example.treemap.ui.components.MangroveStatusLegend
import com.example.treemap.ui.components.ReportsDetailSheet
import com.example.treemap.ui.components.SectorOverviewCard
import com.example.treemap.ui.components.StatsOverview
import com.example.treemap.ui.components.TopMangroveAppBar
import com.example.treemap.ui.theme.MangroveDeepTeal
import com.example.treemap.ui.theme.MangroveTealPrimary
import com.example.treemap.util.LocationHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var showApiKeyInfoDialog by remember { mutableStateOf(false) }

    // Live GPS Location Permission Launcher
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
                snackbarHostState.showSnackbar("Location permission required to pinpoint your live location.")
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
            onDirectEmailAccess = { email ->
                viewModel.directEmailAccess(email)
            },
            errorMessage = uiState.loginErrorMessage
        )
        return
    }

    val user = currentUser!!

    ModalNavigationDrawer(
        drawerState = drawerState,
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
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (user.isAdmin) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
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

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = user.displayName,
                        style = MaterialTheme.typography.titleMedium.copy(
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

                Text(
                    text = "MONITORING SECTORS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                viewModel.zones.forEach { zone ->
                    val isSelected = uiState.selectedZone.id == zone.id && uiState.currentTab == AppTab.MAP
                    NavigationDrawerItem(
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(zone.strokeColor)
                            )
                        },
                        label = {
                            Text(
                                text = zone.name,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
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
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )
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
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopMangroveAppBar(
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    onMenuClick = {
                        coroutineScope.launch {
                            if (drawerState.isClosed) drawerState.open() else drawerState.close()
                        }
                    },
                    onProfileClick = {
                        if (user.isAdmin) {
                            viewModel.setTab(AppTab.ADMIN_PANEL)
                        } else {
                            coroutineScope.launch { drawerState.open() }
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.navigationBarsPadding()
                ) {
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

                    NavigationBarItem(
                        selected = uiState.currentTab == AppTab.JOURNAL,
                        onClick = { viewModel.setTab(AppTab.JOURNAL) },
                        icon = {
                            Icon(
                                imageVector = if (uiState.currentTab == AppTab.JOURNAL) Icons.Filled.ListAlt else Icons.Outlined.ListAlt,
                                contentDescription = "Journal"
                            )
                        },
                        label = { Text("Field Journal") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MangroveTealPrimary,
                            selectedTextColor = MangroveTealPrimary,
                            indicatorColor = MangroveTealPrimary.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier.testTag("nav_tab_journal")
                    )

                    NavigationBarItem(
                        selected = uiState.currentTab == AppTab.COMMUNITY_STATS,
                        onClick = { viewModel.setTab(AppTab.COMMUNITY_STATS) },
                        icon = {
                            Icon(
                                imageVector = if (uiState.currentTab == AppTab.COMMUNITY_STATS) Icons.Filled.Assessment else Icons.Outlined.Assessment,
                                contentDescription = "Analytics"
                            )
                        },
                        label = { Text("Impact & Stats") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MangroveTealPrimary,
                            selectedTextColor = MangroveTealPrimary,
                            indicatorColor = MangroveTealPrimary.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier.testTag("nav_tab_stats")
                    )

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
                            label = { Text("Admin Panel") },
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
                    .padding(innerPadding)
            ) {
                when (uiState.currentTab) {
                    AppTab.MAP -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // 1. Interactive Map View
                            InteractiveMapView(
                                entries = entries,
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
                                modifier = Modifier.fillMaxSize()
                            )

                            // 2. Floating Mangrove Status Legend (adapts position when card collapses/expands)
                            MangroveStatusLegend(
                                activeCategory = uiState.selectedCategory,
                                onCategoryClick = { viewModel.setCategoryFilter(it) },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(
                                        end = 12.dp,
                                        bottom = if (uiState.isSectorOverviewCollapsed) 80.dp else 260.dp
                                    )
                            )

                            // 3. Bottom Overview Card (Collapsible & Draggable)
                            SectorOverviewCard(
                                zone = uiState.selectedZone,
                                isCollapsed = uiState.isSectorOverviewCollapsed,
                                onToggleCollapse = { viewModel.toggleSectorOverviewCollapsed() },
                                onLogObservation = { viewModel.openAddDialogAtCurrentCenter() },
                                onViewDetails = { viewModel.openReportsDialog() },
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )
                        }
                    }

                    AppTab.JOURNAL -> {
                        JournalListView(
                            entries = entries,
                            activeCategory = uiState.selectedCategory,
                            onSelectEntry = { entry -> viewModel.focusOnEntry(entry) },
                            onDeleteEntry = { id -> viewModel.deleteEntry(id) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    AppTab.COMMUNITY_STATS -> {
                        AnalyticsDashboardView(
                            stats = stats,
                            zones = viewModel.zones,
                            activeCategory = uiState.selectedCategory,
                            onCategoryClick = { viewModel.setCategoryFilter(it) },
                            onSelectZone = { zone ->
                                viewModel.selectZone(zone)
                                viewModel.setTab(AppTab.MAP)
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

    if (uiState.isReportsDialogOpen) {
        val zoneEntries = entries.filter { it.zoneId == uiState.selectedZone.id }
        ReportsDetailSheet(
            zone = uiState.selectedZone,
            entriesInZone = zoneEntries,
            onDismiss = { viewModel.closeReportsDialog() },
            onSelectEntry = { entry ->
                viewModel.selectEntry(entry)
                viewModel.focusOnEntry(entry)
            }
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

@Composable
private fun AnalyticsDashboardView(
    stats: com.example.treemap.data.model.EntryStats,
    zones: List<MangroveZone>,
    activeCategory: EntryCategory?,
    onCategoryClick: (EntryCategory?) -> Unit,
    onSelectZone: (MangroveZone) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Community Impact & Analytics",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Real-time ecological indicators across coastal sectors",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            StatsOverview(
                stats = stats,
                activeCategory = activeCategory,
                onCategoryClick = onCategoryClick
            )
        }

        item {
            Text(
                text = "SECTOR HEALTH OVERVIEW",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(zones) { zone ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSelectZone(zone) }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = zone.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MangroveTealPrimary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "${zone.totalAreaHectares} ha",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MangroveTealPrimary
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = zone.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${zone.thrivingPercent}% Thriving",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = com.example.treemap.ui.theme.StatusThriving
                        )
                        Text(
                            text = "${zone.fairPercent}% Fair",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = com.example.treemap.ui.theme.StatusFair
                        )
                        Text(
                            text = "${zone.atRiskPercent}% At Risk",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = com.example.treemap.ui.theme.StatusAtRisk
                        )
                    }
                }
            }
        }
    }
}
