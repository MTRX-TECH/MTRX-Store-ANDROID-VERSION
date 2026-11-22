package com.mtrx.store.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtrx.store.core.Resource
import com.mtrx.store.data.repository.StoreRepository
import com.mtrx.store.domain.model.AppStatus
import com.mtrx.store.domain.model.DownloadState
import com.mtrx.store.domain.model.StoreApp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val featuredApps: List<StoreApp> = emptyList(),
    val recentlyUpdatedApps: List<StoreApp> = emptyList(),
    val allApps: List<StoreApp> = emptyList(),
    val categories: List<String> = emptyList(),
    val selectedCategory: String = "All",
    val searchQuery: String = "",
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val showPermissionDialog: Boolean = false,
    val pendingInstallPackage: String? = null
)

private data class FilterState(
    val selectedCategory: String = "All",
    val searchQuery: String = "",
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val showPermissionDialog: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: StoreRepository
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow("All")
    private val _searchQuery = MutableStateFlow("")
    private val _isRefreshing = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _showPermissionDialog = MutableStateFlow(false)
    private val _pendingInstallPackage = MutableStateFlow<String?>(null)

    // In-memory map of active downloads for instant UI progress
    private val _activeDownloads = MutableStateFlow<Map<String, AppStatus.Downloading>>(emptyMap())
    private val downloadJobs = mutableMapOf<String, Job>()

    private val filterStateFlow = combine(
        _selectedCategory,
        _searchQuery,
        _isRefreshing,
        _errorMessage,
        _showPermissionDialog
    ) { category, query, refreshing, error, permDialog ->
        FilterState(category, query, refreshing, error, permDialog)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        repository.getAllApps(),
        repository.getCategories(),
        filterStateFlow,
        _activeDownloads
    ) { apps: List<StoreApp>, categories: List<String>, filter: FilterState, downloads: Map<String, AppStatus.Downloading> ->

        val appsWithDownloads = apps.map { app ->
            val activeDownload = downloads[app.packageName]
            if (activeDownload != null) {
                app.copy(status = activeDownload)
            } else {
                app
            }
        }

        val filteredApps = appsWithDownloads.filter { app ->
            val matchesCategory = filter.selectedCategory == "All" || app.category.equals(filter.selectedCategory, ignoreCase = true)
            val matchesQuery = filter.searchQuery.isBlank() ||
                    app.name.contains(filter.searchQuery, ignoreCase = true) ||
                    app.shortDescription.contains(filter.searchQuery, ignoreCase = true) ||
                    app.category.contains(filter.searchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }

        val featured = appsWithDownloads.take(3)
        val recentlyUpdated = appsWithDownloads.sortedByDescending { it.updatedAt }.take(6)
        val fullCategories = listOf("All") + categories.filter { it.isNotBlank() }

        HomeUiState(
            featuredApps = featured,
            recentlyUpdatedApps = recentlyUpdated,
            allApps = filteredApps,
            categories = fullCategories,
            selectedCategory = filter.selectedCategory,
            searchQuery = filter.searchQuery,
            isRefreshing = filter.isRefreshing,
            errorMessage = filter.errorMessage,
            showPermissionDialog = filter.showPermissionDialog,
            pendingInstallPackage = _pendingInstallPackage.value
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    init {
        refreshCatalog()
    }

    fun refreshCatalog() {
        viewModelScope.launch {
            _isRefreshing.value = true
            when (val result = repository.syncCatalog()) {
                is Resource.Error -> {
                    _errorMessage.value = result.message
                }
                is Resource.Success -> {
                    _errorMessage.value = null
                }
                else -> Unit
            }
            repository.refreshInstalledStatus()
            _isRefreshing.value = false
        }
    }

    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun dismissPermissionDialog() {
        _showPermissionDialog.value = false
        _pendingInstallPackage.value = null
    }

    fun onAppActionClick(app: StoreApp) {
        when (app.status) {
            is AppStatus.NotInstalled, is AppStatus.UpdateAvailable, is AppStatus.Error -> {
                startDownloadAndInstall(app.packageName)
            }
            is AppStatus.Installed -> {
                repository.launchApp(app.packageName)
            }
            else -> Unit
        }
    }

    private fun startDownloadAndInstall(packageName: String) {
        if (downloadJobs.containsKey(packageName)) return

        // Instant visual feedback: Show loading animation immediately on first click
        _activeDownloads.value = _activeDownloads.value + (packageName to AppStatus.Downloading(
            progressFraction = 0f,
            speedText = "Connecting...",
            bytesText = "Starting..."
        ))

        val job = viewModelScope.launch {
            repository.downloadApp(packageName).collect { state ->
                when (state) {
                    is DownloadState.Progress -> {
                        _activeDownloads.value = _activeDownloads.value + (packageName to AppStatus.Downloading(
                            progressFraction = state.progressFraction,
                            speedText = "",
                            bytesText = ""
                        ))
                    }
                    is DownloadState.VerifyingHash -> {
                        _activeDownloads.value = _activeDownloads.value + (packageName to AppStatus.Downloading(
                            progressFraction = 1f,
                            speedText = "Verifying SHA-256...",
                            bytesText = ""
                        ))
                    }
                    is DownloadState.Success -> {
                        _activeDownloads.value = _activeDownloads.value - packageName
                        downloadJobs.remove(packageName)
                        triggerInstall(packageName)
                    }
                    is DownloadState.Failed -> {
                        _activeDownloads.value = _activeDownloads.value - packageName
                        downloadJobs.remove(packageName)
                        _errorMessage.value = "Download failed for $packageName: ${state.error}"
                    }
                    is DownloadState.Cancelled -> {
                        _activeDownloads.value = _activeDownloads.value - packageName
                        downloadJobs.remove(packageName)
                    }
                }
            }
        }
        downloadJobs[packageName] = job
    }

    private fun triggerInstall(packageName: String) {
        if (!repository.canRequestPackageInstalls()) {
            _pendingInstallPackage.value = packageName
            _showPermissionDialog.value = true
            return
        }

        viewModelScope.launch {
            val result = repository.installApp(packageName)
            if (result.isFailure) {
                _errorMessage.value = "Installation dispatch failed: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun proceedWithPendingInstall() {
        val pkg = _pendingInstallPackage.value ?: return
        dismissPermissionDialog()
        triggerInstall(pkg)
    }

    fun getManageUnknownSourcesIntent() = repository.createManageUnknownAppSourcesIntent()
}
