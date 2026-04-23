package com.myimdad_por.core.security.app.anti_tamper

import android.content.Context
import com.myimdad_por.core.security.AppSignatureVerifier
import com.myimdad_por.core.security.TamperProtection
import java.security.MessageDigest

/**
 * Verifies the identity of the installed app.
 *
 * This layer checks:
 * - package name
 * - signing certificate SHA-256 digests
 * - installer source
 * - cloned or repackaged builds
 *
 * Expected signatures should be provided from a protected source
 * such as encrypted build config or a secure remote policy.
 */
object SignatureChecker {

    private const val EXPECTED_PACKAGE_NAME = "com.myimdad_por"

    private val TRUSTED_INSTALLERS = setOf(
        "com.android.vending",
        "com.amazon.venezia"
    )

    @Volatile
    private var trustedSignatureDigests: List<ByteArray> = emptyList()

    enum class Signal {
        PACKAGE_NAME_MISMATCH,
        NO_TRUSTED_SIGNATURES_CONFIGURED,
        SIGNATURE_MISMATCH,
        UNTRUSTED_INSTALLER,
        SIDE_LOADED_INSTALLATION,
        CLONED_OR_REPACKAGED_APP
    }

    data class SecurityStatus(
        val verified: Boolean,
        val signals: List<Signal>
    ) {
        val compromised: Boolean get() = !verified
        val riskScore: Int get() = signals.size
    }

    /**
     * Provide one or more trusted SHA-256 certificate digests.
     *
     * Accepts hex strings with or without separators.
     * Example:
     * - "ab12cd..."
     * - "ab:12:cd:..."
     */
    @Synchronized
    fun configureTrustedSignatureHashes(hexDigests: Collection<String>) {
        require(hexDigests.isNotEmpty()) { "hexDigests must not be empty." }
        trustedSignatureDigests = hexDigests.map { hexToBytes(normalizeHexDigest(it)) }
    }

    @Synchronized
    fun clearTrustedSignatureHashes() {
        trustedSignatureDigests = emptyList()
    }

    fun verifyAppIntegrity(
        context: Context,
        expectedPackageName: String = EXPECTED_PACKAGE_NAME
    ): Boolean {
        return inspect(context, expectedPackageName).verified
    }

    fun verifyAppIntegrityOrThrow(
        context: Context,
        expectedPackageName: String = EXPECTED_PACKAGE_NAME
    ): Result<Unit> {
        return runCatching {
            check(verifyAppIntegrity(context, expectedPackageName)) {
                "App integrity verification failed."
            }
        }
    }

    fun inspect(
        context: Context,
        expectedPackageName: String = EXPECTED_PACKAGE_NAME
    ): SecurityStatus {
        val signals = linkedSetOf<Signal>()

        if (!isPackageNameValid(context, expectedPackageName)) {
            signals += Signal.PACKAGE_NAME_MISMATCH
        }

        if (trustedSignatureDigests.isEmpty()) {
            signals += Signal.NO_TRUSTED_SIGNATURES_CONFIGURED
        } else if (!isSignatureValid(context, trustedSignatureDigests)) {
            signals += Signal.SIGNATURE_MISMATCH
        }

        if (TamperProtection.isSideLoaded(context, TRUSTED_INSTALLERS)) {
            signals += Signal.SIDE_LOADED_INSTALLATION
        }

        if (!isTrustedInstaller(context)) {
            signals += Signal.UNTRUSTED_INSTALLER
        }

        if (isAppClonedOrRepackaged(context, expectedPackageName)) {
            signals += Signal.CLONED_OR_REPACKAGED_APP
        }

        return SecurityStatus(
            verified = signals.isEmpty(),
            signals = signals.toList()
        )
    }

    fun isPackageNameValid(
        context: Context,
        expectedPackageName: String = EXPECTED_PACKAGE_NAME
    ): Boolean {
        require(expectedPackageName.isNotBlank()) { "expectedPackageName must not be blank." }
        return context.packageName == expectedPackageName
    }

    fun isSignatureValid(
        context: Context,
        expectedSignatureDigests: Collection<ByteArray> = trustedSignatureDigests
    ): Boolean {
        if (expectedSignatureDigests.isEmpty()) return false

        val currentDigests = AppSignatureVerifier.getCurrentSignatureDigests(context)
        if (currentDigests.isEmpty()) return false

        return currentDigests.any { current ->
            expectedSignatureDigests.any { expected ->
                MessageDigest.isEqual(current, expected)
            }
        }
    }

    fun isTrustedInstaller(
        context: Context,
        trustedInstallers: Set<String> = TRUSTED_INSTALLERS
    ): Boolean {
        val installer = getInstallerPackageName(context) ?: return false
        return installer in trustedInstallers
    }

    fun isAppClonedOrRepackaged(
        context: Context,
        expectedPackageName: String = EXPECTED_PACKAGE_NAME
    ): Boolean {
        val packageNameMismatch = !isPackageNameValid(context, expectedPackageName)
        val signatureMismatch = trustedSignatureDigests.isNotEmpty() &&
            !isSignatureValid(context, trustedSignatureDigests)
        val suspiciousInstaller = !isTrustedInstaller(context)

        return packageNameMismatch || signatureMismatch || suspiciousInstaller
    }

    fun getCurrentSignatureHexDigests(context: Context): List<String> {
        return AppSignatureVerifier.getCurrentSignatureDigestHex(context)
    }

    fun getRiskReasons(
        context: Context,
        expectedPackageName: String = EXPECTED_PACKAGE_NAME
    ): List<String> {
        return inspect(context, expectedPackageName).signals.map { it.name }
    }

    private fun getInstallerPackageName(context: Context): String? {
        return runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getInstallerPackageName(context.packageName)
        }.getOrNull()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }

    private fun normalizeHexDigest(value: String): String {
        require(value.isNotBlank()) { "value must not be blank." }

        return value
            .trim()
            .removePrefix("sha256/")
            .removePrefix("SHA256/")
            .replace(":", "")
            .replace(" ", "")
            .lowercase()
    }

    private fun hexToBytes(hex: String): ByteArray {
        require(hex.isNotBlank()) { "hex must not be blank." }
        require(hex.length % 2 == 0) { "hex length must be even." }

        val result = ByteArray(hex.length / 2)
        var index = 0

        while (index < hex.length) {
            val high = hexCharToInt(hex[index])
            val low = hexCharToInt(hex[index + 1])
            result[index / 2] = ((high shl 4) or low).toByte()
            index += 2
        }

        return result
    }

    private fun hexCharToInt(ch: Char): Int {
        return when (ch) {
            in '0'..'9' -> ch.code - '0'.code
            in 'a'..'f' -> ch.code - 'a'.code + 10
            in 'A'..'F' -> ch.code - 'A'.code + 10
            else -> throw IllegalArgumentException("Invalid hex character: $ch")
        }
    }
}