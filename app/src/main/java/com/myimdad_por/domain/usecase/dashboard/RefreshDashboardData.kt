package com.myimdad_por.domain.usecase.dashboard

import com.myimdad_por.domain.model.Dashboard
import com.myimdad_por.domain.repository.DashboardRepository
import javax.inject.Inject
/**
 * تحديث بيانات لوحة التحكم من المصدر المعتمد ثم إعادة البيانات بشكل اختياري بعد الفلترة.
 */
class RefreshDashboardData  @Inject constructor (
    private val repository: DashboardRepository,
    private val getFilteredDashboardData: GetFilteredDashboardData = GetFilteredDashboardData()
) {

    suspend operator fun invoke(
        forceRefresh: Boolean = true,
        filter: DashboardDataFilter = DashboardDataFilter()
    ): Result<Dashboard> {
        return repository.refreshDashboard().map { dashboard ->
            getFilteredDashboardData(
                dashboard = dashboard,
                filter = filter
            )
        }
    }
}