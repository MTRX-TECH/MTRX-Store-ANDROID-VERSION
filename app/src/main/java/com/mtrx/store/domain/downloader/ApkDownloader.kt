package com.mtrx.store.domain.downloader

import android.content.Context
import com.mtrx.store.core.HashVerifier
import com.mtrx.store.domain.model.DownloadState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApkDownloader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    private val downloadDir: File by lazy {
        File(context.cacheDir, "apks").apply {
            if (!exists()) mkdirs()
        }
    }

    fun downloadApk(
        url: String,
        packageName: String,
        expectedSha256: String,
        expectedSizeBytes: Long
    ): Flow<DownloadState> = flow {
        val targetFile = File(downloadDir, "$packageName.apk")
        if (targetFile.exists()) {
            targetFile.delete()
        }

        val request = Request.Builder()
            .url(url)
            .build()

        var call = okHttpClient.newCall(request)

        try {
            val response = call.execute()
            if (!response.isSuccessful) {
                emit(DownloadState.Failed("Server returned HTTP ${response.code}: ${response.message}"))
                return@flow
            }

            val body = response.body
            if (body == null) {
                emit(DownloadState.Failed("Download body was empty"))
                return@flow
            }

            val totalBytes = if (body.contentLength() > 0) body.contentLength() else expectedSizeBytes
            var bytesCopied = 0L
            val buffer = ByteArray(8192)

            val startTime = System.currentTimeMillis()
            var lastUpdateTime = startTime
            var bytesSinceLastUpdate = 0L
            var currentSpeed = 0L

            body.byteStream().use { input ->
                FileOutputStream(targetFile).use { output ->
                    var bytes: Int
                    while (input.read(buffer).also { bytes = it } != -1) {
                        if (!currentCoroutineContext().isActive) {
                            targetFile.delete()
                            emit(DownloadState.Cancelled)
                            return@flow
                        }

                        output.write(buffer, 0, bytes)
                        bytesCopied += bytes
                        bytesSinceLastUpdate += bytes

                        val now = System.currentTimeMillis()
                        val elapsed = now - lastUpdateTime
                        if (elapsed >= 300) { // update progress ~3 times per second
                            currentSpeed = (bytesSinceLastUpdate * 1000) / elapsed
                            val fraction = if (totalBytes > 0) {
                                (bytesCopied.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                            } else 0f

                            emit(
                                DownloadState.Progress(
                                    bytesDownloaded = bytesCopied,
                                    totalBytes = totalBytes,
                                    progressFraction = fraction,
                                    speedBytesPerSec = currentSpeed
                                )
                            )
                            lastUpdateTime = now
                            bytesSinceLastUpdate = 0L
                        }
                    }
                    output.flush()
                }
            }

            // Verification Stage: Validate File Size
            if (expectedSizeBytes > 0 && targetFile.length() < (expectedSizeBytes * 0.9)) {
                targetFile.delete()
                emit(DownloadState.Failed("Downloaded file size mismatch (Expected $expectedSizeBytes, got ${targetFile.length()})"))
                return@flow
            }

            // Verification Stage: Validate SHA-256 Hash
            emit(DownloadState.VerifyingHash)
            val isValidHash = HashVerifier.verify(targetFile, expectedSha256)
            if (!isValidHash) {
                targetFile.delete()
                emit(DownloadState.Failed("Security verification failed: SHA-256 checksum mismatch. File rejected."))
                return@flow
            }

            emit(DownloadState.Success(targetFile.absolutePath))

        } catch (e: IOException) {
            targetFile.delete()
            emit(DownloadState.Failed(e.message ?: "Network error during download"))
        } catch (e: Exception) {
            targetFile.delete()
            emit(DownloadState.Failed(e.message ?: "Unexpected error during download"))
        }
    }.flowOn(Dispatchers.IO)

    fun deleteCachedApk(packageName: String) {
        val file = File(downloadDir, "$packageName.apk")
        if (file.exists()) {
            file.delete()
        }
    }

    fun clearAllCachedApks() {
        downloadDir.listFiles()?.forEach { it.delete() }
    }
}
