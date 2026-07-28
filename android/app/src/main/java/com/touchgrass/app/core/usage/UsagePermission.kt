package com.touchgrass.app.core.usage

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings

/**
 * Usage access is a "special access" permission, and it does not behave like
 * anything else in Android:
 *
 *  - You cannot request it with `requestPermissions()`. You send the user to
 *    a Settings screen and hope they come back.
 *  - `checkSelfPermission()` always reports DENIED for it, even when granted.
 *    The only reliable check is [AppOpsManager].
 *
 * Both of those surprise everyone the first time. Hence this file.
 */
object UsagePermission {

    /**
     * True if the user has granted usage access.
     *
     * Uses AppOpsManager because the standard permission API lies about this
     * one. MODE_DEFAULT means "not explicitly set", which we treat as denied.
     */
    fun isGranted(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            ?: return false

        // unsafeCheckOpNoThrow only exists from API 29. Below that the same
        // check is the (now deprecated) checkOpNoThrow. Without this split
        // the app crashes on Android 8 and 9, which are inside our minSdk 26
        // support range.
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Opens the system Usage Access screen.
     *
     * There is no result to listen for — the user may grant it, ignore it, or
     * wander off. Always re-check with [isGranted] when the app resumes
     * rather than assuming the trip succeeded.
     */
    fun buildSettingsIntent(): Intent =
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
}
