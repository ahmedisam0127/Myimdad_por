package com.myimdad_por.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.myimdad_por.core.dispatchers.AppDispatchers
import com.myimdad_por.core.network.ApiException
import com.myimdad_por.core.network.NetworkResult
import com.myimdad_por.core.network.NetworkUnavailableException
import com.myimdad_por.core.network.RequestTimeoutException
import com.myimdad_por.data.local.dao.DashboardDao
import com.myimdad_por.data.local.entity.DashboardCacheEntity
import com.myimdad_por.data.remote.datasource.DashboardRemoteDataSource
import com.myimdad_por.data.remote.dto.DashboardAlertDto
import com.myimdad_por.data.remote.dto.DashboardDto
import com.myimdad_por.data.remote.dto.DashboardQuickActionDto
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Worker لمزامنة Dashboard مع الكاش المحلي.
 *
 * الفكرة:
 * - يجلب البيانات من الشبكة.
 * - يحولها إلى كاش محلي ثابت الصف.
 * - يعيد Retry فقط للأخطاء القابلة لإعادة المحاولة.
 */
@HiltWorker
class DashboardSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val remoteDataSource: DashboardRemoteDataSource,
    private val dashboardDao: DashboardDao,
    private val dispatchers: AppDispatchers
) : CoroutineWorker(appContext, workerParams) {

    private val gson = Gson()

    override suspend fun doWork(): Result = withContext(dispatchers.io) {
        try {
            when (val result = remoteDataSource.getDashboard(
                storeId = inputData.getString(KEY_STORE_ID),
                branchId = inputData.getString(KEY_BRANCH_ID),
                currency = inputData.getString(KEY_CURRENCY),
                timezone = inputData.getString(KEY_TIMEZONE)
            )) {
                is NetworkResult.Success -> {
                    dashboardDao.upsertCachedDashboard(
                        result.data.toCacheEntity(gson)
                    )
                    Result.success()
                }

                is NetworkResult.Error -> result.exception.toWorkResult()

                NetworkResult.Loading -> Result.retry()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            t.toApiException().toWorkResult()
        }
    }

    private fun ApiException.toWorkResult(): Result {
        return when (this) {
            is NetworkUnavailableException,
            is RequestTimeoutException -> Result.retry()
            else -> Result.failure()
        }
    }

    private fun Throwable.toApiException(): ApiException {
        return when (this) {
            is ApiException -> this
            is SocketTimeoutException -> RequestTimeoutException(cause = this)
            is UnknownHostException -> NetworkUnavailableException(cause = this)
            else -> ApiException.unexpected(
                message = message ?: "حدث خطأ غير متوقع أثناء مزامنة لوحة التحكم",
                cause = this
            )
        }
    }

    private fun DashboardDto.toCacheEntity(gson: Gson): DashboardCacheEntity {
        val overviewDto = overview
        val salesDto = sales
        val inventoryDto = inventory
        val customersDto = customers
        val financialDto = financial

        return DashboardCacheEntity(
            id = 0,

            overviewTitle = overviewDto?.title.orDefault("لوحة التحكم"),
            overviewSubtitle = overviewDto?.subtitle,
            overviewGreeting = overviewDto?.greeting,
            overviewTotalMetrics = overviewDto?.totalMetrics.orZero(),
            overviewPositiveMetrics = overviewDto?.positiveMetrics.orZero(),
            overviewNegativeMetrics = overviewDto?.negativeMetrics.orZero(),

            salesTodaySalesCount = salesDto?.todaySalesCount.orZero(),
            salesTodayRevenue = salesDto?.todayRevenue.toMoneyString(),
            salesMonthSalesCount = salesDto?.monthSalesCount.orZero(),
            salesMonthRevenue = salesDto?.monthRevenue.toMoneyString(),
            salesPendingInvoicesCount = salesDto?.pendingInvoicesCount.orZero(),
            salesReturnsCount = salesDto?.returnsCount.orZero(),
            salesTopSellingProductName = salesDto?.topSellingProductName,
            salesGrowthRatePercent = salesDto?.growthRatePercent.toOptionalDecimalString(),

            inventoryProductsCount = inventoryDto?.productsCount.orZero(),
            inventoryLowStockCount = inventoryDto?.lowStockCount.orZero(),
            inventoryOutOfStockCount = inventoryDto?.outOfStockCount.orZero(),
            inventoryTotalStockValue = inventoryDto?.totalStockValue.toMoneyString(),
            inventoryReservedItemsCount = inventoryDto?.reservedItemsCount.orZero(),
            inventoryMostCriticalProductName = inventoryDto?.mostCriticalProductName,

            customersCount = customersDto?.customersCount.orZero(),
            customersNewCustomersCount = customersDto?.newCustomersCount.orZero(),
            customersActiveCustomersCount = customersDto?.activeCustomersCount.orZero(),
            customersDueCustomersCount = customersDto?.dueCustomersCount.orZero(),
            customersTopCustomerName = customersDto?.topCustomerName,
            customersAverageOrderValue = customersDto?.averageOrderValue.toMoneyString(),

            financialTotalCashIn = financialDto?.totalCashIn.toMoneyString(),
            financialTotalCashOut = financialDto?.totalCashOut.toMoneyString(),
            financialNetBalance = financialDto?.netBalance.toMoneyString(),
            financialReceivables = financialDto?.receivables.toMoneyString(),
            financialPayables = financialDto?.payables.toMoneyString(),
            financialProfitEstimate = financialDto?.profitEstimate.toOptionalMoneyString(),
            financialCurrencyCode = financialDto?.currencyCode.orDefault("SDG"),

            alertsJson = gson.toJson(
                alerts.orEmpty().map { it.toSnapshot() }
            ),
            quickActionsJson = gson.toJson(
                quickActions.orEmpty().map { it.toSnapshot() }
            ),

            lastUpdatedAtEpochMillis = lastUpdatedAtEpochMillis
        )
    }

    private fun DashboardAlertDto.toSnapshot(): DashboardAlertSnapshot {
        return DashboardAlertSnapshot(
            id = id.orEmpty(),
            title = title.orDefault("تنبيه"),
            message = message.orDefault(""),
            level = normalizeAlertLevel(level),
            actionLabel = actionLabel,
            targetRoute = targetRoute,
            isDismissible = isDismissible != false
        )
    }

    private fun DashboardQuickActionDto.toSnapshot(): DashboardQuickActionSnapshot {
        return DashboardQuickActionSnapshot(
            id = id.orEmpty(),
            title = title.orDefault("إجراء سريع"),
            description = description,
            iconName = iconName,
            route = route,
            isEnabled = isEnabled != false,
            requiresPermission = requiresPermission
        )
    }

    private fun normalizeAlertLevel(rawLevel: String?): String {
        return when (rawLevel?.trim()?.lowercase()) {
            "critical" -> "Critical"
            "warning" -> "Warning"
            "info" -> "Info"
            else -> "Info"
        }
    }

    private fun String?.orDefault(defaultValue: String): String {
        return this?.trim().takeUnless { it.isNullOrBlank() } ?: defaultValue
    }

    private fun Int?.orZero(): Int = this ?: 0

    private fun String?.toMoneyString(): String {
        return runCatching {
            val value = this?.trim().orEmpty()
            if (value.isBlank()) "0.00"
            else BigDecimal(value).setScale(2, RoundingMode.HALF_UP).toPlainString()
        }.getOrDefault("0.00")
    }

    private fun String?.toOptionalMoneyString(): String? {
        val value = this?.trim().orEmpty()
        if (value.isBlank()) return null
        return runCatching {
            BigDecimal(value).setScale(2, RoundingMode.HALF_UP).toPlainString()
        }.getOrNull()
    }

    private fun String?.toOptionalDecimalString(): String? {
        val value = this?.trim().orEmpty()
        if (value.isBlank()) return null
        return runCatching {
            BigDecimal(value).stripTrailingZeros().toPlainString()
        }.getOrNull()
    }

    companion object {
        const val KEY_STORE_ID = "dashboard_store_id"
        const val KEY_BRANCH_ID = "dashboard_branch_id"
        const val KEY_CURRENCY = "dashboard_currency"
        const val KEY_TIMEZONE = "dashboard_timezone"
    }
}

private data class DashboardAlertSnapshot(
    val id: String,
    val title: String,
    val message: String,
    val level: String,
    val actionLabel: String? = null,
    val targetRoute: String? = null,
    val isDismissible: Boolean = true
)

private data class DashboardQuickActionSnapshot(
    val id: String,
    val title: String,
    val description: String? = null,
    val iconName: String? = null,
    val route: String? = null,
    val isEnabled: Boolean = true,
    val requiresPermission: String? = null
)