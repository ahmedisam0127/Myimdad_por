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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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
import com.myimdad_por.ui.features.inventory.InventoryUiState
import com.myimdad_por.ui.features.sales.SalesScreen
import com.myimdad_por.ui.features.sales.SalesUiEvent
import com.myimdad_por.ui.features.sales.SalesUiState
import kotlinx.coroutines.launch

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

    LaunchedEffect(showDrawer) {
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
                            restoreState = false
                            popUpTo(startDestination) { }
                        }
                    },
                    onLogout = {
                        scope.launch { drawerState.close() }
                        SessionManager.logout()
                        navController.navigate(ScreenRoutes.Login) {
                            popUpTo(0)
                            launchSingleTop = true
                            restoreState = false
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

            composable(ScreenRoutes.Home) {
                DashboardRouteContent(
                    onSaleClick = { saleId -> actions.navigateToSaleDetails(saleId) },
                    onExpenseClick = { expenseId -> actions.navigateToExpenseDetails(expenseId) },
                    onReportClick = { _ -> actions.navigateToReports() },
                    onTransactionClick = { transactionId -> actions.navigateToPaymentDetails(transactionId) },
                    onNavigateToSales = actions::navigateToSales,
                    onNavigateToExpenses = actions::navigateToExpenses,
                    onNavigateToReports = actions::navigateToReports,
                )
            }

            composable(ScreenRoutes.Dashboard) {
                DashboardRouteContent(
                    onSaleClick = { saleId -> actions.navigateToSaleDetails(saleId) },
                    onExpenseClick = { expenseId -> actions.navigateToExpenseDetails(expenseId) },
                    onReportClick = { _ -> actions.navigateToReports() },
                    onTransactionClick = { transactionId -> actions.navigateToPaymentDetails(transactionId) },
                    onNavigateToSales = actions::navigateToSales,
                    onNavigateToExpenses = actions::navigateToExpenses,
                    onNavigateToReports = actions::navigateToReports,
                )
            }

            composable(ScreenRoutes.Customers) { PlaceholderRoute("العملاء") }
            composable(ScreenRoutes.Suppliers) { PlaceholderRoute("الموردون") }
            composable(ScreenRoutes.Inventory) { InventoryRouteContent() }

            composable(ScreenRoutes.Sales) { SalesRouteContent() }

            composable(ScreenRoutes.Purchases) { PlaceholderRoute("المشتريات") }
            composable(ScreenRoutes.Payments) { PlaceholderRoute("المدفوعات") }
            composable(ScreenRoutes.Returns) { PlaceholderRoute("المرتجعات") }
            composable(ScreenRoutes.Expenses) { PlaceholderRoute("المصروفات") }
            composable(ScreenRoutes.Reports) { PlaceholderRoute("التقارير") }
            composable(ScreenRoutes.Subscription) { PlaceholderRoute("الاشتراكات") }
            composable(ScreenRoutes.Settings) { PlaceholderRoute("الإعدادات") }
            composable(ScreenRoutes.Accounting) { PlaceholderRoute("المحاسبة") }
            composable(ScreenRoutes.Security) { PlaceholderRoute("الحماية والأمان") }
        }
    }
}

@Composable
private fun InventoryRouteContent() {
    val state = remember { InventoryUiState() }

    InventoryScreen(
        state = state,
        onEvent = { event ->
            when (event) {
                else -> Unit
            }
        }
    )
}

@Composable
private fun SalesRouteContent() {
    val state = remember { SalesUiState() }

    SalesScreen(
        state = state,
        onEvent = { event ->
            when (event) {
                else -> Unit
            }
        }
    )
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
                is DashboardUiEvent.OnSaleClick -> onSaleClick(event.saleId)
                is DashboardUiEvent.OnExpenseClick -> onExpenseClick(event.expenseId)
                is DashboardUiEvent.OnReportClick -> onReportClick(event.reportId)
                is DashboardUiEvent.OnTransactionClick -> onTransactionClick(event.transactionId)
                DashboardUiEvent.NavigateToSales -> onNavigateToSales()
                DashboardUiEvent.NavigateToExpenses -> onNavigateToExpenses()
                DashboardUiEvent.NavigateToReports -> onNavigateToReports()
            }
        },
    )
}

@Composable
private fun PlaceholderRoute(title: String) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
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
        "customer_details/",
        "supplier_details/",
        "product_details/",
        "invoice_details/",
        "purchase_details/",
        "sale_details/",
        "payment_details/",
        "return_details/",
        "expense_details/",
    )

    return detailPrefixes.any { route.startsWith(it) }
}
