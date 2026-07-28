package com.touchgrass.app.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Fixed measurements from design_theme.md §6.2 and §9.
 *
 * Sizes live here rather than scattered through components so the whole
 * system can be re-proportioned from one place.
 */
object Dimens {

    /**
     * Thickness of a single bevel line.
     *
     * design_theme.md says "1px hard lines". Taken literally on a modern
     * 3x-density phone that's a third of a dp — a hairline, essentially
     * invisible. The theme is pixel art *scaled up*, so we render each bevel
     * line at 2.dp, which reads as a crisp chunky edge at phone scale.
     *
     * Tune here if the chrome feels too heavy or too delicate.
     */
    val BevelLine: Dp = 2.dp

    /** Total inset of a double bevel (outer + inner). */
    val BevelTotal: Dp = BevelLine * 2

    // ---- Component heights (§6.2) ----
    val TitleBarHeight: Dp = 32.dp
    val MenuBarHeight: Dp = 28.dp
    val StatusBarHeight: Dp = 24.dp
    val ContextMenuRowHeight: Dp = 40.dp
    val TaskbarHeight: Dp = 40.dp
    val ListRowHeight: Dp = 40.dp

    /**
     * Minimum interactive size (§9). Non-negotiable.
     * Visual chrome may render smaller — pad the hit area, not the pixels.
     */
    val MinTouchTarget: Dp = 48.dp

    // ---- Spacing ----
    /** Inset between a window frame and its content. */
    val WindowPadding: Dp = 4.dp
    val FieldPadding: Dp = 12.dp
    val ContentPadding: Dp = 16.dp
    val ItemSpacing: Dp = 8.dp

    // ---- Icons (§6.2 form 10) ----
    /** Authoring grid. Scale only at integer multiples: 32 → 64 → 96. */
    val IconBase: Dp = 32.dp
    val IconLarge: Dp = 64.dp

    // ---- Misc ----
    val ScrollbarWidth: Dp = 20.dp
    val CheckboxSize: Dp = 24.dp
    val ProgressBlockWidth: Dp = 12.dp
    val ProgressHeight: Dp = 24.dp
}
