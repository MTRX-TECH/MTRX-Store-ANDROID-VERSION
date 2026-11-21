package com.mtrx.store.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mtrx.store.data.repository.StoreRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PackageChangeReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: StoreRepository

    override fun onReceive(context: Context, intent: Intent) {
        val uri = intent.data ?: return
        val packageName = uri.schemeSpecificPart ?: return

        // When any managed app is added, replaced, or removed, sync repository state
        CoroutineScope(Dispatchers.IO).launch {
            repository.refreshInstalledStatus()
        }
    }
}
