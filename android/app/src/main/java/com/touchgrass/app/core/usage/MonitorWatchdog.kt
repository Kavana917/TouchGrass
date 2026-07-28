package com.touchgrass.app.core.usage

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.touchgrass.app.core.data.settings.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Restarts the monitor when Android has killed it.
 *
 * ⚠️ THIS IS THE MITIGATION FOR THE TOP RISK IN THE PROJECT.
 *
 * app_plan.md §2.7: Xiaomi, Oppo/Realme, Vivo and Samsung all kill
 * foreground services aggressively, regardless of what Android's own rules
 * say. When they do, the Pass stops working *silently* — no wall, no error,
 * no indication. The user concludes the app is broken, and they're right.
 *
 * A periodic worker is not a guarantee — the same battery managers can
 * defer WorkManager too — but it recovers the common case where the service
 * was killed for memory and nothing has restarted it since.
 *
 * 15 minutes is WorkManager's minimum period. Anything shorter is silently
 * clamped to it.
 */
@HiltWorker
class MonitorWatchdogWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val settings: SettingsRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val shouldRun = settings.monitorEnabled.first()
        val hasPermission = UsagePermission.isGranted(applicationContext)

        if (shouldRun && hasPermission) {
            // Starting an already-running service is a no-op, so this is
            // safe to fire unconditionally.
            runCatching { UsageMonitorService.start(applicationContext) }
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "monitor_watchdog"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<MonitorWatchdogWorker>(
                15, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                // KEEP, not UPDATE: re-enqueuing on every app open would
                // restart the 15-minute clock each time and the worker would
                // never actually run.
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
