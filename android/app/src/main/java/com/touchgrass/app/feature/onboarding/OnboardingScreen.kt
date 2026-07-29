package com.touchgrass.app.feature.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.touchgrass.app.core.permissions.AppPermissions
import com.touchgrass.app.core.permissions.PermissionId
import com.touchgrass.app.ui.components.BodyText
import com.touchgrass.app.ui.components.GrooveSeparator
import com.touchgrass.app.ui.components.PixelText
import com.touchgrass.app.ui.components.RetroButton
import com.touchgrass.app.ui.components.RetroCheckbox
import com.touchgrass.app.ui.components.RetroWindow
import com.touchgrass.app.ui.components.Wallpaper
import com.touchgrass.app.ui.theme.Dimens
import com.touchgrass.app.ui.theme.RetroTheme

/**
 * Setup Wizard (design_theme.md §10) — `< Back` / `Next >` bottom-right,
 * one decision per pane.
 *
 * This is the highest-stakes screen in the app. TouchGrass asks for usage
 * access AND draw-over-other-apps, which together are the exact permission
 * profile of spyware (app_plan.md §6.2). Explanation quality here is the
 * difference between installs and uninstalls, so each permission gets its
 * own moment with a plain reason attached.
 */
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Permissions are granted on Settings screens with no callback, so
    // re-check every time we come back.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshPermissions()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { viewModel.refreshPermissions() }

    Wallpaper(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Dimens.ItemSpacing),
            verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)
        ) {
            RetroWindow(
                title = "TouchGrass Setup",
                statusText = "Step ${state.pane.ordinal + 1} of ${OnboardingPane.COUNT}"
            ) {
                when (state.pane) {
                    OnboardingPane.IDEA -> IdeaPane()
                    OnboardingPane.PICK_APPS -> PickAppsPane(state, viewModel)
                    OnboardingPane.SET_BUDGET -> BudgetPane(state, viewModel)
                    OnboardingPane.PERM_USAGE,
                    OnboardingPane.PERM_OVERLAY,
                    OnboardingPane.PERM_NOTIFICATIONS,
                    OnboardingPane.PERM_BATTERY -> SinglePermissionPane(
                        state = state,
                        permissionId = state.permissionFor(state.pane)!!,
                        onGrant = { id ->
                            if (id == PermissionId.NOTIFICATIONS &&
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                            ) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                AppPermissions.open(context, id)
                            }
                        }
                    )
                    OnboardingPane.PRIVACY -> PrivacyPane()
                    OnboardingPane.DONE -> DonePane()
                }

                GrooveSeparator()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        Dimens.ItemSpacing,
                        Alignment.End
                    )
                ) {
                    if (state.pane != OnboardingPane.IDEA) {
                        RetroButton(text = "< Back", onClick = { viewModel.back() })
                    }
                    when (state.pane) {
                        OnboardingPane.PRIVACY -> RetroButton(
                            text = "Finish",
                            primary = true,
                            onClick = { viewModel.finish(onFinished) }
                        )
                        OnboardingPane.DONE -> RetroButton(
                            text = "Done",
                            primary = true,
                            onClick = onFinished
                        )
                        else -> RetroButton(
                            text = "Next >",
                            primary = true,
                            enabled = when (state.pane) {
                                OnboardingPane.PICK_APPS -> state.canLeaveAppPicker
                                else -> true
                            },
                            onClick = { viewModel.next() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IdeaPane() {
    PixelText("The idea", style = RetroTheme.typography.heading)
    BodyText(
        "TouchGrass doesn't block anything. It charges a price."
    )
    BodyText(
        "You get a set amount of time in the apps you choose. When it runs " +
            "out, you can have more — but you have to write an essay by hand " +
            "first, on a random word, with no pasting."
    )
    BodyText(
        "Nothing is forbidden. It's just expensive enough that opening " +
            "Instagram without thinking stops happening, and only the times " +
            "you actually meant to survive."
    )
}

@Composable
private fun PickAppsPane(
    state: OnboardingState,
    viewModel: OnboardingViewModel
) {
    PixelText("Which apps?", style = RetroTheme.typography.heading)
    BodyText("Pick the ones you open without deciding to.")

    if (state.loadingApps) {
        BodyText("Loading…")
        return
    }

    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(max = 360.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Column(Modifier.fillMaxWidth()) {
            state.apps.forEach { app ->
                RetroCheckbox(
                    checked = app.selected,
                    onCheckedChange = { viewModel.toggleApp(app.packageName) },
                    label = app.label
                )
            }
        }
    }

    PixelText(
        text = "${state.selectedCount} selected",
        color = RetroTheme.colors.surfaceShadow
    )
}

@Composable
private fun BudgetPane(
    state: OnboardingState,
    viewModel: OnboardingViewModel
) {
    PixelText("How long a day?", style = RetroTheme.typography.heading)

    PixelText(
        text = "${state.budgetMinutes} min",
        style = RetroTheme.typography.numeral
    )

    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        RetroButton(text = "−5", onClick = { viewModel.adjustBudget(-5) })
        RetroButton(text = "−1", onClick = { viewModel.adjustBudget(-1) })
        RetroButton(text = "+1", onClick = { viewModel.adjustBudget(1) })
        RetroButton(text = "+5", onClick = { viewModel.adjustBudget(5) })
    }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(15, 30, 45, 60).forEach { preset ->
            RetroButton(
                text = "$preset",
                primary = state.budgetMinutes == preset,
                onClick = { viewModel.setBudget(preset) }
            )
        }
    }

    BodyText(
        "30 minutes is a starting point, not a recommendation. The right " +
            "number is the one you actually notice — you can change it any " +
            "time, though raising it waits until tomorrow."
    )
}

@Composable
private fun SinglePermissionPane(
    state: OnboardingState,
    permissionId: PermissionId,
    onGrant: (PermissionId) -> Unit
) {
    val permission = state.permissions.firstOrNull { it.id == permissionId }
    if (permission == null) {
        BodyText("Loading…")
        return
    }

    PixelText(permission.title, style = RetroTheme.typography.heading)
    BodyText(permission.reason)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PixelText(
            text = if (permission.granted) "✓ granted" else "not granted yet",
            color = if (permission.granted) {
                RetroTheme.colors.bodyText
            } else {
                RetroTheme.colors.surfaceShadow
            }
        )
    }
    if (!permission.granted) {
        PixelText(
            text = permission.ifDenied,
            color = RetroTheme.colors.surfaceShadow
        )
        RetroButton(
            text = "Grant",
            primary = permission.required,
            onClick = { onGrant(permission.id) }
        )
    }
}

@Composable
private fun PrivacyPane() {
    PixelText("Your writing", style = RetroTheme.typography.heading)
    BodyText("Your essays never leave this phone.")
    BodyText(
        "Not synced, not uploaded, not analysed, not read. Same for your " +
            "drawings and your usage history. There is no account and no " +
            "server holding any of it."
    )
    GrooveSeparator()
    BodyText(
        "Usage access lets TouchGrass see app names and how long they were " +
            "open. It cannot see anything that happens inside them — not " +
            "messages, not what you looked at, nothing.",
        style = RetroTheme.typography.bodySmall
    )
}

@Composable
private fun DonePane() {
    PixelText("Ready", style = RetroTheme.typography.heading)
    BodyText("The monitor is running. Go and use your phone normally.")
}
