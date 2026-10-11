package com.example.checklistapp.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.checklistapp.ui.checklist.ChecklistScreen
import com.example.checklistapp.ui.home.HomeScreen

object Routes {
    const val HOME = "home"
    const val LIST = "list/{listId}"
    fun list(id: Long) = "list/$id"
}

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.HOME) {

        composable(Routes.HOME) {
            HomeScreen(onOpenList = { id -> navController.navigate(Routes.list(id)) })
        }

        composable(
            route = Routes.LIST,
            arguments = listOf(navArgument("listId") { type = NavType.LongType }),
            deepLinks = listOf(navDeepLink { uriPattern = "checklistapp://list/{listId}" })
        ) { entry ->
            val listId = entry.arguments?.getLong("listId") ?: -1L
            if (listId != -1L) {
                ChecklistScreen(listId = listId, onBack = { navController.popBackStack() })
            }
        }
    }
}
