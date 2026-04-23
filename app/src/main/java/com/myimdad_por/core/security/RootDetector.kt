package com.myimdad_por.core.security

import android.content.Context
import android.os.Build
import java.io.File

/**
 * Local environment consistency checks.
 *
 * This is a defensive signal, not an absolute proof.
 * Use it to:
 * - restrict sensitive features
 * - raise risk level
 * - combine with server-side integrity checks later
 */
object RootDetector {

    private val suspiciousBinaryPaths = listOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/system/sbin/su",
        "/vendor/bin/su",
        "/su/bin/su",
        "/system/app/Superuser.apk",
        "/system/bin/.ext/.su",
        "/system/usr/we-need-root/su-backup",
        "/system/xbin/mu"
    )

    private val suspiciousPackages = listOf(
        "com.topjohnwu.magisk",
        "com.koushikdutta.superuser",
        "eu.chainfire.supersu",
        "com.noshufou.android.su",
        "com.thirdparty.superuser",
        "com.yellowes.su",
        "com.kingroot.kinguser",
        "com.kingo.root",
        "com.smedialink.oneclickroot"
    )

    enum class Signal {
        TEST_KEYS,
        SU_BINARY,
        ROOT_APP,
        DANGEROUS_SYSTEM_PROPERTY
    }

    data class SecurityStatus(
        val compromised: Boolean,
        val signals: List<Signal>
    ) {
        val riskScore: Int get() = signals.size
    }

    fun inspect(context: Context): SecurityStatus {
        val signals = linkedSetOf<Signal>()

        if (hasTestKeys()) {
            signals += Signal.TEST_KEYS
        }

        if (hasSuBinary()) {
            signals += Signal.SU_BINARY
        }

        if (hasKnownRootApps(context)) {
            signals += Signal.ROOT_APP
        }

        if (hasDangerousBuildProfile()) {
            signals += Signal.DANGEROUS_SYSTEM_PROPERTY
        }

        return SecurityStatus(
            compromised = signals.isNotEmpty(),
            signals = signals.toList()
        )
    }

    fun isSystemCompromised(context: Context): Boolean {
        return inspect(context).compromised
    }

    fun shouldRestrictSensitiveFeatures(context: Context): Boolean {
        return isSystemCompromised(context)
    }

    fun getRiskLevel(context: Context): Int {
        return inspect(context).riskScore
    }

    fun getRiskReasons(context: Context): List<String> {
        return inspect(context).signals.map { it.name }
    }

    private fun hasTestKeys(): Boolean {
        return Build.TAGS?.contains("test-keys", ignoreCase = true) == true
    }

    private fun hasSuBinary(): Boolean {
        return suspiciousBinaryPaths.any { path ->
            runCatching { File(path).exists() }.getOrDefault(false)
        }
    }

    private fun hasKnownRootApps(context: Context): Boolean {
        val packageManager = context.packageManager

        return suspiciousPackages.any { packageName ->
            runCatching {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }.isSuccess
        }
    }

    private fun hasDangerousBuildProfile(): Boolean {
        val fingerprint = Build.FINGERPRINT.lowercase()
        val model = Build.MODEL.lowercase()
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        val device = Build.DEVICE.lowercase()
        val product = Build.PRODUCT.lowercase()

        return fingerprint.contains("generic") ||
            fingerprint.contains("test-keys") ||
            model.contains("google_sdk") ||
            model.contains("emulator") ||
            model.contains("android sdk built for x86") ||
            manufacturer.contains("genymotion") ||
            brand.startsWith("generic") && device.startsWith("generic") ||
            product.contains("sdk") ||
            product.contains("emulator")
    }
}