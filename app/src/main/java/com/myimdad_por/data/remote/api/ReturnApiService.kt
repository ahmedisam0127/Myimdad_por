package com.myimdad_por.data.remote.api

import com.myimdad_por.data.remote.dto.ReturnDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface ReturnApiService {

    @GET(Paths.BASE)
    suspend fun listReturns(
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("status") status: String? = null,
        @Query("returnType") returnType: String? = null,
        @Query("partyId") partyId: String? = null,
        @QueryMap filters: Map<String, String> = emptyMap()
    ): Response<List<ReturnDto>>

    @GET(Paths.BY_ID)
    suspend fun getReturn(
        @Path("id") id: String
    ): Response<ReturnDto>

    @POST(Paths.BASE)
    suspend fun createReturn(
        @Body request: ReturnDto
    ): Response<ReturnDto>

    @PUT(Paths.BY_ID)
    suspend fun updateReturn(
        @Path("id") id: String,
        @Body request: ReturnDto
    ): Response<ReturnDto>

    @DELETE(Paths.BY_ID)
    suspend fun deleteReturn(
        @Path("id") id: String
    ): Response<Unit>

    @POST(Paths.BY_ID + "/approve")
    suspend fun approveReturn(
        @Path("id") id: String
    ): Response<ReturnDto>

    object Paths {
        const val BASE = "returns"
        const val BY_ID = "returns/{id}"
    }
}