package com.example.aiassistant.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.aiassistant.AppContainer
import com.example.aiassistant.ui.screens.chat.ChatScreen
import com.example.aiassistant.ui.screens.history.HistoryScreen
import com.example.aiassistant.ui.screens.models.ModelPickerScreen
import com.example.aiassistant.ui.screens.settings.ApiManagementScreen
import com.example.aiassistant.ui.screens.settings.ApiSettingsScreen
import com.example.aiassistant.ui.screens.states.ErrorStateScreen

@Composable
fun AppNavHost(
    container: AppContainer,
    darkModeEnabled: Boolean,
    markdownRenderingEnabled: Boolean,
    onToggleDarkMode: () -> Unit
) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = AppRoutes.Chat) {
        composable(
            route = AppRoutes.Chat,
            enterTransition = { chatEnterTransition(initialState) },
            popEnterTransition = { chatEnterTransition(initialState) }
        ) {
            ChatScreen(
                container = container,
                conversationId = null,
                markdownRenderingEnabled = markdownRenderingEnabled,
                onOpenHistory = { navController.navigateSingleTop(AppRoutes.History) },
                onOpenSettings = { navController.navigateSingleTop(AppRoutes.ApiManagement) }
            )
        }
        composable(
            route = AppRoutes.ChatWithConversation,
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType }),
            enterTransition = { chatEnterTransition(initialState) },
            popEnterTransition = { chatEnterTransition(initialState) }
        ) { backStackEntry ->
            ChatScreen(
                container = container,
                conversationId = backStackEntry.arguments?.getString("conversationId"),
                markdownRenderingEnabled = markdownRenderingEnabled,
                onOpenHistory = { navController.navigateSingleTop(AppRoutes.History) },
                onOpenSettings = { navController.navigateSingleTop(AppRoutes.ApiManagement) }
            )
        }
        composable(
            route = AppRoutes.History,
            enterTransition = { historyEnterTransition(initialState) },
            exitTransition = { historyExitTransition(targetState) },
            popExitTransition = { historyExitTransition(targetState) }
        ) {
            HistoryScreen(
                container = container,
                onBack = { navController.navigateUp() },
                onOpenChat = { conversationId ->
                    navController.navigate(AppRoutes.chat(conversationId)) {
                        popUpTo(AppRoutes.Chat)
                    }
                },
                onClearHistoryComplete = {
                    navController.navigate(AppRoutes.Chat) {
                        popUpTo(AppRoutes.Chat) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(
            route = AppRoutes.ApiManagement,
            enterTransition = { settingsEnterTransition(initialState) },
            exitTransition = { settingsExitTransition(targetState) },
            popExitTransition = { settingsExitTransition(targetState) }
        ) {
            ApiManagementScreen(
                container = container,
                darkModeEnabled = darkModeEnabled,
                onToggleDarkMode = onToggleDarkMode,
                onBack = { navController.navigateUp() },
                onOpenCreate = { navController.navigateSingleTop(AppRoutes.settings()) },
                onOpenEdit = { profileId -> navController.navigateSingleTop(AppRoutes.settings(profileId)) }
            )
        }
        composable(
            route = AppRoutes.SettingsWithProfile,
            arguments = listOf(
                navArgument("profileId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            ),
            enterTransition = { settingsEnterTransition(initialState) },
            exitTransition = { settingsExitTransition(targetState) },
            popExitTransition = { settingsExitTransition(targetState) }
        ) { backStackEntry ->
            ApiSettingsScreen(
                container = container,
                profileId = backStackEntry.arguments?.getString("profileId"),
                onBack = { navController.navigateUp() },
                onOpenModels = { navController.navigateSingleTop(AppRoutes.Models) },
                onSaved = { navController.popBackStack(AppRoutes.ApiManagement, false) }
            )
        }
        composable(AppRoutes.Models) {
            ModelPickerScreen(container = container, onBack = { navController.navigateUp() })
        }
        composable(AppRoutes.States) {
            ErrorStateScreen(onBack = { navController.navigateUp() }, onOpenSettings = { navController.navigate(AppRoutes.settings()) })
        }
    }
}

private const val TransitionDurationMillis = 260

private val transitionSpec = tween<IntOffset>(durationMillis = TransitionDurationMillis)
private val fadeSpec = tween<Float>(durationMillis = TransitionDurationMillis)

private fun chatEnterTransition(from: NavBackStackEntry): EnterTransition? {
    return when (from.destination.route) {
        AppRoutes.History, AppRoutes.Settings, AppRoutes.ApiManagement -> fadeIn(animationSpec = fadeSpec)
        else -> null
    }
}

private fun historyEnterTransition(from: NavBackStackEntry): EnterTransition? {
    return if (from.destination.route.isChatRoute()) {
        slideInHorizontally(animationSpec = transitionSpec, initialOffsetX = { -it }) +
            fadeIn(animationSpec = fadeSpec)
    } else {
        null
    }
}

private fun historyExitTransition(to: NavBackStackEntry): ExitTransition? {
    return if (to.destination.route.isChatRoute()) {
        slideOutHorizontally(animationSpec = transitionSpec, targetOffsetX = { -it }) +
            fadeOut(animationSpec = fadeSpec)
    } else {
        null
    }
}

private fun settingsEnterTransition(from: NavBackStackEntry): EnterTransition? {
    return if (from.destination.route.isChatRoute() || from.destination.route == AppRoutes.States || from.destination.route == AppRoutes.ApiManagement) {
        slideInHorizontally(animationSpec = transitionSpec, initialOffsetX = { it }) +
            fadeIn(animationSpec = fadeSpec)
    } else {
        null
    }
}

private fun settingsExitTransition(to: NavBackStackEntry): ExitTransition? {
    return if (to.destination.route.isChatRoute() || to.destination.route == AppRoutes.ApiManagement) {
        slideOutHorizontally(animationSpec = transitionSpec, targetOffsetX = { it }) +
            fadeOut(animationSpec = fadeSpec)
    } else {
        null
    }
}

private fun String?.isChatRoute(): Boolean {
    return this == AppRoutes.Chat || this == AppRoutes.ChatWithConversation
}

private fun androidx.navigation.NavHostController.navigateSingleTop(route: String) {
    navigate(route) {
        launchSingleTop = true
    }
}
