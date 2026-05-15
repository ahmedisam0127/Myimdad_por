package com.myimdad_por.domain.usecase

import com.myimdad_por.domain.model.Customer
import com.myimdad_por.domain.repository.CustomerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

data class CustomerQuery(
    val id: String? = null,
    val code: String? = null,
    val email: String? = null,
    val text: String? = null,
    val onlyWithDebt: Boolean = false,
    val onlyOverLimit: Boolean = false
)

/**
 * محرك استرجاع العملاء.
 * يختار أفضل مسار حسب نوع الطلب:
 * - بالمعرّف
 * - بالكود
 * - بالبريد
 * - بالنص
 * - بالديون
 * - أو بكل العملاء
 */
class GetCustomersUseCase @Inject constructor(
    private val customerRepository: CustomerRepository
) {

    suspend operator fun invoke(query: CustomerQuery = CustomerQuery()): List<Customer> {
        query.id?.trim()?.takeIf { it.isNotBlank() }?.let {
            return listOfNotNull(customerRepository.getCustomerById(it))
        }

        query.code?.trim()?.takeIf { it.isNotBlank() }?.let {
            return listOfNotNull(customerRepository.getCustomerByCode(it))
        }

        query.email?.trim()?.takeIf { it.isNotBlank() }?.let {
            return listOfNotNull(customerRepository.getCustomerByEmail(it))
        }

        if (query.onlyWithDebt) {
            return customerRepository.getCustomersWithDebt()
        }

        if (query.onlyOverLimit) {
            return customerRepository.getCustomersOverLimit()
        }

        query.text?.trim()?.takeIf { it.isNotBlank() }?.let {
            return customerRepository.searchCustomers(it)
        }

        return customerRepository.observeAllCustomers().first()
    }

    fun observeAll(): Flow<List<Customer>> {
        return customerRepository.observeAllCustomers()
    }

    fun observeById(id: String): Flow<Customer?> {
        require(id.isNotBlank()) { "id cannot be blank" }
        return customerRepository.observeCustomerById(id)
    }

    fun observeByEmail(email: String): Flow<Customer?> {
        require(email.isNotBlank()) { "email cannot be blank" }
        return customerRepository.observeCustomerByEmail(email)
    }

    suspend fun getById(id: String): Customer? {
        require(id.isNotBlank()) { "id cannot be blank" }
        return customerRepository.getCustomerById(id)
    }

    suspend fun getByCode(code: String): Customer? {
        require(code.isNotBlank()) { "code cannot be blank" }
        return customerRepository.getCustomerByCode(code)
    }

    suspend fun getByEmail(email: String): Customer? {
        require(email.isNotBlank()) { "email cannot be blank" }
        return customerRepository.getCustomerByEmail(email)
    }

    suspend fun search(text: String): List<Customer> {
        require(text.isNotBlank()) { "text cannot be blank" }
        return customerRepository.searchCustomers(text)
    }

    suspend fun countAll(): Long {
        return customerRepository.countCustomers()
    }

    suspend fun countActive(): Long {
        return customerRepository.countActiveCustomers()
    }
}