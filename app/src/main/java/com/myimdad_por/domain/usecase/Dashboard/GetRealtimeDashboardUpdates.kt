package com.myimdad_por.domain.usecase.dashboard

import com.myimdad_por.domain.model.Dashboard
import com.myimdad_por.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * يراقب التحديثات اللحظية لبيانات لوحة التحكم من المستودع،
 * ثم يمررها عبر الفلترة المطلوبة قبل إظهارها لطبقة الواجهة.
 */
class GetRealtimeDashboardUpdates(
    private val repository: DashboardRepository,
    private val getFilteredDashboardData: GetFilteredDashboardData = GetFilteredDashboardData()
) {

    operator fun invoke(
        filter: DashboardDataFilter = DashboardDataFilter()
    ): Flow<Dashboard> {
        return repository.observeDashboard().map { dashboard ->
            getFilteredDashboardData(
                dashboard = dashboard,
                filter = filter
            )
        }
    }
}