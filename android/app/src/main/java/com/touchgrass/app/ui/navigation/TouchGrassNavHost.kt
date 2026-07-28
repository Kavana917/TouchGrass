package com.touchgrass.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.touchgrass.app.feature.gallery.ComponentGalleryScreen

/**
 * Every screen in the app is registered here. Adding a screen means adding a
 * route constant and a `composable(...)` block below.
 *
 * Currently only the dev component gallery. Real routes arrive in Phase 2
 * (pass status, essay editor) and Phase 6 (live feed).
 */
object Routes {
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
        startDestination = Routes.GALLERY,
        modifier = modifier
    ) {
        composable(Routes.GALLERY) {
            ComponentGalleryScreen(
                isNight = isNight,
                onToggleNight = onToggleNight
            )
        }
    }
}
