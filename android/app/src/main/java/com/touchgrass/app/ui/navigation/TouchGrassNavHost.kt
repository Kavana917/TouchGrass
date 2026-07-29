package com.touchgrass.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.touchgrass.app.BuildConfig
import com.touchgrass.app.feature.budget.BudgetSettingsScreen
import com.touchgrass.app.feature.essay.EssayDetailScreen
import com.touchgrass.app.feature.essay.EssayHistoryScreen
import com.touchgrass.app.feature.essay.EssayScreen
import com.touchgrass.app.feature.gallery.ComponentGalleryScreen
import com.touchgrass.app.feature.home.HomeScreen
import com.touchgrass.app.feature.onboarding.OnboardingScreen
import com.touchgrass.app.feature.permissions.PermissionsScreen
import com.touchgrass.app.feature.settings.OemHelpScreen
import com.touchgrass.app.feature.settings.PrivacyScreen
import com.touchgrass.app.feature.settings.WatchedAppsScreen
import com.touchgrass.app.feature.usage.UsageDebugScreen

/**
 * Every screen in the app is registered here. Adding a screen means adding a
 * route constant and a `composable(...)` block below.
 *
 * Still to come: drawing book, FOMO digest.
 */
object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val DEBUG = "debug"
    const val ESSAY = "essay?targetPackage={targetPackage}"
    const val BUDGET = "budget"
    const val ESSAY_HISTORY = "essay_history"
    const val ESSAY_DETAIL = "essay_detail/{essayId}"
    const val PERMISSIONS = "permissions"
    const val WATCHED_APPS = "watched_apps"
    const val OEM_HELP = "oem_help"
    const val PRIVACY = "privacy"
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
            startOnEssay -> Routes.essay(returnToPackage)
            !setupComplete -> Routes.ONBOARDING
            else -> Routes.HOME
        },
        modifier = modifier
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                onWriteEssay = { navController.navigate(Routes.essay()) },
                onOpenHistory = { navController.navigate(Routes.ESSAY_HISTORY) },
                onOpenWatchedApps = { navController.navigate(Routes.WATCHED_APPS) },
                onOpenBudget = { navController.navigate(Routes.BUDGET) },
                onOpenPermissions = { navController.navigate(Routes.PERMISSIONS) },
                onOpenOemHelp = { navController.navigate(Routes.OEM_HELP) },
                onOpenPrivacy = { navController.navigate(Routes.PRIVACY) },
                onOpenDebug = {
                    if (BuildConfig.DEBUG) {
                        navController.navigate(Routes.DEBUG)
                    }
                }
            )
        }
        if (BuildConfig.DEBUG) {
            composable(Routes.DEBUG) {
                UsageDebugScreen(
                    onWriteEssay = { navController.navigate(Routes.essay()) },
                    onOpenHistory = { navController.navigate(Routes.ESSAY_HISTORY) },
                    onOpenPermissions = { navController.navigate(Routes.PERMISSIONS) },
                    onOpenBudget = { navController.navigate(Routes.BUDGET) },
                    onOpenGallery = { navController.navigate(Routes.GALLERY) },
                    onBack = { navController.popBackStack() }
                )
            }
        }
        composable(Routes.WATCHED_APPS) {
            WatchedAppsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.OEM_HELP) {
            OemHelpScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.PRIVACY) {
            PrivacyScreen(onBack = { navController.popBackStack() })
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
                    if (returnToPackage != null) {
                        onReturnToApp(returnToPackage)
                    } else if (!navController.popBackStack()) {
                        navController.navigate(Routes.HOME) {
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
