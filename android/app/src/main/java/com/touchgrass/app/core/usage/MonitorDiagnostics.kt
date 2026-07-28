package com.touchgrass.app.core.usage

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What the monitor is actually doing, so failures are visible instead of
 * silent.
 *
 * The wall depends on a chain of preconditions — permission granted, service
 * alive, app watched, budget spent, overlay permitted — and when it doesn't
 * appear, ALL of them look the same from outside: nothing happens. This
 * exposes each link so the broken one can be identified rather than guessed.
 *
 * app_plan.md §2.7 calls silent failure the number one driver of one-star
 * reviews for apps in this category. This is the beginning of the
 * self-diagnosis that Phase 5 builds out properly.
 */
data class MonitorSnapshot(
    val lastPollAt: Long = 0L,
    val pollCount: Long = 0L,
    val foregroundPackage: String? = null,
    val watchedInForeground: Boolean = false,
    val remainingMinutes: Int = -1,
    val shouldShowWall: Boolean = false,
    val wallShowing: Boolean = false,
    val overlayPermitted: Boolean = false,
    val screenOn: Boolean = true,
    /** Set if a poll threw. The loop keeps running; this records what broke. */
    val lastError: String? = null,
    val lastErrorAt: Long = 0L,
    /** Why the overlay refused to appear, if it did. */
    val wallError: String? = null,
    /**
     * Recent polls, newest first.
     *
     * Needed because the live fields above can only ever be read while
     * TouchGrass is in the foreground — which is, by definition, the one
     * moment the wall should NOT be showing. Without a history, the panel
     * can't say anything about what happened while you were in Instagram,
     * which is the only thing worth knowing.
     */
    val recentPolls: List<PollRecord> = emptyList()
)

data class PollRecord(
    val at: Long,
    val foregroundPackage: String?,
    val watchedInForeground: Boolean,
    val remainingMinutes: Int,
    val shouldShowWall: Boolean,
    val wallShowing: Boolean
)

@Singleton
class MonitorDiagnostics @Inject constructor() {

    private val _state = MutableStateFlow(MonitorSnapshot())
    val state: StateFlow<MonitorSnapshot> = _state.asStateFlow()

    fun recordPoll(
        foregroundPackage: String?,
        watchedInForeground: Boolean,
        remainingMinutes: Int,
        shouldShowWall: Boolean,
        wallShowing: Boolean,
        overlayPermitted: Boolean,
        screenOn: Boolean,
        wallError: String? = null
    ) {
        val now = System.currentTimeMillis()
        val record = PollRecord(
            at = now,
            foregroundPackage = foregroundPackage,
            watchedInForeground = watchedInForeground,
            remainingMinutes = remainingMinutes,
            shouldShowWall = shouldShowWall,
            wallShowing = wallShowing
        )

        _state.value = _state.value.copy(
            lastPollAt = now,
            pollCount = _state.value.pollCount + 1,
            foregroundPackage = foregroundPackage,
            watchedInForeground = watchedInForeground,
            remainingMinutes = remainingMinutes,
            shouldShowWall = shouldShowWall,
            wallShowing = wallShowing,
            overlayPermitted = overlayPermitted,
            screenOn = screenOn,
            wallError = wallError,
            // Only keep transitions and wall-relevant moments, so the log
            // isn't 40 identical lines of "sitting in the launcher".
            recentPolls = (listOf(record) + _state.value.recentPolls)
                .filterIndexed { index, entry ->
                    index == 0 || entry.watchedInForeground || entry.shouldShowWall
                }
                .take(MAX_RECORDS)
        )
    }

    fun recordError(throwable: Throwable) {
        _state.value = _state.value.copy(
            lastError = "${throwable::class.simpleName}: ${throwable.message}",
            lastErrorAt = System.currentTimeMillis()
        )
    }

    fun clearError() {
        _state.value = _state.value.copy(lastError = null, lastErrorAt = 0L)
    }

    fun reset() {
        _state.value = MonitorSnapshot()
    }

    private companion object {
        const val MAX_RECORDS = 12
    }
}
