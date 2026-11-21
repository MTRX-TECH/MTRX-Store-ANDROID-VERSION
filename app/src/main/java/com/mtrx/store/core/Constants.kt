package com.mtrx.store.core

object Constants {
    // Configurable default catalog raw URL
    const val DEFAULT_CATALOG_URL = "https://raw.githubusercontent.com/MarapathranGroups/MTRX-Store-Catalog/main/catalog.json"

    // Network timeouts in seconds
    const val NETWORK_TIMEOUT_SECONDS = 30L

    // Notification Channel IDs
    const val CHANNEL_UPDATES_ID = "mtrx_store_updates_channel"
    const val CHANNEL_DOWNLOADS_ID = "mtrx_store_downloads_channel"

    // Notification IDs
    const val NOTIFICATION_UPDATES_ID = 1001
    const val NOTIFICATION_DOWNLOAD_BASE_ID = 2000

    // PackageInstaller Status Action
    const val ACTION_INSTALL_STATUS = "com.mtrx.store.ACTION_INSTALL_STATUS"
    const val EXTRA_PACKAGE_NAME = "com.mtrx.store.EXTRA_PACKAGE_NAME"

    // WorkManager Tag
    const val WORK_UPDATE_CHECK_TAG = "mtrx_periodic_update_check"
}
