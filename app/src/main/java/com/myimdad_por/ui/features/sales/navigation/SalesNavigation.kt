package com.myimdad_por.ui.features.sales.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.myimdad_por.ui.features.sales.SalesRoute
import com.myimdad_por.ui.navigation.ScreenRoutes

// ---------------------------------------------------------------------------
// Public entry point
// ---------------------------------------------------------------------------

/**
 * Registers all Sales-feature destinations into the host [NavGraphBuilder].
 *
 * Call this inside your root `NavHost` block:
 * ```kotlin
 * NavHost(navController = navController, startDestination = ScreenRoutes.Home) {
 *     salesGraph(navController)
 *     // … other feature graphs
 * }
 * ```
 *
 * @param navController The host controller shared across the whole app.
 */
fun NavGraphBuilder.salesGraph(navController: NavHostController) {
    salesScreen(navController)
}

// ---------------------------------------------------------------------------
// Individual destinations
// ---------------------------------------------------------------------------

/**
 * [ScreenRoutes.Sales] destination — the main Sales / POS screen.
 */
private fun NavGraphBuilder.salesScreen(navController: NavHostController) {
    composable(route = ScreenRoutes.Sales) { backStackEntry ->
        SalesRoute(
            navController  = navController,
            backStackEntry = backStackEntry
        )
    }
}
