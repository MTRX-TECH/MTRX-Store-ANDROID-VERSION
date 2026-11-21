package com.mtrx.store.domain.model

sealed interface AppStatus {
    object NotInstalled : AppStatus
    data class Installed(val versionName: String, val versionCode: Long) : AppStatus
    data class UpdateAvailable(
        val installedVersionName: String,
        val newVersionName: String,
        val newVersionCode: Long
    ) : AppStatus
    data class Downloading(val progressFraction: Float, val speedText: String, val bytesText: String) : AppStatus
    object Installing : AppStatus
    data class Error(val message: String) : AppStatus
}
