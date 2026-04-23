package com.myimdad_por.domain.usecase.dashboard

import com.myimdad_por.domain.model.Dashboard
import com.myimdad_por.domain.model.DashboardQuickAction
import com.myimdad_por.domain.model.DashboardAlertLevel

/**
 * مدير ذكي لقطع لوحة التحكم (Widgets).
 *
 * يحول بيانات الـ Dashboard الخام إلى حالة عرض غنية يمكن للواجهة استهلاكها مباشرة:
 * - ترتيب القطع حسب الأهمية
 * - تثبيت بعض القطع في المقدمة
 * - إخفاء القطع الفارغة
 * - اختيار القطع المسموح بها فقط
 * - اقتراح القطع الأكثر فائدة
 */
class ManageDashboardWidgets {

    operator fun invoke(
        dashboard: Dashboard?,
        config: DashboardWidgetConfig = DashboardWidgetConfig()
    ): DashboardWidgetsState {
        if (dashboard == null) {
            return DashboardWidgetsState(
                visibleWidgets = emptyList(),
                pinnedWidgets = emptyList(),
                hiddenWidgets = emptyList(),
                recommendedWidgets = defaultRecommendations(emptyList(), config),
                totalWidgets = 0,
                hasCriticalWidgets = false,
                hasPinnedWidgets = false
            )
        }

        val allWidgets = buildWidgets(dashboard, config)
        val filteredWidgets = allWidgets
            .filter { widget ->
                config.allowedWidgetIds.isNullOrEmpty() || widget.id in config.allowedWidgetIds
            }
            .filter { widget ->
                !config.onlyEnabledWidgets || widget.isEnabled
            }
            .filter { widget ->
                !config.hideEmptyWidgets || !widget.isEmpty()
            }

        val pinned = filteredWidgets
            .filter { it.isPinned }
            .sortedWith(widgetComparator(config))

        val visible = filteredWidgets
            .filterNot { it.isPinned }
            .sortedWith(widgetComparator(config))
            .let { list ->
                if (config.maxVisibleWidgets != null) list.take(config.maxVisibleWidgets) else list
            }

        val visibleCombined = (pinned + visible).distinctBy { it.id }

        val hidden = allWidgets.filterNot { widget -> visibleCombined.any { it.id == widget.id } }

        val recommendations = defaultRecommendations(allWidgets, config)
            .filterNot { recommended -> visibleCombined.any { it.id == recommended.id } }
            .take(config.maxRecommendations)

        return DashboardWidgetsState(
            visibleWidgets = visibleCombined,
            pinnedWidgets = pinned,
            hiddenWidgets = hidden,
            recommendedWidgets = recommendations,
            totalWidgets = allWidgets.size,
            hasCriticalWidgets = allWidgets.any { it.priority >= DashboardWidgetPriority.Critical },
            hasPinnedWidgets = pinned.isNotEmpty(),
            summary = buildSummary(dashboard, allWidgets, visibleCombined)
        )
    }

    private fun buildWidgets(
        dashboard: Dashboard,
        config: DashboardWidgetConfig
    ): List<DashboardWidget> {
        val widgets = buildList {
            add(
                DashboardWidget(
                    id = "overview",
                    title = dashboard.overview.title,
                    description = dashboard.overview.subtitle ?: "ملخص عام للوضع الحالي",
                    type = DashboardWidgetType.Overview,
                    priority = DashboardWidgetPriority.High,
                    isEnabled = dashboard.overview.totalMetrics > 0,
                    isPinned = config.pinnedWidgetIds.contains("overview"),
                    accentKey = "overview"
                )
            )

            add(
                DashboardWidget(
                    id = "sales_today",
                    title = "مبيعات اليوم",
                    description = "عدد المبيعات والإيراد اليومي",
                    type = DashboardWidgetType.Sales,
                    priority = DashboardWidgetPriority.Critical,
                    isEnabled = dashboard.sales.todaySalesCount > 0 || dashboard.sales.todayRevenue > java.math.BigDecimal.ZERO,
                    isPinned = config.pinnedWidgetIds.contains("sales_today"),
                    valueLabel = dashboard.sales.todaySalesCount.toString(),
                    accentKey = "sales"
                )
            )

            add(
                DashboardWidget(
                    id = "sales_month",
                    title = "أداء الشهر",
                    description = "الإيراد الشهري واتجاه النمو",
                    type = DashboardWidgetType.Trend,
                    priority = DashboardWidgetPriority.High,
                    isEnabled = dashboard.sales.monthRevenue > java.math.BigDecimal.ZERO,
                    isPinned = config.pinnedWidgetIds.contains("sales_month"),
                    accentKey = "month_revenue"
                )
            )

            add(
                DashboardWidget(
                    id = "inventory",
                    title = "المخزون",
                    description = "المنتجات والتنبيهات المرتبطة بالمخزون",
                    type = DashboardWidgetType.Inventory,
                    priority = if (dashboard.inventory.hasStockRisk) DashboardWidgetPriority.Critical else DashboardWidgetPriority.High,
                    isEnabled = dashboard.inventory.productsCount > 0,
                    isPinned = config.pinnedWidgetIds.contains("inventory"),
                    accentKey = "inventory"
                )
            )

            add(
                DashboardWidget(
                    id = "customers",
                    title = "العملاء",
                    description = "إجمالي العملاء والجدد والنشطين",
                    type = DashboardWidgetType.Customers,
                    priority = DashboardWidgetPriority.Normal,
                    isEnabled = dashboard.customers.customersCount > 0,
                    isPinned = config.pinnedWidgetIds.contains("customers"),
                    accentKey = "customers"
                )
            )

            add(
                DashboardWidget(
                    id = "financial",
                    title = "المالية",
                    description = "صافي الرصيد والذمم والربح التقديري",
                    type = DashboardWidgetType.Financial,
                    priority = DashboardWidgetPriority.Critical,
                    isEnabled = true,
                    isPinned = config.pinnedWidgetIds.contains("financial"),
                    accentKey = "finance"
                )
            )

            add(
                DashboardWidget(
                    id = "alerts",
                    title = "التنبيهات",
                    description = "التنبيهات الحرجة والمهمة",
                    type = DashboardWidgetType.Alerts,
                    priority = when {
                        dashboard.alerts.any { it.level == DashboardAlertLevel.Critical } -> DashboardWidgetPriority.Critical
                        dashboard.alerts.any { it.level == DashboardAlertLevel.Warning } -> DashboardWidgetPriority.High
                        else -> DashboardWidgetPriority.Normal
                    },
                    isEnabled = dashboard.alerts.isNotEmpty(),
                    isPinned = config.pinnedWidgetIds.contains("alerts"),
                    accentKey = "alerts"
                )
            )

            add(
                DashboardWidget(
                    id = "quick_actions",
                    title = "إجراءات سريعة",
                    description = "الاختصارات الأكثر استخدامًا",
                    type = DashboardWidgetType.QuickActions,
                    priority = DashboardWidgetPriority.Normal,
                    isEnabled = dashboard.quickActions.isNotEmpty(),
                    isPinned = config.pinnedWidgetIds.contains("quick_actions"),
                    accentKey = "actions"
                )
            )

            dashboard.sales.topSellingProductName?.let { topProduct ->
                add(
                    DashboardWidget(
                        id = "top_product",
                        title = "الأكثر مبيعًا",
                        description = topProduct,
                        type = DashboardWidgetType.Highlight,
                        priority = DashboardWidgetPriority.Normal,
                        isEnabled = true,
                        isPinned = config.pinnedWidgetIds.contains("top_product"),
                        accentKey = "top_product"
                    )
                )
            }

            dashboard.customers.topCustomerName?.let { topCustomer ->
                add(
                    DashboardWidget(
                        id = "top_customer",
                        title = "أفضل عميل",
                        description = topCustomer,
                        type = DashboardWidgetType.Highlight,
                        priority = DashboardWidgetPriority.Normal,
                        isEnabled = true,
                        isPinned = config.pinnedWidgetIds.contains("top_customer"),
                        accentKey = "top_customer"
                    )
                )
            }

            dashboard.inventory.mostCriticalProductName?.let { criticalProduct ->
                add(
                    DashboardWidget(
                        id = "critical_stock",
                        title = "أخطر صنف",
                        description = criticalProduct,
                        type = DashboardWidgetType.Risk,
                        priority = DashboardWidgetPriority.Critical,
                        isEnabled = dashboard.inventory.hasStockRisk,
                        isPinned = config.pinnedWidgetIds.contains("critical_stock"),
                        accentKey = "critical_stock"
                    )
                )
            }
        }

        return if (config.searchQuery.isNullOrBlank()) {
            widgets
        } else {
            widgets.filter { widget ->
                widget.title.contains(config.searchQuery, ignoreCase = true) ||
                    (widget.description?.contains(config.searchQuery, ignoreCase = true) == true)
            }
        }
    }

    private fun defaultRecommendations(
        widgets: List<DashboardWidget>,
        config: DashboardWidgetConfig
    ): List<DashboardWidget> {
        return widgets
            .filter { widget -> widget.isEnabled }
            .filter { widget -> config.allowedWidgetIds.isNullOrEmpty() || widget.id in config.allowedWidgetIds }
            .sortedWith(
                compareByDescending<DashboardWidget> { it.isPinned }
                    .thenByDescending { it.priority.rank }
                    .thenBy { it.title }
            )
    }

    private fun widgetComparator(config: DashboardWidgetConfig): Comparator<DashboardWidget> {
        return compareByDescending<DashboardWidget> { it.isPinned }
            .thenByDescending { it.priority.rank }
            .thenBy { it.title }
            .let { comparator ->
                if (config.compactMode) {
                    comparator.thenByDescending { it.type.compactWeight }
                } else {
                    comparator
                }
            }
    }

    private fun buildSummary(
        dashboard: Dashboard,
        allWidgets: List<DashboardWidget>,
        visibleWidgets: List<DashboardWidget>
    ): DashboardWidgetsSummary {
        return DashboardWidgetsSummary(
            title = dashboard.overview.title,
            subtitle = dashboard.overview.subtitle,
            totalMetrics = dashboard.overview.totalMetrics,
            visibleWidgetsCount = visibleWidgets.size,
            hiddenWidgetsCount = (allWidgets.size - visibleWidgets.size).coerceAtLeast(0),
            criticalWidgetsCount = allWidgets.count { it.priority == DashboardWidgetPriority.Critical },
            enabledWidgetsCount = allWidgets.count { it.isEnabled },
            disabledWidgetsCount = allWidgets.count { !it.isEnabled }
        )
    }
}

/**
 * إعدادات التحكم في القطع.
 */
data class DashboardWidgetConfig(
    val allowedWidgetIds: Set<String>? = null,
    val pinnedWidgetIds: Set<String> = emptySet(),
    val searchQuery: String? = null,
    val hideEmptyWidgets: Boolean = true,
    val onlyEnabledWidgets: Boolean = true,
    val maxVisibleWidgets: Int? = null,
    val maxRecommendations: Int = 3,
    val compactMode: Boolean = false
)

/**
 * حالة جاهزة للعرض في الواجهة.
 */
data class DashboardWidgetsState(
    val visibleWidgets: List<DashboardWidget> = emptyList(),
    val pinnedWidgets: List<DashboardWidget> = emptyList(),
    val hiddenWidgets: List<DashboardWidget> = emptyList(),
    val recommendedWidgets: List<DashboardWidget> = emptyList(),
    val totalWidgets: Int = 0,
    val hasCriticalWidgets: Boolean = false,
    val hasPinnedWidgets: Boolean = false,
    val summary: DashboardWidgetsSummary = DashboardWidgetsSummary()
)

/**
 * ملخص سريع لقطع اللوحة.
 */
data class DashboardWidgetsSummary(
    val title: String = "لوحة القطع",
    val subtitle: String? = null,
    val totalMetrics: Int = 0,
    val visibleWidgetsCount: Int = 0,
    val hiddenWidgetsCount: Int = 0,
    val criticalWidgetsCount: Int = 0,
    val enabledWidgetsCount: Int = 0,
    val disabledWidgetsCount: Int = 0
)

/**
 * قطعة لوحة تحكم واحدة.
 */
data class DashboardWidget(
    val id: String,
    val title: String,
    val description: String? = null,
    val type: DashboardWidgetType,
    val priority: DashboardWidgetPriority = DashboardWidgetPriority.Normal,
    val isEnabled: Boolean = true,
    val isPinned: Boolean = false,
    val valueLabel: String? = null,
    val accentKey: String? = null
) {
    fun isEmpty(): Boolean {
        return !isEnabled || (title.isBlank() && description.isNullOrBlank() && valueLabel.isNullOrBlank())
    }
}

/**
 * أنواع القطع الممكنة.
 */
enum class DashboardWidgetType(val compactWeight: Int) {
    Overview(100),
    Sales(95),
    Trend(90),
    Inventory(85),
    Customers(75),
    Financial(80),
    Alerts(98),
    QuickActions(60),
    Highlight(70),
    Risk(96)
}

/**
 * أولوية العرض.
 */
enum class DashboardWidgetPriority(val rank: Int) {
    Low(1),
    Normal(2),
    High(3),
    Critical(4)
}

/**
 * ربط اختياري بين الإجراء السريع والقطعة، إن احتجته لاحقًا في الواجهة.
 */
data class DashboardWidgetAction(
    val widgetId: String,
    val quickAction: DashboardQuickAction
)