package com.myimdad_por.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.myimdad_por.core.dispatchers.AppDispatchers
import com.myimdad_por.core.network.NetworkResult
import com.myimdad_por.data.local.dao.DashboardDao
import com.myimdad_por.data.local.entity.DashboardCacheEntity
import com.myimdad_por.data.mapper.toDomain
import com.myimdad_por.data.remote.datasource.DashboardRemoteDataSource
import com.myimdad_por.domain.model.Dashboard
import com.myimdad_por.domain.model.DashboardAlert
import com.myimdad_por.domain.model.DashboardQuickAction
import com.myimdad_por.domain.repository.DashboardRepository as DashboardRepositoryContract
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import javax.inject.Inject

class DashboardRepositoryImpl @Inject constructor(
    private val remoteDataSource: DashboardRemoteDataSource,
    private val dashboardDao: DashboardDao,
    private val dispatchers: AppDispatchers
) : DashboardRepositoryContract {

    private val gson = Gson()
    private val alertListType = object : TypeToken<List<DashboardAlert>>() {}.type
    private val quickActionListType = object : TypeToken<List<DashboardQuickAction>>() {}.type

    override suspend fun getDashboard(forceRefresh: Boolean): Result<Dashboard> {
        if (!forceRefresh) {
            getCachedDashboard()?.let { cached ->
                return Result.success(cached)
            }
        }
        return fetchAndCacheDashboard()
    }

    override suspend fun refreshDashboard(): Result<Dashboard> {
        return fetchAndCacheDashboard()
    }

    override fun observeDashboard(): Flow<Dashboard> {
        return dashboardDao.observeCachedDashboard()
            .map { entity -> entity?.toDomain() ?: Dashboard() }
            .flowOn(dispatchers.io)
    }

    override suspend fun getCachedDashboard(): Dashboard? {
        return withContext(dispatchers.io) {
            dashboardDao.getCachedDashboard()?.toDomain()
        }
    }

    override suspend fun markAlertAsRead(alertId: String): Result<Unit> {
        if (alertId.isBlank()) {
            return Result.failure(IllegalArgumentException("alertId must not be blank"))
        }

        return withContext(dispatchers.io) {
            val cached = dashboardDao.getCachedDashboard() ?: return@withContext Result.success(Unit)
            val currentAlerts = cached.alertsJson.decodeAlerts()
            if (currentAlerts.none { it.id == alertId }) {
                return@withContext Result.success(Unit)
            }
            Result.success(Unit)
        }
    }

    override suspend fun dismissAlert(alertId: String): Result<Unit> {
        if (alertId.isBlank()) {
            return Result.failure(IllegalArgumentException("alertId must not be blank"))
        }

        return withContext(dispatchers.io) {
            val cached = dashboardDao.getCachedDashboard() ?: return@withContext Result.success(Unit)
            val currentDashboard = cached.toDomain()
            val updatedDashboard = currentDashboard.copy(
                alerts = currentDashboard.alerts.filterNot { it.id == alertId }
            )
            dashboardDao.upsertCachedDashboard(updatedDashboard.toCacheEntity())
            Result.success(Unit)
        }
    }

    private suspend fun fetchAndCacheDashboard(): Result<Dashboard> {
        return when (val result = remoteDataSource.getDashboard()) {
            is NetworkResult.Success -> {
                val dashboard = result.data.toDomain()
                cacheDashboard(dashboard)
                Result.success(dashboard)
            }

            is NetworkResult.Error -> {
                val cached = getCachedDashboard()
                if (cached != null) {
                    Result.success(cached)
                } else {
                    Result.failure(result.exception)
                }
            }

            NetworkResult.Loading -> {
                Result.failure(IllegalStateException("Unexpected loading state"))
            }
        }
    }

    private suspend fun cacheDashboard(dashboard: Dashboard) {
        withContext(dispatchers.io) {
            dashboardDao.upsertCachedDashboard(dashboard.toCacheEntity())
        }
    }

    private fun DashboardCacheEntity.toDomain(): Dashboard {
        return Dashboard(
            overview = com.myimdad_por.domain.model.DashboardOverview(
                title = overviewTitle,
                subtitle = overviewSubtitle,
                greeting = overviewGreeting,
                totalMetrics = overviewTotalMetrics,
                positiveMetrics = overviewPositiveMetrics,
                negativeMetrics = overviewNegativeMetrics
            ),
            sales = com.myimdad_por.domain.model.DashboardSalesSummary(
                todaySalesCount = salesTodaySalesCount,
                todayRevenue = salesTodayRevenue.toBigDecimalSafe(),
                monthSalesCount = salesMonthSalesCount,
                monthRevenue = salesMonthRevenue.toBigDecimalSafe(),
                pendingInvoicesCount = salesPendingInvoicesCount,
                returnsCount = salesReturnsCount,
                topSellingProductName = salesTopSellingProductName,
                growthRatePercent = salesGrowthRatePercent?.toBigDecimalSafe()
            ),
            inventory = com.myimdad_por.domain.model.DashboardInventorySummary(
                productsCount = inventoryProductsCount,
                lowStockCount = inventoryLowStockCount,
                outOfStockCount = inventoryOutOfStockCount,
                totalStockValue = inventoryTotalStockValue.toBigDecimalSafe(),
                reservedItemsCount = inventoryReservedItemsCount,
                mostCriticalProductName = inventoryMostCriticalProductName
            ),
            customers = com.myimdad_por.domain.model.DashboardCustomerSummary(
                customersCount = customersCount,
                newCustomersCount = customersNewCustomersCount,
                activeCustomersCount = customersActiveCustomersCount,
                dueCustomersCount = customersDueCustomersCount,
                topCustomerName = customersTopCustomerName,
                averageOrderValue = customersAverageOrderValue.toBigDecimalSafe()
            ),
            financial = com.myimdad_por.domain.model.DashboardFinancialSummary(
                totalCashIn = financialTotalCashIn.toBigDecimalSafe(),
                totalCashOut = financialTotalCashOut.toBigDecimalSafe(),
                netBalance = financialNetBalance.toBigDecimalSafe(),
                receivables = financialReceivables.toBigDecimalSafe(),
                payables = financialPayables.toBigDecimalSafe(),
                profitEstimate = financialProfitEstimate?.toBigDecimalSafe(),
                currencyCode = financialCurrencyCode.ifBlank { "---" }
            ),
            alerts = alertsJson.decodeAlerts(),
            quickActions = quickActionsJson.decodeQuickActions(),
            lastUpdatedAtEpochMillis = lastUpdatedAtEpochMillis
        )
    }

    private fun Dashboard.toCacheEntity(): DashboardCacheEntity {
        return DashboardCacheEntity(
            id = 0,
            overviewTitle = overview.title.ifBlank { "لوحة التحكم" },
            overviewSubtitle = overview.subtitle,
            overviewGreeting = overview.greeting,
            overviewTotalMetrics = overview.totalMetrics,
            overviewPositiveMetrics = overview.positiveMetrics,
            overviewNegativeMetrics = overview.negativeMetrics,
            salesTodaySalesCount = sales.todaySalesCount,
            salesTodayRevenue = sales.todayRevenue.toPlainStringSafe(),
            salesMonthSalesCount = sales.monthSalesCount,
            salesMonthRevenue = sales.monthRevenue.toPlainStringSafe(),
            salesPendingInvoicesCount = sales.pendingInvoicesCount,
            salesReturnsCount = sales.returnsCount,
            salesTopSellingProductName = sales.topSellingProductName,
            salesGrowthRatePercent = sales.growthRatePercent?.toPlainStringSafe(),
            inventoryProductsCount = inventory.productsCount,
            inventoryLowStockCount = inventory.lowStockCount,
            inventoryOutOfStockCount = inventory.outOfStockCount,
            inventoryTotalStockValue = inventory.totalStockValue.toPlainStringSafe(),
            inventoryReservedItemsCount = inventory.reservedItemsCount,
            inventoryMostCriticalProductName = inventory.mostCriticalProductName,
            customersCount = customers.customersCount,
            customersNewCustomersCount = customers.newCustomersCount,
            customersActiveCustomersCount = customers.activeCustomersCount,
            customersDueCustomersCount = customers.dueCustomersCount,
            customersTopCustomerName = customers.topCustomerName,
            customersAverageOrderValue = customers.averageOrderValue.toPlainStringSafe(),
            financialTotalCashIn = financial.totalCashIn.toPlainStringSafe(),
            financialTotalCashOut = financial.totalCashOut.toPlainStringSafe(),
            financialNetBalance = financial.netBalance.toPlainStringSafe(),
            financialReceivables = financial.receivables.toPlainStringSafe(),
            financialPayables = financial.payables.toPlainStringSafe(),
            financialProfitEstimate = financial.profitEstimate?.toPlainStringSafe(),
            financialCurrencyCode = financial.currencyCode.ifBlank { "---" },
            alertsJson = gson.toJson(alerts),
            quickActionsJson = gson.toJson(quickActions),
            lastUpdatedAtEpochMillis = lastUpdatedAtEpochMillis
        )
    }

    private fun String?.decodeAlerts(): List<DashboardAlert> {
        if (this.isNullOrBlank()) return emptyList()
        return runCatching {
            val decoded: List<DashboardAlert>? = gson.fromJson(this, alertListType)
            decoded.orEmpty()
        }.getOrDefault(emptyList())
    }

    private fun String?.decodeQuickActions(): List<DashboardQuickAction> {
        if (this.isNullOrBlank()) return emptyList()
        return runCatching {
            val decoded: List<DashboardQuickAction>? = gson.fromJson(this, quickActionListType)
            decoded.orEmpty()
        }.getOrDefault(emptyList())
    }

    private fun String.toBigDecimalSafe(): BigDecimal {
        return runCatching { BigDecimal(this) }.getOrDefault(BigDecimal.ZERO)
    }

    private fun BigDecimal.toPlainStringSafe(): String {
        return toPlainString()
    }
}