package com.mtrx.store.ui.screens.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtrx.store.data.repository.StoreRepository
import com.mtrx.store.domain.model.AppStatus
import com.mtrx.store.domain.model.DownloadState
import com.mtrx.store.domain.model.StoreApp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppDetailUiState(
    val app: StoreApp? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val showPermissionDialog: Boolean = false,
    val currentDownloadStatus: AppStatus.Downloading? = null
)

@HiltViewModel
class AppDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: StoreRepository
) : ViewModel() {

    private val packageName: String = checkNotNull(savedStateHandle["packageName"])
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _showPermissionDialog = MutableStateFlow(false)
    private val _downloadStatus = MutableStateFlow<AppStatus.Downloading?>(null)
    private val _isInstalling = MutableStateFlow(false)
    private var downloadJob: Job? = null

    val uiState: StateFlow<AppDetailUiState> = combine(
        repository.getApp(packageName),
        _errorMessage,
        _showPermissionDialog,
        _downloadStatus,
        _isInstalling
    ) { app, error, permDialog, dlStatus, isInstalling ->

        val updatedApp = if (app != null) {
            when {
                isInstalling && !app.isInstalled -> app.copy(status = AppStatus.Installing)
                dlStatus != null -> app.copy(status = dlStatus)
                else -> app
            }
        } else {
            app
        }

        if (app?.isInstalled == true && _isInstalling.value) {
            _isInstalling.value = false
        }

        AppDetailUiState(
            app = updatedApp,
            isLoading = app == null,
            errorMessage = error,
            showPermissionDialog = permDialog,
            currentDownloadStatus = dlStatus
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppDetailUiState()
    )

    fun onActionClick() {
        val app = uiState.value.app ?: return
        when (app.status) {
            is AppStatus.NotInstalled, is AppStatus.UpdateAvailable, is AppStatus.Error -> {
                startDownload()
            }
            is AppStatus.Installed -> {
                repository.launchApp(packageName)
            }
            else -> Unit
        }
    }

    private fun startDownload() {
        if (downloadJob != null && downloadJob?.isActive == true) return

        // Instant visual feedback: Show loading state immediately on first click
        _downloadStatus.value = AppStatus.Downloading(
            progressFraction = 0f,
            speedText = "Connecting...",
            bytesText = "Starting..."
        )

        downloadJob = viewModelScope.launch {
            repository.downloadApp(packageName).collect { state ->
                when (state) {
                    is DownloadState.Progress -> {
                        _downloadStatus.value = AppStatus.Downloading(
                            progressFraction = state.progressFraction,
                            speedText = if (state.speedBytesPerSec > 0) "${state.speedBytesPerSec / 1024} KB/s" else "Downloading...",
                            bytesText = "${state.bytesDownloaded / 1024 / 1024} MB / ${state.totalBytes / 1024 / 1024} MB"
                        )
                    }
                    is DownloadState.VerifyingHash -> {
                        _downloadStatus.value = AppStatus.Downloading(
                            progressFraction = 1f,
                            speedText = "Verifying package...",
                            bytesText = ""
                        )
                    }
                    is DownloadState.Success -> {
                        _downloadStatus.value = null
                        _isInstalling.value = true
                        triggerInstall()
                    }
                    is DownloadState.Failed -> {
                        _downloadStatus.value = null
                        _isInstalling.value = false
                        _errorMessage.value = "Download failed: ${state.error}"
                    }
                    is DownloadState.Cancelled -> {
                        _downloadStatus.value = null
                        _isInstalling.value = false
                    }
                }
            }
        }
    }

    private fun triggerInstall() {
        if (!repository.canRequestPackageInstalls()) {
            _isInstalling.value = false
            _showPermissionDialog.value = true
            return
        }

        viewModelScope.launch {
            val result = repository.installApp(packageName)
            if (result.isFailure) {
                _isInstalling.value = false
                _errorMessage.value = "Installation failed: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun proceedWithInstallAfterPermission() {
        _showPermissionDialog.value = false
        triggerInstall()
    }

    fun dismissPermissionDialog() {
        _showPermissionDialog.value = false
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun createUninstallIntent() = repository.createUninstallIntent(packageName)
    fun getManageUnknownSourcesIntent() = repository.createManageUnknownAppSourcesIntent()

    fun refreshStatus() {
        viewModelScope.launch {
            repository.refreshInstalledStatus()
            val currentApp = repository.getApp(packageName).first()
            if (currentApp?.isInstalled == true) {
                _isInstalling.value = false
            }
        }
    }
}
