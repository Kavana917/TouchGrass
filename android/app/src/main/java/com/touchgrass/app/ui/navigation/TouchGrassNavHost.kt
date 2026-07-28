package com.touchgrass.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.touchgrass.app.feature.budget.BudgetSettingsScreen
import com.touchgrass.app.feature.essay.EssayDetailScreen
import com.touchgrass.app.feature.essay.EssayHistoryScreen
import com.touchgrass.app.feature.essay.EssayScreen
import com.touchgrass.app.feature.gallery.ComponentGalleryScreen
import com.touchgrass.app.feature.onboarding.OnboardingScreen
import com.touchgrass.app.feature.permissions.PermissionsScreen
import com.touchgrass.app.feature.usage.UsageDebugScreen

/**
 * Every screen in the app is registered here. Adding a screen means adding a
 * route constant and a `composable(...)` block below.
 *
 * Still to come: live feed (Phase 6), drawing book (Phase 8), FOMO (Phase 10).
 */
object Routes {
    const val ONBOARDING = "onboarding"
    const val USAGE = "usage"
    const val ESSAY = "essay?targetPackage={targetPackage}"
    const val BUDGET = "budget"
    const val ESSAY_HISTORY = "essay_history"
    const val ESSAY_DETAIL = "essay_detail/{essayId}"
    const val PERMISSIONS = "permissions"
    const val GALLERY = "gallery"

    fun essayDetail(id: Long) = "essay_detail/$id"

    fun essay(targetPackage: String? = null): String =
        if (targetPackage.isNullOrBlank()) "essay" else "essay?targetPackage=$targetPackage"
}

@Composable
fun TouchGrassNavHost(
    isNight: Boolean,
    onToggleNight: () -> Unit,
    modifier: Modifier = Modifier,
    startOnEssay: Boolean = false,
    setupComplete: Boolean = true,
    returnToPackage: String? = null,
    onReturnToApp: (String) -> Unit = {},
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = when {
            // Arriving from the wall goes straight to the essay. Making
            // someone navigate there themselves after being interrupted
            // would be friction on top of friction.
            startOnEssay -> Routes.essay(returnToPackage)
            !setupComplete -> Routes.ONBOARDING
            else -> Routes.USAGE
        },
        modifier = modifier
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Routes.USAGE) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.USAGE) {
            UsageDebugScreen(
                onWriteEssay = { navController.navigate(Routes.essay()) },
                onOpenHistory = { navController.navigate(Routes.ESSAY_HISTORY) },
                onOpenPermissions = { navController.navigate(Routes.PERMISSIONS) },
                onOpenBudget = { navController.navigate(Routes.BUDGET) },
                onOpenGallery = { navController.navigate(Routes.GALLERY) }
            )
        }
        composable(Routes.PERMISSIONS) {
            PermissionsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.BUDGET) {
            BudgetSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.ESSAY,
            arguments = listOf(
                navArgument("targetPackage") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            EssayScreen(
                onDone = {
                    // Paid the toll from the wall? Go back to where you were.
                    // Otherwise just close the screen.
                    if (returnToPackage != null) {
                        onReturnToApp(returnToPackage)
                    } else if (!navController.popBackStack()) {
                        navController.navigate(Routes.USAGE) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
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
