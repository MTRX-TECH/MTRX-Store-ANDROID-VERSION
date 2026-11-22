package com.mtrx.store.core

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object HashVerifier {

    /**
     * Computes the SHA-256 hex string of a given file.
     * Uses a 64KB buffered stream for rapid hashing of APKs.
     */
    fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        BufferedInputStream(FileInputStream(file), 64 * 1024).use { bis ->
            val buffer = ByteArray(64 * 1024)
            var bytesRead: Int
            while (bis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        val hashBytes = digest.digest()
        val sb = StringBuilder(hashBytes.size * 2)
        for (b in hashBytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }

    /**
     * Verifies whether the file's computed SHA-256 matches the expected hash.
     * Case-insensitive.
     */
    fun verify(file: File, expectedHash: String): Boolean {
        if (!file.exists() || !file.isFile) return false
        val computed = calculateSha256(file)
        return computed.equals(expectedHash.trim(), ignoreCase = true)
    }
}
