package com.touchgrass.app.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.touchgrass.app.BuildConfig
import com.touchgrass.app.core.data.settings.BudgetMode
import com.touchgrass.app.core.usage.MonitorHealth
import com.touchgrass.app.ui.components.BodyText
import com.touchgrass.app.ui.components.GrooveSeparator
import com.touchgrass.app.ui.components.PixelText
import com.touchgrass.app.ui.components.RetroButton
import com.touchgrass.app.ui.components.RetroCheckbox
import com.touchgrass.app.ui.components.RetroDialog
import com.touchgrass.app.ui.components.RetroWindow
import com.touchgrass.app.ui.components.SegmentedProgress
import com.touchgrass.app.ui.components.Taskbar
import com.touchgrass.app.ui.components.Wallpaper
import com.touchgrass.app.ui.theme.Dimens
import com.touchgrass.app.ui.theme.RetroTheme

/**
 * The Pass status screen — the app home (Phase 5).
 *
 * Replaces the Phase 2 debug scaffolding with the surface a real user lives
 * on: time left, the toll, and the settings that keep the monitor alive.
 */
@Composable
fun HomeScreen(
    onWriteEssay: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenWatchedApps: () -> Unit,
    onOpenBudget: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenOemHelp: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenDebug: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val panicLeft by viewModel.panicUnlocksLeft.collectAsStateWithLifecycle()
    val panicGranted by viewModel.panicGranted.collectAsStateWithLifecycle()
    val budget = state.budget

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onResume()
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
                state.healthIssue?.let { issue ->
                    HealthBanner(
                        issue = issue,
                        onRestart = { viewModel.restartMonitor() },
                        onOpenPermissions = onOpenPermissions,
                        onOpenOemHelp = onOpenOemHelp
                    )
                }

                RetroWindow(
                    title = "Today",
                    statusText = budget.dayKey.ifBlank { "—" },
                    statusSecondary = if (budget.monitorRunning) "monitor on" else "monitor off"
                ) {
                    PixelText(
                        text = "${budget.remainingMinutes} min left",
                        style = RetroTheme.typography.numeral
                    )
                    if (budget.mode == BudgetMode.SHARED) {
                        PixelText("used ${budget.usedMinutes} of ${budget.totalAllowanceMinutes}")
                        if (budget.bonusMinutes > 0) {
                            PixelText("${budget.budgetMinutes} free + ${budget.bonusMinutes} earned")
                        }
                        SegmentedProgress(
                            progress = if (budget.totalAllowanceMinutes > 0) {
                                budget.usedMinutes.toFloat() / budget.totalAllowanceMinutes
                            } else 0f
                        )
                    } else {
                        PixelText("per-app limits")
                        budget.appBudgets.forEach { app ->
                            PixelText(
                                "${app.remainingMinutes} / ${app.allowanceMinutes} min  " +
                                    app.packageName.substringAfterLast('.')
                            )
                        }
                    }
                    if (!state.permissionsReady) {
                        GrooveSeparator()
                        BodyText(
                            "Permissions are missing — the Pass can't enforce until they're on.",
                            style = RetroTheme.typography.bodySmall
                        )
                        RetroButton(
                            text = "Fix permissions",
                            primary = true,
                            onClick = onOpenPermissions
                        )
                    }
                }

                RetroWindow(
                    title = "The Pass",
                    statusText = if (budget.isSpent) "expired" else "active"
                ) {
                    BodyText(
                        if (budget.isSpent) {
                            "Time's up for today. Writing an essay earns more."
                        } else {
                            "You can bank a pass now to have it ready later."
                        }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        RetroButton(
                            text = "Write an essay",
                            primary = budget.isSpent,
                            onClick = onWriteEssay
                        )
                        RetroButton(text = "Past essays", onClick = onOpenHistory)
                    }
                }

                RetroWindow(
                    title = "Emergency",
                    statusText = "$panicLeft left this month"
                ) {
                    BodyText(
                        "If you genuinely need in right now, take one. No essay, no questions."
                    )
                    RetroButton(
                        text = "Unlock now",
                        enabled = panicLeft > 0,
                        onClick = { viewModel.usePanicUnlock() }
                    )
                }

                RetroWindow(
                    title = "Settings",
                    statusText = "${state.watchedCount} watched apps"
                ) {
                    RetroButton(text = "Watched apps", onClick = onOpenWatchedApps)
                    RetroButton(text = "Budget & toll", onClick = onOpenBudget)
                    RetroButton(
                        text = "Permissions",
                        primary = !state.permissionsReady,
                        onClick = onOpenPermissions
                    )
                    RetroButton(text = "Phone survival", onClick = onOpenOemHelp)
                    RetroButton(text = "Privacy", onClick = onOpenPrivacy)
                    GrooveSeparator()
                    RetroCheckbox(
                        checked = state.notificationGraceOn,
                        onCheckedChange = { viewModel.setNotificationGraceEnabled(it) },
                        label = "60s grace when opening a watched app"
                    )
                    BodyText(
                        "When time is spent, you get one minute after opening " +
                            "Instagram before the wall appears — enough to read a DM.",
                        style = RetroTheme.typography.bodySmall
                    )
                    state.pendingBudget?.let { pending ->
                        GrooveSeparator()
                        BodyText(
                            "Budget rises to $pending min at your next reset.",
                            style = RetroTheme.typography.bodySmall
                        )
                    }
                }

                if (BuildConfig.DEBUG) {
                    RetroWindow(title = "Developer") {
                        RetroButton(text = "Debug tools", onClick = onOpenDebug)
                    }
                }
            }

            Taskbar(
                timeRemaining = "${budget.remainingMinutes} min",
                onMenuClick = { }
            )
        }

        panicGranted?.let { minutes ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                RetroDialog(
                    title = "TouchGrass",
                    message = "$minutes minutes added.",
                    primaryLabel = "OK",
                    onPrimary = { viewModel.dismissPanicResult() }
                )
            }
        }
    }
}

@Composable
private fun HealthBanner(
    issue: MonitorHealth.Issue,
    onRestart: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenOemHelp: () -> Unit
) {
    RetroWindow(
        title = issue.title,
        statusText = "action needed"
    ) {
        BodyText(issue.detail)
        when (issue.kind) {
            MonitorHealth.Issue.Kind.MONITOR_STOPPED ->
                RetroButton(text = "Restart monitor", primary = true, onClick = onRestart)
            MonitorHealth.Issue.Kind.PERMISSIONS ->
                RetroButton(text = "Open permissions", primary = true, onClick = onOpenPermissions)
            MonitorHealth.Issue.Kind.OEM ->
                RetroButton(text = "Phone survival", primary = true, onClick = onOpenOemHelp)
        }
    }
}
