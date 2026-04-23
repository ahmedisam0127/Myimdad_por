package com.myimdad_por.ui.features.dashboard

/**
 * أحداث واجهة المستخدم في شاشة Dashboard.
 *
 * تُرسل من الـ UI إلى الـ ViewModel للتفاعل مع المستخدم.
 */
sealed interface DashboardUiEvent {

    /** تحميل أولي */
    data object Load : DashboardUiEvent

    /** تحديث (Pull to refresh) */
    data object Refresh : DashboardUiEvent

    /** تغيير الفترة (اليوم / أسبوع / شهر...) */
    data class ChangePeriod(val period: DashboardPeriod) : DashboardUiEvent

    /** الضغط على عملية بيع */
    data class OnSaleClick(val saleId: String) : DashboardUiEvent

    /** الضغط على مصروف */
    data class OnExpenseClick(val expenseId: String) : DashboardUiEvent

    /** الضغط على تقرير */
    data class OnReportClick(val reportId: String) : DashboardUiEvent

    /** الضغط على معاملة دفع */
    data class OnTransactionClick(val transactionId: String) : DashboardUiEvent

    /** الانتقال إلى شاشة المبيعات */
    data object NavigateToSales : DashboardUiEvent

    /** الانتقال إلى شاشة المصروفات */
    data object NavigateToExpenses : DashboardUiEvent

    /** الانتقال إلى شاشة التقارير */
    data object NavigateToReports : DashboardUiEvent

    /** إعادة المحاولة بعد خطأ */
    data object Retry : DashboardUiEvent
}
