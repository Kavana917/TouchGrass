package com.touchgrass.app.core.overlay

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.FrameLayout
import com.touchgrass.app.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Puts the wall on screen, over whatever app the user is in.
 *
 * ⚠️ THE CENTRAL CONSTRAINT OF THIS FEATURE:
 *
 * Since Android 10, apps cannot start an Activity from the background.
 * startActivity() from the monitor service does not throw, does not log, and
 * does not show anything — it silently does nothing. Every "my blocker
 * stopped working on Android 10" bug report is this.
 *
 * The only supported way to put UI over another app is to add a
 * TYPE_APPLICATION_OVERLAY window through WindowManager.
 *
 * ⚠️ AND THE HARD-WON ONE:
 *
 * Every step of that runs on the main thread inside a Service, where there
 * is no Activity to absorb a failure — so anything that throws kills the
 * whole app. The first version wrapped only addView() and crashed on view
 * construction instead. The entire operation is now guarded, and the reason
 * is recorded rather than discarded.
 */
@Singleton
class WallOverlayManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private var rootView: FrameLayout? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    /** The package that triggered the wall, so we can return there after. */
    @Volatile
    var triggeringPackage: String? = null
        private set

    /** Why the last attempt failed, or null if it worked. */
    @Volatile
    var lastError: String? = null
        private set

    val isShowing: Boolean get() = rootView != null

    fun show(fromPackage: String?) {
        // WindowManager is main-thread only, and the poll loop is not.
        onMain {
            if (isShowing) return@onMain

            if (!OverlayPermission.isGranted(context)) {
                lastError = "Draw-over-apps permission is off"
                return@onMain
            }

            val windowManager =
                context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            if (windowManager == null) {
                lastError = "No WindowManager available"
                return@onMain
            }

            triggeringPackage = fromPackage

            // Everything from here — view construction, layout params, the
            // addView call — is inside one guard. A throw anywhere used to
            // take the app down with it.
            runCatching {
                val content = WallView.create(
                    context = context,
                    message = "The pass has expired.",
                    primaryLabel = "Write an essay",
                    secondaryLabel = "Not now",
                    onPrimary = { writeEssay() },
                    onSecondary = { backOff() }
                )

                // Wrapper exists purely to intercept Back, so the wall reads
                // as a toll rather than a trap.
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
                root.addView(content)

                windowManager.addView(root, buildLayoutParams())
                rootView = root
                lastError = null
            }.onFailure { error ->
                lastError = "${error::class.simpleName}: ${error.message}"
                rootView = null
            }
        }
    }

    fun hide() {
        onMain {
            val view = rootView ?: return@onMain
            val windowManager =
                context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            runCatching { windowManager?.removeView(view) }
            rootView = null
        }
    }

    private fun buildLayoutParams() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        overlayType(),
        // Focusable on purpose: the wall is modal and needs key events so
        // Back can be handled rather than silently eaten.
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.CENTER
    }

    /** Dismiss and open the essay screen, remembering where to return. */
    private fun writeEssay() {
        val returnTo = triggeringPackage
        hide()
        runCatching {
            context.startActivity(
                Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra(MainActivity.EXTRA_OPEN_ESSAY, true)
                    putExtra(MainActivity.EXTRA_RETURN_TO_PACKAGE, returnTo)
                }
            )
        }
    }

    /**
     * Dismiss and send the user to their home screen.
     *
     * Deliberately NOT "close the dialog and leave them in Instagram" — that
     * would put them back exactly where they were, with the wall about to
     * fire again. The point is to get out.
     */
    private fun backOff() {
        triggeringPackage = null
        hide()
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }

    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

    private inline fun onMain(crossinline block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post { block() }
        }
    }
}
