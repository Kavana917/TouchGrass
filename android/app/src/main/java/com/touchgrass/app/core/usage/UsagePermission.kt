package com.touchgrass.app.core.usage

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
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
     * Opens the system Usage Access screen, landing on TouchGrass's own entry
     * where possible rather than the full list of installed apps.
     *
     * Attaching a `package:` URI to the intent is what asks Settings to jump
     * straight to one app. It works on most devices but is not part of the
     * documented contract, so OEMs are free to ignore it and show the plain
     * list — hence the fallback. Both paths end up somewhere the user can
     * complete the grant; the deep link just saves them hunting through a
     * long alphabetical list.
     *
     * There is no result to listen for — the user may grant it, ignore it, or
     * wander off. Always re-check with [isGranted] when the app resumes
     * rather than assuming the trip succeeded.
     */
    fun openSettings(context: Context) {
        val appSpecific = Intent(
            Settings.ACTION_USAGE_ACCESS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val generic = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        // Try the deep link, fall back to the list, and if even that is
        // missing do nothing rather than crash — some heavily customised
        // builds have no usage-access screen at all.
        runCatching { context.startActivity(appSpecific) }
            .recoverCatching { context.startActivity(generic) }
    }
}
