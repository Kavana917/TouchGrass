package com.touchgrass.app.feature.permissions

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.touchgrass.app.core.permissions.AppPermissions
import com.touchgrass.app.core.permissions.PermissionId
import com.touchgrass.app.core.permissions.PermissionInfo
import com.touchgrass.app.ui.components.BodyText
import com.touchgrass.app.ui.components.PixelText
import com.touchgrass.app.ui.components.RetroButton
import com.touchgrass.app.ui.components.RetroWindow
import com.touchgrass.app.ui.components.Wallpaper
import com.touchgrass.app.ui.theme.Dimens
import com.touchgrass.app.ui.theme.RetroTheme

/**
 * The permissions screen — Control Panel, in the desktop metaphor
 * (design_theme.md §10).
 *
 * Standing surface, not a warning banner. This app asks for a set that
 * legitimately looks alarming, so "what does it have access to, and why"
 * needs to be answerable at any moment — including long after everything is
 * already granted.
 *
 * Status refreshes automatically on resume, because these permissions are
 * granted on a Settings screen we get no callback from. Without that you'd
 * come back from Settings and still see "Not granted".
 */
@Composable
fun PermissionsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var permissions by remember { mutableStateOf(AppPermissions.all(context)) }

    fun refresh() {
        permissions = AppPermissions.all(context)
    }

    // POST_NOTIFICATIONS is the one permission here that IS a normal runtime
    // request, so it gets a real dialog rather than a trip to Settings.
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { refresh() }

    // Re-check whenever the user comes back from a Settings screen.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val grantedCount = permissions.count { it.granted }
    val requiredMissing = permissions.count { it.required && !it.granted }

    Wallpaper(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Dimens.ItemSpacing),
            verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)
        ) {
            RetroWindow(
                title = "Permissions",
                statusText = "$grantedCount of ${permissions.size} granted",
                statusSecondary = if (requiredMissing == 0) "ready" else "$requiredMissing needed"
            ) {
                BodyText(
                    "TouchGrass asks for a lot, and some of it looks alarming. " +
                        "Here's exactly what each one is for."
                )
                BodyText(
                    "Nothing you write or draw ever leaves this phone.",
                    style = RetroTheme.typography.bodySmall
                )
            }

            permissions.forEach { permission ->
                PermissionCard(
                    permission = permission,
                    onGrant = {
                        if (permission.id == PermissionId.NOTIFICATIONS &&
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                        ) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            AppPermissions.open(context, permission.id)
                        }
                    }
                )
            }

            RetroWindow(title = "All app settings") {
                BodyText(
                    "Android's own settings page for TouchGrass, if you'd " +
                        "rather manage everything there."
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    RetroButton(
                        text = "Open",
                        onClick = { AppPermissions.openAppDetails(context) }
                    )
                    RetroButton(text = "Re-check", onClick = { refresh() })
                }
            }

            RetroWindow(title = "") {
                RetroButton(text = "Back", onClick = onBack)
            }
        }
    }
}

@Composable
private fun PermissionCard(
    permission: PermissionInfo,
    onGrant: () -> Unit
) {
    RetroWindow(
        title = permission.title,
        statusText = if (permission.granted) "granted" else "not granted",
        statusSecondary = if (permission.required) "required" else "recommended"
    ) {
        BodyText(permission.reason)

        if (!permission.granted) {
            // Stated as a consequence, not a warning. The app never scolds,
            // and that applies to its own setup screens too (§11).
            PixelText(
                text = permission.ifDenied,
                color = RetroTheme.colors.surfaceShadow
            )
            RetroButton(
                text = "Grant",
                primary = permission.required,
                onClick = onGrant,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                PixelText("✓ granted")
                RetroButton(text = "Change", onClick = onGrant)
            }
        }
    }
}
