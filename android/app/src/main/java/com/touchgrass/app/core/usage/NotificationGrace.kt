package com.touchgrass.app.core.usage

import com.touchgrass.app.core.data.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Brief grace when a watched app comes back to the foreground with a spent
 * budget (app_plan.md §2.6).
 *
 * A notification tap often lands you in Instagram for ten seconds to read one
 * message. The wall firing instantly on that landing would punish the exact
 * reflex the grace is meant to spare.
 */
@Singleton
class NotificationGrace @Inject constructor(
    private val settings: SettingsRepository
) {
    @Volatile private var graceUntilMs: Long = 0L

    @Volatile private var lastForeground: String? = null

    suspend fun onForegroundChange(
        foreground: String?,
        watched: Set<String>,
        spent: Boolean
    ) {
        if (!settings.notificationGraceEnabled.first()) return

        val previous = lastForeground
        lastForeground = foreground

        val enteredWatched = foreground != null &&
            foreground in watched &&
            foreground != previous

        if (enteredWatched && spent) {
            graceUntilMs = System.currentTimeMillis() + GRACE_MS
        }
    }

    fun isActive(): Boolean = System.currentTimeMillis() < graceUntilMs

    companion object {
        private const val GRACE_MS = 60_000L
    }
}
