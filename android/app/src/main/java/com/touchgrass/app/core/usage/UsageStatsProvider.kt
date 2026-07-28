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
     * Everything we need from one replay of the event log: how long each
     * watched app has been open, and what's on screen right now.
     *
     * ⚠️ WHY THESE ARE COMPUTED TOGETHER, FROM THE SAME WINDOW:
     *
     * They used to be separate, and the foreground check looked back only 60
     * seconds for a resume event. That silently broke the wall: sit in
     * Instagram for two minutes without switching apps and the resume event
     * falls outside the window, so "what's in the foreground?" answers
     * "nothing" — while the usage total, queried from the start of the day,
     * kept counting correctly.
     *
     * The result was a budget that hit zero and a wall that never fired.
     *
     * The fix is to determine the foreground app the same way we determine
     * duration: replay from [since], track which sessions are still open,
     * and take the one most recently resumed. An app open for six hours is
     * still the foreground app.
     */
    data class ForegroundReport(
        /** Milliseconds in the foreground, per watched package. */
        val perAppMillis: Map<String, Long>,
        /** The package on screen right now, watched or not. */
        val currentPackage: String?
    )

    fun foregroundReport(packages: Set<String>, since: Long): ForegroundReport {
        val manager = usageStatsManager ?: return ForegroundReport(emptyMap(), null)

        val now = System.currentTimeMillis()
        val events = manager.queryEvents(since, now)

        // Adapt the Android cursor into plain data, then hand off to the pure
        // function below so the logic can be tested without a device.
        val parsed = mutableListOf<AppEvent>()
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            parsed += AppEvent(pkg, event.eventType, event.timeStamp)
        }

        return buildReport(parsed, packages, now)
    }

    /**
     * What's on screen right now.
     *
     * Looks back far enough to find an app that was opened hours ago and
     * never left — see the warning on [foregroundReport].
     */
    fun currentForegroundPackage(lookbackMillis: Long = DEFAULT_LOOKBACK_MILLIS): String? =
        foregroundReport(
            packages = emptySet(),
            since = System.currentTimeMillis() - lookbackMillis
        ).currentPackage

    /** Total foreground milliseconds per package since [since]. */
    fun foregroundMillisSince(packages: Set<String>, since: Long): Map<String, Long> =
        foregroundReport(packages, since).perAppMillis

    /** One foreground transition, decoupled from the Android event cursor. */
    data class AppEvent(
        val packageName: String,
        val eventType: Int,
        val timestamp: Long
    )

    companion object {

        /**
         * The actual logic, as a pure function — no Android, no clock.
         *
         * Walks the event log tracking which sessions are open. Sessions are
         * tracked for EVERY package, not just watched ones, because we need
         * to know when the user switches to something unwatched — that's
         * exactly when the wall should come down.
         *
         * @param now the instant the query was taken; still-open sessions
         *   are counted up to here, which is what makes totals tick upward
         *   live while an app is open.
         */
        fun buildReport(
            events: List<AppEvent>,
            packages: Set<String>,
            now: Long
        ): ForegroundReport {
            val totals = mutableMapOf<String, Long>()
            val openedAt = mutableMapOf<String, Long>()

            events.sortedBy { it.timestamp }.forEach { event ->
                when (event.eventType) {
                    EVENT_RESUMED -> openedAt[event.packageName] = event.timestamp

                    EVENT_PAUSED, EVENT_STOPPED -> {
                        val start = openedAt.remove(event.packageName) ?: return@forEach
                        if (event.packageName in packages) {
                            val duration = (event.timestamp - start).coerceAtLeast(0L)
                            totals[event.packageName] =
                                (totals[event.packageName] ?: 0L) + duration
                        }
                    }
                }
            }

            openedAt.forEach { (pkg, start) ->
                if (pkg in packages) {
                    totals[pkg] = (totals[pkg] ?: 0L) + (now - start).coerceAtLeast(0L)
                }
            }

            // The still-open session with the latest resume is what's on
            // screen. Crucially this has no recency cut-off — an app opened
            // six hours ago and never left is still the foreground app.
            val current = openedAt.maxByOrNull { it.value }?.key

            return ForegroundReport(perAppMillis = totals, currentPackage = current)
        }

        /**
         * ACTIVITY_RESUMED / ACTIVITY_PAUSED were added in API 29, but they
         * are the same integer values as the older MOVE_TO_FOREGROUND /
         * MOVE_TO_BACKGROUND constants they renamed. Using the raw ints keeps
         * one code path working from our minSdk of 26 upward, without
         * deprecation warnings or a version check.
         */
        const val EVENT_RESUMED = 1   // ACTIVITY_RESUMED / MOVE_TO_FOREGROUND
        const val EVENT_PAUSED = 2    // ACTIVITY_PAUSED  / MOVE_TO_BACKGROUND
        const val EVENT_STOPPED = 23  // ACTIVITY_STOPPED (API 29+; ignored below that)

        /**
         * How far back to look when asking what's on screen.
         *
         * 12 hours, because the question is "which session is still open?"
         * not "did something change recently?" — an app opened this morning
         * and never closed is still the foreground app.
         */
        private const val DEFAULT_LOOKBACK_MILLIS = 12 * 60 * 60 * 1000L

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
