package com.mtrx.store.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "apps")
data class AppEntity(
    @PrimaryKey
    val packageName: String,
    val name: String,
    val description: String,
    val shortDescription: String,
    val iconUrl: String,
    val screenshots: List<String>,
    val category: String,
    val versionName: String,
    val versionCode: Long,
    val apkUrl: String,
    val sha256: String,
    val sizeBytes: Long,
    val changelog: String,
    val minSdk: Int,
    val updatedAt: String,
    val isInstalled: Boolean = false,
    val installedVersionCode: Long = 0L,
    val installedVersionName: String = "",
    val downloadedApkPath: String? = null
)
