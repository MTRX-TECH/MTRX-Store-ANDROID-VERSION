package com.mtrx.store.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mtrx.store.data.local.entity.AppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    @Query("SELECT * FROM apps ORDER BY name ASC")
    fun getAllApps(): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps WHERE category = :category ORDER BY name ASC")
    fun getAppsByCategory(category: String): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps WHERE packageName = :packageName LIMIT 1")
    fun getAppByPackageNameFlow(packageName: String): Flow<AppEntity?>

    @Query("SELECT * FROM apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getAppByPackageName(packageName: String): AppEntity?

    @Query("SELECT * FROM apps WHERE isInstalled = 1 ORDER BY name ASC")
    fun getInstalledApps(): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps WHERE isInstalled = 1 AND versionCode > installedVersionCode ORDER BY name ASC")
    fun getAppsWithUpdates(): Flow<List<AppEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApps(apps: List<AppEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: AppEntity)

    @Update
    suspend fun updateApp(app: AppEntity)

    @Query("UPDATE apps SET isInstalled = :isInstalled, installedVersionCode = :installedVersionCode, installedVersionName = :installedVersionName WHERE packageName = :packageName")
    suspend fun updateInstalledStatus(
        packageName: String,
        isInstalled: Boolean,
        installedVersionCode: Long,
        installedVersionName: String
    )

    @Query("UPDATE apps SET downloadedApkPath = :path WHERE packageName = :packageName")
    suspend fun updateDownloadedApkPath(packageName: String, path: String?)

    @Query("SELECT DISTINCT category FROM apps WHERE category != '' ORDER BY category ASC")
    fun getCategories(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM apps WHERE isInstalled = 1 AND versionCode > installedVersionCode")
    fun getUpdatesCount(): Flow<Int>

    @Query("DELETE FROM apps WHERE packageName NOT IN (:packageNames)")
    suspend fun deleteAppsNotIn(packageNames: List<String>)

    @Query("DELETE FROM apps")
    suspend fun clearAll()
}
