package com.myimdad_por.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.myimdad_por.ui.features.auth.ForgotPasswordScreen
import com.myimdad_por.ui.features.auth.LoginScreen
import com.myimdad_por.ui.features.auth.RegisterScreen
import com.myimdad_por.ui.features.dashboard.DashboardRoute // استيراد صفحة لوحة التحكم

@Composable
fun AppNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = ScreenRoutes.Login
) {
    val actions = remember(navController) { NavigationActions(navController) }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier.fillMaxSize()
    ) {
        // --- شاشة تسجيل الدخول ---
        composable(route = ScreenRoutes.Login) {
            LoginScreen(
                onNavigateToRegister = actions::navigateToRegister,
                onNavigateToForgotPassword = actions::navigateToForgotPassword,
                onLoginSuccess = actions::clearBackStackAndGoToHome
            )
        }

        // --- شاشة إنشاء حساب ---
        composable(route = ScreenRoutes.Register) {
            RegisterScreen(
                onNavigateToLogin = actions::navigateToLogin,
                onNavigateBack = { navController.popBackStack() },
                onRegisterSuccess = actions::clearBackStackAndGoToHome
            )
        }

        // --- شاشة استعادة كلمة المرور ---
        composable(route = ScreenRoutes.ForgotPassword) {
            ForgotPasswordScreen(
                onBack = { navController.popBackStack() },
                onComplete = { actions.navigateToLogin() }
            )
        }

        // --- شاشة لوحة التحكم (تم التعديل هنا) ---
        composable(route = ScreenRoutes.Home) {
            DashboardRoute(
                onSaleClick = { saleId -> 
                    // هنا يمكنك إضافة التنقل لتفاصيل الفاتورة مستقبلاً
                    // actions.navigateToSaleDetails(saleId) 
                },
                onExpenseClick = { expenseId -> 
                    // actions.navigateToExpenseDetails(expenseId)
                },
                onReportClick = { reportId -> 
                    // actions.navigateToReportDetails(reportId)
                },
                onTransactionClick = { txId -> 
                    // actions.navigateToTransactionDetails(txId)
                },
                onNavigateToSales = { 
                    // navController.navigate(ScreenRoutes.SalesList) 
                },
                onNavigateToExpenses = { 
                    // navController.navigate(ScreenRoutes.ExpensesList) 
                },
                onNavigateToReports = { 
                    // navController.navigate(ScreenRoutes.ReportsList) 
                }
            )
        }
    }
}
