package com.myimdad_por.domain.usecase.dashboard

import com.myimdad_por.domain.model.Dashboard
import com.myimdad_por.domain.model.DashboardAlert
import com.myimdad_por.domain.model.DashboardAlertLevel
import com.myimdad_por.domain.model.DashboardQuickAction
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * يحول بيانات لوحة التحكم إلى تقرير قابل للتصدير أو العرض أو الحفظ لاحقًا.
 *
 * المخرجات تشمل:
 * - ملخصًا عامًا
 * - أقسامًا منظمة
 * - تمثيلًا نصيًا جاهزًا
 * - تمثيلًا Markdown مناسبًا للتصدير أو المشاركة
 */
class ExportDashboardReport {

    operator fun invoke(
        dashboard: Dashboard?,
        config: DashboardReportConfig = DashboardReportConfig()
    ): DashboardReport {
        if (dashboard == null) {
            return DashboardReport(
                title = config.title,
                generatedAtEpochMillis = System.currentTimeMillis(),
                sections = emptyList(),
                overview = DashboardReportOverview(
                    title = config.title,
                    subtitle = "لا توجد بيانات متاحة للتصدير",
                    totalSections = 0,
                    totalItems = 0,
                    alertCount = 0,
                    criticalAlertCount = 0,
                    quickActionCount = 0
                ),
                plainText = "لا توجد بيانات لوحة تحكم متاحة للتصدير.",
                markdown = "# ${config.title}\n\nلا توجد بيانات لوحة تحكم متاحة للتصدير."
            )
        }

        val sections = buildSections(dashboard, config)
        val totalItems = sections.sumOf { it.items.size }
        val alertCount = dashboard.alerts.size
        val criticalAlertCount = dashboard.alerts.count { it.level == DashboardAlertLevel.Critical }
        val quickActionCount = dashboard.quickActions.count { it.isEnabled }

        val overview = DashboardReportOverview(
            title = dashboard.overview.title,
            subtitle = dashboard.overview.subtitle ?: config.title,
            totalSections = sections.size,
            totalItems = totalItems,
            alertCount = alertCount,
            criticalAlertCount = criticalAlertCount,
            quickActionCount = quickActionCount
        )

        return DashboardReport(
            title = config.title,
            generatedAtEpochMillis = System.currentTimeMillis(),
            sections = sections,
            overview = overview,
            plainText = buildPlainText(config.title, overview, sections),
            markdown = buildMarkdown(config.title, overview, sections)
        )
    }

    private fun buildSections(
        dashboard: Dashboard,
        config: DashboardReportConfig
    ): List<DashboardReportSection> {
        return buildList {
            if (config.includeOverview) {
                add(
                    DashboardReportSection(
                        id = "overview",
                        title = "الملخص العام",
                        items = buildList {
                            add(
                                DashboardReportItem(
                                    label = "العنوان",
                                    value = dashboard.overview.title
                                )
                            )
                            dashboard.overview.subtitle?.takeIf { it.isNotBlank() }?.let {
                                add(DashboardReportItem(label = "الوصف", value = it))
                            }
                            dashboard.overview.greeting?.takeIf { it.isNotBlank() }?.let {
                                add(DashboardReportItem(label = "التحية", value = it))
                            }
                            add(DashboardReportItem(label = "إجمالي المؤشرات", value = dashboard.overview.totalMetrics.toString()))
                            add(DashboardReportItem(label = "المؤشرات الإيجابية", value = dashboard.overview.positiveMetrics.toString()))
                            add(DashboardReportItem(label = "المؤشرات السلبية", value = dashboard.overview.negativeMetrics.toString()))
                        }
                    )
                )
            }

            if (config.includeSales) {
                add(
                    DashboardReportSection(
                        id = "sales",
                        title = "المبيعات",
                        items = buildList {
                            add(DashboardReportItem(label = "مبيعات اليوم", value = dashboard.sales.todaySalesCount.toString()))
                            add(DashboardReportItem(label = "إيراد اليوم", value = dashboard.sales.todayRevenue.clean().toPlainString()))
                            add(DashboardReportItem(label = "مبيعات الشهر", value = dashboard.sales.monthSalesCount.toString()))
                            add(DashboardReportItem(label = "إيراد الشهر", value = dashboard.sales.monthRevenue.clean().toPlainString()))
                            add(DashboardReportItem(label = "الفواتير المعلقة", value = dashboard.sales.pendingInvoicesCount.toString()))
                            add(DashboardReportItem(label = "المرتجعات", value = dashboard.sales.returnsCount.toString()))
                            dashboard.sales.topSellingProductName?.takeIf { it.isNotBlank() }?.let {
                                add(DashboardReportItem(label = "الأكثر مبيعًا", value = it))
                            }
                            dashboard.sales.growthRatePercent?.let {
                                add(DashboardReportItem(label = "نسبة النمو", value = "${it.clean().toPlainString()}%"))
                            }
                        }
                    )
                )
            }

            if (config.includeInventory) {
                add(
                    DashboardReportSection(
                        id = "inventory",
                        title = "المخزون",
                        items = buildList {
                            add(DashboardReportItem(label = "عدد المنتجات", value = dashboard.inventory.productsCount.toString()))
                            add(DashboardReportItem(label = "منخفض المخزون", value = dashboard.inventory.lowStockCount.toString()))
                            add(DashboardReportItem(label = "نفد المخزون", value = dashboard.inventory.outOfStockCount.toString()))
                            add(DashboardReportItem(label = "إجمالي قيمة المخزون", value = dashboard.inventory.totalStockValue.clean().toPlainString()))
                            add(DashboardReportItem(label = "المخزون المحجوز", value = dashboard.inventory.reservedItemsCount.toString()))
                            dashboard.inventory.mostCriticalProductName?.takeIf { it.isNotBlank() }?.let {
                                add(DashboardReportItem(label = "أخطر صنف", value = it))
                            }
                        }
                    )
                )
            }

            if (config.includeCustomers) {
                add(
                    DashboardReportSection(
                        id = "customers",
                        title = "العملاء",
                        items = buildList {
                            add(DashboardReportItem(label = "إجمالي العملاء", value = dashboard.customers.customersCount.toString()))
                            add(DashboardReportItem(label = "العملاء الجدد", value = dashboard.customers.newCustomersCount.toString()))
                            add(DashboardReportItem(label = "العملاء النشطون", value = dashboard.customers.activeCustomersCount.toString()))
                            add(DashboardReportItem(label = "العملاء المستحقون", value = dashboard.customers.dueCustomersCount.toString()))
                            add(DashboardReportItem(label = "متوسط قيمة الطلب", value = dashboard.customers.averageOrderValue.clean().toPlainString()))
                            dashboard.customers.topCustomerName?.takeIf { it.isNotBlank() }?.let {
                                add(DashboardReportItem(label = "أفضل عميل", value = it))
                            }
                        }
                    )
                )
            }

            if (config.includeFinancial) {
                add(
                    DashboardReportSection(
                        id = "financial",
                        title = "المالية",
                        items = buildList {
                            add(DashboardReportItem(label = "الداخل النقدي", value = dashboard.financial.totalCashIn.clean().toPlainString()))
                            add(DashboardReportItem(label = "الخارج النقدي", value = dashboard.financial.totalCashOut.clean().toPlainString()))
                            add(DashboardReportItem(label = "صافي الرصيد", value = dashboard.financial.netBalance.clean().toPlainString()))
                            add(DashboardReportItem(label = "الذمم المدينة", value = dashboard.financial.receivables.clean().toPlainString()))
                            add(DashboardReportItem(label = "الذمم الدائنة", value = dashboard.financial.payables.clean().toPlainString()))
                            dashboard.financial.profitEstimate?.let {
                                add(DashboardReportItem(label = "الربح التقديري", value = it.clean().toPlainString()))
                            }
                            add(DashboardReportItem(label = "العملة", value = dashboard.financial.currencyCode))
                        }
                    )
                )
            }

            if (config.includeAlerts) {
                add(
                    DashboardReportSection(
                        id = "alerts",
                        title = "التنبيهات",
                        items = dashboard.alerts
                            .filter { alert ->
                                config.allowedAlertLevels.isNullOrEmpty() || alert.level in config.allowedAlertLevels
                            }
                            .takeIf { config.maxAlertsCount != null }
                            ?.take(config.maxAlertsCount!!)
                            ?.map { it.toReportItem() }
                            ?: dashboard.alerts
                                .filter { alert ->
                                    config.allowedAlertLevels.isNullOrEmpty() || alert.level in config.allowedAlertLevels
                                }
                                .map { it.toReportItem() }
                    )
                )
            }

            if (config.includeQuickActions) {
                add(
                    DashboardReportSection(
                        id = "quick_actions",
                        title = "الإجراءات السريعة",
                        items = dashboard.quickActions
                            .filter { action -> !config.onlyEnabledActions || action.isEnabled }
                            .takeIf { config.maxQuickActionsCount != null }
                            ?.take(config.maxQuickActionsCount!!)
                            ?.map { it.toReportItem() }
                            ?: dashboard.quickActions
                                .filter { action -> !config.onlyEnabledActions || action.isEnabled }
                                .map { it.toReportItem() }
                    )
                )
            }
        }.filter { it.items.isNotEmpty() || config.keepEmptySections }
    }

    private fun buildPlainText(
        title: String,
        overview: DashboardReportOverview,
        sections: List<DashboardReportSection>
    ): String {
        val lines = mutableListOf<String>()

        lines += title
        lines += "تم الإنشاء: ${System.currentTimeMillis()}"
        lines += "الأقسام: ${overview.totalSections} | العناصر: ${overview.totalItems}"
        lines += "التنبيهات: ${overview.alertCount} | الحرجة: ${overview.criticalAlertCount}"
        lines += "الإجراءات السريعة: ${overview.quickActionCount}"
        lines += ""

        sections.forEach { section ->
            lines += "[$section.title]"
            section.items.forEach { item ->
                lines += "- ${item.label}: ${item.value}"
            }
            lines += ""
        }

        return lines.joinToString("\n").trim()
    }

    private fun buildMarkdown(
        title: String,
        overview: DashboardReportOverview,
        sections: List<DashboardReportSection>
    ): String {
        val builder = StringBuilder()

        builder.appendLine("# $title")
        builder.appendLine()
        builder.appendLine("- الأقسام: ${overview.totalSections}")
        builder.appendLine("- العناصر: ${overview.totalItems}")
        builder.appendLine("- التنبيهات: ${overview.alertCount}")
        builder.appendLine("- التنبيهات الحرجة: ${overview.criticalAlertCount}")
        builder.appendLine("- الإجراءات السريعة: ${overview.quickActionCount}")
        builder.appendLine()

        sections.forEach { section ->
            builder.appendLine("## ${section.title}")
            builder.appendLine()
            section.items.forEach { item ->
                builder.appendLine("- **${item.label}**: ${item.value}")
            }
            builder.appendLine()
        }

        return builder.toString().trim()
    }

    private fun DashboardAlert.toReportItem(): DashboardReportItem {
        return DashboardReportItem(
            label = title,
            value = buildString {
                append(message)
                append(" | المستوى: ")
                append(level.name)
                if (!actionLabel.isNullOrBlank()) {
                    append(" | الإجراء: ")
                    append(actionLabel)
                }
                if (!targetRoute.isNullOrBlank()) {
                    append(" | الوجهة: ")
                    append(targetRoute)
                }
            }
        )
    }

    private fun DashboardQuickAction.toReportItem(): DashboardReportItem {
        return DashboardReportItem(
            label = title,
            value = buildString {
                append(description?.takeIf { it.isNotBlank() } ?: "إجراء سريع")
                if (!route.isNullOrBlank()) {
                    append(" | الوجهة: ")
                    append(route)
                }
                if (!requiresPermission.isNullOrBlank()) {
                    append(" | الصلاحية: ")
                    append(requiresPermission)
                }
            }
        )
    }

    private fun BigDecimal.clean(scale: Int = 2): BigDecimal {
        return setScale(scale, RoundingMode.HALF_UP)
    }
}

/**
 * إعدادات التصدير.
 */
data class DashboardReportConfig(
    val title: String = "تقرير لوحة التحكم",
    val includeOverview: Boolean = true,
    val includeSales: Boolean = true,
    val includeInventory: Boolean = true,
    val includeCustomers: Boolean = true,
    val includeFinancial: Boolean = true,
    val includeAlerts: Boolean = true,
    val includeQuickActions: Boolean = true,
    val allowedAlertLevels: Set<DashboardAlertLevel>? = null,
    val maxAlertsCount: Int? = null,
    val maxQuickActionsCount: Int? = null,
    val onlyEnabledActions: Boolean = true,
    val keepEmptySections: Boolean = false
)

/**
 * النتيجة النهائية الجاهزة للتصدير.
 */
data class DashboardReport(
    val title: String,
    val generatedAtEpochMillis: Long,
    val sections: List<DashboardReportSection>,
    val overview: DashboardReportOverview,
    val plainText: String,
    val markdown: String
) {
    val hasContent: Boolean
        get() = sections.isNotEmpty()
}

/**
 * ملخص التقرير.
 */
data class DashboardReportOverview(
    val title: String = "تقرير لوحة التحكم",
    val subtitle: String? = null,
    val totalSections: Int = 0,
    val totalItems: Int = 0,
    val alertCount: Int = 0,
    val criticalAlertCount: Int = 0,
    val quickActionCount: Int = 0
)

/**
 * قسم داخل التقرير.
 */
data class DashboardReportSection(
    val id: String,
    val title: String,
    val items: List<DashboardReportItem> = emptyList()
) {
    val isEmpty: Boolean
        get() = items.isEmpty()
}

/**
 * عنصر فردي داخل القسم.
 */
data class DashboardReportItem(
    val label: String,
    val value: String
)