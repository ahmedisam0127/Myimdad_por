package com.myimdad_por.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.myimdad_por.core.security.SessionManager
import com.myimdad_por.ui.components.SideMenu
import com.myimdad_por.ui.features.auth.ForgotPasswordScreen
import com.myimdad_por.ui.features.auth.LoginScreen
import com.myimdad_por.ui.features.auth.RegisterScreen
import com.myimdad_por.ui.features.dashboard.DashboardPeriod
import com.myimdad_por.ui.features.dashboard.DashboardScreen
import com.myimdad_por.ui.features.dashboard.DashboardUiEvent
import com.myimdad_por.ui.features.dashboard.DashboardUiState
import com.myimdad_por.ui.features.inventory.InventoryScreen
import com.myimdad_por.ui.features.inventory.InventoryViewModel
import com.myimdad_por.ui.features.sales.SalesScreen
import com.myimdad_por.ui.features.subscription.SubscriptionScreen
import kotlinx.coroutines.launch

private const val PRODUCT_DETAILS_ROUTE = "product_details"
private const val STOCK_DETAILS_ROUTE = "stock_details"

@Composable
fun AppNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = ScreenRoutes.Login,
) {
    val actions = remember(navController) { NavigationActions(navController) }
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val showDrawer = shouldShowDrawer(currentRoute)

    LaunchedEffect(showDrawer, drawerState.isOpen) {
        if (!showDrawer && drawerState.isOpen) {
            drawerState.close()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = showDrawer,
        drawerContent = {
            if (showDrawer) {
                SideMenu(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        scope.launch { drawerState.close() }

                        navController.navigate(route) {
                            launchSingleTop = true
                        }
                    },
                    onLogout = {
                        scope.launch { drawerState.close() }

                        SessionManager.logout()

                        navController.navigate(ScreenRoutes.Login) {
                            popUpTo(ScreenRoutes.Login) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    }
                )
            } else {
                Box(modifier = Modifier.fillMaxSize())
            }
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = modifier.fillMaxSize(),
        ) {

            // ----------------------------------------------------------------
            // Auth
            // ----------------------------------------------------------------

            composable(ScreenRoutes.Login) {
                LoginScreen(
                    onNavigateToRegister = actions::navigateToRegister,
                    onNavigateToForgotPassword = actions::navigateToForgotPassword,
                    onLoginSuccess = actions::clearBackStackAndGoToHome,
                )
            }

            composable(ScreenRoutes.Register) {
                RegisterScreen(
                    onNavigateToLogin = actions::navigateToLogin,
                    onNavigateBack = actions::navigateBack,
                    onRegisterSuccess = actions::clearBackStackAndGoToHome,
                )
            }

            composable(ScreenRoutes.ForgotPassword) {
                ForgotPasswordScreen(
                    onBack = actions::navigateBack,
                    onComplete = actions::navigateToLogin,
                )
            }

            // ----------------------------------------------------------------
            // Home / Dashboard
            // ----------------------------------------------------------------

            composable(ScreenRoutes.Home) {
                DashboardRouteContent(
                    onSaleClick = { saleId ->
                        actions.navigateToSaleDetails(saleId)
                    },
                    onExpenseClick = { expenseId ->
                        actions.navigateToExpenseDetails(expenseId)
                    },
                    onReportClick = {
                        actions.navigateToReports()
                    },
                    onTransactionClick = { transactionId ->
                        actions.navigateToPaymentDetails(transactionId)
                    },
                    onNavigateToSales = actions::navigateToSales,
                    onNavigateToExpenses = actions::navigateToExpenses,
                    onNavigateToReports = actions::navigateToReports,
                )
            }

            composable(ScreenRoutes.Dashboard) {
                DashboardRouteContent(
                    onSaleClick = { saleId ->
                        actions.navigateToSaleDetails(saleId)
                    },
                    onExpenseClick = { expenseId ->
                        actions.navigateToExpenseDetails(expenseId)
                    },
                    onReportClick = {
                        actions.navigateToReports()
                    },
                    onTransactionClick = { transactionId ->
                        actions.navigateToPaymentDetails(transactionId)
                    },
                    onNavigateToSales = actions::navigateToSales,
                    onNavigateToExpenses = actions::navigateToExpenses,
                    onNavigateToReports = actions::navigateToReports,
                )
            }

            // ----------------------------------------------------------------
            // Customers / Suppliers
            // ----------------------------------------------------------------

            composable(ScreenRoutes.Customers) {
                PlaceholderRoute("العملاء")
            }

            composable(ScreenRoutes.Suppliers) {
                PlaceholderRoute("الموردون")
            }

            // ----------------------------------------------------------------
            // Inventory
            // ----------------------------------------------------------------

            composable(ScreenRoutes.Inventory) {

                val inventoryViewModel: InventoryViewModel = hiltViewModel()

                InventoryScreen(
                    viewModel = inventoryViewModel,
                    onNavigateBack = actions::navigateBack,
                    onNavigateToProductDetails = { barcode ->
                        navController.navigateToDetails(
                            baseRoute = PRODUCT_DETAILS_ROUTE,
                            barcode = barcode
                        )
                    }
                )
            }

            // ----------------------------------------------------------------
            // Sales
            // ----------------------------------------------------------------

            composable(ScreenRoutes.Sales) {

                SalesScreen(
                    onNavigateBack = actions::navigateBack,
                    onNavigateToReports = actions::navigateToReports,
                    onNavigateToCustomers = {
                        navController.navigate(ScreenRoutes.Customers) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToProducts = {
                        navController.navigate(ScreenRoutes.Inventory) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToSubscription = {
                        navController.navigate(ScreenRoutes.Subscription) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToInvoiceDetails = { invoiceId ->
                        navController.navigate("invoice_details/$invoiceId") {
                            launchSingleTop = true
                        }
                    }
                )
            }

            // ----------------------------------------------------------------
            // Other Screens
            // ----------------------------------------------------------------

            composable(ScreenRoutes.Purchases) {
                PlaceholderRoute("المشتريات")
            }

            composable(ScreenRoutes.Payments) {
                PlaceholderRoute("المدفوعات")
            }

            composable(ScreenRoutes.Returns) {
                PlaceholderRoute("المرتجعات")
            }

            composable(ScreenRoutes.Expenses) {
                PlaceholderRoute("المصروفات")
            }

            composable(ScreenRoutes.Reports) {
                PlaceholderRoute("التقارير")
            }

            composable(ScreenRoutes.Subscription) {
                SubscriptionScreen(
                    onBackClick = actions::navigateBack
                )
            }

            composable(ScreenRoutes.Settings) {
                PlaceholderRoute("الإعدادات")
            }

            composable(ScreenRoutes.Accounting) {
                PlaceholderRoute("المحاسبة")
            }

            composable(ScreenRoutes.Security) {
                PlaceholderRoute("الحماية والأمان")
            }

            // ----------------------------------------------------------------
            // Product Details
            // ----------------------------------------------------------------

            composable(
                route = "$PRODUCT_DETAILS_ROUTE/{barcode}",
                arguments = listOf(
                    navArgument("barcode") {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->

                PlaceholderRoute(
                    title = "تفاصيل المنتج: ${
                        backStackEntry.arguments
                            ?.getString("barcode")
                            .orEmpty()
                    }"
                )
            }

            // ----------------------------------------------------------------
            // Stock Details
            // ----------------------------------------------------------------

            composable(
                route = "$STOCK_DETAILS_ROUTE/{barcode}",
                arguments = listOf(
                    navArgument("barcode") {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->

                PlaceholderRoute(
                    title = "تفاصيل المخزون: ${
                        backStackEntry.arguments
                            ?.getString("barcode")
                            .orEmpty()
                    }"
                )
            }
        }
    }
}

@Composable
private fun DashboardRouteContent(
    onSaleClick: (String) -> Unit,
    onExpenseClick: (String) -> Unit,
    onReportClick: (String) -> Unit,
    onTransactionClick: (String) -> Unit,
    onNavigateToSales: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToReports: () -> Unit,
) {

    val state = remember {
        DashboardUiState(
            isLoading = false,
            isRefreshing = false,
            selectedPeriod = DashboardPeriod.Today,
        )
    }

    DashboardScreen(
        state = state,
        onEvent = { event ->

            when (event) {

                DashboardUiEvent.Load -> Unit

                DashboardUiEvent.Refresh -> Unit

                DashboardUiEvent.Retry -> Unit

                is DashboardUiEvent.ChangePeriod -> Unit

                is DashboardUiEvent.OnSaleClick -> {
                    onSaleClick(event.saleId)
                }

                is DashboardUiEvent.OnExpenseClick -> {
                    onExpenseClick(event.expenseId)
                }

                is DashboardUiEvent.OnReportClick -> {
                    onReportClick(event.reportId)
                }

                is DashboardUiEvent.OnTransactionClick -> {
                    onTransactionClick(event.transactionId)
                }

                DashboardUiEvent.NavigateToSales -> {
                    onNavigateToSales()
                }

                DashboardUiEvent.NavigateToExpenses -> {
                    onNavigateToExpenses()
                }

                DashboardUiEvent.NavigateToReports -> {
                    onNavigateToReports()
                }
            }
        }
    )
}

@Composable
private fun PlaceholderRoute(title: String) {

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {

        Box(
            modifier = Modifier.fillMaxSize()
        ) {

            androidx.compose.material3.Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

private fun shouldShowDrawer(route: String?): Boolean {

    if (route.isNullOrBlank()) return false

    val exactRoutes = setOf(
        ScreenRoutes.Home,
        ScreenRoutes.Dashboard,
        ScreenRoutes.Customers,
        ScreenRoutes.Suppliers,
        ScreenRoutes.Inventory,
        ScreenRoutes.Sales,
        ScreenRoutes.Purchases,
        ScreenRoutes.Payments,
        ScreenRoutes.Returns,
        ScreenRoutes.Expenses,
        ScreenRoutes.Reports,
        ScreenRoutes.Subscription,
        ScreenRoutes.Settings,
        ScreenRoutes.Accounting,
        ScreenRoutes.Security,
    )

    if (route in exactRoutes) return true

    val detailPrefixes = listOf(
        "$PRODUCT_DETAILS_ROUTE/",
        "$STOCK_DETAILS_ROUTE/",
        "customer_details/",
        "supplier_details/",
        "invoice_details/",
        "purchase_details/",
        "sale_details/",
        "payment_details/",
        "return_details/",
        "expense_details/",
    )

    return detailPrefixes.any { route.startsWith(it) }
}

private fun NavController.navigateToDetails(
    baseRoute: String,
    barcode: String
) {

    navigate("$baseRoute/$barcode") {
        launchSingleTop = true
    }
}