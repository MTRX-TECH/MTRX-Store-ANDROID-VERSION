package com.mtrx.store.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mtrx.store.core.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mtrx_store_settings")

enum class AppTheme {
    DARK,
    LIGHT,
    SYSTEM
}

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val KEY_THEME = stringPreferencesKey("app_theme")
        val KEY_CATALOG_URL = stringPreferencesKey("catalog_url")
        val KEY_UPDATE_INTERVAL_HOURS = longPreferencesKey("update_interval_hours")
        val KEY_LAST_UPDATE_CHECK = longPreferencesKey("last_update_check")
    }

    val themeMode: Flow<AppTheme> = dataStore.data.map { preferences ->
        when (preferences[KEY_THEME]) {
            AppTheme.LIGHT.name -> AppTheme.LIGHT
            AppTheme.SYSTEM.name -> AppTheme.SYSTEM
            else -> AppTheme.DARK // Dark is default
        }
    }

    val catalogUrl: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_CATALOG_URL] ?: Constants.DEFAULT_CATALOG_URL
    }

    val updateIntervalHours: Flow<Long> = dataStore.data.map { preferences ->
        preferences[KEY_UPDATE_INTERVAL_HOURS] ?: 12L
    }

    val lastUpdateCheckTimestamp: Flow<Long> = dataStore.data.map { preferences ->
        preferences[KEY_LAST_UPDATE_CHECK] ?: 0L
    }

    suspend fun setThemeMode(theme: AppTheme) {
        dataStore.edit { preferences ->
            preferences[KEY_THEME] = theme.name
        }
    }

    suspend fun setCatalogUrl(url: String) {
        dataStore.edit { preferences ->
            preferences[KEY_CATALOG_URL] = url
        }
    }

    suspend fun setUpdateIntervalHours(hours: Long) {
        dataStore.edit { preferences ->
            preferences[KEY_UPDATE_INTERVAL_HOURS] = hours
        }
    }

    suspend fun recordUpdateCheckTimestamp(timestamp: Long = System.currentTimeMillis()) {
        dataStore.edit { preferences ->
            preferences[KEY_LAST_UPDATE_CHECK] = timestamp
        }
    }
}
