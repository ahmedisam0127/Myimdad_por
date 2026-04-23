package com.myimdad_por.data.remote.api

import com.myimdad_por.data.remote.dto.PurchaseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface PurchaseApiService {

    @GET(Paths.BASE)
    suspend fun listPurchases(
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("status") status: String? = null,
        @Query("supplierId") supplierId: String? = null,
        @Query("invoiceNumber") invoiceNumber: String? = null,
        @QueryMap filters: Map<String, String> = emptyMap()
    ): Response<List<PurchaseDto>>

    @GET(Paths.BY_ID)
    suspend fun getPurchase(
        @Path("id") id: String
    ): Response<PurchaseDto>

    @POST(Paths.BASE)
    suspend fun createPurchase(
        @Body request: PurchaseDto
    ): Response<PurchaseDto>

    @PUT(Paths.BY_ID)
    suspend fun updatePurchase(
        @Path("id") id: String,
        @Body request: PurchaseDto
    ): Response<PurchaseDto>

    @DELETE(Paths.BY_ID)
    suspend fun deletePurchase(
        @Path("id") id: String
    ): Response<Unit>

    object Paths {
        const val BASE = "purchases"
        const val BY_ID = "purchases/{id}"
    }
}