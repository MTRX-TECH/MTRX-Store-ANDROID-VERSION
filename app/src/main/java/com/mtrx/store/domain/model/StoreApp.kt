package com.mtrx.store.domain.model

data class StoreApp(
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
    val isInstalled: Boolean,
    val installedVersionCode: Long,
    val installedVersionName: String,
    val downloadedApkPath: String?,
    val status: AppStatus
)
