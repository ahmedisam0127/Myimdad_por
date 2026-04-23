package com.myimdad_por.data.remote.api

import com.myimdad_por.data.remote.dto.ProductDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface ProductApiService {

    @GET(Paths.BASE)
    suspend fun listProducts(
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("search") search: String? = null,
        @Query("barcode") barcode: String? = null,
        @Query("active") active: Boolean? = null,
        @QueryMap filters: Map<String, String> = emptyMap()
    ): Response<List<ProductDto>>

    @GET(Paths.BY_ID)
    suspend fun getProduct(
        @Path("id") id: String
    ): Response<ProductDto>

    @GET(Paths.BY_BARCODE)
    suspend fun getProductByBarcode(
        @Path("barcode") barcode: String
    ): Response<ProductDto>

    @POST(Paths.BASE)
    suspend fun createProduct(
        @Body request: ProductDto
    ): Response<ProductDto>

    @PUT(Paths.BY_ID)
    suspend fun updateProduct(
        @Path("id") id: String,
        @Body request: ProductDto
    ): Response<ProductDto>

    @DELETE(Paths.BY_ID)
    suspend fun deleteProduct(
        @Path("id") id: String
    ): Response<Unit>

    object Paths {
        const val BASE = "products"
        const val BY_ID = "products/{id}"
        const val BY_BARCODE = "products/barcode/{barcode}"
    }
}