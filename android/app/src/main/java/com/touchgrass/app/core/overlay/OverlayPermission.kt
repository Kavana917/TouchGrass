package com.touchgrass.app.core.overlay

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * "Draw over other apps" permission.
 *
 * Like usage access, this is a special access permission granted on a
 * Settings screen rather than through a dialog — but unlike usage access,
 * [Settings.canDrawOverlays] is a reliable check, so no AppOpsManager
 * gymnastics are needed here.
 */
object OverlayPermission {

    fun isGranted(context: Context): Boolean = Settings.canDrawOverlays(context)

    /**
     * Opens the overlay permission screen for this app.
     *
     * Unlike usage access, the `package:` URI here IS documented and
     * reliably lands on our own entry rather than a list.
     */
    fun openSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        runCatching { context.startActivity(intent) }
            .recoverCatching {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
    }
}
