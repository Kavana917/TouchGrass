package com.touchgrass.app.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.touchgrass.app.core.permissions.AppPermissions
import com.touchgrass.app.core.usage.OemGuidance
import com.touchgrass.app.ui.components.BodyText
import com.touchgrass.app.ui.components.PixelText
import com.touchgrass.app.ui.components.RetroButton
import com.touchgrass.app.ui.components.RetroCheckbox
import com.touchgrass.app.ui.components.RetroWindow
import com.touchgrass.app.ui.components.Wallpaper
import com.touchgrass.app.ui.theme.Dimens
import com.touchgrass.app.ui.theme.RetroTheme

@Composable
fun WatchedAppsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WatchedAppsViewModel = hiltViewModel()
) {
    val apps by viewModel.apps.collectAsStateWithLifecycle()

    Wallpaper(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Dimens.ItemSpacing),
            verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)
        ) {
            RetroWindow(
                title = "Watched apps",
                statusText = "${apps.count { it.watched }} selected"
            ) {
                BodyText("Tick the apps whose time should count against the budget.")
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        apps.forEach { app ->
                            RetroCheckbox(
                                checked = app.watched,
                                onCheckedChange = { viewModel.toggle(app.packageName) },
                                label = app.label
                            )
                        }
                    }
                }
            }

            RetroWindow(title = "") {
                RetroButton(text = "Back", onClick = onBack)
            }
        }
    }
}

@Composable
fun OemHelpScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val steps = OemGuidance.forThisDevice()

    Wallpaper(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Dimens.ItemSpacing),
            verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)
        ) {
            RetroWindow(
                title = "Phone survival",
                statusText = steps.manufacturer
            ) {
                BodyText(
                    "Phones aggressively kill background apps. When yours does, " +
                        "TouchGrass stops working silently. These settings stop that:"
                )
                steps.steps.forEachIndexed { index, step ->
                    BodyText(
                        "${index + 1}. $step",
                        style = RetroTheme.typography.bodySmall
                    )
                }
                if (steps.needsBackgroundPopupPermission) {
                    BodyText(
                        "Your phone may also need a second permission beyond " +
                            "draw-over-apps — often called \"display pop-up windows " +
                            "while running in background\".",
                        style = RetroTheme.typography.bodySmall
                    )
                }
                RetroButton(
                    text = "Open app settings",
                    onClick = { AppPermissions.openAppDetails(context) }
                )
            }

            RetroWindow(title = "") {
                RetroButton(text = "Back", onClick = onBack)
            }
        }
    }
}

@Composable
fun PrivacyScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Wallpaper(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Dimens.ItemSpacing),
            verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)
        ) {
            RetroWindow(title = "Privacy") {
                PixelText("Your writing", style = RetroTheme.typography.heading)
                BodyText("Your essays never leave this phone.")
                BodyText(
                    "Not synced, not uploaded, not analysed, not read. Same for your " +
                        "drawings and your usage history. There is no account and no " +
                        "server holding any of it."
                )
                BodyText(
                    "Usage access lets TouchGrass see app names and how long they were " +
                        "open. It cannot see anything that happens inside them — not " +
                        "messages, not what you looked at, nothing.",
                    style = RetroTheme.typography.bodySmall
                )
                BodyText(
                    "The only network call in this app will fetch a daily digest of " +
                        "public headlines (FOMO) — no personal data is sent.",
                    style = RetroTheme.typography.bodySmall
                )
            }

            RetroWindow(title = "") {
                RetroButton(text = "Back", onClick = onBack)
            }
        }
    }
}
