package com.touchgrass.app.core.usage

import com.touchgrass.app.core.usage.UsageStatsProvider.AppEvent
import com.touchgrass.app.core.usage.UsageStatsProvider.Companion.EVENT_PAUSED
import com.touchgrass.app.core.usage.UsageStatsProvider.Companion.EVENT_RESUMED
import com.touchgrass.app.core.usage.UsageStatsProvider.Companion.buildReport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private const val INSTAGRAM = "com.instagram.android"
private const val YOUTUBE = "com.google.android.youtube"
private const val LAUNCHER = "com.android.launcher"

private const val MINUTE = 60_000L

class UsageStatsProviderTest {

    private val watched = setOf(INSTAGRAM, YOUTUBE)

    @Test
    fun `counts a closed session`() {
        val report = buildReport(
            events = listOf(
                AppEvent(INSTAGRAM, EVENT_RESUMED, 0),
                AppEvent(INSTAGRAM, EVENT_PAUSED, 5 * MINUTE)
            ),
            packages = watched,
            now = 10 * MINUTE
        )
        assertEquals(5 * MINUTE, report.perAppMillis[INSTAGRAM])
    }

    @Test
    fun `an open session counts up to now`() {
        val report = buildReport(
            events = listOf(AppEvent(INSTAGRAM, EVENT_RESUMED, 0)),
            packages = watched,
            now = 7 * MINUTE
        )
        assertEquals(7 * MINUTE, report.perAppMillis[INSTAGRAM])
    }

    /**
     * THE REGRESSION THIS FILE EXISTS FOR.
     *
     * The old implementation looked back only 60 seconds for a resume event,
     * so sitting in one app without switching made the foreground check
     * report "nothing" — and the wall silently never fired even though the
     * budget had hit zero.
     */
    @Test
    fun `an app open for hours is still the foreground app`() {
        val sixHours = 6 * 60 * MINUTE
        val report = buildReport(
            events = listOf(AppEvent(INSTAGRAM, EVENT_RESUMED, 0)),
            packages = watched,
            now = sixHours
        )
        assertEquals(INSTAGRAM, report.currentPackage)
    }

    @Test
    fun `foreground follows the most recent resume`() {
        val report = buildReport(
            events = listOf(
                AppEvent(INSTAGRAM, EVENT_RESUMED, 0),
                AppEvent(INSTAGRAM, EVENT_PAUSED, MINUTE),
                AppEvent(YOUTUBE, EVENT_RESUMED, MINUTE)
            ),
            packages = watched,
            now = 3 * MINUTE
        )
        assertEquals(YOUTUBE, report.currentPackage)
    }

    @Test
    fun `switching to an unwatched app clears the foreground`() {
        // The wall must come down when the user leaves for something we
        // don't police, so unwatched apps have to be tracked too.
        val report = buildReport(
            events = listOf(
                AppEvent(INSTAGRAM, EVENT_RESUMED, 0),
                AppEvent(INSTAGRAM, EVENT_PAUSED, MINUTE),
                AppEvent(LAUNCHER, EVENT_RESUMED, MINUTE)
            ),
            packages = watched,
            now = 2 * MINUTE
        )
        assertEquals(LAUNCHER, report.currentPackage)
        assertEquals(MINUTE, report.perAppMillis[INSTAGRAM])
    }

    @Test
    fun `unwatched apps contribute no time`() {
        val report = buildReport(
            events = listOf(
                AppEvent(LAUNCHER, EVENT_RESUMED, 0),
                AppEvent(LAUNCHER, EVENT_PAUSED, 30 * MINUTE)
            ),
            packages = watched,
            now = 30 * MINUTE
        )
        assertNull(report.perAppMillis[LAUNCHER])
    }

    @Test
    fun `separate sessions accumulate`() {
        val report = buildReport(
            events = listOf(
                AppEvent(INSTAGRAM, EVENT_RESUMED, 0),
                AppEvent(INSTAGRAM, EVENT_PAUSED, 2 * MINUTE),
                AppEvent(INSTAGRAM, EVENT_RESUMED, 10 * MINUTE),
                AppEvent(INSTAGRAM, EVENT_PAUSED, 13 * MINUTE)
            ),
            packages = watched,
            now = 20 * MINUTE
        )
        assertEquals(5 * MINUTE, report.perAppMillis[INSTAGRAM])
    }

    @Test
    fun `a pause with no matching resume is ignored`() {
        // Happens whenever the query window starts mid-session.
        val report = buildReport(
            events = listOf(AppEvent(INSTAGRAM, EVENT_PAUSED, MINUTE)),
            packages = watched,
            now = 2 * MINUTE
        )
        assertNull(report.perAppMillis[INSTAGRAM])
        assertNull(report.currentPackage)
    }

    @Test
    fun `no events means nothing in the foreground`() {
        val report = buildReport(emptyList(), watched, now = MINUTE)
        assertNull(report.currentPackage)
        assertEquals(0, report.perAppMillis.size)
    }
}
