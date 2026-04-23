package com.myimdad_por.data.remote.api

import com.myimdad_por.data.remote.dto.CustomerDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface CustomerApiService {

    @GET(Paths.BASE)
    suspend fun listCustomers(
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("search") search: String? = null,
        @Query("active") active: Boolean? = null,
        @QueryMap filters: Map<String, String> = emptyMap()
    ): Response<List<CustomerDto>>

    @GET(Paths.BY_ID)
    suspend fun getCustomer(
        @Path("id") id: String
    ): Response<CustomerDto>

    @POST(Paths.BASE)
    suspend fun createCustomer(
        @Body request: CustomerDto
    ): Response<CustomerDto>

    @PUT(Paths.BY_ID)
    suspend fun updateCustomer(
        @Path("id") id: String,
        @Body request: CustomerDto
    ): Response<CustomerDto>

    @DELETE(Paths.BY_ID)
    suspend fun deleteCustomer(
        @Path("id") id: String
    ): Response<Unit>

    object Paths {
        const val BASE = "customers"
        const val BY_ID = "customers/{id}"
    }
}