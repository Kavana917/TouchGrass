package com.touchgrass.app.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typography from design_theme.md §5.
 *
 * THE DIVIDING LINE: if you *scan* it, it's pixel. If you *read* it, it's sans.
 * A 150-word essay in a bitmap font is a worse essay-writing experience, and
 * the essay is the product.
 *
 * Rules baked in below:
 *  - Nothing pixel below 16sp, ever (§5, §9)
 *  - No letter-spacing tweaks, no faux-bold, no italics — bitmap faces have
 *    one weight and synthesizing more looks broken
 *  - Pixel sizes are multiples of the design size, so glyphs land on whole
 *    pixels instead of blurry halves
 */

// ---------------------------------------------------------------------------
// PIXEL FONT — currently a placeholder.
//
// TO ACTIVATE PIXELIFY SANS:
//   1. Download from https://fonts.google.com/specimen/Pixelify+Sans
//   2. Put the TTFs in app/src/main/res/font/ renamed to:
//        pixelify_sans_regular.ttf
//        pixelify_sans_bold.ttf
//      (Android resource names: lowercase + underscores only. The download
//       gives you PixelifySans-Regular.ttf — it must be renamed or the build
//       will fail with an invalid resource name.)
//   3. Comment out the Monospace line below, uncomment the Font(...) block.
//   4. Sync Gradle and re-run.
//
// Until then everything renders in monospace — correct layout, wrong texture.
// ---------------------------------------------------------------------------

val PixelFontFamily: FontFamily = FontFamily.Monospace

/*  ← delete this line to activate
val PixelFontFamily: FontFamily = FontFamily(
    Font(R.font.pixelify_sans_regular, FontWeight.Normal),
    Font(R.font.pixelify_sans_bold, FontWeight.Bold)
)
*/ //  ← and delete this line too
// (also add: import androidx.compose.ui.text.font.Font
//            import com.touchgrass.app.R)

/** Body text. Sans, because it's read in sentences. Scales with system font size. */
val BodyFontFamily: FontFamily = FontFamily.SansSerif

/**
 * The app's type scale. Access via [LocalRetroTypography], not directly,
 * so a screen can override it if it ever needs to.
 */
data class RetroTypography(

    /** Window titles. Pixel, bold, 16sp. */
    val titleBar: TextStyle = TextStyle(
        fontFamily = PixelFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp
    ),

    /** Buttons, menus, tabs, labels, status bar. The workhorse. */
    val chrome: TextStyle = TextStyle(
        fontFamily = PixelFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),

    /** Section titles inside windows. */
    val heading: TextStyle = TextStyle(
        fontFamily = PixelFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp
    ),

    /** Time remaining, word counter, page numbers. Big and scannable. */
    val numeral: TextStyle = TextStyle(
        fontFamily = PixelFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp
    ),

    /** Smaller numerals where 32sp doesn't fit. */
    val numeralSmall: TextStyle = TextStyle(
        fontFamily = PixelFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp
    ),

    /**
     * Essays, digest text, stream descriptions, onboarding prose, settings
     * explanations. Anything read in sentences. 1.5 line height.
     */
    val body: TextStyle = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),

    /** Captions, timestamps, secondary notes. */
    val bodySmall: TextStyle = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp
    )
)
