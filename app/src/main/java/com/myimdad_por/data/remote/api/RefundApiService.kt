package com.myimdad_por.data.remote.api

import com.myimdad_por.data.remote.dto.RefundDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface RefundApiService {

    @GET(Paths.BASE)
    suspend fun listRefunds(
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("status") status: String? = null,
        @Query("transactionId") transactionId: String? = null,
        @QueryMap filters: Map<String, String> = emptyMap()
    ): Response<List<RefundDto>>

    @GET(Paths.BY_ID)
    suspend fun getRefund(
        @Path("id") id: String
    ): Response<RefundDto>

    @POST(Paths.BASE)
    suspend fun createRefund(
        @Body request: RefundDto
    ): Response<RefundDto>

    @PUT(Paths.BY_ID)
    suspend fun updateRefund(
        @Path("id") id: String,
        @Body request: RefundDto
    ): Response<RefundDto>

    @DELETE(Paths.BY_ID)
    suspend fun deleteRefund(
        @Path("id") id: String
    ): Response<Unit>

    @POST(Paths.BY_ID + "/verify")
    suspend fun verifyRefund(
        @Path("id") id: String
    ): Response<RefundDto>

    object Paths {
        const val BASE = "refunds"
        const val BY_ID = "refunds/{id}"
    }
}