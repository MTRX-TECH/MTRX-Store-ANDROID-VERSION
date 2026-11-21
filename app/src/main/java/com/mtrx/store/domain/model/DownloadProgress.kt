package com.mtrx.store.domain.model

sealed interface DownloadState {
    data class Progress(
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val progressFraction: Float,
        val speedBytesPerSec: Long
    ) : DownloadState

    object VerifyingHash : DownloadState
    data class Success(val apkFilePath: String) : DownloadState
    data class Failed(val error: String) : DownloadState
    object Cancelled : DownloadState
}
