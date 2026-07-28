package com.touchgrass.app.core.permissions

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.touchgrass.app.core.overlay.OverlayPermission
import com.touchgrass.app.core.usage.UsagePermission

/**
 * Every permission the app asks for, in one place.
 *
 * app_plan.md §6.2 has the reasoning: this app requests a genuinely
 * alarming-looking set — usage access plus draw-over-other-apps is the same
 * profile as spyware. The mitigation isn't to hide that, it's to explain
 * each one precisely and let the user inspect the state whenever they want.
 *
 * Hence a standing screen rather than a warning banner that disappears once
 * granted: "what has this app got access to, and why" should be answerable
 * at any time, not only when something is broken.
 */
enum class PermissionId {
    USAGE_ACCESS,
    DRAW_OVER_APPS,
    NOTIFICATIONS,
    BATTERY_UNRESTRICTED
}

data class PermissionInfo(
    val id: PermissionId,
    val title: String,
    /** What it lets the app do, in the user's terms. */
    val reason: String,
    /** What breaks without it — stated plainly, not as a threat. */
    val ifDenied: String,
    val required: Boolean,
    val granted: Boolean
)

object AppPermissions {

    fun all(context: Context): List<PermissionInfo> = listOf(
        PermissionInfo(
            id = PermissionId.USAGE_ACCESS,
            title = "Usage access",
            reason = "Lets TouchGrass see which app is open, so it can count " +
                "time spent. It reads app names and durations only — it " +
                "cannot see anything that happens inside your apps.",
            ifDenied = "The Pass cannot work at all.",
            required = true,
            granted = UsagePermission.isGranted(context)
        ),
        PermissionInfo(
            id = PermissionId.DRAW_OVER_APPS,
            title = "Draw over other apps",
            reason = "Lets the wall appear on top of Instagram when your time " +
                "runs out. Android gives a background app no other way to " +
                "put something on screen.",
            ifDenied = "Time still gets counted, but nothing stops you.",
            required = true,
            granted = OverlayPermission.isGranted(context)
        ),
        PermissionInfo(
            id = PermissionId.NOTIFICATIONS,
            title = "Notifications",
            reason = "Shows the quiet ongoing notification that keeps the " +
                "monitor alive, plus the two-minute heads-up.",
            ifDenied = "The monitor may be stopped by Android at any time.",
            required = true,
            granted = hasNotificationPermission(context)
        ),
        PermissionInfo(
            id = PermissionId.BATTERY_UNRESTRICTED,
            title = "Unrestricted battery",
            reason = "Stops your phone's battery saver from killing the " +
                "monitor while you're in another app.",
            ifDenied = "The monitor may stop silently — the most common " +
                "reason this kind of app appears broken.",
            required = false,
            granted = isIgnoringBatteryOptimizations(context)
        )
    )

    fun open(context: Context, id: PermissionId) {
        when (id) {
            PermissionId.USAGE_ACCESS -> UsagePermission.openSettings(context)
            PermissionId.DRAW_OVER_APPS -> OverlayPermission.openSettings(context)
            PermissionId.NOTIFICATIONS -> openAppNotificationSettings(context)
            PermissionId.BATTERY_UNRESTRICTED -> openBatterySettings(context)
        }
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        // POST_NOTIFICATIONS only became a runtime permission in API 33.
        // Below that, notifications are granted at install time.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager =
            context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    private fun openAppNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        runCatching { context.startActivity(intent) }
            .recoverCatching { openAppDetails(context) }
    }

    /**
     * Opens the battery-optimisation LIST rather than firing
     * ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS directly.
     *
     * The direct request needs the REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
     * permission, which Play scrutinises heavily and rejects for most apps.
     * Sending the user to the list costs one extra tap and avoids putting a
     * flagged permission in the manifest.
     */
    private fun openBatterySettings(context: Context) {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        runCatching { context.startActivity(intent) }
            .recoverCatching { openAppDetails(context) }
    }

    /** Last resort: this app's own settings page. Always exists. */
    fun openAppDetails(context: Context) {
        context.startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
