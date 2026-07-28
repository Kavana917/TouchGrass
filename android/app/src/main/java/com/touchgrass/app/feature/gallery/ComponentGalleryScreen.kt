package com.touchgrass.app.feature.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.touchgrass.app.ui.components.BevelStyle
import com.touchgrass.app.ui.components.BodyText
import com.touchgrass.app.ui.components.ContextMenu
import com.touchgrass.app.ui.components.ContextMenuItem
import com.touchgrass.app.ui.components.GrooveSeparator
import com.touchgrass.app.ui.components.PixelIcon
import com.touchgrass.app.ui.components.PixelIcons
import com.touchgrass.app.ui.components.PixelText
import com.touchgrass.app.ui.components.RetroButton
import com.touchgrass.app.ui.components.RetroCheckbox
import com.touchgrass.app.ui.components.RetroDialog
import com.touchgrass.app.ui.components.RetroListRow
import com.touchgrass.app.ui.components.RetroListView
import com.touchgrass.app.ui.components.RetroRadio
import com.touchgrass.app.ui.components.RetroScrollbar
import com.touchgrass.app.ui.components.RetroWindow
import com.touchgrass.app.ui.components.SegmentedProgress
import com.touchgrass.app.ui.components.SunkenField
import com.touchgrass.app.ui.components.SunkenTextField
import com.touchgrass.app.ui.components.Taskbar
import com.touchgrass.app.ui.components.TitleBar
import com.touchgrass.app.ui.components.Wallpaper
import com.touchgrass.app.ui.components.bevel
import com.touchgrass.app.ui.theme.Dimens
import com.touchgrass.app.ui.theme.RetroTheme

/**
 * DEV-ONLY screen. Renders every component from design_theme.md §6 in every
 * state, so the design system can be reviewed in one place.
 *
 * This is Phase 1's deliverable and its test. Keep it — as components change
 * in later phases, this is where regressions show up first.
 */
@Composable
fun ComponentGalleryScreen(
    isNight: Boolean,
    onToggleNight: () -> Unit,
    modifier: Modifier = Modifier
) {
    var checked by remember { mutableStateOf(true) }
    var radioIndex by remember { mutableStateOf(0) }
    var fieldText by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }

    Wallpaper(modifier = modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(Dimens.ItemSpacing),
                verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)
            ) {

                // ---- 1. Window + title bar + menu bar + status bar ----
                RetroWindow(
                    title = "Component Gallery",
                    titleIcon = PixelIcons.Folder,
                    menuItems = listOf("File", "Edit", "View", "Help"),
                    statusText = "10 components",
                    statusSecondary = if (isNight) "night" else "day",
                    onClose = { }
                ) {
                    BodyText(
                        "Every form in the design system. Body copy is sans " +
                            "because you read it; everything else is pixel " +
                            "because you scan it."
                    )
                }

                // ---- 2. Buttons, all states ----
                Section("Buttons") {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        RetroButton(text = "OK", onClick = { }, primary = true)
                        RetroButton(text = "Cancel", onClick = { })
                        RetroButton(text = "Disabled", onClick = { }, enabled = false)
                    }
                    PixelText("Hold one — the bevel inverts and the label shifts.")
                }

                // ---- 3. Inactive title bar ----
                Section("Title bar — inactive") {
                    TitleBar(title = "Background Window", active = false)
                }

                // ---- 4. Fields ----
                Section("Sunken fields") {
                    SunkenTextField(
                        value = fieldText,
                        onValueChange = { fieldText = it },
                        placeholder = "Type here — sans, 16sp, generous line height"
                    )
                    SunkenField {
                        BodyText("A read-only sunken well.")
                    }
                }

                // ---- 5. List view ----
                Section("List view") {
                    RetroListView(
                        headers = listOf("Name", "Kind", "Size"),
                        weights = listOf(2f, 1f, 1f)
                    ) {
                        RetroListRow(listOf("essay_01", "Text", "1 KB"))
                        RetroListRow(listOf("lofoten", "Stream", "—"), selected = true)
                        RetroListRow(listOf("page_04", "Drawing", "8 KB"))
                    }
                }

                // ---- 6. Context menu ----
                Section("Context menu") {
                    ContextMenu(
                        groups = listOf(
                            listOf(
                                ContextMenuItem("Arrange Icons", hasSubmenu = true),
                                ContextMenuItem("Line up Icons")
                            ),
                            listOf(
                                ContextMenuItem("Paste"),
                                ContextMenuItem("Paste Shortcut", enabled = false)
                            ),
                            listOf(ContextMenuItem("New", hasSubmenu = true))
                        )
                    )
                }

                // ---- 7. Selectors ----
                Section("Checkbox & radio") {
                    RetroCheckbox(
                        checked = checked,
                        onCheckedChange = { checked = it },
                        label = "Clear Mode"
                    )
                    RetroCheckbox(
                        checked = false,
                        onCheckedChange = { },
                        label = "Disabled option",
                        enabled = false
                    )
                    GrooveSeparator()
                    listOf("Rivers", "Coasts", "Mountains").forEachIndexed { i, name ->
                        RetroRadio(
                            selected = radioIndex == i,
                            onSelect = { radioIndex = i },
                            label = name
                        )
                    }
                }

                // ---- 8. Progress ----
                Section("Segmented progress") {
                    SegmentedProgress(progress = 0.45f)
                    PixelText("Discrete blocks — never a smooth bar.")
                }

                // ---- 9. Pixel icons ----
                Section("Pixel icons") {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PixelIcon(PixelIcons.Folder, "Folder", Modifier.size(32.dp))
                        PixelIcon(PixelIcons.Clock, "Clock", Modifier.size(32.dp))
                        PixelIcon(PixelIcons.Heart, "Heart", Modifier.size(32.dp))
                        PixelIcon(PixelIcons.Grass, "Grass", Modifier.size(32.dp))
                        PixelIcon(PixelIcons.Grass, "Grass large", Modifier.size(64.dp))
                    }
                    PixelText("Drawn as rects — no bitmap, so no filtering.")
                }

                // ---- 10. Scrollbar ----
                Section("Scrollbar") {
                    Row(Modifier.height(140.dp)) {
                        Box(
                            Modifier
                                .weight(1f)
                                .bevel(BevelStyle.SUNKEN)
                        )
                        RetroScrollbar(progress = 0.35f, modifier = Modifier.height(140.dp))
                    }
                    PixelText("Always visible — it shows the surface has a bottom.")
                }

                // ---- 11. Dialog ----
                Section("Dialog") {
                    RetroButton(text = "Show dialog", onClick = { showDialog = true })
                }

                // ---- Night toggle ----
                Section("Day / night") {
                    RetroButton(
                        text = if (isNight) "Switch to day" else "Switch to night",
                        onClick = onToggleNight
                    )
                    BodyText(
                        "Only the wallpaper changes. Windows, buttons and text " +
                            "stay exactly the same at all hours."
                    )
                }

                Box(Modifier.height(Dimens.ContentPadding))
            }

            Taskbar(timeRemaining = "24 min", onMenuClick = { })
        }

        if (showDialog) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                RetroDialog(
                    title = "TouchGrass",
                    icon = PixelIcons.Clock,
                    message = "The pass has expired.",
                    primaryLabel = "Write an essay",
                    onPrimary = { showDialog = false },
                    secondaryLabel = "Not now",
                    onSecondary = { showDialog = false }
                )
            }
        }
    }
}

/** A labelled group inside the gallery. */
@Composable
private fun Section(
    title: String,
    content: @Composable () -> Unit
) {
    RetroWindow(title = title) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)
        ) {
            content()
        }
    }
}
