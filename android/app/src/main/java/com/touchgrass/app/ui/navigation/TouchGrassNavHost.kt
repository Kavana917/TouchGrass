package com.touchgrass.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.touchgrass.app.feature.essay.EssayDetailScreen
import com.touchgrass.app.feature.essay.EssayHistoryScreen
import com.touchgrass.app.feature.essay.EssayScreen
import com.touchgrass.app.feature.gallery.ComponentGalleryScreen
import com.touchgrass.app.feature.usage.UsageDebugScreen

/**
 * Every screen in the app is registered here. Adding a screen means adding a
 * route constant and a `composable(...)` block below.
 *
 * Still to come: live feed (Phase 6), drawing book (Phase 8), FOMO (Phase 10).
 */
object Routes {
    const val USAGE = "usage"
    const val ESSAY = "essay"
    const val ESSAY_HISTORY = "essay_history"
    const val ESSAY_DETAIL = "essay_detail/{essayId}"
    const val GALLERY = "gallery"

    fun essayDetail(id: Long) = "essay_detail/$id"
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
                onWriteEssay = { navController.navigate(Routes.ESSAY) },
                onOpenHistory = { navController.navigate(Routes.ESSAY_HISTORY) },
                onOpenGallery = { navController.navigate(Routes.GALLERY) }
            )
        }
        composable(Routes.ESSAY) {
            EssayScreen(
                onDone = { navController.popBackStack() }
            )
        }
        composable(Routes.ESSAY_HISTORY) {
            EssayHistoryScreen(
                onBack = { navController.popBackStack() },
                onOpenEssay = { id -> navController.navigate(Routes.essayDetail(id)) }
            )
        }
        composable(
            route = Routes.ESSAY_DETAIL,
            arguments = listOf(navArgument("essayId") { type = NavType.LongType })
        ) {
            EssayDetailScreen(
                onBack = { navController.popBackStack() }
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
