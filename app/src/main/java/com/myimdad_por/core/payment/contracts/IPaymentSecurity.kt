package com.myimdad_por.core.payment.contracts

/**
 * Security layer for payment payloads and transport validation.
 *
 * This contract is responsible for:
 * - encrypting sensitive payloads
 * - signing requests
 * - verifying connection security
 */
interface IPaymentSecurity {

    fun encryptPaymentPayload(data: String): String

    fun signPaymentRequest(params: Map<String, Any>): String

    fun validateConnectionSecurity(): Boolean
}