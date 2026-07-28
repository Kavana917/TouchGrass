package com.touchgrass.app.core.usage

import android.os.Build

/**
 * Per-manufacturer instructions for stopping the phone killing the monitor.
 *
 * app_plan.md §2.7 calls this the number one negative-review driver for
 * every app in this category, and the reason is structural: the setting is
 * buried somewhere different on every brand, isn't discoverable, and when
 * it's wrong the app fails silently rather than complaining.
 *
 * Steps are deliberately short and phrased as menu paths, since that's what
 * the user has to navigate. Wording drifts between OS versions — these are
 * close enough to find the screen, not exact transcriptions.
 */
data class OemSteps(
    val manufacturer: String,
    val steps: List<String>,
    /** Skins that need a second permission for background overlays. */
    val needsBackgroundPopupPermission: Boolean = false
)

object OemGuidance {

    fun forThisDevice(): OemSteps = forManufacturer(Build.MANUFACTURER)

    fun forManufacturer(raw: String?): OemSteps {
        return when (raw?.lowercase()?.trim()) {
            "xiaomi", "redmi", "poco" -> OemSteps(
                manufacturer = "Xiaomi / Redmi / POCO",
                needsBackgroundPopupPermission = true,
                steps = listOf(
                    "Settings → Apps → Manage apps → TouchGrass",
                    "Turn on Autostart",
                    "Battery saver → No restrictions",
                    "Other permissions → Display pop-up windows while running in background → Allow",
                    "In Recents, pull TouchGrass down and tap the padlock to keep it"
                )
            )

            "oppo", "realme", "oneplus" -> OemSteps(
                manufacturer = "OPPO / realme / OnePlus",
                needsBackgroundPopupPermission = true,
                steps = listOf(
                    "Settings → Battery → App battery usage → TouchGrass → Allow background activity",
                    "Settings → Apps → TouchGrass → Allow auto launch",
                    "Settings → Apps → TouchGrass → Display over other apps → Allow",
                    "In Recents, lock TouchGrass so it isn't cleared"
                )
            )

            "vivo", "iqoo" -> OemSteps(
                manufacturer = "vivo / iQOO",
                needsBackgroundPopupPermission = true,
                steps = listOf(
                    "Settings → Battery → High background power consumption → allow TouchGrass",
                    "Settings → More settings → Permission manager → Autostart → TouchGrass on",
                    "Settings → Apps → TouchGrass → Floating window / Display over other apps → Allow"
                )
            )

            "samsung" -> OemSteps(
                manufacturer = "Samsung",
                steps = listOf(
                    "Settings → Battery → Background usage limits",
                    "Make sure TouchGrass is NOT in 'Sleeping apps' or 'Deep sleeping apps'",
                    "Settings → Apps → TouchGrass → Battery → Unrestricted"
                )
            )

            "huawei", "honor" -> OemSteps(
                manufacturer = "Huawei / Honor",
                needsBackgroundPopupPermission = true,
                steps = listOf(
                    "Settings → Battery → App launch → TouchGrass → Manage manually",
                    "Turn on Auto-launch, Secondary launch and Run in background",
                    "Settings → Apps → TouchGrass → Display over other apps → Allow"
                )
            )

            "google" -> OemSteps(
                manufacturer = "Google Pixel",
                steps = listOf(
                    "Settings → Apps → TouchGrass → Battery → Unrestricted",
                    "Pixels are generally well behaved — if the monitor stops, this is the setting to check"
                )
            )

            else -> OemSteps(
                manufacturer = raw?.replaceFirstChar { it.uppercase() } ?: "Your phone",
                steps = listOf(
                    "Settings → Apps → TouchGrass → Battery → Unrestricted (or Allow background activity)",
                    "Look for an 'Autostart' or 'Auto-launch' setting and turn it on",
                    "If your phone has a 'Display pop-up while in background' permission, allow it",
                    "In Recents, lock TouchGrass so clearing recents doesn't kill it"
                )
            )
        }
    }
}
