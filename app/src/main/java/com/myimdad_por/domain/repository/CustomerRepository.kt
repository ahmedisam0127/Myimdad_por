package com.myimdad_por.domain.repository

import com.myimdad_por.domain.model.Customer
import kotlinx.coroutines.flow.Flow

/**
 * Customer persistence and query contract.
 */
interface CustomerRepository {

    fun observeAllCustomers(): Flow<List<Customer>>

    fun observeCustomerById(id: String): Flow<Customer?>

    fun observeCustomerByEmail(email: String): Flow<Customer?>

    suspend fun getCustomerById(id: String): Customer?

    suspend fun getCustomerByCode(code: String): Customer?

    suspend fun getCustomerByEmail(email: String): Customer?

    suspend fun searchCustomers(query: String): List<Customer>

    suspend fun saveCustomer(customer: Customer): Result<Customer>

    suspend fun saveCustomers(customers: List<Customer>): Result<List<Customer>>

    suspend fun updateCustomer(customer: Customer): Result<Customer>

    suspend fun deleteCustomer(id: String): Result<Unit>

    suspend fun deleteCustomers(ids: List<String>): Result<Int>

    suspend fun clearAll(): Result<Unit>

    suspend fun countCustomers(): Long

    suspend fun countActiveCustomers(): Long

    suspend fun getCustomersWithDebt(): List<Customer>

    suspend fun getCustomersOverLimit(): List<Customer>
}