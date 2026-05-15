package com.myimdad_por.data.remote.api
import retrofit2.http.Body // تأكد من وجود هذا السطر

import com.myimdad_por.data.remote.dto.SubscriptionResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface SubscriptionApiService {

    @GET(Paths.CURRENT)
    suspend fun getCurrentSubscription(): Response<SubscriptionResponseDto>

    @GET(Paths.BY_ID)
    suspend fun getSubscription(
        @Path("id") id: String
    ): Response<SubscriptionResponseDto>

    @POST(Paths.RENEW)
    suspend fun renewSubscription(
        @Path("id") id: String
    ): Response<SubscriptionResponseDto>

    @PUT(Paths.BY_ID)
    suspend fun updateSubscription(
        @Path("id") id: String,
        @Body request: SubscriptionResponseDto
    ): Response<SubscriptionResponseDto>

    object Paths {
        const val CURRENT = "subscription/current"
        const val BY_ID = "subscription/{id}"
        const val RENEW = "subscription/{id}/renew"
    }
}