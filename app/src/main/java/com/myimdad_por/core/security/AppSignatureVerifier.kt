package com.myimdad_por.core.security

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import java.security.MessageDigest

/**
 * Verifies that the running app is the original signed package.
 *
 * Use this in high-value flows too, not only at startup:
 * - login
 * - sensitive decryption
 * - payment actions
 * - admin-only screens
 *
 * The expected certificate digests should come from a secure source
 * (for example encrypted config or a protected build-time value).
 */
object AppSignatureVerifier {

    private const val EXPECTED_PACKAGE_NAME = "com.myimdad_por"
    private const val SHA_256_ALGORITHM = "SHA-256"

    fun isPackageNameValid(
        context: Context,
        expectedPackageName: String = EXPECTED_PACKAGE_NAME
    ): Boolean {
        require(expectedPackageName.isNotBlank()) { "expectedPackageName must not be blank." }
        return context.packageName == expectedPackageName
    }

    fun getCurrentSignatureDigests(context: Context): List<ByteArray> {
        val signatures = getCurrentSignatures(context)
        return signatures.map { signature ->
            sha256(signature.toByteArray())
        }
    }

    fun getCurrentSignatureDigestHex(context: Context): List<String> {
        return getCurrentSignatureDigests(context).map { it.toHexString() }
    }

    fun verifySignature(
        context: Context,
        expectedSignatureDigests: Collection<ByteArray>,
        expectedPackageName: String = EXPECTED_PACKAGE_NAME
    ): Boolean {
        if (expectedSignatureDigests.isEmpty()) return false
        if (!isPackageNameValid(context, expectedPackageName)) return false

        val currentDigests = getCurrentSignatureDigests(context)
        return currentDigests.any { current ->
            expectedSignatureDigests.any { expected ->
                MessageDigest.isEqual(current, expected)
            }
        }
    }

    fun verifySignatureOrThrow(
        context: Context,
        expectedSignatureDigests: Collection<ByteArray>,
        expectedPackageName: String = EXPECTED_PACKAGE_NAME
    ): Result<Unit> {
        return runCatching {
            check(expectedSignatureDigests.isNotEmpty()) { "expectedSignatureDigests must not be empty." }
            check(isPackageNameValid(context, expectedPackageName)) {
                "Package name mismatch."
            }
            check(verifySignature(context, expectedSignatureDigests, expectedPackageName)) {
                "App signature verification failed."
            }
        }
    }

    fun verifyPackageAndSignature(
        context: Context,
        expectedSignatureDigests: Collection<ByteArray>,
        expectedPackageName: String = EXPECTED_PACKAGE_NAME
    ): Boolean {
        return verifySignature(context, expectedSignatureDigests, expectedPackageName)
    }

    private fun getCurrentSignatures(context: Context): List<Signature> {
        val packageInfo = getPackageInfo(context)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = packageInfo.signingInfo ?: return emptyList()

            if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners?.toList().orEmpty()
            } else {
                signingInfo.signingCertificateHistory?.toList()
                    ?: signingInfo.apkContentsSigners?.toList().orEmpty()
            }
        } else {
            @Suppress("DEPRECATION")
            packageInfo.signatures?.toList().orEmpty()
        }
    }

    @Suppress("DEPRECATION")
    private fun getPackageInfo(context: Context): PackageInfo {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }

        return context.packageManager.getPackageInfo(context.packageName, flags)
    }

    private fun sha256(data: ByteArray): ByteArray {
        return MessageDigest.getInstance(SHA_256_ALGORITHM).digest(data)
    }

    private fun ByteArray.toHexString(): String {
        val hexChars = CharArray(size * 2)
        var index = 0

        forEach { byte ->
            val value = byte.toInt() and 0xFF
            hexChars[index++] = HEX_CHARS[value ushr 4]
            hexChars[index++] = HEX_CHARS[value and 0x0F]
        }

        return String(hexChars)
    }

    private val HEX_CHARS = "0123456789abcdef".toCharArray()
}