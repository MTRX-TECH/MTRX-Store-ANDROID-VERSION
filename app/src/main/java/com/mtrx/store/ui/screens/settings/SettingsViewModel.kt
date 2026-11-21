package com.mtrx.store.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtrx.store.core.Constants
import com.mtrx.store.data.local.preferences.AppTheme
import com.mtrx.store.data.local.preferences.SettingsDataStore
import com.mtrx.store.data.repository.StoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val currentTheme: AppTheme = AppTheme.DARK,
    val catalogUrl: String = Constants.DEFAULT_CATALOG_URL,
    val updateIntervalHours: Long = 12L,
    val lastCheckTimestamp: Long = 0L,
    val cacheClearedMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val repository: StoreRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsDataStore.themeMode,
        settingsDataStore.catalogUrl,
        settingsDataStore.updateIntervalHours,
        settingsDataStore.lastUpdateCheckTimestamp
    ) { theme, url, interval, lastCheck ->
        SettingsUiState(
            currentTheme = theme,
            catalogUrl = url,
            updateIntervalHours = interval,
            lastCheckTimestamp = lastCheck
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            settingsDataStore.setThemeMode(theme)
        }
    }

    fun setCatalogUrl(url: String) {
        viewModelScope.launch {
            settingsDataStore.setCatalogUrl(url)
            repository.syncCatalog()
        }
    }

    fun resetCatalogUrlToDefault() {
        setCatalogUrl(Constants.DEFAULT_CATALOG_URL)
    }

    fun setUpdateInterval(hours: Long) {
        viewModelScope.launch {
            settingsDataStore.setUpdateIntervalHours(hours)
        }
    }

    fun clearCache() {
        repository.clearCache()
    }
}
