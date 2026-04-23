package com.myimdad_por.data.remote.dto.auth

data class LoginRequestDto(
    val identifier: String,
    val password: String,
    val deviceId: String? = null,
    val rememberMe: Boolean = false,
    val clientTimeMillis: Long = System.currentTimeMillis()
) {
    init {
        require(identifier.isNotBlank()) { "identifier cannot be blank." }
        require(password.isNotBlank()) { "password cannot be blank." }
        require(clientTimeMillis > 0L) { "clientTimeMillis must be greater than zero." }
        deviceId?.let {
            require(it.isNotBlank()) { "deviceId cannot be blank when provided." }
        }
    }
}