package com.touchgrass.app.core.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.touchgrass.app.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Puts the wall on screen, over whatever app the user is in.
 *
 * ⚠️ THE CENTRAL CONSTRAINT OF THIS PHASE:
 *
 * Since Android 10, apps cannot start an Activity from the background.
 * Calling startActivity() from the monitor service does not throw, does not
 * log, and does not show anything — it silently does nothing. Every
 * "my blocker stopped working on Android 10" bug report is this.
 *
 * The only supported way to put UI over another app is to add a
 * TYPE_APPLICATION_OVERLAY window through WindowManager. That's what this
 * class does.
 */
@Singleton
class WallOverlayManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private var rootView: FrameLayout? = null
    private var owner: OverlayViewOwner? = null

    /** The package that triggered the wall, so we can return there after. */
    @Volatile
    var triggeringPackage: String? = null
        private set

    val isShowing: Boolean get() = rootView != null

    @SuppressLint("InflateParams")
    fun show(fromPackage: String?) {
        if (isShowing) return
        if (!OverlayPermission.isGranted(context)) return

        val windowManager =
            context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return

        triggeringPackage = fromPackage

        val viewOwner = OverlayViewOwner().apply { onAttach() }

        // A plain FrameLayout wrapper so we can intercept the back key.
        // Without this the overlay swallows Back and the wall feels like a
        // trap rather than a toll.
        val root = object : FrameLayout(context) {
            override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                if (event.keyCode == KeyEvent.KEYCODE_BACK &&
                    event.action == KeyEvent.ACTION_UP
                ) {
                    backOff()
                    return true
                }
                return super.dispatchKeyEvent(event)
            }
        }

        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(viewOwner)
            setViewTreeViewModelStoreOwner(viewOwner)
            setViewTreeSavedStateRegistryOwner(viewOwner)
            setContent {
                WallContent(
                    onWriteEssay = { writeEssay() },
                    onBackOff = { backOff() }
                )
            }
        }
        root.addView(composeView)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayType(),
            // Focusable on purpose: the wall is modal, and it needs key
            // events so Back can be handled rather than silently eaten.
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        runCatching { windowManager.addView(root, params) }
            .onSuccess {
                rootView = root
                owner = viewOwner
            }
            .onFailure {
                // Permission revoked between the check and the add, or an
                // OEM refusing the window type. Nothing useful to do except
                // not crash the monitor.
                viewOwner.onDetach()
            }
    }

    fun hide() {
        val windowManager =
            context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        rootView?.let { view ->
            runCatching { windowManager?.removeView(view) }
        }
        owner?.onDetach()
        rootView = null
        owner = null
    }

    /** Dismiss and open the essay screen, remembering where to return. */
    private fun writeEssay() {
        val returnTo = triggeringPackage
        hide()
        context.startActivity(
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(MainActivity.EXTRA_OPEN_ESSAY, true)
                putExtra(MainActivity.EXTRA_RETURN_TO_PACKAGE, returnTo)
            }
        )
    }

    /**
     * Dismiss and send the user to their home screen.
     *
     * Deliberately NOT "close the dialog and leave them in Instagram" — that
     * would put them back exactly where they were, with the wall about to
     * fire again. The point is to get out.
     *
     * app_plan.md §2.2 has this path also suggesting the Live Feed. That
     * arrives in Phase 6; for now, home.
     */
    private fun backOff() {
        triggeringPackage = null
        hide()
        context.startActivity(
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
}
