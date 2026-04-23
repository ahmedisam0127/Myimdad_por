package com.myimdad_por.data.remote.api

import com.myimdad_por.data.remote.dto.StockDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface StockApiService {

    @GET(Paths.BASE)
    suspend fun listStockEntries(
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("barcode") barcode: String? = null,
        @Query("location") location: String? = null,
        @Query("movementType") movementType: String? = null,
        @QueryMap filters: Map<String, String> = emptyMap()
    ): Response<List<StockDto>>

    @GET(Paths.BY_ID)
    suspend fun getStockEntry(
        @Path("id") id: String
    ): Response<StockDto>

    @POST(Paths.BASE)
    suspend fun createStockEntry(
        @Body request: StockDto
    ): Response<StockDto>

    @PUT(Paths.BY_ID)
    suspend fun updateStockEntry(
        @Path("id") id: String,
        @Body request: StockDto
    ): Response<StockDto>

    @DELETE(Paths.BY_ID)
    suspend fun deleteStockEntry(
        @Path("id") id: String
    ): Response<Unit>

    object Paths {
        const val BASE = "stock"
        const val BY_ID = "stock/{id}"
    }
}