package com.mtrx.store.ui.screens.updates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtrx.store.data.repository.StoreRepository
import com.mtrx.store.domain.model.AppStatus
import com.mtrx.store.domain.model.DownloadState
import com.mtrx.store.domain.model.StoreApp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UpdatesUiState(
    val appsWithUpdates: List<StoreApp> = emptyList(),
    val isUpdatingAll: Boolean = false,
    val showPermissionDialog: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class UpdatesViewModel @Inject constructor(
    private val repository: StoreRepository
) : ViewModel() {

    private val _isUpdatingAll = MutableStateFlow(false)
    private val _showPermissionDialog = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _activeDownloads = MutableStateFlow<Map<String, AppStatus.Downloading>>(emptyMap())

    val uiState: StateFlow<UpdatesUiState> = combine(
        repository.getAppsWithUpdates(),
        _isUpdatingAll,
        _showPermissionDialog,
        _errorMessage,
        _activeDownloads
    ) { apps, updatingAll, permDialog, error, downloads ->

        val updatedApps = apps.map { app ->
            val dl = downloads[app.packageName]
            if (dl != null) app.copy(status = dl) else app
        }

        UpdatesUiState(
            appsWithUpdates = updatedApps,
            isUpdatingAll = updatingAll,
            showPermissionDialog = permDialog,
            errorMessage = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UpdatesUiState()
    )

    fun onUpdateAppClick(packageName: String) {
        startDownloadAndInstall(packageName)
    }

    fun onUpdateAllClick() {
        val appsToUpdate = uiState.value.appsWithUpdates
        if (appsToUpdate.isEmpty()) return

        _isUpdatingAll.value = true
        viewModelScope.launch {
            for (app in appsToUpdate) {
                startDownloadAndInstall(app.packageName)
            }
            _isUpdatingAll.value = false
        }
    }

    private fun startDownloadAndInstall(packageName: String) {
        // Instant visual feedback: Show loading animation immediately on first click
        _activeDownloads.value = _activeDownloads.value + (packageName to AppStatus.Downloading(
            progressFraction = 0f,
            speedText = "Connecting...",
            bytesText = "Starting..."
        ))

        viewModelScope.launch {
            repository.downloadApp(packageName).collect { state ->
                when (state) {
                    is DownloadState.Progress -> {
                        _activeDownloads.value = _activeDownloads.value + (packageName to AppStatus.Downloading(
                            progressFraction = state.progressFraction,
                            speedText = "",
                            bytesText = ""
                        ))
                    }
                    is DownloadState.Success -> {
                        _activeDownloads.value = _activeDownloads.value - packageName
                        triggerInstall(packageName)
                    }
                    is DownloadState.Failed -> {
                        _activeDownloads.value = _activeDownloads.value - packageName
                        _errorMessage.value = "Failed update for $packageName: ${state.error}"
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun triggerInstall(packageName: String) {
        if (!repository.canRequestPackageInstalls()) {
            _showPermissionDialog.value = true
            return
        }

        viewModelScope.launch {
            val result = repository.installApp(packageName)
            if (result.isFailure) {
                _errorMessage.value = "Install failed: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun dismissPermissionDialog() {
        _showPermissionDialog.value = false
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun getManageUnknownSourcesIntent() = repository.createManageUnknownAppSourcesIntent()
}
