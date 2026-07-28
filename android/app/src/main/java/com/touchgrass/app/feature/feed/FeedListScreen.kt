package com.touchgrass.app.feature.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.touchgrass.app.core.feed.Stream
import com.touchgrass.app.ui.components.BodyText
import com.touchgrass.app.ui.components.GrooveSeparator
import com.touchgrass.app.ui.components.PixelText
import com.touchgrass.app.ui.components.RetroButton
import com.touchgrass.app.ui.components.RetroWindow
import com.touchgrass.app.ui.components.SunkenTextField
import com.touchgrass.app.ui.components.Wallpaper
import com.touchgrass.app.ui.theme.Dimens
import com.touchgrass.app.ui.theme.RetroTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * The Live Feed — somewhere else to put your attention.
 *
 * app_plan.md §3: the urge to scroll is usually just an urge to do something
 * with your eyes for a minute. This answers that with a real place,
 * happening right now, that has no next video and no reason to keep you.
 *
 * ⚠️ THIS COSTS NO PASS, EVER (§3.5). It is the door next to the wall. A
 * wall with no door is a wall people uninstall, and rationing the
 * alternative would make the whole app a punishment box.
 */
@Composable
fun FeedListScreen(
    onOpenStream: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FeedViewModel = hiltViewModel()
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
            RetroWindow(
                title = "Live Feed",
                statusText = "${state.visible.size} places",
                statusSecondary = "free"
            ) {
                BodyText(
                    "Somewhere real, happening right now. No feed, no next " +
                        "video, and it never costs you a pass."
                )

                // Category chips overflow a phone width, so they scroll.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    RetroButton(
                        text = "All",
                        primary = state.categoryFilter == null,
                        onClick = { viewModel.setCategoryFilter(null) }
                    )
                    state.categories.forEach { category ->
                        RetroButton(
                            text = category.label,
                            primary = state.categoryFilter == category,
                            onClick = { viewModel.setCategoryFilter(category) }
                        )
                    }
                }
            }

            // ---- Add your own ----
            AddStreamPanel(viewModel)

            if (state.streams.isEmpty()) {
                RetroWindow(title = "No streams") {
                    BodyText("The stream registry didn't load.")
                }
            }

            state.visible.forEach { stream ->
                StreamCard(
                    stream = stream,
                    favourite = stream.id in state.favourites,
                    onOpen = { onOpenStream(stream.id) },
                    onToggleFavourite = { viewModel.toggleFavourite(stream.id) }
                )
            }

            RetroWindow(title = "") {
                RetroButton(text = "Back", onClick = onBack)
            }
        }
    }
}

/**
 * Paste a link, name it, done.
 *
 * This is the answer to a problem I can't solve from the build side: I have
 * no way to check a stream is alive, and a pinned video ID rots within days.
 * The person holding the phone can verify instantly — so give them the tool
 * rather than shipping guesses.
 */
@Composable
private fun AddStreamPanel(viewModel: FeedViewModel) {
    var title by remember { mutableStateOf("") }
    var place by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val error by viewModel.addError.collectAsStateWithLifecycle()
    val succeeded by viewModel.addSucceeded.collectAsStateWithLifecycle()

    LaunchedEffect(succeeded) {
        if (succeeded) {
            title = ""; place = ""; link = ""
            expanded = false
            viewModel.clearAddResult()
        }
    }

    RetroWindow(
        title = "Add a stream",
        statusText = if (expanded) "paste a link" else "tap to open"
    ) {
        if (!expanded) {
            BodyText("Found a good one? Add it yourself.")
            RetroButton(text = "Add a stream", onClick = { expanded = true })
            return@RetroWindow
        }

        BodyText(
            "Paste a YouTube link — a channel link is best, because it keeps " +
                "working when the broadcaster restarts the stream. A direct " +
                ".m3u8 webcam URL works too."
        )

        SunkenTextField(
            value = title,
            onValueChange = { title = it },
            placeholder = "Name — e.g. Lofoten Harbour"
        )
        SunkenTextField(
            value = place,
            onValueChange = { place = it },
            placeholder = "Place — e.g. Reine, Norway"
        )
        SunkenTextField(
            value = link,
            onValueChange = { link = it },
            placeholder = "youtube.com/channel/UC…"
        )

        error?.let { message ->
            PixelText(text = message, color = RetroTheme.colors.accentRed)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            RetroButton(
                text = "Add",
                primary = true,
                onClick = { viewModel.addStream(title, place, link) }
            )
            RetroButton(
                text = "Cancel",
                onClick = {
                    expanded = false
                    viewModel.clearAddResult()
                }
            )
        }
    }
}

@Composable
private fun StreamCard(
    stream: Stream,
    favourite: Boolean,
    onOpen: () -> Unit,
    onToggleFavourite: () -> Unit
) {
    val localTime = remember(stream.timezone) { localTimeAt(stream.timezone) }

    RetroWindow(
        title = stream.title,
        statusText = stream.place.ifBlank { "—" },
        statusSecondary = localTime
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PixelText(
                text = stream.category.label,
                color = RetroTheme.colors.surfaceShadow,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
            if (!stream.hasAudio) {
                PixelText(text = "silent", color = RetroTheme.colors.surfaceShadow)
            }
        }

        if (stream.attribution != null) {
            BodyText(stream.attribution, style = RetroTheme.typography.bodySmall)
        }

        GrooveSeparator()

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            RetroButton(text = "Watch", primary = true, onClick = onOpen)
            RetroButton(
                text = if (favourite) "★ Saved" else "☆ Save",
                onClick = onToggleFavourite
            )
        }
    }
}

/** Local wall-clock time where the camera is — part of the "somewhere else" feeling. */
private fun localTimeAt(timezone: String?): String {
    if (timezone.isNullOrBlank()) return "—"
    return runCatching {
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        formatter.timeZone = TimeZone.getTimeZone(timezone)
        formatter.format(Date())
    }.getOrDefault("—")
}

