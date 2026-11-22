package com.mtrx.store.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mtrx.store.core.Resource
import com.mtrx.store.data.local.dao.AppDao
import com.mtrx.store.data.local.entity.AppEntity
import com.mtrx.store.data.local.preferences.SettingsDataStore
import com.mtrx.store.data.remote.CatalogApiService
import com.mtrx.store.data.remote.dto.AppDto
import com.mtrx.store.domain.downloader.ApkDownloader
import com.mtrx.store.domain.installer.PackageInstallerHelper
import com.mtrx.store.domain.model.AppStatus
import com.mtrx.store.domain.model.DownloadState
import com.mtrx.store.domain.model.StoreApp
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoreRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: CatalogApiService,
    private val appDao: AppDao,
    private val settingsDataStore: SettingsDataStore,
    private val apkDownloader: ApkDownloader,
    private val packageInstallerHelper: PackageInstallerHelper
) : StoreRepository {

    private val packageManager: PackageManager = context.packageManager
    private val gson = Gson()

    override fun getAllApps(): Flow<List<StoreApp>> {
        return appDao.getAllApps().map { entities ->
            entities.map { mapEntityToDomain(it) }
        }
    }

    override fun getFeaturedApps(): Flow<List<StoreApp>> {
        return appDao.getAllApps().map { entities ->
            entities.take(3).map { mapEntityToDomain(it) }
        }
    }

    override fun getRecentlyUpdatedApps(): Flow<List<StoreApp>> {
        return appDao.getAllApps().map { entities ->
            entities.sortedByDescending { it.updatedAt }.take(6).map { mapEntityToDomain(it) }
        }
    }

    override fun getAppsByCategory(category: String): Flow<List<StoreApp>> {
        return (if (category.equals("All", ignoreCase = true) || category.isEmpty()) {
            appDao.getAllApps()
        } else {
            appDao.getAppsByCategory(category)
        }).map { entities ->
            entities.map { mapEntityToDomain(it) }
        }
    }

    override fun getApp(packageName: String): Flow<StoreApp?> {
        return appDao.getAppByPackageNameFlow(packageName).map { entity ->
            entity?.let { mapEntityToDomain(it) }
        }
    }

    override fun getInstalledApps(): Flow<List<StoreApp>> {
        return appDao.getInstalledApps().map { entities ->
            entities.map { mapEntityToDomain(it) }
        }
    }

    override fun getAppsWithUpdates(): Flow<List<StoreApp>> {
        return appDao.getAppsWithUpdates().map { entities ->
            entities.map { mapEntityToDomain(it) }
        }
    }

    override fun getUpdatesCount(): Flow<Int> {
        return appDao.getUpdatesCount()
    }

    override fun getCategories(): Flow<List<String>> {
        return appDao.getCategories()
    }

    override suspend fun syncCatalog(): Resource<Unit> = withContext(Dispatchers.IO) {
        val catalogUrl = settingsDataStore.catalogUrl.first()

        try {
            // Attempt remote fetch
            val remoteDtos = apiService.getCatalog(catalogUrl)
            saveCatalogDtos(remoteDtos)
            settingsDataStore.recordUpdateCheckTimestamp()
            Resource.Success(Unit)
        } catch (e: Exception) {
            // Fallback to local asset if database is currently empty
            try {
                val currentCount = appDao.getAllApps().first().size
                if (currentCount == 0) {
                    val assetInput = context.assets.open("catalog.json")
                    val listType = object : TypeToken<List<AppDto>>() {}.type
                    val localDtos: List<AppDto> = gson.fromJson(InputStreamReader(assetInput), listType)
                    saveCatalogDtos(localDtos)
                    Resource.Success(Unit)
                } else {
                    refreshInstalledStatus()
                    val errorDetail = when (e) {
                        is retrofit2.HttpException -> "HTTP ${e.code()} ${e.message()}"
                        is java.net.UnknownHostException -> "No internet connection"
                        else -> e.localizedMessage ?: "Network error"
                    }
                    Resource.Error("Could not reach catalog server ($errorDetail). Showing cached catalog.", Unit)
                }
            } catch (fallbackEx: Exception) {
                Resource.Error(e.message ?: "Failed to load catalog", Unit)
            }
        }
    }

    private suspend fun saveCatalogDtos(dtos: List<AppDto>) {
        val entities = dtos.map { dto ->
            val installedInfo = getInstalledPackageInfo(dto.packageName)
            val isInstalled = installedInfo != null
            val installedCode = if (installedInfo != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    installedInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    installedInfo.versionCode.toLong()
                }
            } else 0L
            val installedName = installedInfo?.versionName ?: ""

            // Preserve local apk path if exists
            val existing = appDao.getAppByPackageName(dto.packageName)

            AppEntity(
                packageName = dto.packageName,
                name = dto.name,
                description = dto.description,
                shortDescription = dto.shortDescription,
                iconUrl = dto.iconUrl,
                screenshots = dto.screenshots ?: emptyList(),
                category = dto.category,
                versionName = dto.versionName,
                versionCode = dto.versionCode,
                apkUrl = dto.apkUrl,
                sha256 = dto.sha256,
                sizeBytes = dto.sizeBytes,
                changelog = dto.changelog ?: "",
                minSdk = dto.minSdk,
                updatedAt = dto.updatedAt ?: "",
                isInstalled = isInstalled,
                installedVersionCode = installedCode,
                installedVersionName = installedName,
                downloadedApkPath = existing?.downloadedApkPath
            )
        }
        appDao.insertApps(entities)
    }

    override suspend fun refreshInstalledStatus() = withContext(Dispatchers.IO) {
        val allApps = appDao.getAllApps().first()
        for (app in allApps) {
            val installedInfo = getInstalledPackageInfo(app.packageName)
            val isInstalled = installedInfo != null
            val installedCode = if (installedInfo != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    installedInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    installedInfo.versionCode.toLong()
                }
            } else 0L
            val installedName = installedInfo?.versionName ?: ""

            if (app.isInstalled != isInstalled || app.installedVersionCode != installedCode) {
                appDao.updateInstalledStatus(
                    packageName = app.packageName,
                    isInstalled = isInstalled,
                    installedVersionCode = installedCode,
                    installedVersionName = installedName
                )
            }
        }
    }

    private fun getInstalledPackageInfo(packageName: String): PackageInfo? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    private fun mapEntityToDomain(entity: AppEntity): StoreApp {
        val status: AppStatus = when {
            !entity.isInstalled -> AppStatus.NotInstalled
            entity.versionCode > entity.installedVersionCode -> AppStatus.UpdateAvailable(
                installedVersionName = entity.installedVersionName,
                newVersionName = entity.versionName,
                newVersionCode = entity.versionCode
            )
            else -> AppStatus.Installed(
                versionName = entity.installedVersionName.ifEmpty { entity.versionName },
                versionCode = entity.installedVersionCode
            )
        }

        return StoreApp(
            packageName = entity.packageName,
            name = entity.name,
            description = entity.description,
            shortDescription = entity.shortDescription,
            iconUrl = entity.iconUrl,
            screenshots = entity.screenshots,
            category = entity.category,
            versionName = entity.versionName,
            versionCode = entity.versionCode,
            apkUrl = entity.apkUrl,
            sha256 = entity.sha256,
            sizeBytes = entity.sizeBytes,
            changelog = entity.changelog,
            minSdk = entity.minSdk,
            updatedAt = entity.updatedAt,
            isInstalled = entity.isInstalled,
            installedVersionCode = entity.installedVersionCode,
            installedVersionName = entity.installedVersionName,
            downloadedApkPath = entity.downloadedApkPath,
            status = status
        )
    }

    override fun downloadApp(packageName: String): Flow<DownloadState> = flow {
        val app = appDao.getAppByPackageName(packageName)
        if (app == null) {
            emit(DownloadState.Failed("Application not found in catalog"))
            return@flow
        }

        apkDownloader.downloadApk(
            url = app.apkUrl,
            packageName = app.packageName,
            expectedSha256 = app.sha256,
            expectedSizeBytes = app.sizeBytes
        ).collect { state ->
            if (state is DownloadState.Success) {
                appDao.updateDownloadedApkPath(packageName, state.apkFilePath)
            } else if (state is DownloadState.Failed || state is DownloadState.Cancelled) {
                appDao.updateDownloadedApkPath(packageName, null)
            }
            emit(state)
        }
    }

    override suspend fun installApp(packageName: String): Result<Unit> {
        val app = appDao.getAppByPackageName(packageName)
            ?: return Result.failure(IllegalArgumentException("App not found in catalog"))

        val apkPath = app.downloadedApkPath
            ?: return Result.failure(IllegalStateException("No downloaded APK file found for ${app.name}"))

        val apkFile = File(apkPath)
        return packageInstallerHelper.installApk(apkFile, packageName)
    }

    override fun launchApp(packageName: String): Boolean {
        return packageInstallerHelper.launchApp(packageName)
    }

    override fun createUninstallIntent(packageName: String): Intent {
        return packageInstallerHelper.createUninstallIntent(packageName)
    }

    override fun canRequestPackageInstalls(): Boolean {
        return packageInstallerHelper.canRequestPackageInstalls()
    }

    override fun createManageUnknownAppSourcesIntent(): Intent {
        return packageInstallerHelper.createManageUnknownAppSourcesIntent()
    }

    override fun clearCache() {
        apkDownloader.clearAllCachedApks()
    }
}
