package com.mtrx.store.core

import java.text.DecimalFormat

object FileSizeFormatter {
    private val df = DecimalFormat("#,##0.#")

    fun format(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = digitGroups.coerceIn(0, units.size - 1)
        return "${df.format(bytes / Math.pow(1024.0, index.toDouble()))} ${units[index]}"
    }
}
