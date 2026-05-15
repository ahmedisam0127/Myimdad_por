package com.myimdad_por.data.remote.api

import com.myimdad_por.data.remote.dto.AuditLogDto
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface AuditLogApiService {

    @GET(Paths.BASE)
    suspend fun listAuditLogs(
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("severity") severity: String? = null,
        @Query("action") action: String? = null,
        @Query("actorId") actorId: String? = null,
        @Query("targetType") targetType: String? = null,
        @Query("targetId") targetId: String? = null,
        @QueryMap filters: Map<String, String> = emptyMap()
    ): Response<List<AuditLogDto>>

    @GET(Paths.BY_ID)
    suspend fun getAuditLog(
        @Path("id") id: String
    ): Response<AuditLogDto>

    @DELETE(Paths.BY_ID)
    suspend fun deleteAuditLog(
        @Path("id") id: String
    ): Response<Unit>

    object Paths {
        const val BASE = "audit-logs"
        const val BY_ID = "audit-logs/{id}"
    }
}