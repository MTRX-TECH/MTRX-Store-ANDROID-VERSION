package com.mtrx.store.data.repository

import com.mtrx.store.core.Resource
import com.mtrx.store.domain.model.DownloadState
import com.mtrx.store.domain.model.StoreApp
import kotlinx.coroutines.flow.Flow

interface StoreRepository {
    fun getAllApps(): Flow<List<StoreApp>>
    fun getFeaturedApps(): Flow<List<StoreApp>>
    fun getRecentlyUpdatedApps(): Flow<List<StoreApp>>
    fun getAppsByCategory(category: String): Flow<List<StoreApp>>
    fun getApp(packageName: String): Flow<StoreApp?>
    fun getInstalledApps(): Flow<List<StoreApp>>
    fun getAppsWithUpdates(): Flow<List<StoreApp>>
    fun getUpdatesCount(): Flow<Int>
    fun getCategories(): Flow<List<String>>

    suspend fun syncCatalog(): Resource<Unit>
    suspend fun refreshInstalledStatus()
    fun downloadApp(packageName: String): Flow<DownloadState>
    suspend fun installApp(packageName: String): Result<Unit>
    fun launchApp(packageName: String): Boolean
    fun createUninstallIntent(packageName: String): android.content.Intent
    fun canRequestPackageInstalls(): Boolean
    fun createManageUnknownAppSourcesIntent(): android.content.Intent
    fun clearCache()
}
