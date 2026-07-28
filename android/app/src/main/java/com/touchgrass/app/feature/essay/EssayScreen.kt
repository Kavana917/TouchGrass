package com.touchgrass.app.feature.essay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.touchgrass.app.ui.components.BevelStyle
import com.touchgrass.app.ui.components.BodyText
import com.touchgrass.app.ui.components.PixelText
import com.touchgrass.app.ui.components.RetroButton
import com.touchgrass.app.ui.components.RetroDialog
import com.touchgrass.app.ui.components.RetroWindow
import com.touchgrass.app.ui.components.Wallpaper
import com.touchgrass.app.ui.components.bevel
import com.touchgrass.app.ui.theme.Dimens
import com.touchgrass.app.ui.theme.RetroTheme

/**
 * The essay screen — Notepad, essentially (design_theme.md §10).
 *
 * §8 makes this one of the three surfaces where the theme steps back: the
 * window frame stays retro, but the writing area is a plain sunken white
 * field with 16sp sans and generous line height. Someone is hand-typing 150
 * words under mild frustration; nothing inside that rectangle may add
 * friction. That's why the paper is not styled and the body font is not
 * pixel.
 */
@Composable
fun EssayScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EssayViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Wallpaper(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimens.ItemSpacing),
            verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)
        ) {
            RetroWindow(
                title = "Untitled — Essay",
                menuItems = listOf("File", "Edit", "View"),
                statusText = "words: ${state.wordCount} / ${state.requiredWords}",
                statusSecondary = if (state.canSubmit) "ready" else "writing"
            ) {
                // ---- The prompt ----
                PixelText(
                    text = state.word.uppercase(),
                    style = RetroTheme.typography.numeral
                )

                if (state.rerollsLeft > 0 && state.text.isEmpty()) {
                    RetroButton(
                        text = "Different word (${state.rerollsLeft} left)",
                        enabled = state.canReroll,
                        onClick = { viewModel.reroll() }
                    )
                } else if (state.text.isEmpty()) {
                    PixelText(
                        text = "No swaps left — this is the one.",
                        color = RetroTheme.colors.surfaceShadow
                    )
                }

                // Human voice, not machine voice (§11). The last sentence is
                // load-bearing: it removes performance anxiety, which is the
                // usual reason someone abandons the essay and uninstalls.
                BodyText(
                    "Write ${state.requiredWords} words. Anything you like — a " +
                        "memory, a story, why you hate the word, what it makes " +
                        "you think of. Nobody reads this but you."
                )

                // ---- The paper ----
                EssayPaper(
                    value = state.text,
                    onValueChange = viewModel::onTextChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                )

                if (state.notice != null) {
                    PixelText(
                        text = state.notice!!,
                        color = RetroTheme.colors.bodyText
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        Dimens.ItemSpacing,
                        Alignment.End
                    )
                ) {
                    RetroButton(text = "Cancel", onClick = onDone)
                    RetroButton(
                        text = if (state.canSubmit) "Submit" else "${state.remainingWords} to go",
                        primary = true,
                        enabled = state.canSubmit,
                        onClick = { viewModel.submit() }
                    )
                }
            }
        }

        // ---- Pass issued ----
        state.grantedMinutes?.let { minutes ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                RetroDialog(
                    title = "TouchGrass",
                    // Machine voice for the state change (§11): a statement of
                    // fact, no congratulation, no judgement either way.
                    message = "Essay saved. $minutes minutes added.",
                    primaryLabel = "OK",
                    onPrimary = onDone
                )
            }
        }
    }
}

/**
 * The writing surface.
 *
 * TWO ANTI-CHEAT MEASURES LIVE HERE (app_plan.md §2.5):
 *
 *  1. [NoTextToolbar] removes the selection popup entirely, so there is no
 *     Paste button to press.
 *  2. Suggestions and autocorrect are off, so the keyboard won't write the
 *     essay for you.
 *
 * The real backstop is bulk-insert detection in TypingGuard, which catches
 * paste routes this UI can't see — a keyboard's own clipboard, autofill, an
 * accessibility tool. Disabling the menu is the polite layer; the length
 * check is the one that actually holds.
 */
@Composable
private fun EssayPaper(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalTextToolbar provides NoTextToolbar) {
        Box(
            modifier = modifier
                .heightIn(min = 220.dp)
                .bevel(BevelStyle.SUNKEN)
                .padding(Dimens.FieldPadding)
                .verticalScroll(rememberScrollState())
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = RetroTheme.typography.body.copy(
                    color = RetroTheme.colors.bodyText
                ),
                cursorBrush = SolidColor(RetroTheme.colors.bodyText),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    autoCorrect = false,
                    keyboardType = KeyboardType.Text
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** A text toolbar that never appears — no Cut, Copy, Paste or Select All. */
private object NoTextToolbar : TextToolbar {
    override val status: TextToolbarStatus = TextToolbarStatus.Hidden

    override fun hide() = Unit

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) = Unit
}
