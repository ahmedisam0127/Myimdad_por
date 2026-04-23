package com.myimdad_por.data.remote.api

import com.myimdad_por.data.remote.dto.PaymentDto
import com.myimdad_por.data.remote.dto.PaymentVerificationDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface PaymentApiService {

    @GET(Paths.BASE)
    suspend fun listPayments(
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("status") status: String? = null,
        @Query("transactionId") transactionId: String? = null,
        @Query("invoiceId") invoiceId: String? = null,
        @QueryMap filters: Map<String, String> = emptyMap()
    ): Response<List<PaymentDto>>

    @GET(Paths.BY_ID)
    suspend fun getPayment(
        @Path("id") id: String
    ): Response<PaymentDto>

    @POST(Paths.BASE)
    suspend fun createPayment(
        @Body request: PaymentDto
    ): Response<PaymentDto>

    @PUT(Paths.BY_ID)
    suspend fun updatePayment(
        @Path("id") id: String,
        @Body request: PaymentDto
    ): Response<PaymentDto>

    @DELETE(Paths.BY_ID)
    suspend fun deletePayment(
        @Path("id") id: String
    ): Response<Unit>

    @POST(Paths.VERIFY)
    suspend fun verifyPayment(
        @Body request: PaymentVerificationDto
    ): Response<PaymentVerificationDto>

    @GET(Paths.VERIFICATION_BY_TRANSACTION)
    suspend fun getPaymentVerification(
        @Path("transactionId") transactionId: String
    ): Response<PaymentVerificationDto>

    object Paths {
        const val BASE = "payments"
        const val BY_ID = "payments/{id}"
        const val VERIFY = "payments/verify"
        const val VERIFICATION_BY_TRANSACTION = "payments/verification/{transactionId}"
    }
}