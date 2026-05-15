package com.myimdad_por.data.mapper

import com.myimdad_por.data.remote.dto.auth.AuthResponseDto
import com.myimdad_por.data.remote.dto.auth.AuthUserDto
import com.myimdad_por.data.remote.dto.auth.ForgotPasswordRequest
import com.myimdad_por.data.remote.dto.auth.LoginRequestDto
import com.myimdad_por.data.remote.dto.auth.RegisterRequestDto
import com.myimdad_por.data.remote.dto.auth.ResetPasswordRequest
import com.myimdad_por.data.remote.dto.auth.VerifyOtpRequest
import com.myimdad_por.domain.model.Permission
import com.myimdad_por.domain.model.Role
import com.myimdad_por.domain.model.User

object AuthMapper {

    fun toAuthResponseDto(
        user: User,
        accessToken: String? = null,
        refreshToken: String? = null,
        expiresInSeconds: Long? = null,
        requiresOtp: Boolean = false,
        sessionId: String? = null,
        message: String? = null
    ): AuthResponseDto {
        return AuthResponseDto(
            success = true,
            message = message,
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresInSeconds = expiresInSeconds,
            requiresOtp = requiresOtp,
            sessionId = sessionId,
            user = user.toAuthUserDto()
        )
    }

    fun AuthResponseDto.toDomainUser(): User? {
        val remoteUser = user ?: return null
        return User(
            id = remoteUser.id,
            fullName = remoteUser.fullName,
            role = mapRole(remoteUser.role),
            username = remoteUser.username,
            email = remoteUser.email,
            phoneNumber = remoteUser.phoneNumber,
            isActive = remoteUser.isActive,
            permissions = remoteUser.permissions.mapNotNull { mapPermission(it) }.toSet(),
            lastLoginAtMillis = remoteUser.lastLoginAtMillis
        )
    }

    fun User.toAuthUserDto(): AuthUserDto {
        return AuthUserDto(
            id = id,
            fullName = fullName,
            username = username,
            email = email,
            phoneNumber = phoneNumber,
            role = role.name,
            permissions = effectivePermissions.map { it.key }.sorted(),
            isActive = isActive,
            lastLoginAtMillis = lastLoginAtMillis
        )
    }

    fun mapRole(roleKey: String?): Role {
        val normalized = roleKey?.trim().orEmpty()
        if (normalized.isBlank()) return Role.EMPLOYEE
        return Role.values().firstOrNull { it.name.equals(normalized, ignoreCase = true) }
            ?: Role.EMPLOYEE
    }

    fun mapPermission(permissionKey: String?): Permission? {
        val normalized = permissionKey?.trim().orEmpty()
        if (normalized.isBlank()) return null
        return Permission.values().firstOrNull { permission ->
            permission.key.equals(normalized, ignoreCase = true) ||
                permission.name.equals(normalized, ignoreCase = true)
        }
    }

    fun mapPermissions(permissionKeys: Collection<String>?): Set<Permission> {
        return permissionKeys
            .orEmpty()
            .mapNotNull { mapPermission(it) }
            .toSet()
    }

    fun mapPermissionKeys(permissions: Collection<Permission>?): List<String> {
        return permissions
            .orEmpty()
            .map { it.key }
            .distinct()
            .sorted()
    }

    fun LoginRequestDto.toRequestLogLabel(): String {
        return "login:${identifier.trim()}"
    }

    fun RegisterRequestDto.toRequestLogLabel(): String {
        return "register:${fullName.trim()}"
    }

    fun ForgotPasswordRequest.toRequestLogLabel(): String {
        return "forgot:${identifier.trim()}"
    }

    fun VerifyOtpRequest.toRequestLogLabel(): String {
        return "verify-otp:${identifier.trim()}:${purpose.trim()}"
    }

    fun ResetPasswordRequest.toRequestLogLabel(): String {
        return "reset:${identifier.trim()}"
    }
}