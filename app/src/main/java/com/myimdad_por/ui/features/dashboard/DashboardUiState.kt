package com.myimdad_por.ui.features.dashboard

import com.myimdad_por.domain.model.Expense
import com.myimdad_por.domain.model.PaymentTransaction
import com.myimdad_por.domain.model.Report
import com.myimdad_por.domain.model.Sale
import java.math.BigDecimal

/**
 * حالة شاشة لوحة التحكم.
 *
 * تُستخدم كـ UI State واحد يجمع:
 * - حالة التحميل والخطأ
 * - ملخص الأرقام
 * - أحدث السجلات
 * - فترة العرض الحالية
 */
data class DashboardUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val selectedPeriod: DashboardPeriod = DashboardPeriod.Today,
    val currencyCode: String = "SDG",

    val salesCount: Int = 0,
    val expensesCount: Int = 0,
    val reportsCount: Int = 0,
    val paymentTransactionsCount: Int = 0,
    val pendingPaymentsCount: Int = 0,
    val paidPaymentsCount: Int = 0,

    val totalSalesAmount: BigDecimal = BigDecimal.ZERO,
    val totalExpensesAmount: BigDecimal = BigDecimal.ZERO,
    val netAmount: BigDecimal = BigDecimal.ZERO,
    val totalPaidAmount: BigDecimal = BigDecimal.ZERO,
    val totalPendingAmount: BigDecimal = BigDecimal.ZERO,

    val recentSales: List<Sale> = emptyList(),
    val recentExpenses: List<Expense> = emptyList(),
    val recentReports: List<Report> = emptyList(),
    val recentTransactions: List<PaymentTransaction> = emptyList(),

    val canAccessReports: Boolean = false,
    val canUsePaidFeatures: Boolean = false,
    val isReadOnlyMode: Boolean = false,
    val lastUpdatedAtMillis: Long? = null
) {
    val hasContent: Boolean
        get() = recentSales.isNotEmpty() ||
            recentExpenses.isNotEmpty() ||
            recentReports.isNotEmpty() ||
            recentTransactions.isNotEmpty() ||
            salesCount > 0 ||
            expensesCount > 0 ||
            reportsCount > 0 ||
            paymentTransactionsCount > 0

    val hasError: Boolean
        get() = !errorMessage.isNullOrBlank()
}

/**
 * فترة العرض في لوحة التحكم.
 */
enum class DashboardPeriod {
    Today,
    Week,
    Month,
    Year,
    AllTime
}
