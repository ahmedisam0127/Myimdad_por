package com.myimdad_por.ui.features.sales

import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController

/**
 * Sales navigation route composable.
 *
 * Wires [SalesScreen] into the navigation graph and translates
 * navigation-layer callbacks into [NavigationActions] calls.
 *
 * Usage inside NavHost:
 * ```
 * composable(ScreenRoutes.Sales) { backStackEntry ->
 *     SalesRoute(navController = navController, backStackEntry = backStackEntry)
 * }
 * ```
 */
@Composable
fun SalesRoute(
    navController: NavHostController,
    @Suppress("UNUSED_PARAMETER") backStackEntry: NavBackStackEntry
) {
    val actions = rememberNavigationActions(navController)

    SalesScreen(
        onNavigateBack             = actions::navigateBack,
        onNavigateToReports        = actions::navigateToReports,
        onNavigateToCustomers      = actions::navigateToCustomers,
        onNavigateToProducts       = actions::navigateToProducts,
        onNavigateToSubscription   = actions::navigateToSubscription,
        onNavigateToInvoiceDetails = actions::navigateToInvoiceDetails
    )
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/**
 * Lightweight wrapper that exposes the navigation callbacks required by
 * [SalesRoute] without allocating a full [NavigationActions] instance on
 * every recomposition.
 */
private class SalesNavigationActions(
    private val navController: NavHostController
) {
    fun navigateBack() {
        navController.popBackStack()
    }

    fun navigateToReports() {
        navController.navigateSingleTopTo(com.myimdad_por.ui.navigation.ScreenRoutes.Reports)
    }

    fun navigateToCustomers() {
        navController.navigateSingleTopTo(com.myimdad_por.ui.navigation.ScreenRoutes.Customers)
    }

    fun navigateToProducts() {
        navController.navigateSingleTopTo(com.myimdad_por.ui.navigation.ScreenRoutes.Products)
    }

    fun navigateToSubscription() {
        navController.navigateSingleTopTo(com.myimdad_por.ui.navigation.ScreenRoutes.Subscription)
    }

    fun navigateToInvoiceDetails(invoiceId: String) {
        navController.navigate(
            com.myimdad_por.ui.navigation.ScreenRoutes.invoiceDetails(invoiceId)
        )
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private fun NavHostController.navigateSingleTopTo(route: String) {
        navigate(route) {
            launchSingleTop = true
            restoreState    = true
            popUpTo(graph.startDestinationId) {
                saveState = true
            }
        }
    }
}

/**
 * Returns a stable [SalesNavigationActions] instance that survives
 * recompositions as long as [navController] stays the same.
 */
@Composable
private fun rememberNavigationActions(
    navController: NavHostController
): SalesNavigationActions =
    androidx.compose.runtime.remember(navController) {
        SalesNavigationActions(navController)
    }
