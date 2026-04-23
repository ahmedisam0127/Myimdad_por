package com.myimdad_por.domain.usecase.dashboard

import com.myimdad_por.domain.model.Dashboard
import com.myimdad_por.domain.repository.DashboardRepository

/**
 * يضمن وجود بيانات لوحة التحكم في الكاش،
 * ثم يعيد النسخة المطلوبة بعد الفلترة.
 *
 * المنطق:
 * - إذا كان هناك كاش جاهز ولم يكن هناك طلب تحديث إجباري، يتم استخدامه مباشرة.
 * - إذا لم يوجد كاش، أو كان `forceRefresh = true`، يتم جلب البيانات من المصدر عبر المستودع.
 */
class CacheDashboardData(
    private val repository: DashboardRepository,
    private val getFilteredDashboardData: GetFilteredDashboardData = GetFilteredDashboardData()
) {

    suspend operator fun invoke(
        forceRefresh: Boolean = false,
        filter: DashboardDataFilter = DashboardDataFilter()
    ): Result<Dashboard> {
        if (!forceRefresh) {
            val cachedDashboard = repository.getCachedDashboard()
            if (cachedDashboard != null) {
                return Result.success(
                    getFilteredDashboardData(
                        dashboard = cachedDashboard,
                        filter = filter
                    )
                )
            }
        }

        return repository.refreshDashboard().map { dashboard ->
            getFilteredDashboardData(
                dashboard = dashboard,
                filter = filter
            )
        }
    }
}