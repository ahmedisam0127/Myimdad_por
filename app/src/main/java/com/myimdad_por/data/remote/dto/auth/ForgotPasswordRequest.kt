package com.myimdad_por.data.remote.dto.auth

data class ForgotPasswordRequest(
    val identifier: String,
    val deliveryMethod: String = "otp",
    val deviceId: String? = null,
    val locale: String? = null,
    val reason: String? = null
) {
    init {
        require(identifier.isNotBlank()) { "identifier cannot be blank." }
        require(deliveryMethod.isNotBlank()) { "deliveryMethod cannot be blank." }
        deviceId?.let {
            require(it.isNotBlank()) { "deviceId cannot be blank when provided." }
        }
        locale?.let {
            require(it.isNotBlank()) { "locale cannot be blank when provided." }
        }
        reason?.let {
            require(it.isNotBlank()) { "reason cannot be blank when provided." }
        }
    }
}