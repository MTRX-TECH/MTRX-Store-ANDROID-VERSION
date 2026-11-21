package com.mtrx.store.ui.screens.myapps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtrx.store.data.repository.StoreRepository
import com.mtrx.store.domain.model.StoreApp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyAppsUiState(
    val installedApps: List<StoreApp> = emptyList(),
    val totalStorageBytes: Long = 0L
)

@HiltViewModel
class MyAppsViewModel @Inject constructor(
    private val repository: StoreRepository
) : ViewModel() {

    val uiState: StateFlow<MyAppsUiState> = repository.getInstalledApps().map { apps ->
        val totalBytes = apps.sumOf { it.sizeBytes }
        MyAppsUiState(
            installedApps = apps,
            totalStorageBytes = totalBytes
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MyAppsUiState()
    )

    fun launchApp(packageName: String) {
        repository.launchApp(packageName)
    }

    fun createUninstallIntent(packageName: String) = repository.createUninstallIntent(packageName)

    fun refresh() {
        viewModelScope.launch {
            repository.refreshInstalledStatus()
        }
    }
}
