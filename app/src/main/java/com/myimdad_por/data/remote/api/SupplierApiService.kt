package com.myimdad_por.data.remote.api

import com.myimdad_por.data.remote.dto.SupplierDto
import com.myimdad_por.domain.repository.SupplierPerformanceSummary
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.PATCH
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * واجهة API للموردين.
 *
 * مصممة لتغطية:
 * - القراءة الفردية والجماعية
 * - البحث
 * - الحفظ والتعديل
 * - إدارة الدين والحالة
 * - الإحصاءات والأداء
 */
interface SupplierApiService {

    @GET("suppliers")
    suspend fun getSuppliers(): Response<List<SupplierDto>>

    @GET("suppliers/{id}")
    suspend fun getSupplierById(
        @Path("id") id: String
    ): Response<SupplierDto>

    @GET("suppliers/code/{supplierCode}")
    suspend fun getSupplierByCode(
        @Path("supplierCode") supplierCode: String
    ): Response<SupplierDto>

    @GET("suppliers/search")
    suspend fun searchSuppliers(
        @Query("q") query: String
    ): Response<List<SupplierDto>>

    @GET("suppliers/debt")
    suspend fun getSuppliersWithDebt(): Response<List<SupplierDto>>

    @GET("suppliers/preferred")
    suspend fun getPreferredSuppliers(): Response<List<SupplierDto>>

    @GET("suppliers/active")
    suspend fun getActiveSuppliers(): Response<List<SupplierDto>>

    @POST("suppliers")
    suspend fun saveSupplier(
        @Body supplier: SupplierDto
    ): Response<SupplierDto>

    @POST("suppliers/bulk")
    suspend fun saveSuppliers(
        @Body suppliers: List<SupplierDto>
    ): Response<List<SupplierDto>>

    @PUT("suppliers/{id}")
    suspend fun updateSupplier(
        @Path("id") id: String,
        @Body supplier: SupplierDto
    ): Response<SupplierDto>

    @PATCH("suppliers/{id}/debt")
    suspend fun updateSupplierDebt(
        @Path("id") id: String,
        @Body request: UpdateSupplierDebtRequest
    ): Response<SupplierDto>

    @PATCH("suppliers/{id}/preferred")
    suspend fun markSupplierPreferred(
        @Path("id") id: String,
        @Body request: MarkSupplierPreferredRequest
    ): Response<SupplierDto>

    @PATCH("suppliers/{id}/active")
    suspend fun markSupplierActive(
        @Path("id") id: String,
        @Body request: MarkSupplierActiveRequest
    ): Response<SupplierDto>

    @DELETE("suppliers/{id}")
    suspend fun deleteSupplier(
        @Path("id") id: String
    ): Response<Unit>

    @HTTP(method = "DELETE", path = "suppliers/bulk", hasBody = true)
    suspend fun deleteSuppliers(
        @Body request: DeleteSuppliersRequest
    ): Response<DeleteSuppliersResponse>

    @DELETE("suppliers")
    suspend fun clearAll(): Response<Unit>

    @GET("suppliers/count")
    suspend fun countSuppliers(): Response<CountResponse>

    @GET("suppliers/count/active")
    suspend fun countActiveSuppliers(): Response<CountResponse>

    @GET("suppliers/count/preferred")
    suspend fun countPreferredSuppliers(): Response<CountResponse>

    @GET("suppliers/count/debt")
    suspend fun countSuppliersWithDebt(): Response<CountResponse>

    @GET("suppliers/{id}/performance")
    suspend fun getSupplierPerformance(
        @Path("id") supplierId: String,
        @Query("from") fromMillis: Long? = null,
        @Query("to") toMillis: Long? = null
    ): Response<SupplierPerformanceDto>

    @GET("suppliers/{id}/balance")
    suspend fun getSupplierBalance(
        @Path("id") supplierId: String
    ): Response<MoneyDto>

    @GET("suppliers/code/{supplierCode}/balance")
    suspend fun getSupplierBalanceByCode(
        @Path("supplierCode") supplierCode: String
    ): Response<MoneyDto>
}

data class UpdateSupplierDebtRequest(
    val balanceDelta: String
)

data class MarkSupplierPreferredRequest(
    val preferred: Boolean
)

data class MarkSupplierActiveRequest(
    val active: Boolean
)

data class DeleteSuppliersRequest(
    val ids: List<String>
)

data class DeleteSuppliersResponse(
    val deletedCount: Int
)

data class CountResponse(
    val count: Long
)

data class MoneyDto(
    val amount: String,
    val currency: String? = null
)

data class SupplierPerformanceDto(
    val supplierId: String,
    val totalPurchases: Long,
    val totalSpent: String,
    val totalOutstanding: String,
    val onTimeDeliveries: Long = 0L,
    val lateDeliveries: Long = 0L,
    val rejectedOrders: Long = 0L,
    val averageDeliveryDays: Double = 0.0
) {
    fun toDomain(currencyCode: String = "SDG"): SupplierPerformanceSummary {
        return SupplierPerformanceSummary(
            supplierId = supplierId,
            totalPurchases = totalPurchases,
            totalSpent = totalSpent.toBigDecimalOrZero(),
            totalOutstanding = totalOutstanding.toBigDecimalOrZero(),
            onTimeDeliveries = onTimeDeliveries,
            lateDeliveries = lateDeliveries,
            rejectedOrders = rejectedOrders,
            averageDeliveryDays = averageDeliveryDays
        )
    }
}

private fun String.toBigDecimalOrZero(): java.math.BigDecimal {
    return runCatching { java.math.BigDecimal(this) }
        .getOrElse { java.math.BigDecimal.ZERO }
}