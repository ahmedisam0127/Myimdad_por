package com.myimdad_por.data.remote.dto.auth

data class VerifyOtpRequest(
    val identifier: String,
    val otpCode: String,
    val purpose: String = "PASSWORD_RESET",
    val sessionId: String? = null,
    val deviceId: String? = null
) {
    init {
        require(identifier.isNotBlank()) { "identifier cannot be blank." }
        require(otpCode.isNotBlank()) { "otpCode cannot be blank." }
        require(purpose.isNotBlank()) { "purpose cannot be blank." }
        sessionId?.let {
            require(it.isNotBlank()) { "sessionId cannot be blank when provided." }
        }
        deviceId?.let {
            require(it.isNotBlank()) { "deviceId cannot be blank when provided." }
        }
    }
}