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
    val lastErrorAt: Long = 0L
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
        screenOn: Boolean
    ) {
        _state.value = _state.value.copy(
            lastPollAt = System.currentTimeMillis(),
            pollCount = _state.value.pollCount + 1,
            foregroundPackage = foregroundPackage,
            watchedInForeground = watchedInForeground,
            remainingMinutes = remainingMinutes,
            shouldShowWall = shouldShowWall,
            wallShowing = wallShowing,
            overlayPermitted = overlayPermitted,
            screenOn = screenOn
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
}
