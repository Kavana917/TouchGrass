package com.touchgrass.app.core.usage

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.touchgrass.app.MainActivity
import com.touchgrass.app.R
import com.touchgrass.app.core.data.settings.SettingsRepository
import com.touchgrass.app.core.overlay.WallOverlayManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * The usage monitor.
 *
 * Runs as a foreground service so Android doesn't reclaim it while a watched
 * app is open. Polls [UsageStatsProvider] on an adaptive interval, writes the
 * running total to Room, and keeps its notification current.
 *
 * WHAT THIS PHASE DOES NOT DO: nothing happens when the budget hits zero.
 * The wall arrives in Phase 4. Right now this only has to count correctly,
 * and keep counting correctly across screen-offs, restarts and reboots.
 */
@AndroidEntryPoint
class UsageMonitorService : Service() {

    @Inject lateinit var usageRepository: UsageRepository
    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var provider: UsageStatsProvider
    @Inject lateinit var wallOverlay: WallOverlayManager

    /** Guards against re-showing the 2-minute warning on every poll. */
    private var warnedForDay: String? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var pollJob: Job? = null

    @Volatile private var screenOn: Boolean = true

    /**
     * Screen state matters a lot: pocket time is not scroll time
     * (app_plan.md §2.6). When the screen goes off we stop polling entirely,
     * which is both correct and free.
     */
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> screenOn = true
                Intent.ACTION_SCREEN_OFF -> screenOn = false
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        screenOn = powerManager?.isInteractive ?: true

        registerReceiver(
            screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification("Watching your time"))
        startPolling()
        // START_STICKY: if Android kills us for memory, restart when it can.
        // Not a guarantee - OEM battery managers ignore it, which is what
        // Phase 5 is about.
        return START_STICKY
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = scope.launch {
            scope.launch { settings.setMonitorEnabled(true) }

            while (true) {
                val interval = pollOnce()
                delay(interval)
            }
        }
    }

    /** One poll cycle. Returns how long to wait before the next one. */
    private suspend fun pollOnce(): Long {
        if (!UsagePermission.isGranted(this)) {
            updateNotification("Usage access needed")
            return BudgetUrgency.IDLE.pollIntervalMillis
        }

        val watched = settings.watchedPackages.first()
        if (watched.isEmpty()) {
            updateNotification("No apps being watched")
            return BudgetUrgency.IDLE.pollIntervalMillis
        }

        if (!screenOn) {
            // Screen is off. Nothing can be accumulating, so don't burn
            // cycles asking. Also take the wall down — there is nothing to
            // block, and leaving it up means it's the first thing on screen
            // when they unlock for an unrelated reason.
            if (wallOverlay.isShowing) withContext(Dispatchers.Main) { wallOverlay.hide() }
            return BudgetUrgency.IDLE.pollIntervalMillis
        }

        val resetHour = settings.resetHour.first()
        val budget = settings.dailyBudgetMinutes.first()
        val bonus = usageRepository.bonusMinutesFor(resetHour)

        val usedMinutes = usageRepository.refreshUsage(resetHour, watched)
        val allowance = budget + bonus
        val remaining = (allowance - usedMinutes).coerceAtLeast(0)

        val foreground = provider.currentForegroundPackage()
        val watchedInForeground = foreground in watched

        updateNotification(
            if (remaining > 0) "$remaining min left today"
            else "Time's up for today"
        )

        handleWall(
            watchedInForeground = watchedInForeground,
            foreground = foreground,
            remaining = remaining,
            dayKey = UsageStatsProvider.budgetDayKey(resetHour)
        )

        // The adaptive-polling table from app_plan.md §2.7.
        return when {
            !watchedInForeground -> BudgetUrgency.IDLE
            remaining <= 0 -> BudgetUrgency.ARMED
            remaining <= 5 -> BudgetUrgency.APPROACHING
            else -> BudgetUrgency.RELAXED
        }.pollIntervalMillis
    }

    /**
     * Decides whether the wall should be up right now.
     *
     * Deliberately re-evaluated every poll rather than fired once on the
     * zero crossing: the user can dismiss the wall, wander back into
     * Instagram, and it must reappear. A one-shot trigger would let them
     * back in for free.
     */
    private suspend fun handleWall(
        watchedInForeground: Boolean,
        foreground: String?,
        remaining: Int,
        dayKey: String
    ) = withContext(Dispatchers.Main) {
        val shouldShow = watchedInForeground && remaining <= 0

        when {
            shouldShow && !wallOverlay.isShowing -> wallOverlay.show(foreground)

            // They left the watched app, or earned more time. Either way the
            // wall has no business being on screen.
            !shouldShow && wallOverlay.isShowing -> wallOverlay.hide()
        }

        // Gentle heads-up at 2 minutes (app_plan.md §2.2) — a nudge, not the
        // wall. Once per day, so it never becomes nagging.
        if (watchedInForeground && remaining in 1..2 && warnedForDay != dayKey) {
            warnedForDay = dayKey
            Toast.makeText(
                this@UsageMonitorService,
                "$remaining min left today",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ---- Notification ----

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Usage monitor",
            // MIN keeps it as quiet as a foreground service is allowed to be.
            // The app should be unobtrusive; a nagging notification would
            // contradict the whole product.
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = "Keeps track of time spent in watched apps."
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TouchGrass")
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openApp)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    override fun onDestroy() {
        pollJob?.cancel()
        // Never leave the wall stranded on screen with nothing running to
        // take it down again.
        runCatching { wallOverlay.hide() }
        runCatching { unregisterReceiver(screenReceiver) }
        scope.launch { settings.setMonitorEnabled(false) }
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "usage_monitor"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, UsageMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, UsageMonitorService::class.java))
        }
    }
}
