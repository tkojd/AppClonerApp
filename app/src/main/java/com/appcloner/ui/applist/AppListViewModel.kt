package com.appcloner.ui.applist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appcloner.data.AppRepository
import com.appcloner.data.PreferencesManager
import com.appcloner.data.model.AppInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the installed-apps list screen.
 *
 * [apps] is the full, unfiltered list loaded from the repository. [filteredApps] applies
 * the current [searchQuery] and is what the UI should render.
 */
data class AppListUiState(
    val apps: List<AppInfo> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val showSystemApps: Boolean = false,
    val error: String? = null
) {
    /** Apps filtered by the current search query (case-insensitive, name or package). */
    val filteredApps: List<AppInfo>
        get() {
            if (searchQuery.isBlank()) return apps
            val q = searchQuery.trim().lowercase()
            return apps.filter {
                it.appName.lowercase().contains(q) || it.packageName.lowercase().contains(q)
            }
        }
}

/**
 * Loads and filters the list of installed applications for [AppListFragment].
 */
@HiltViewModel
class AppListViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AppListUiState(showSystemApps = preferencesManager.showSystemApps)
    )
    val uiState: StateFlow<AppListUiState> = _uiState.asStateFlow()

    init {
        // The "show system apps" preference is the single source of truth. React to changes
        // made either here (via the filter chip) or on the Settings screen and reload.
        viewModelScope.launch {
            preferencesManager.showSystemAppsFlow().collect { show ->
                if (_uiState.value.showSystemApps != show || _uiState.value.apps.isEmpty()) {
                    _uiState.update { it.copy(showSystemApps = show) }
                    loadApps()
                }
            }
        }
    }

    /** Loads apps honoring the current [AppListUiState.showSystemApps] toggle. */
    fun loadApps() {
        val includeSystem = _uiState.value.showSystemApps
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                appRepository.getInstalledApps(includeSystem).collect { apps ->
                    _uiState.update { it.copy(apps = apps, isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Unknown error")
                }
            }
        }
    }

    /** Updates the search query used to filter the list. */
    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    /**
     * Toggles inclusion of system apps by writing to the shared preference. The preference
     * flow observed in [init] then updates the state and reloads, keeping this screen and
     * the Settings screen in agreement.
     */
    fun toggleSystemApps(show: Boolean) {
        if (preferencesManager.showSystemApps == show) return
        preferencesManager.showSystemApps = show
    }
}
