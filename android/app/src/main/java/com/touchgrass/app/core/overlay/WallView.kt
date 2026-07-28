package com.touchgrass.app.core.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/**
 * The wall, built from plain Android Views.
 *
 * ⚠️ WHY NOT COMPOSE, WHEN THE REST OF THE APP IS COMPOSE:
 *
 * A ComposeView inside a WindowManager window added by a Service has to be
 * hand-wired with a LifecycleOwner, a ViewModelStoreOwner, a
 * SavedStateRegistryOwner and a correctly-hosted Recomposer. Miss any of it
 * and it throws — taking the whole app down, since this runs on the main
 * thread from a Service with no Activity to absorb the failure.
 *
 * That is a lot of fragility to accept for a static dialog with a title, one
 * sentence and two buttons. Plain Views have none of those requirements and
 * cannot fail this way.
 *
 * The retro look is reproduced directly (design_theme.md §4.1, §6.1): the
 * same #C0C0C0 face, #000080 title bar, and hard bevel edges. Colours are
 * duplicated here rather than read from the Compose theme, because the
 * Compose theme is only available inside a composition.
 */
object WallView {

    // design_theme.md §4.1 — chrome tokens.
    private const val SURFACE = 0xFFC0C0C0.toInt()
    private const val SURFACE_LIGHT = 0xFFDFDFDF.toInt()
    private const val SURFACE_WHITE = 0xFFFFFFFF.toInt()
    private const val SURFACE_SHADOW = 0xFF808080.toInt()
    private const val SURFACE_BLACK = 0xFF0A0A0A.toInt()
    private const val TITLE_ACTIVE = 0xFF000080.toInt()
    private const val PAPER_CREAM = 0xFFFDF3D3.toInt()
    private const val DIM = 0xB8000000.toInt()

    fun create(
        context: Context,
        message: String,
        primaryLabel: String,
        secondaryLabel: String,
        onPrimary: () -> Unit,
        onSecondary: () -> Unit
    ): FrameLayout {
        val root = FrameLayout(context).apply {
            setBackgroundColor(DIM)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = raisedBevel(PAPER_CREAM)
            val pad = context.dp(4)
            setPadding(pad, pad, pad, pad)
            layoutParams = FrameLayout.LayoutParams(
                context.dp(320),
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        }

        card.addView(titleBar(context, "TouchGrass"))
        card.addView(messageView(context, message))
        card.addView(buttonRow(context, primaryLabel, secondaryLabel, onPrimary, onSecondary))

        root.addView(card)
        return root
    }

    private fun titleBar(context: Context, title: String) = TextView(context).apply {
        text = title
        setTextColor(Color.WHITE)
        setTypeface(Typeface.MONOSPACE, Typeface.BOLD)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        setBackgroundColor(TITLE_ACTIVE)
        gravity = Gravity.CENTER_VERTICAL
        val pad = context.dp(8)
        setPadding(pad, pad, pad, pad)
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    private fun messageView(context: Context, message: String) = TextView(context).apply {
        text = message
        setTextColor(SURFACE_BLACK)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        val pad = context.dp(20)
        setPadding(pad, pad, pad, pad)
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    private fun buttonRow(
        context: Context,
        primaryLabel: String,
        secondaryLabel: String,
        onPrimary: () -> Unit,
        onSecondary: () -> Unit
    ) = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.END
        val pad = context.dp(12)
        setPadding(pad, 0, pad, pad)
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        addView(retroButton(context, secondaryLabel, onSecondary))
        addView(View(context).apply {
            layoutParams = LinearLayout.LayoutParams(context.dp(8), 1)
        })
        addView(retroButton(context, primaryLabel.uppercase(), onPrimary))
    }

    private fun retroButton(
        context: Context,
        label: String,
        onClick: () -> Unit
    ) = Button(context).apply {
        text = label
        isAllCaps = false
        setTextColor(SURFACE_BLACK)
        setTypeface(Typeface.MONOSPACE, Typeface.NORMAL)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        background = raisedBevel(SURFACE)
        // §9: 48dp minimum touch target, no exceptions.
        minHeight = context.dp(48)
        minWidth = context.dp(96)
        val padH = context.dp(14)
        setPadding(padH, 0, padH, 0)
        stateListAnimator = null
        setOnClickListener { onClick() }
    }

    /**
     * The §6.1 raised bevel, approximated with a layered drawable: light on
     * the outside, dark border, solid face. Not the exact four-line recipe —
     * that needs a custom Drawable — but the same hard-edged, zero-radius,
     * shadowless read.
     */
    private fun raisedBevel(face: Int): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(face)
        cornerRadius = 0f
        setStroke(2, SURFACE_BLACK)
    }

    private fun Context.dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
