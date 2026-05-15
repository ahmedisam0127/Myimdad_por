package com.myimdad_por.data.remote.api

import com.myimdad_por.data.remote.dto.ExpenseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface ExpenseApiService {

    @GET(Paths.BASE)
    suspend fun listExpenses(
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("status") status: String? = null,
        @Query("category") category: String? = null,
        @Query("supplierName") supplierName: String? = null,
        @QueryMap filters: Map<String, String> = emptyMap()
    ): Response<List<ExpenseDto>>

    @GET(Paths.BY_ID)
    suspend fun getExpense(
        @Path("id") id: String
    ): Response<ExpenseDto>

    @POST(Paths.BASE)
    suspend fun createExpense(
        @Body request: ExpenseDto
    ): Response<ExpenseDto>

    @PUT(Paths.BY_ID)
    suspend fun updateExpense(
        @Path("id") id: String,
        @Body request: ExpenseDto
    ): Response<ExpenseDto>

    @DELETE(Paths.BY_ID)
    suspend fun deleteExpense(
        @Path("id") id: String
    ): Response<Unit>

    object Paths {
        const val BASE = "expenses"
        const val BY_ID = "expenses/{id}"
    }
}