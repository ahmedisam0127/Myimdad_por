package com.myimdad_por.domain.usecase

import com.myimdad_por.domain.model.Supplier
import com.myimdad_por.domain.repository.SupplierRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

data class SupplierQuery(
    val id: String? = null,
    val code: String? = null,
    val email: String? = null,
    val text: String? = null,
    val activeOnly: Boolean = false,
    val preferredOnly: Boolean = false,
    val withDebtOnly: Boolean = false
)

class GetSuppliersUseCase @Inject constructor(
    private val supplierRepository: SupplierRepository
) {

    suspend operator fun invoke(query: SupplierQuery = SupplierQuery()): List<Supplier> {
        query.id?.trim()?.takeIf { it.isNotBlank() }?.let {
            return listOfNotNull(supplierRepository.getSupplierById(it))
        }

        query.code?.trim()?.takeIf { it.isNotBlank() }?.let {
            return listOfNotNull(supplierRepository.getSupplierByCode(it))
        }

        query.email?.trim()?.takeIf { it.isNotBlank() }?.let {
            return listOfNotNull(supplierRepository.getSupplierByEmail(it))
        }

        val base = when {
            query.withDebtOnly -> supplierRepository.getSuppliersWithDebt()

            query.preferredOnly && !query.activeOnly ->
                supplierRepository.observePreferredSuppliers().first()

            query.activeOnly && !query.preferredOnly ->
                supplierRepository.observeActiveSuppliers().first()

            else ->
                supplierRepository.observeAllSuppliers().first()
        }

        return base.filterWithText(query.text)
    }

    fun observeAll(): Flow<List<Supplier>> = supplierRepository.observeAllSuppliers()

    fun observeById(id: String): Flow<Supplier?> {
        require(id.isNotBlank()) { "id cannot be blank" }
        return supplierRepository.observeSupplierById(id)
    }

    fun observeByCode(code: String): Flow<Supplier?> {
        require(code.isNotBlank()) { "code cannot be blank" }
        return supplierRepository.observeSupplierByCode(code)
    }

    fun observeActive(): Flow<List<Supplier>> = supplierRepository.observeActiveSuppliers()

    fun observePreferred(): Flow<List<Supplier>> = supplierRepository.observePreferredSuppliers()

    fun observeWithDebt(): Flow<List<Supplier>> = supplierRepository.observeSuppliersWithDebt()

    suspend fun getById(id: String): Supplier? {
        require(id.isNotBlank()) { "id cannot be blank" }
        return supplierRepository.getSupplierById(id)
    }

    suspend fun getByCode(code: String): Supplier? {
        require(code.isNotBlank()) { "code cannot be blank" }
        return supplierRepository.getSupplierByCode(code)
    }

    suspend fun getByEmail(email: String): Supplier? {
        require(email.isNotBlank()) { "email cannot be blank" }
        return supplierRepository.getSupplierByEmail(email)
    }

    suspend fun search(query: String): List<Supplier> {
        require(query.isNotBlank()) { "query cannot be blank" }
        return supplierRepository.searchSuppliers(query)
    }

    suspend fun getBalance(supplierId: String) = supplierRepository.getSupplierBalance(supplierId)

    suspend fun getBalanceByCode(supplierCode: String) =
        supplierRepository.getSupplierBalanceByCode(supplierCode)

    suspend fun countAll(): Long = supplierRepository.countSuppliers()

    suspend fun countActive(): Long = supplierRepository.countActiveSuppliers()

    suspend fun countPreferred(): Long = supplierRepository.countPreferredSuppliers()

    suspend fun countWithDebt(): Long = supplierRepository.countSuppliersWithDebt()

    suspend fun performance(
        supplierId: String,
        from: Long? = null,
        to: Long? = null
    ) = supplierRepository.getSupplierPerformance(
        supplierId = supplierId,
        from = from?.let { java.time.LocalDateTime.ofEpochSecond(it / 1000, 0, java.time.ZoneOffset.UTC) },
        to = to?.let { java.time.LocalDateTime.ofEpochSecond(it / 1000, 0, java.time.ZoneOffset.UTC) }
    )

    private fun List<Supplier>.filterWithText(text: String?): List<Supplier> {
        val q = text?.trim().orEmpty()
        if (q.isBlank()) return this

        return asSequence()
            .filter { it.toString().contains(q, ignoreCase = true) }
            .toList()
    }
}