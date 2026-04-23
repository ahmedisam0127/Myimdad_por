package com.myimdad_por.data.remote.api

import com.myimdad_por.data.remote.dto.SaleInvoiceDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface InvoiceApiService {

    @GET(Paths.BASE)
    suspend fun listInvoices(
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("status") status: String? = null,
        @Query("invoiceNumber") invoiceNumber: String? = null,
        @Query("customerName") customerName: String? = null,
        @QueryMap filters: Map<String, String> = emptyMap()
    ): Response<List<SaleInvoiceDto>>

    @GET(Paths.BY_ID)
    suspend fun getInvoice(
        @Path("id") id: String
    ): Response<SaleInvoiceDto>

    @POST(Paths.BASE)
    suspend fun createInvoice(
        @Body request: SaleInvoiceDto
    ): Response<SaleInvoiceDto>

    @PUT(Paths.BY_ID)
    suspend fun updateInvoice(
        @Path("id") id: String,
        @Body request: SaleInvoiceDto
    ): Response<SaleInvoiceDto>

    @DELETE(Paths.BY_ID)
    suspend fun deleteInvoice(
        @Path("id") id: String
    ): Response<Unit>

    @POST(Paths.BY_ID + "/send")
    suspend fun sendInvoice(
        @Path("id") id: String
    ): Response<SaleInvoiceDto>

    object Paths {
        const val BASE = "invoices"
        const val BY_ID = "invoices/{id}"
    }
}