package com.myimdad_por.data.remote.api

import com.myimdad_por.data.remote.dto.ReportDto
import com.myimdad_por.data.remote.dto.ReportExportRequestDto
import com.myimdad_por.data.remote.dto.ReportGenerateRequestDto
import com.myimdad_por.data.remote.dto.ReportListResponseDto
import com.myimdad_por.data.remote.dto.ReportSyncRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ReportApiService {

    @GET("reports")
    suspend fun getReports(
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("type") type: String? = null,
        @Query("exported") exported: Boolean? = null,
        @Query("generatedByUserId") generatedByUserId: String? = null,
        @Query("fromMillis") fromMillis: Long? = null,
        @Query("toMillis") toMillis: Long? = null,
        @Query("query") query: String? = null,
        @Query("sortBy") sortBy: String? = null,
        @Query("sortOrder") sortOrder: String? = null
    ): Response<ReportListResponseDto>

    @GET("reports/{reportId}")
    suspend fun getReportById(
        @Path("reportId") reportId: String
    ): Response<ReportDto>

    @POST("reports")
    suspend fun createReport(
        @Body request: ReportGenerateRequestDto
    ): Response<ReportDto>

    @PUT("reports/{reportId}")
    suspend fun updateReport(
        @Path("reportId") reportId: String,
        @Body request: ReportDto
    ): Response<ReportDto>

    @DELETE("reports/{reportId}")
    suspend fun deleteReport(
        @Path("reportId") reportId: String
    ): Response<Unit>

    @POST("reports/{reportId}/export")
    suspend fun exportReport(
        @Path("reportId") reportId: String,
        @Body request: ReportExportRequestDto
    ): Response<ReportDto>

    @POST("reports/sync")
    suspend fun syncReports(
        @Body request: ReportSyncRequestDto
    ): Response<ReportListResponseDto>

    @HTTP(method = "DELETE", path = "reports/bulk", hasBody = true)
    suspend fun deleteReportsBulk(
        @Body reportIds: List<String>
    ): Response<Unit>
}