package com.touchgrass.app.feature.usage

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.touchgrass.app.core.data.settings.BudgetMode
import com.touchgrass.app.ui.components.BodyText
import com.touchgrass.app.ui.components.PixelText
import com.touchgrass.app.ui.components.RetroButton
import com.touchgrass.app.ui.components.RetroCheckbox
import com.touchgrass.app.ui.components.RetroWindow
import com.touchgrass.app.ui.components.SegmentedProgress
import com.touchgrass.app.ui.components.Taskbar
import com.touchgrass.app.ui.components.Wallpaper
import com.touchgrass.app.ui.theme.Dimens
import com.touchgrass.app.ui.theme.RetroTheme

/**
 * Phase 2 debug screen — scaffolding, not product.
 *
 * The real Pass status screen and watched-app picker land in Phase 5. This
 * exists so the monitor can be *observed working* on a real device: pick an
 * app, set a tiny budget, open that app, come back, watch the number move.
 */
@Composable
fun UsageDebugScreen(
    modifier: Modifier = Modifier,
    onWriteEssay: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onOpenPermissions: () -> Unit = {},
    onOpenBudget: () -> Unit = {},
    onOpenGallery: () -> Unit = {},
    viewModel: UsageDebugViewModel = hiltViewModel()
) {
    val state by viewModel.budgetState.collectAsStateWithLifecycle()
    val granted by viewModel.permissionGranted.collectAsStateWithLifecycle()
    val overlayGranted by viewModel.overlayGranted.collectAsStateWithLifecycle()
    val apps by viewModel.installedApps.collectAsStateWithLifecycle()
    val foreground by viewModel.liveForeground.collectAsStateWithLifecycle()

    // Permissions are granted on Settings screens we get no callback from,
    // so re-check whenever this screen comes back to the foreground.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshPermission()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Wallpaper(modifier = modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(Dimens.ItemSpacing),
                verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)
            ) {

                // ---- Budget ----
                RetroWindow(
                    title = "Today",
                    statusText = state.dayKey.ifBlank { "—" },
                    statusSecondary = if (state.monitorRunning) "running" else "stopped"
                ) {
                    PixelText(
                        text = "${state.remainingMinutes} min left",
                        style = RetroTheme.typography.numeral
                    )
                    PixelText("used ${state.usedMinutes} of ${state.totalAllowanceMinutes}")
                    if (state.bonusMinutes > 0) {
                        PixelText("${state.budgetMinutes} free + ${state.bonusMinutes} earned")
                    }
                    SegmentedProgress(
                        progress = if (state.totalAllowanceMinutes > 0) {
                            state.usedMinutes.toFloat() / state.totalAllowanceMinutes
                        } else 0f
                    )
                    if (state.perApp.isNotEmpty()) {
                        state.perApp.forEach { (pkg, minutes) ->
                            PixelText("$minutes min  ${pkg.takeLast(28)}")
                        }
                    }
                }

                // ---- Permissions ----
                // Always present, granted or not — see PermissionsScreen for
                // why this isn't a disappearing warning banner.
                RetroWindow(
                    title = "Permissions",
                    statusText = if (granted && overlayGranted) {
                        "ready"
                    } else {
                        "action needed"
                    }
                ) {
                    if (!granted || !overlayGranted) {
                        BodyText(
                            buildString {
                                if (!granted) append("Usage access is missing. ")
                                if (!overlayGranted) append("Draw over apps is missing. ")
                                append("The Pass can't work until both are on.")
                            }
                        )
                    } else {
                        BodyText("Usage access and draw-over-apps are both on.")
                    }
                    RetroButton(
                        text = "Manage permissions",
                        primary = !granted || !overlayGranted,
                        onClick = onOpenPermissions
                    )
                }

                // ---- Monitor control ----
                RetroWindow(title = "Monitor") {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        RetroButton(
                            text = "Start",
                            primary = true,
                            enabled = granted,
                            onClick = { viewModel.startMonitor() }
                        )
                        RetroButton(text = "Stop", onClick = { viewModel.stopMonitor() })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        RetroButton(text = "Poll now", onClick = { viewModel.pollNow() })
                        RetroButton(text = "Reset day", onClick = { viewModel.resetToday() })
                    }
                    PixelText("foreground: ${foreground?.takeLast(30) ?: "—"}")
                }

                // ---- Budget ----
                RetroWindow(
                    title = "Budget",
                    statusText = if (state.mode == BudgetMode.SHARED) {
                        "${state.budgetMinutes} min shared"
                    } else {
                        "per app"
                    }
                ) {
                    if (state.mode == BudgetMode.PER_APP) {
                        state.appBudgets.forEach { app ->
                            PixelText(
                                "${app.remainingMinutes} / ${app.allowanceMinutes} min  " +
                                    app.packageName.takeLast(24)
                            )
                        }
                    } else {
                        BodyText("Quick presets for testing:")
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(1, 5, 30).forEach { minutes ->
                                RetroButton(
                                    text = "$minutes min",
                                    primary = state.budgetMinutes == minutes,
                                    onClick = { viewModel.setBudget(minutes) }
                                )
                            }
                        }
                    }
                    RetroButton(text = "Budget settings", onClick = onOpenBudget)
                }

                // ---- Watched apps ----
                RetroWindow(
                    title = "Watched apps",
                    statusText = "${apps.count { it.watched }} selected"
                ) {
                    BodyText("Tick the apps whose time should count against the budget.")
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Column(Modifier.fillMaxWidth()) {
                            apps.forEach { app ->
                                RetroCheckbox(
                                    checked = app.watched,
                                    onCheckedChange = { viewModel.toggleWatched(app.packageName) },
                                    label = app.label
                                )
                            }
                        }
                    }
                }

                // ---- The Pass ----
                RetroWindow(
                    title = "The Pass",
                    statusText = if (state.isSpent) "expired" else "active"
                ) {
                    BodyText(
                        if (state.isSpent) {
                            "Time's up for today. Writing an essay earns more."
                        } else {
                            "You can bank a pass now to have it ready later."
                        }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        RetroButton(
                            text = "Write an essay",
                            primary = state.isSpent,
                            onClick = onWriteEssay
                        )
                        RetroButton(text = "Past essays", onClick = onOpenHistory)
                    }
                }

                RetroWindow(title = "Dev") {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        RetroButton(text = "Permissions", onClick = onOpenPermissions)
                        RetroButton(text = "Gallery", onClick = onOpenGallery)
                    }
                }

                Box(Modifier.height(Dimens.ContentPadding))
            }

            Taskbar(
                timeRemaining = "${state.remainingMinutes} min",
                onMenuClick = { }
            )
        }
    }
}
