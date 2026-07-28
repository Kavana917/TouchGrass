package com.touchgrass.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.touchgrass.app.feature.gallery.ComponentGalleryScreen
import com.touchgrass.app.feature.usage.UsageDebugScreen

/**
 * Every screen in the app is registered here. Adding a screen means adding a
 * route constant and a `composable(...)` block below.
 *
 * Real routes arrive as the phases land: essay editor (Phase 3),
 * live feed (Phase 6), drawing book (Phase 8).
 */
object Routes {
    const val USAGE = "usage"
    const val GALLERY = "gallery"
}

@Composable
fun TouchGrassNavHost(
    isNight: Boolean,
    onToggleNight: () -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.USAGE,
        modifier = modifier
    ) {
        composable(Routes.USAGE) {
            UsageDebugScreen(
                onOpenGallery = { navController.navigate(Routes.GALLERY) }
            )
        }
        composable(Routes.GALLERY) {
            ComponentGalleryScreen(
                isNight = isNight,
                onToggleNight = onToggleNight
            )
        }
    }
}
