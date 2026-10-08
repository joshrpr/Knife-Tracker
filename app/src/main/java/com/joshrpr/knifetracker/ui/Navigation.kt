package com.joshrpr.knifetracker.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

private const val NEW_KNIFE = 0L

@Composable
fun KnifeTrackerNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "knives") {
        composable("knives") {
            KnifeListScreen(
                onAddKnife = { nav.navigate("edit/$NEW_KNIFE") },
                onOpenKnife = { id -> nav.navigate("knife/$id") },
            )
        }
        composable(
            "knife/{knifeId}",
            arguments = listOf(navArgument("knifeId") { type = NavType.LongType }),
        ) {
            KnifeDetailScreen(
                onBack = { nav.popBackStack() },
                onEdit = { id -> nav.navigate("edit/$id") },
            )
        }
        composable(
            "edit/{knifeId}",
            arguments = listOf(navArgument("knifeId") { type = NavType.LongType }),
        ) {
            KnifeEditScreen(
                onDone = { savedId, wasNew ->
                    nav.popBackStack()
                    if (wasNew) nav.navigate("knife/$savedId")
                },
                onCancel = { nav.popBackStack() },
            )
        }
    }
}
