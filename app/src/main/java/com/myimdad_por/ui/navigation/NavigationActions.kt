package com.myimdad_por.ui.navigation

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

/**
 * Centralized navigation actions to keep screen navigation clean and consistent.
 */
class NavigationActions(
    private val navController: NavHostController
) {

    fun navigateToSplash() = navController.navigateSingleTopTo(ScreenRoutes.Splash)
    fun navigateToOnboarding() = navController.navigateSingleTopTo(ScreenRoutes.Onboarding)
    fun navigateToLogin() = navController.navigateSingleTopTo(ScreenRoutes.Login)
    fun navigateToRegister() = navController.navigateSingleTopTo(ScreenRoutes.Register)
    fun navigateToForgotPassword() = navController.navigateSingleTopTo(ScreenRoutes.ForgotPassword)

    fun navigateToHome() = navController.navigateSingleTopTo(ScreenRoutes.Home)
    fun navigateToDashboard() = navController.navigateSingleTopTo(ScreenRoutes.Dashboard)
    fun navigateToProfile() = navController.navigateSingleTopTo(ScreenRoutes.Profile)
    fun navigateToSettings() = navController.navigateSingleTopTo(ScreenRoutes.Settings)
    fun navigateToNotifications() = navController.navigateSingleTopTo(ScreenRoutes.Notifications)
    fun navigateToSearch() = navController.navigateSingleTopTo(ScreenRoutes.Search)

    fun navigateToCustomers() = navController.navigateSingleTopTo(ScreenRoutes.Customers)
    fun navigateToCustomerCreate() = navController.navigateSingleTopTo(ScreenRoutes.CustomerCreate)
    fun navigateToCustomerDetails(id: String) = navController.navigate(ScreenRoutes.customerDetails(id))

    fun navigateToSuppliers() = navController.navigateSingleTopTo(ScreenRoutes.Suppliers)
    fun navigateToSupplierCreate() = navController.navigateSingleTopTo(ScreenRoutes.SupplierCreate)
    fun navigateToSupplierDetails(id: String) = navController.navigate(ScreenRoutes.supplierDetails(id))

    fun navigateToProducts() = navController.navigateSingleTopTo(ScreenRoutes.Products)
    fun navigateToProductCreate() = navController.navigateSingleTopTo(ScreenRoutes.ProductCreate)
    fun navigateToProductDetails(id: String) = navController.navigate(ScreenRoutes.productDetails(id))

    fun navigateToInventory() = navController.navigateSingleTopTo(ScreenRoutes.Inventory)
    fun navigateToStockMovement() = navController.navigateSingleTopTo(ScreenRoutes.StockMovement)

    fun navigateToInvoices() = navController.navigateSingleTopTo(ScreenRoutes.Invoices)
    fun navigateToInvoiceCreate() = navController.navigateSingleTopTo(ScreenRoutes.InvoiceCreate)
    fun navigateToInvoiceDetails(id: String) = navController.navigate(ScreenRoutes.invoiceDetails(id))

    fun navigateToPurchases() = navController.navigateSingleTopTo(ScreenRoutes.Purchases)
    fun navigateToPurchaseCreate() = navController.navigateSingleTopTo(ScreenRoutes.PurchaseCreate)
    fun navigateToPurchaseDetails(id: String) = navController.navigate(ScreenRoutes.purchaseDetails(id))

    fun navigateToSales() = navController.navigateSingleTopTo(ScreenRoutes.Sales)
    fun navigateToSaleCreate() = navController.navigateSingleTopTo(ScreenRoutes.SaleCreate)
    fun navigateToSaleDetails(id: String) = navController.navigate(ScreenRoutes.saleDetails(id))

    fun navigateToPayments() = navController.navigateSingleTopTo(ScreenRoutes.Payments)
    fun navigateToPaymentCreate() = navController.navigateSingleTopTo(ScreenRoutes.PaymentCreate)
    fun navigateToPaymentDetails(id: String) = navController.navigate(ScreenRoutes.paymentDetails(id))

    fun navigateToReturns() = navController.navigateSingleTopTo(ScreenRoutes.Returns)
    fun navigateToReturnCreate() = navController.navigateSingleTopTo(ScreenRoutes.ReturnCreate)
    fun navigateToReturnDetails(id: String) = navController.navigate(ScreenRoutes.returnDetails(id))

    fun navigateToExpenses() = navController.navigateSingleTopTo(ScreenRoutes.Expenses)
    fun navigateToExpenseCreate() = navController.navigateSingleTopTo(ScreenRoutes.ExpenseCreate)
    fun navigateToExpenseDetails(id: String) = navController.navigate(ScreenRoutes.expenseDetails(id))

    fun navigateToReports() = navController.navigateSingleTopTo(ScreenRoutes.Reports)
    fun navigateToAnalytics() = navController.navigateSingleTopTo(ScreenRoutes.Analytics)
    fun navigateToAuditLogs() = navController.navigateSingleTopTo(ScreenRoutes.AuditLogs)

    fun navigateToSubscription() = navController.navigateSingleTopTo(ScreenRoutes.Subscription)
    fun navigateToSubscriptionExpired() = navController.navigateSingleTopTo(ScreenRoutes.SubscriptionExpired)
    fun navigateToSubscriptionRenew() = navController.navigateSingleTopTo(ScreenRoutes.SubscriptionRenew)

    fun navigateBack() {
        navController.popBackStack()
    }

    fun navigateBackTo(route: String, inclusive: Boolean = false) {
        navController.popBackStack(route, inclusive)
    }

    fun clearBackStackAndGoToHome() {
        navController.navigate(ScreenRoutes.Home) {
            popUpTo(0)
            launchSingleTop = true
            restoreState = false
        }
    }

    private fun NavHostController.navigateSingleTopTo(route: String) {
        navigate(route) {
            launchSingleTop = true
            restoreState = true
            popUpTo(graph.findStartDestination().id) {
                saveState = true
            }
        }
    }
}