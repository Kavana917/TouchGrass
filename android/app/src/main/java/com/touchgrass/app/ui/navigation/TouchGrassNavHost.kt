package com.touchgrass.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.touchgrass.app.feature.scratch.ScratchHomeScreen
import com.touchgrass.app.feature.scratch.ScratchSecondScreen

/**
 * Every screen in the app is registered here. Adding a screen means adding a
 * route constant and a `composable(...)` block below.
 */
object Routes {
    const val HOME = "home"
    const val SECOND = "second"
}

@Composable
fun TouchGrassNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier
    ) {
        composable(Routes.HOME) {
            ScratchHomeScreen(
                onNavigateToSecond = { navController.navigate(Routes.SECOND) }
            )
        }
        composable(Routes.SECOND) {
            ScratchSecondScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
