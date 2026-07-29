package com.touchgrass.app.core.usage

import android.content.Context
import com.touchgrass.app.core.data.settings.SettingsRepository
import com.touchgrass.app.core.overlay.OverlayPermission
import com.touchgrass.app.core.permissions.AppPermissions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Self-diagnosis on app open (Phase 5 / app_plan.md §2.7).
 *
 * Silent monitor death is the top negative-review driver for apps in this
 * category. This surfaces it as a plain sentence with a next step, instead
 * of leaving the user to conclude the app is broken.
 */
@Singleton
class MonitorHealth @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
    private val diagnostics: MonitorDiagnostics
) {

    data class Issue(
        val title: String,
        val detail: String,
        val kind: Kind
    ) {
        enum class Kind {
            PERMISSIONS,
            MONITOR_STOPPED,
            OEM
        }
    }

    suspend fun check(): Issue? {
        val monitorEnabled = settings.monitorEnabled.first()
        if (!monitorEnabled) return null

        val missingRequired = AppPermissions.all(context)
            .filter { it.required && !it.granted }
        if (missingRequired.isNotEmpty()) {
            val names = missingRequired.joinToString(", ") { it.title.lowercase() }
            return Issue(
                title = "Permissions still needed",
                detail = "The Pass can't work until $names is on.",
                kind = Issue.Kind.PERMISSIONS
            )
        }

        val now = System.currentTimeMillis()
        val lastSeen = settings.lastSeenAt.first()
        val heartbeat = settings.lastMonitorHeartbeatAt.first()
        val awayMs = (now - lastSeen).coerceAtLeast(0L)
        val staleMs = if (heartbeat > 0L) (now - heartbeat).coerceAtLeast(0L) else Long.MAX_VALUE

        // User was away long enough that polling should have happened, but
        // the heartbeat hasn't moved — the phone probably killed us.
        if (awayMs > AWAY_THRESHOLD_MS && staleMs > STALE_THRESHOLD_MS) {
            return Issue(
                title = "Monitor may have stopped",
                detail = "Looks like your phone stopped TouchGrass in the " +
                    "background. Open Phone survival below and follow the " +
                    "steps for your brand, then come back here.",
                kind = Issue.Kind.OEM
            )
        }

        val snapshot = diagnostics.state.value
        if (snapshot.pollCount == 0L && awayMs > STARTUP_GRACE_MS) {
            return Issue(
                title = "Monitor isn't polling yet",
                detail = "TouchGrass should be counting time, but hasn't " +
                    "polled once. Check permissions, then tap Restart monitor.",
                kind = Issue.Kind.MONITOR_STOPPED
            )
        }

        if (snapshot.wallError != null && OverlayPermission.isGranted(context)) {
            return Issue(
                title = "Wall couldn't appear",
                detail = snapshot.wallError,
                kind = Issue.Kind.OEM
            )
        }

        return null
    }

    companion object {
        private const val AWAY_THRESHOLD_MS = 15 * 60_000L
        private const val STALE_THRESHOLD_MS = 20 * 60_000L
        private const val STARTUP_GRACE_MS = 2 * 60_000L
    }
}
