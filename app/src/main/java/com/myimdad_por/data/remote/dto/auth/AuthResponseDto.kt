package com.myimdad_por.data.remote.dto.auth

data class AuthResponseDto(
    val success: Boolean = true,
    val message: String? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val tokenType: String = "Bearer",
    val expiresInSeconds: Long? = null,
    val requiresOtp: Boolean = false,
    val sessionId: String? = null,
    val user: AuthUserDto? = null,
    val serverTimeMillis: Long = System.currentTimeMillis()
) {
    init {
        require(tokenType.isNotBlank()) { "tokenType cannot be blank." }
        require(serverTimeMillis > 0L) { "serverTimeMillis must be greater than zero." }
        expiresInSeconds?.let {
            require(it > 0L) { "expiresInSeconds must be greater than zero when provided." }
        }
        sessionId?.let {
            require(it.isNotBlank()) { "sessionId cannot be blank when provided." }
        }
    }
}

data class AuthUserDto(
    val id: String,
    val fullName: String,
    val username: String? = null,
    val email: String? = null,
    val phoneNumber: String? = null,
    val role: String,
    val permissions: List<String> = emptyList(),
    val isActive: Boolean = true,
    val lastLoginAtMillis: Long? = null
) {
    init {
        require(id.isNotBlank()) { "id cannot be blank." }
        require(fullName.isNotBlank()) { "fullName cannot be blank." }
        require(role.isNotBlank()) { "role cannot be blank." }
        lastLoginAtMillis?.let {
            require(it > 0L) { "lastLoginAtMillis must be greater than zero when provided." }
        }
    }
}