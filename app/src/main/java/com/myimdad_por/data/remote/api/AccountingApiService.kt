package com.myimdad_por.data.remote.api

import com.myimdad_por.data.remote.dto.AccountingDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface AccountingApiService {

    @GET(Paths.BASE)
    suspend fun listEntries(
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("status") status: String? = null,
        @Query("source") source: String? = null,
        @Query("referenceId") referenceId: String? = null,
        @QueryMap filters: Map<String, String> = emptyMap()
    ): Response<List<AccountingDto>>

    @GET(Paths.BY_ID)
    suspend fun getEntry(
        @Path("id") id: String
    ): Response<AccountingDto>

    @POST(Paths.BASE)
    suspend fun createEntry(
        @Body request: AccountingDto
    ): Response<AccountingDto>

    @PUT(Paths.BY_ID)
    suspend fun updateEntry(
        @Path("id") id: String,
        @Body request: AccountingDto
    ): Response<AccountingDto>

    @DELETE(Paths.BY_ID)
    suspend fun deleteEntry(
        @Path("id") id: String
    ): Response<Unit>

    @POST(Paths.BY_ID + "/post")
    suspend fun postEntry(
        @Path("id") id: String
    ): Response<AccountingDto>

    object Paths {
        const val BASE = "accounting"
        const val BY_ID = "accounting/{id}"
    }
}