package com.mtrx.store.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.widget.Toast
import com.mtrx.store.core.Constants
import com.mtrx.store.data.local.dao.AppDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class InstallResultReceiver : BroadcastReceiver() {

    @Inject
    lateinit var appDao: AppDao

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Constants.ACTION_INSTALL_STATUS) return

        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        val targetPackage = intent.getStringExtra(Constants.EXTRA_PACKAGE_NAME) ?: ""
        val apkPath = intent.getStringExtra("apk_path")

        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                // User confirmation dialog needed (system prompt)
                val confirmIntent = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                if (confirmIntent != null) {
                    confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(confirmIntent)
                }
            }

            PackageInstaller.STATUS_SUCCESS -> {
                Toast.makeText(context, "Successfully installed $targetPackage", Toast.LENGTH_SHORT).show()

                // Security requirement: Delete APK after installation
                if (!apkPath.isNullOrEmpty()) {
                    File(apkPath).delete()
                }

                CoroutineScope(Dispatchers.IO).launch {
                    val app = appDao.getAppByPackageName(targetPackage)
                    if (app != null) {
                        appDao.updateInstalledStatus(
                            packageName = targetPackage,
                            isInstalled = true,
                            installedVersionCode = app.versionCode,
                            installedVersionName = app.versionName
                        )
                        appDao.updateDownloadedApkPath(targetPackage, null)
                    }
                }
            }

            PackageInstaller.STATUS_FAILURE_STORAGE -> {
                Toast.makeText(context, "Installation failed: Insufficient device storage", Toast.LENGTH_LONG).show()
            }

            PackageInstaller.STATUS_FAILURE_CONFLICT -> {
                Toast.makeText(context, "Installation failed: Signature conflict with existing app", Toast.LENGTH_LONG).show()
            }

            PackageInstaller.STATUS_FAILURE_INVALID -> {
                Toast.makeText(context, "Installation failed: APK package was invalid", Toast.LENGTH_LONG).show()
            }

            else -> {
                val errorMsg = message ?: "Installation canceled or failed (code: $status)"
                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
            }
        }
    }
}
