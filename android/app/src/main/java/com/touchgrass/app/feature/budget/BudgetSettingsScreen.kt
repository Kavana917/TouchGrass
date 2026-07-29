package com.touchgrass.app.feature.budget

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.touchgrass.app.core.data.settings.BudgetMode
import com.touchgrass.app.ui.components.BodyText
import com.touchgrass.app.ui.components.GrooveSeparator
import com.touchgrass.app.ui.components.PixelText
import com.touchgrass.app.ui.components.RetroButton
import com.touchgrass.app.ui.components.RetroRadio
import com.touchgrass.app.ui.components.RetroWindow
import com.touchgrass.app.ui.components.Wallpaper
import com.touchgrass.app.ui.theme.Dimens
import com.touchgrass.app.ui.theme.RetroTheme

/**
 * Budget settings — Control Panel, in the desktop metaphor (§10).
 *
 * Covers the two things that decide how hard the app is on you: how much
 * time you get, and whether that time is one pool or one per app.
 */
@Composable
fun BudgetSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BudgetSettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Wallpaper(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Dimens.ItemSpacing),
            verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)
        ) {

            // ---- Mode ----
            RetroWindow(
                title = "How time is shared",
                statusText = if (state.mode == BudgetMode.SHARED) "one pool" else "per app"
            ) {
                RetroRadio(
                    selected = state.mode == BudgetMode.SHARED,
                    onSelect = { viewModel.setMode(BudgetMode.SHARED) },
                    label = "One shared budget"
                )
                BodyText(
                    "All watched apps draw from the same pool. Hopping from " +
                        "Instagram to TikTok to Reddit gives you the same " +
                        "total time, not three times as much.",
                    style = RetroTheme.typography.bodySmall
                )

                GrooveSeparator()

                RetroRadio(
                    selected = state.mode == BudgetMode.PER_APP,
                    onSelect = { viewModel.setMode(BudgetMode.PER_APP) },
                    label = "A budget per app"
                )
                BodyText(
                    "Each app gets its own limit. More precise — but running " +
                        "out in one app no longer stops you opening another, " +
                        "so app-hopping does add up.",
                    style = RetroTheme.typography.bodySmall
                )
            }

            // ---- Daily budget ----
            if (state.mode == BudgetMode.SHARED) {
                RetroWindow(
                    title = "Daily budget",
                    statusText = "${state.dailyBudget} min"
                ) {
                    MinuteStepper(
                        value = state.dailyBudget,
                        onAdjust = { viewModel.adjustDailyBudget(it) }
                    )
                    PresetRow(
                        presets = listOf(1, 5, 15, 30, 45, 60, 90, 120),
                        selected = state.dailyBudget,
                        onSelect = { viewModel.setDailyBudget(it) }
                    )
                    BodyText(
                        "30 minutes is the starting suggestion, not a " +
                            "recommendation — the right number is the one " +
                            "you actually notice.",
                        style = RetroTheme.typography.bodySmall
                    )
                    state.pendingBudget?.let { pending ->
                        BodyText(
                            "Raises to $pending min at your next reset.",
                            style = RetroTheme.typography.bodySmall
                        )
                    }
                }
            } else {
                RetroWindow(
                    title = "Per-app limits",
                    statusText = "${state.apps.size} app${if (state.apps.size == 1) "" else "s"}"
                ) {
                    if (state.apps.isEmpty()) {
                        BodyText("No watched apps yet. Pick some first.")
                    } else {
                        state.apps.forEachIndexed { index, app ->
                            if (index > 0) GrooveSeparator()
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    PixelText(
                                        text = app.label,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f)
                                    )
                                    PixelText(
                                        text = "${app.minutes} min",
                                        style = RetroTheme.typography.numeralSmall
                                    )
                                }
                                if (!app.explicit) {
                                    PixelText(
                                        text = "using the default",
                                        color = RetroTheme.colors.surfaceShadow
                                    )
                                }
                                MinuteStepper(
                                    value = app.minutes,
                                    onAdjust = { viewModel.adjustAppBudget(app.packageName, it) }
                                )
                                PresetRow(
                                    presets = listOf(5, 15, 30, 60),
                                    selected = app.minutes,
                                    onSelect = { viewModel.setAppBudget(app.packageName, it) }
                                )
                            }
                        }
                    }
                }
            }

            // ---- The toll ----
            RetroWindow(
                title = "The toll",
                statusText = "${state.essayWords} words → ${state.passMinutes} min"
            ) {
                PixelText("Words per essay")
                MinuteStepper(
                    value = state.essayWords,
                    step = 25,
                    onAdjust = { viewModel.adjustEssayWords(it) }
                )

                GrooveSeparator()

                PixelText("Minutes each essay buys")
                MinuteStepper(
                    value = state.passMinutes,
                    onAdjust = { viewModel.adjustPassMinutes(it) }
                )
                BodyText(
                    "Keep this below the daily budget. A pass should be a " +
                        "top-up, not a way to start the day over.",
                    style = RetroTheme.typography.bodySmall
                )
            }

            // ---- Reset hour ----
            RetroWindow(
                title = "Day starts at",
                statusText = "%02d:00".format(state.resetHour)
            ) {
                PresetRow(
                    presets = listOf(0, 3, 4, 5, 6),
                    selected = state.resetHour,
                    onSelect = { viewModel.setResetHour(it) },
                    format = { "%02d:00".format(it) }
                )
                BodyText(
                    "4am rather than midnight, so a late-night session " +
                        "doesn't get handed a fresh budget two minutes later.",
                    style = RetroTheme.typography.bodySmall
                )
            }

            RetroWindow(title = "") {
                RetroButton(text = "Back", onClick = onBack)
            }
        }
    }
}

/** − / + stepper for any minute-ish value. */
@Composable
private fun MinuteStepper(
    value: Int,
    onAdjust: (Int) -> Unit,
    modifier: Modifier = Modifier,
    step: Int = 5
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RetroButton(text = "−$step", onClick = { onAdjust(-step) })
        RetroButton(text = "−1", onClick = { onAdjust(-1) })
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            PixelText(
                text = value.toString(),
                style = RetroTheme.typography.numeralSmall
            )
        }
        RetroButton(text = "+1", onClick = { onAdjust(1) })
        RetroButton(text = "+$step", onClick = { onAdjust(step) })
    }
}

@Composable
private fun PresetRow(
    presets: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    format: (Int) -> String = { "$it" }
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        presets.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { preset ->
                    RetroButton(
                        text = format(preset),
                        primary = preset == selected,
                        onClick = { onSelect(preset) }
                    )
                }
            }
        }
    }
}
