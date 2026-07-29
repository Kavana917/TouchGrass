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
import com.touchgrass.app.core.overlay.OverlayPermission
import com.touchgrass.app.core.overlay.WallOverlayManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
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
    @Inject lateinit var diagnostics: MonitorDiagnostics
    @Inject lateinit var notificationGrace: NotificationGrace

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
                // A throw here used to kill the coroutine outright: polling
                // stopped forever, with no crash and no log, and the wall
                // simply never appeared again. Catching keeps the loop alive
                // and records what broke so it's visible in the app.
                val interval = try {
                    pollOnce()
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (error: Throwable) {
                    diagnostics.recordError(error)
                    BudgetUrgency.APPROACHING.pollIntervalMillis
                }
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

        // A raise scheduled yesterday takes effect once the day has actually
        // turned over. Doing it here means it happens even if the app is
        // never opened.
        settings.promotePendingBudget(UsageStatsProvider.budgetDayKey(resetHour))

        // Usage total and foreground app come from the SAME event replay, so
        // they can never disagree about whether an app is open.
        val report = usageRepository.refreshUsage(resetHour, watched)
        val foreground = report.currentPackage
        val watchedInForeground = foreground in watched

        // One snapshot carries mode, per-app limits and grants, so the
        // service doesn't have to re-derive the rules the UI already knows.
        val state = usageRepository.snapshot().copy(
            foregroundPackage = foreground,
            watchedAppInForeground = watchedInForeground,
            screenOn = screenOn
        )

        // In per-app mode the headline number is the app you're actually in,
        // because that's the limit about to bite.
        val remaining = state.budgetFor(foreground)?.remainingMinutes
            ?: state.remainingMinutes

        val spent = state.isSpentFor(foreground)
        notificationGrace.onForegroundChange(foreground, watched, spent)

        val shouldShow = watchedInForeground && spent && !notificationGrace.isActive()

        handleWall(
            shouldShow = shouldShow,
            foreground = foreground,
            remaining = remaining,
            watchedInForeground = watchedInForeground,
            dayKey = state.dayKey
        )

        // The notification is the ONLY surface visible from inside Instagram,
        // which makes it the only place a live diagnosis can be read at the
        // moment it matters. Pull down the shade while scrolling and it says
        // exactly what the monitor thinks is happening.
        updateNotification(
            buildString {
                append("$remaining min · ")
                append(foreground?.substringAfterLast('.') ?: "unknown")
                when {
                    wallOverlay.lastError != null -> append(" · WALL FAILED")
                    wallOverlay.isShowing -> append(" · wall up")
                    shouldShow -> append(" · wall pending")
                }
            }
        )

        diagnostics.recordPoll(
            foregroundPackage = foreground,
            watchedInForeground = watchedInForeground,
            remainingMinutes = remaining,
            shouldShowWall = shouldShow,
            wallShowing = wallOverlay.isShowing,
            overlayPermitted = OverlayPermission.isGranted(this),
            screenOn = screenOn,
            wallError = wallOverlay.lastError
        )

        settings.recordMonitorHeartbeat()

        return state.urgency.pollIntervalMillis
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
        shouldShow: Boolean,
        foreground: String?,
        remaining: Int,
        watchedInForeground: Boolean,
        dayKey: String
    ) = withContext(Dispatchers.Main) {
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
            // The watchdog only matters while the monitor is meant to be
            // running, so its lifecycle is tied to the service's.
            MonitorWatchdogWorker.schedule(context)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, UsageMonitorService::class.java))
            MonitorWatchdogWorker.cancel(context)
        }
    }
}
