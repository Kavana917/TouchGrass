package com.touchgrass.app.core.usage

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.touchgrass.app.core.data.settings.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Restarts the monitor after a reboot.
 *
 * Without this, rebooting the phone would be a free reset — turn it off and
 * on again and the budget stops being enforced. The stored total survives
 * regardless (it lives in Room), but nothing would be updating it.
 *
 * Only restarts if the user had the monitor running before the reboot.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var settings: SettingsRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        // goAsync() buys us a few seconds to read DataStore before the
        // receiver is torn down.
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val shouldRun = settings.monitorEnabled.first()
                val hasPermission = UsagePermission.isGranted(context)
                if (shouldRun && hasPermission) {
                    UsageMonitorService.start(context)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
