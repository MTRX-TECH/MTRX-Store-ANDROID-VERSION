package com.mtrx.store.domain.downloader

import android.content.Context
import com.mtrx.store.core.HashVerifier
import com.mtrx.store.domain.model.DownloadState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
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

        // Emit 0% initial progress
        emit(
            DownloadState.Progress(
                bytesDownloaded = 0L,
                totalBytes = expectedSizeBytes,
                progressFraction = 0f,
                speedBytesPerSec = 0L
            )
        )

        var downloadSuccess = false
        var lastError: Exception? = null

        // Strategy 1: OkHttp (HTTP/2 + HTTP/1.1 with automatic connection pooling and redirects)
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android 12; Mobile) MTRXStore/1.0")
                .header("Accept", "application/vnd.android.package-archive, application/octet-stream, */*")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful && response.body != null) {
                val body = response.body!!
                val length = if (body.contentLength() > 0) body.contentLength() else expectedSizeBytes
                body.byteStream().use { stream ->
                    copyStreamToFile(stream, length, targetFile) { progress ->
                        emit(progress)
                    }
                }
                response.close()
                if (targetFile.exists() && targetFile.length() > 0) {
                    downloadSuccess = true
                }
            } else {
                throw IOException("Server returned HTTP ${response.code}")
            }
        } catch (e: CancellationException) {
            targetFile.delete()
            emit(DownloadState.Cancelled)
            return@flow
        } catch (e: Exception) {
            targetFile.delete()
            lastError = e
        }

        // Strategy 2: Fallback to direct HttpsURLConnection if Strategy 1 fails
        if (!downloadSuccess) {
            try {
                val (inputStream, length) = openStreamWithRedirects(url, expectedSizeBytes)
                inputStream.use { stream ->
                    copyStreamToFile(stream, length, targetFile) { progress ->
                        emit(progress)
                    }
                }
                if (targetFile.exists() && targetFile.length() > 0) {
                    downloadSuccess = true
                }
            } catch (e: CancellationException) {
                targetFile.delete()
                emit(DownloadState.Cancelled)
                return@flow
            } catch (e: Exception) {
                targetFile.delete()
                lastError = e
            }
        }

        if (!downloadSuccess) {
            targetFile.delete()
            emit(DownloadState.Failed(lastError?.message ?: "Failed to download APK"))
            return@flow
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
    }.flowOn(Dispatchers.IO)

    private suspend fun copyStreamToFile(
        inputStream: InputStream,
        totalBytes: Long,
        targetFile: File,
        emitProgress: suspend (DownloadState.Progress) -> Unit
    ) {
        val buffer = ByteArray(64 * 1024)
        var bytesCopied = 0L
        val startTime = System.currentTimeMillis()
        var lastUpdateTime = startTime
        var bytesSinceLastUpdate = 0L
        var currentSpeed = 0L

        BufferedInputStream(inputStream, 64 * 1024).use { input ->
            BufferedOutputStream(FileOutputStream(targetFile), 64 * 1024).use { output ->
                var bytes: Int
                while (input.read(buffer).also { bytes = it } != -1) {
                    if (!currentCoroutineContext().isActive) {
                        throw CancellationException("Download cancelled")
                    }

                    output.write(buffer, 0, bytes)
                    bytesCopied += bytes
                    bytesSinceLastUpdate += bytes

                    val now = System.currentTimeMillis()
                    val elapsed = now - lastUpdateTime
                    if (elapsed >= 250) { // update progress ~4 times per second
                        currentSpeed = (bytesSinceLastUpdate * 1000) / elapsed
                        val fraction = if (totalBytes > 0) {
                            (bytesCopied.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                        } else 0f

                        emitProgress(
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
    }

    private fun openStreamWithRedirects(initialUrl: String, expectedSizeBytes: Long): Pair<InputStream, Long> {
        var currentUrl = initialUrl
        var redirects = 0
        while (redirects < 10) {
            val urlObj = java.net.URL(currentUrl)
            val connection = urlObj.openConnection() as java.net.HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 30000
            connection.readTimeout = 120000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android 12; Mobile) MTRXStore/1.0")
            connection.setRequestProperty("Accept", "application/vnd.android.package-archive, application/octet-stream, */*")

            val responseCode = connection.responseCode
            if (responseCode in 300..399) {
                val location = connection.getHeaderField("Location")
                    ?: throw IOException("Redirect response $responseCode missing Location header")
                connection.disconnect()
                currentUrl = location
                redirects++
            } else if (responseCode in 200..299) {
                val lenHeader = connection.contentLengthLong
                val finalLen = if (lenHeader > 0) lenHeader else expectedSizeBytes
                return Pair(connection.inputStream, finalLen)
            } else {
                throw IOException("Server returned HTTP $responseCode: ${connection.responseMessage}")
            }
        }
        throw IOException("Too many redirects while downloading APK")
    }

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
