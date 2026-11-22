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
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
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

        var streamInfo: Pair<java.io.InputStream, Long>? = null

        // Attempt 1: Try OkHttp
        try {
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.android.package-archive, application/octet-stream, */*")
                .header("Connection", "close")
                .build()
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful && response.body != null) {
                val body = response.body!!
                val length = if (body.contentLength() > 0) body.contentLength() else expectedSizeBytes
                streamInfo = Pair(body.byteStream(), length)
            }
        } catch (_: Exception) {
            // Fall back to direct HttpsURLConnection
        }

        // Attempt 2: Fallback to HttpsURLConnection with redirect handling
        if (streamInfo == null) {
            try {
                streamInfo = openStreamWithRedirects(url, expectedSizeBytes)
            } catch (e: Exception) {
                targetFile.delete()
                emit(DownloadState.Failed(e.message ?: "Failed to connect to download server"))
                return@flow
            }
        }

        try {
            val (inputStream, totalBytes) = streamInfo
            var bytesCopied = 0L
            val buffer = ByteArray(64 * 1024)

            // Emit initial progress immediately once connection is established
            emit(
                DownloadState.Progress(
                    bytesDownloaded = 0L,
                    totalBytes = totalBytes,
                    progressFraction = 0f,
                    speedBytesPerSec = 0L
                )
            )

            val startTime = System.currentTimeMillis()
            var lastUpdateTime = startTime
            var bytesSinceLastUpdate = 0L
            var currentSpeed = 0L

            BufferedInputStream(inputStream, 64 * 1024).use { input ->
                BufferedOutputStream(FileOutputStream(targetFile), 64 * 1024).use { output ->
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
                        if (elapsed >= 250) { // update progress ~4 times per second
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

    private fun openStreamWithRedirects(initialUrl: String, expectedSizeBytes: Long): Pair<java.io.InputStream, Long> {
        var currentUrl = initialUrl
        var redirects = 0
        while (redirects < 10) {
            val urlObj = java.net.URL(currentUrl)
            val connection = urlObj.openConnection() as java.net.HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 30000
            connection.readTimeout = 120000
            connection.setRequestProperty("User-Agent", "MTRXStore/1.0 (Android)")
            connection.setRequestProperty("Accept", "application/vnd.android.package-archive, application/octet-stream, */*")
            connection.setRequestProperty("Connection", "close")

            val responseCode = connection.responseCode
            if (responseCode in 300..399) {
                val location = connection.getHeaderField("Location")
                    ?: throw java.io.IOException("Redirect response $responseCode missing Location header")
                connection.disconnect()
                currentUrl = location
                redirects++
            } else if (responseCode in 200..299) {
                val lenHeader = connection.contentLengthLong
                val finalLen = if (lenHeader > 0) lenHeader else expectedSizeBytes
                return Pair(connection.inputStream, finalLen)
            } else {
                throw java.io.IOException("Server returned HTTP $responseCode: ${connection.responseMessage}")
            }
        }
        throw java.io.IOException("Too many redirects while downloading APK")
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
