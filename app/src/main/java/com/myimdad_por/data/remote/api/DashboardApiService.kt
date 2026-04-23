package com.myimdad_por.data.remote.api

import com.myimdad_por.data.remote.dto.DashboardAnalyticsDto
import com.myimdad_por.data.remote.dto.DashboardDto
import com.myimdad_por.data.remote.dto.DashboardSummaryDto
import retrofit2.http.GET
import retrofit2.http.Query

interface DashboardApiService {

    @GET("dashboard")
    suspend fun getDashboard(
        @Query("store_id") storeId: String? = null,
        @Query("branch_id") branchId: String? = null,
        @Query("currency") currency: String? = null,
        @Query("timezone") timezone: String? = null
    ): DashboardDto

    @GET("dashboard/summary")
    suspend fun getDashboardSummary(
        @Query("store_id") storeId: String? = null,
        @Query("branch_id") branchId: String? = null,
        @Query("currency") currency: String? = null,
        @Query("timezone") timezone: String? = null
    ): DashboardSummaryDto

    @GET("dashboard/analytics")
    suspend fun getDashboardAnalytics(
        @Query("store_id") storeId: String? = null,
        @Query("branch_id") branchId: String? = null,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("period") period: String? = null,
        @Query("group_by") groupBy: String? = null,
        @Query("currency") currency: String? = null,
        @Query("timezone") timezone: String? = null
    ): DashboardAnalyticsDto

    @GET("dashboard/refresh")
    suspend fun refreshDashboard(
        @Query("store_id") storeId: String? = null,
        @Query("branch_id") branchId: String? = null,
        @Query("currency") currency: String? = null,
        @Query("timezone") timezone: String? = null
    ): DashboardDto
}