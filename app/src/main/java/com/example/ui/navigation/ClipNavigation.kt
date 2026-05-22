package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.*
import com.example.ui.viewmodel.ClipViewModel

const val ROUTE_HOME = "home"
const val ROUTE_DETAIL = "detail"
const val ROUTE_TRASH = "trash"
const val ROUTE_SETTINGS = "settings"

@Composable
fun ClipNavigation(viewModel: ClipViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = ROUTE_HOME
    ) {
        composable(ROUTE_HOME) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToDetail = { id -> navController.navigate("$ROUTE_DETAIL/$id") },
                onNavigateToSettings = { navController.navigate(ROUTE_SETTINGS) },
                onNavigateToTrash = { navController.navigate(ROUTE_TRASH) }
            )
        }

        composable(
            route = "$ROUTE_DETAIL/{clipId}",
            arguments = listOf(navArgument("clipId") { type = NavType.IntType })
        ) { backStackEntry ->
            val clipId = backStackEntry.arguments?.getInt("clipId") ?: 0
            DetailScreen(
                clipId = clipId,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(ROUTE_TRASH) {
            TrashScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(ROUTE_SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
