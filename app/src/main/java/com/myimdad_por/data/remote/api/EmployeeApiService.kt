package com.myimdad_por.data.remote.api

import com.myimdad_por.data.remote.dto.EmployeeDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface EmployeeApiService {

    @GET(Paths.BASE)
    suspend fun listEmployees(
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("search") search: String? = null,
        @Query("active") active: Boolean? = null,
        @QueryMap filters: Map<String, String> = emptyMap()
    ): Response<List<EmployeeDto>>

    @GET(Paths.BY_ID)
    suspend fun getEmployee(
        @Path("id") id: String
    ): Response<EmployeeDto>

    @POST(Paths.BASE)
    suspend fun createEmployee(
        @Body request: EmployeeDto
    ): Response<EmployeeDto>

    @PUT(Paths.BY_ID)
    suspend fun updateEmployee(
        @Path("id") id: String,
        @Body request: EmployeeDto
    ): Response<EmployeeDto>

    @DELETE(Paths.BY_ID)
    suspend fun deleteEmployee(
        @Path("id") id: String
    ): Response<Unit>

    object Paths {
        const val BASE = "employees"
        const val BY_ID = "employees/{id}"
    }
}