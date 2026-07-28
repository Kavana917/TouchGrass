package com.touchgrass.app.core.overlay

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner

/**
 * Gives a Compose view somewhere to live outside an Activity.
 *
 * WHY THIS EXISTS: `ComposeView` refuses to render unless it can find a
 * lifecycle owner, a ViewModelStore owner and a SavedStateRegistry owner up
 * its view tree. An Activity supplies all three. A Service supplies none —
 * so a ComposeView added straight to a WindowManager overlay crashes with
 * "ViewTreeLifecycleOwner not found from DecorView".
 *
 * This is a minimal stand-in that provides all three, driven manually by
 * the overlay's own show/hide.
 */
class OverlayViewOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry

    /** Call before attaching the view. */
    fun onAttach() {
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    /** Call after removing the view, so Compose tears down cleanly. */
    fun onDetach() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        store.clear()
    }
}
