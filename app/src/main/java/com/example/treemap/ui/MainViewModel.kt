package com.example.treemap.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.treemap.data.model.EntryCategory
import com.example.treemap.data.model.EntryStats
import com.example.treemap.data.model.MangroveZone
import com.example.treemap.data.model.TreeEntry
import com.example.treemap.data.model.UserAccount
import com.example.treemap.data.repository.TreeRepository
import com.example.treemap.data.repository.UserRepository
import com.example.treemap.util.LocationHelper
import com.example.treemap.util.PlaceSearchResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab(val title: String) {
    MAP("Map View"),
    ANALYSIS("Analysis"),
    MY_PLACES("Impact & Status"),
    ADMIN_PANEL("Admin Panel")
}

data class UiState(
    val currentTab: AppTab = AppTab.MAP,
    val selectedCategory: EntryCategory? = null,
    val selectedEntry: TreeEntry? = null,
    val selectedZone: MangroveZone = MangroveZone.SAMPLE_ZONES.first(),
    val temporaryPin: Pair<Double, Double>? = null,
    val userLiveLocation: Pair<Double, Double>? = null,
    val isFetchingLocation: Boolean = false,
    val isSectorOverviewCollapsed: Boolean = false,
    val centerLat: Double = 1.3521,
    val centerLng: Double = 103.8198,
    val zoomLevel: Float = 1.6f,
    val isAddDialogOpen: Boolean = false,
    val isReportsDialogOpen: Boolean = false,
    val isDrawerOpen: Boolean = false,
    val searchQuery: String = "",
    val lastReporterName: String = "Field Observer Alex",
    val toastMessage: String? = null,
    val loginErrorMessage: String? = null
)

class MainViewModel(
    private val repository: TreeRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    val entries: StateFlow<List<TreeEntry>> = repository.allEntries
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val stats: StateFlow<EntryStats> = repository.stats
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = EntryStats()
        )

    val allUsers: StateFlow<List<UserAccount>> = userRepository.allUsers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val currentUser: StateFlow<UserAccount?> = userRepository.currentUser

    val zones: List<MangroveZone> = MangroveZone.SAMPLE_ZONES

    private val _uiState = MutableStateFlow(
        UiState(
            lastReporterName = repository.getSavedReporter(),
            selectedZone = MangroveZone.SAMPLE_ZONES.first()
        )
    )
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                userRepository.seedDefaultUsersIfEmpty()
                repository.seedSampleDataIfEmpty()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- Authentication & Access Management ---

    fun login(emailOrUser: String, pass: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loginErrorMessage = null)
            val user = userRepository.authenticate(emailOrUser, pass)
            if (user != null) {
                _uiState.value = _uiState.value.copy(
                    lastReporterName = user.displayName,
                    currentTab = if (user.isAdmin) AppTab.ADMIN_PANEL else AppTab.MAP,
                    toastMessage = "Welcome, ${user.displayName} (${user.roleLabel})"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    loginErrorMessage = "Invalid credentials. Use 'admin' & 'admin' or contact administrator."
                )
            }
        }
    }

    fun directEmailAccess(email: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loginErrorMessage = null)
            if (email.isBlank()) {
                _uiState.value = _uiState.value.copy(loginErrorMessage = "Please enter an email address.")
                return@launch
            }
            val user = userRepository.directAccessByEmail(email)
            _uiState.value = _uiState.value.copy(
                lastReporterName = user.displayName,
                currentTab = if (user.isAdmin) AppTab.ADMIN_PANEL else AppTab.MAP,
                toastMessage = "Access verified for ${user.email} (${user.roleLabel})"
            )
        }
    }

    fun register(email: String, name: String, pass: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loginErrorMessage = null)
            val user = userRepository.grantAccess(email, name, UserAccount.ROLE_VOLUNTEER, pass)
            userRepository.authenticate(email, pass)
            _uiState.value = _uiState.value.copy(
                lastReporterName = user.displayName,
                currentTab = AppTab.MAP,
                toastMessage = "Welcome, ${user.displayName}!"
            )
        }
    }

    fun grantAccessToEmail(email: String, name: String, role: String, pass: String) {
        viewModelScope.launch {
            val user = userRepository.grantAccess(email, name, role, pass)
            _uiState.value = _uiState.value.copy(
                toastMessage = "Direct access granted to ${user.email} as ${user.roleLabel}"
            )
        }
    }

    fun toggleUserActive(user: UserAccount) {
        viewModelScope.launch {
            userRepository.updateUser(user.copy(isActive = !user.isActive))
            _uiState.value = _uiState.value.copy(
                toastMessage = "Updated status for ${user.displayName}"
            )
        }
    }

    fun deleteUser(id: Long) {
        viewModelScope.launch {
            userRepository.deleteUser(id)
            _uiState.value = _uiState.value.copy(
                toastMessage = "Account access revoked."
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            userRepository.logout()
            _uiState.value = _uiState.value.copy(
                selectedEntry = null,
                toastMessage = "Logged out successfully"
            )
        }
    }

    // --- Navigation & Filter Operations ---

    fun setTab(tab: AppTab) {
        _uiState.value = _uiState.value.copy(currentTab = tab)
    }

    fun setCategoryFilter(category: EntryCategory?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun selectZone(zone: MangroveZone) {
        _uiState.value = _uiState.value.copy(
            selectedZone = zone,
            centerLat = zone.centerLat,
            centerLng = zone.centerLng,
            zoomLevel = 1.6f,
            selectedEntry = null,
            toastMessage = "Selected ${zone.name}"
        )
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun searchAndNavigate(context: Context, query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return

        viewModelScope.launch {
            val result = LocationHelper.resolveLocation(context, trimmed)
            if (result != null) {
                _uiState.value = _uiState.value.copy(
                    currentTab = AppTab.MAP,
                    centerLat = result.lat,
                    centerLng = result.lng,
                    zoomLevel = 1.5f,
                    temporaryPin = Pair(result.lat, result.lng),
                    searchQuery = result.title,
                    toastMessage = "Redirected to ${result.title} (${"%.4f".format(result.lat)}°, ${"%.4f".format(result.lng)}°)"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    searchQuery = trimmed,
                    toastMessage = "Filtered map entries matching '$trimmed'"
                )
            }
        }
    }

    fun selectPlaceSearchResult(result: PlaceSearchResult) {
        _uiState.value = _uiState.value.copy(
            currentTab = AppTab.MAP,
            centerLat = result.lat,
            centerLng = result.lng,
            zoomLevel = 1.5f,
            temporaryPin = Pair(result.lat, result.lng),
            searchQuery = result.title,
            toastMessage = "Redirected to ${result.title}"
        )
    }

    fun onMapTapped(lat: Double, lng: Double) {
        _uiState.value = _uiState.value.copy(
            temporaryPin = Pair(lat, lng),
            isAddDialogOpen = true
        )
    }

    fun openAddDialogAtCurrentCenter() {
        val center = Pair(_uiState.value.centerLat, _uiState.value.centerLng)
        _uiState.value = _uiState.value.copy(
            temporaryPin = center,
            isAddDialogOpen = true
        )
    }

    fun closeAddDialog() {
        _uiState.value = _uiState.value.copy(
            isAddDialogOpen = false,
            temporaryPin = null
        )
    }

    fun openReportsDialog() {
        _uiState.value = _uiState.value.copy(isReportsDialogOpen = true)
    }

    fun closeReportsDialog() {
        _uiState.value = _uiState.value.copy(isReportsDialogOpen = false)
    }

    fun selectEntry(entry: TreeEntry?) {
        _uiState.value = _uiState.value.copy(selectedEntry = entry)
        if (entry != null) {
            val matchingZone = zones.find { it.id == entry.zoneId }
            if (matchingZone != null) {
                _uiState.value = _uiState.value.copy(selectedZone = matchingZone)
            }
        }
    }

    fun focusOnEntry(entry: TreeEntry) {
        val matchingZone = zones.find { it.id == entry.zoneId } ?: _uiState.value.selectedZone
        _uiState.value = _uiState.value.copy(
            currentTab = AppTab.MAP,
            centerLat = entry.lat,
            centerLng = entry.lng,
            zoomLevel = 2.0f,
            selectedEntry = entry,
            selectedZone = matchingZone
        )
    }

    fun jumpToLocation(lat: Double, lng: Double, name: String) {
        _uiState.value = _uiState.value.copy(
            centerLat = lat,
            centerLng = lng,
            zoomLevel = 1.8f,
            toastMessage = "Navigated to $name"
        )
    }

    fun updateMapCenter(lat: Double, lng: Double, zoom: Float) {
        _uiState.value = _uiState.value.copy(
            centerLat = lat,
            centerLng = lng
        )
    }

    fun zoomIn() {
        _uiState.value = _uiState.value.copy(
            zoomLevel = (_uiState.value.zoomLevel + 0.35f).coerceAtMost(5.5f)
        )
    }

    fun zoomOut() {
        _uiState.value = _uiState.value.copy(
            zoomLevel = (_uiState.value.zoomLevel - 0.35f).coerceAtLeast(0.4f)
        )
    }

    fun adjustZoom(deltaFactor: Float) {
        val newZoom = (_uiState.value.zoomLevel + deltaFactor).coerceIn(0.4f, 5.5f)
        _uiState.value = _uiState.value.copy(zoomLevel = newZoom)
    }

    fun toggleSectorOverviewCollapsed() {
        _uiState.value = _uiState.value.copy(
            isSectorOverviewCollapsed = !_uiState.value.isSectorOverviewCollapsed
        )
    }

    fun setSectorOverviewCollapsed(collapsed: Boolean) {
        _uiState.value = _uiState.value.copy(isSectorOverviewCollapsed = collapsed)
    }

    fun setFetchingLocation(isFetching: Boolean) {
        _uiState.value = _uiState.value.copy(isFetchingLocation = isFetching)
    }

    fun updateUserLiveLocation(lat: Double, lng: Double) {
        _uiState.value = _uiState.value.copy(
            userLiveLocation = Pair(lat, lng),
            centerLat = lat,
            centerLng = lng,
            zoomLevel = 2.0f,
            isFetchingLocation = false,
            toastMessage = "📍 Live GPS location acquired (%.4f, %.4f)".format(lat, lng)
        )
    }

    fun pan(dLat: Float, dLng: Float) {
        _uiState.value = _uiState.value.copy(
            centerLat = (_uiState.value.centerLat + dLat).coerceIn(-85.0, 85.0),
            centerLng = (_uiState.value.centerLng + dLng).coerceIn(-180.0, 180.0)
        )
    }

    fun recenter() {
        _uiState.value = _uiState.value.copy(
            centerLat = 1.3521,
            centerLng = 103.8198,
            zoomLevel = 1.6f
        )
    }

    fun saveEntry(entry: TreeEntry) {
        viewModelScope.launch {
            repository.saveReporter(entry.reporter)
            repository.insert(entry)
            _uiState.value = _uiState.value.copy(
                isAddDialogOpen = false,
                temporaryPin = null,
                lastReporterName = entry.reporter,
                toastMessage = "Observation logged in ${_uiState.value.selectedZone.sectorCode}"
            )
        }
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch {
            repository.delete(id)
            if (_uiState.value.selectedEntry?.id == id) {
                _uiState.value = _uiState.value.copy(selectedEntry = null)
            }
            _uiState.value = _uiState.value.copy(toastMessage = "Observation record removed.")
        }
    }

    fun clearToastMessage() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }

    class Factory(
        private val repository: TreeRepository,
        private val userRepository: UserRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository, userRepository) as T
        }
    }
}
