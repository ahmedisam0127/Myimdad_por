package com.myimdad_por.data.remote.dto.auth

data class ResetPasswordRequest(
    val identifier: String,
    val otpCode: String,
    val newPassword: String,
    val confirmPassword: String,
    val sessionId: String? = null,
    val deviceId: String? = null
) {
    init {
        require(identifier.isNotBlank()) { "identifier cannot be blank." }
        require(otpCode.isNotBlank()) { "otpCode cannot be blank." }
        require(newPassword.isNotBlank()) { "newPassword cannot be blank." }
        require(confirmPassword.isNotBlank()) { "confirmPassword cannot be blank." }
        require(newPassword == confirmPassword) { "newPassword and confirmPassword must match." }
        sessionId?.let {
            require(it.isNotBlank()) { "sessionId cannot be blank when provided." }
        }
        deviceId?.let {
            require(it.isNotBlank()) { "deviceId cannot be blank when provided." }
        }
    }
}