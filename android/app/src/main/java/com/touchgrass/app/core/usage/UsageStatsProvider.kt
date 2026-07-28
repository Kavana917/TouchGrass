package com.touchgrass.app.core.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads foreground-app data from the OS.
 *
 * DESIGN NOTE — why we recompute instead of counting up:
 *
 * The obvious implementation is a ticker that adds a second to a counter
 * every second Instagram is open. It's also fragile: kill the service, miss
 * a poll, reboot the phone, and the count is silently wrong forever.
 *
 * Instead we ask the OS "how long has this package been in the foreground
 * since the day boundary?" on every poll, by replaying its event log. The
 * OS was keeping count the whole time regardless of whether our service was
 * alive, so the answer is always right — and a twenty-minute gap in our
 * monitoring corrects itself on the next poll rather than being lost.
 *
 * This is the single most important decision in Phase 2.
 */
@Singleton
class UsageStatsProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val usageStatsManager: UsageStatsManager?
        get() = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    /**
     * The package currently in the foreground, or null if we can't tell.
     *
     * Works by replaying the last [lookbackMillis] of events and taking the
     * most recent resume. There is no direct "what's on screen right now"
     * API available to a normal app.
     */
    fun currentForegroundPackage(lookbackMillis: Long = 60_000L): String? {
        val manager = usageStatsManager ?: return null
        val now = System.currentTimeMillis()
        val events = manager.queryEvents(now - lookbackMillis, now)

        var latestPackage: String? = null
        var latestTime = 0L
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == EVENT_RESUMED && event.timeStamp >= latestTime) {
                latestTime = event.timeStamp
                latestPackage = event.packageName
            }
        }
        return latestPackage
    }

    /**
     * Total foreground milliseconds per package since [since].
     *
     * Only packages in [packages] are counted. Sessions still open when the
     * query runs are counted up to `now`, which is what makes the number
     * tick upward live while an app is open.
     */
    fun foregroundMillisSince(packages: Set<String>, since: Long): Map<String, Long> {
        if (packages.isEmpty()) return emptyMap()
        val manager = usageStatsManager ?: return emptyMap()

        val now = System.currentTimeMillis()
        val events = manager.queryEvents(since, now)

        val totals = mutableMapOf<String, Long>()
        val openedAt = mutableMapOf<String, Long>()
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            if (pkg !in packages) continue

            when (event.eventType) {
                EVENT_RESUMED -> openedAt[pkg] = event.timeStamp

                EVENT_PAUSED, EVENT_STOPPED -> {
                    val start = openedAt.remove(pkg) ?: continue
                    val duration = (event.timeStamp - start).coerceAtLeast(0L)
                    totals[pkg] = (totals[pkg] ?: 0L) + duration
                }
            }
        }

        // Anything still open contributes right up to this moment.
        openedAt.forEach { (pkg, start) ->
            totals[pkg] = (totals[pkg] ?: 0L) + (now - start).coerceAtLeast(0L)
        }

        return totals
    }

    companion object {
        /**
         * ACTIVITY_RESUMED / ACTIVITY_PAUSED were added in API 29, but they
         * are the same integer values as the older MOVE_TO_FOREGROUND /
         * MOVE_TO_BACKGROUND constants they renamed. Using the raw ints keeps
         * one code path working from our minSdk of 26 upward, without
         * deprecation warnings or a version check.
         */
        private const val EVENT_RESUMED = 1   // ACTIVITY_RESUMED / MOVE_TO_FOREGROUND
        private const val EVENT_PAUSED = 2    // ACTIVITY_PAUSED  / MOVE_TO_BACKGROUND
        private const val EVENT_STOPPED = 23  // ACTIVITY_STOPPED (API 29+; ignored below that)

        /**
         * Start of the current budget day, given a reset hour.
         *
         * If it is 2am and the reset hour is 4am, the current budget day
         * began at 4am *yesterday* — which is exactly the behaviour that
         * stops a late-night session getting a free reset at midnight.
         */
        fun budgetDayStart(resetHour: Int, now: Long = System.currentTimeMillis()): Long {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = now
                set(Calendar.HOUR_OF_DAY, resetHour)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (calendar.timeInMillis > now) {
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            }
            return calendar.timeInMillis
        }

        /** Stable key for the current budget day, e.g. `2026-07-28`. */
        fun budgetDayKey(resetHour: Int, now: Long = System.currentTimeMillis()): String {
            val start = budgetDayStart(resetHour, now)
            val calendar = Calendar.getInstance().apply { timeInMillis = start }
            return "%04d-%02d-%02d".format(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH)
            )
        }
    }
}
