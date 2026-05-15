package com.myimdad_por.data.remote.datasource

import com.myimdad_por.data.remote.dto.ReturnDto
import com.myimdad_por.core.network.NetworkResult

interface ReturnRemoteDataSource {
    suspend fun listReturns(
        page: Int? = null,
        limit: Int? = null,
        status: String? = null,
        returnType: String? = null,
        partyId: String? = null,
        filters: Map<String, String> = emptyMap()
    ): NetworkResult<List<ReturnDto>>

    suspend fun getReturn(id: String): NetworkResult<ReturnDto>

    suspend fun createReturn(request: ReturnDto): NetworkResult<ReturnDto>

    suspend fun updateReturn(id: String, request: ReturnDto): NetworkResult<ReturnDto>

    suspend fun deleteReturn(id: String): NetworkResult<Unit>

    suspend fun approveReturn(id: String): NetworkResult<ReturnDto>
}