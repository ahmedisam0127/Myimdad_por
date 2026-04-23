package com.myimdad_por.core.security

import android.content.Context
import java.io.File

object TamperProtection {

    private val TRUSTED_INSTALLERS = setOf(
        "com.android.vending",
        "com.amazon.venezia"
    )

    private val SUSPICIOUS_MAP_TERMS = listOf(
        "frida",
        "xposed",
        "substrate",
        "lsposed",
        "zygisk",
        "riru",
        "edxp",
        "sandhook",
        "whale"
    )

    private val SUSPICIOUS_LIBRARY_TERMS = listOf(
        "frida",
        "gadget",
        "xposed",
        "substrate",
        "hook",
        "inject",
        "lsposed",
        "zygisk",
        "riru"
    )

    enum class Signal {
        UNTRUSTED_INSTALLER,
        HOOKS_IN_MEMORY,
        SUSPICIOUS_LIBRARY_LOADED,
        SUSPICIOUS_SYSTEM_PROPERTY,
        APK_SIZE_MISMATCH,
        DEX_SIZE_MISMATCH,
        SUSPICIOUS_RUNTIME_PATH,
        PROXY_ENABLED
    }

    data class SecurityStatus(
        val compromised: Boolean,
        val signals: List<Signal>
    ) {
        val riskScore: Int get() = signals.size
    }

    fun inspect(
        context: Context,
        expectedApkSizeBytes: Long? = null,
        expectedDexSizeBytes: Long? = null,
        trustedInstallers: Set<String> = TRUSTED_INSTALLERS
    ): SecurityStatus {
        val signals = linkedSetOf<Signal>()

        if (isSideLoaded(context, trustedInstallers)) {
            signals += Signal.UNTRUSTED_INSTALLER
        }

        if (hasHooksInMemory()) {
            signals += Signal.HOOKS_IN_MEMORY
        }

        if (hasSuspiciousNativeLibraries()) {
            signals += Signal.SUSPICIOUS_LIBRARY_LOADED
        }

        if (hasSuspiciousSystemProperties()) {
            signals += Signal.SUSPICIOUS_SYSTEM_PROPERTY
        }

        if (expectedApkSizeBytes != null && expectedApkSizeBytes > 0L && isApkSizeSuspicious(context, expectedApkSizeBytes)) {
            signals += Signal.APK_SIZE_MISMATCH
        }

        if (expectedDexSizeBytes != null && expectedDexSizeBytes > 0L && isDexSizeSuspicious(context, expectedDexSizeBytes)) {
            signals += Signal.DEX_SIZE_MISMATCH
        }

        if (hasSuspiciousRuntimePath(context)) {
            signals += Signal.SUSPICIOUS_RUNTIME_PATH
        }

        if (isProxyEnabled()) {
            signals += Signal.PROXY_ENABLED
        }

        return SecurityStatus(
            compromised = signals.isNotEmpty(),
            signals = signals.toList()
        )
    }

    fun isSideLoaded(
        context: Context,
        trustedInstallers: Set<String> = TRUSTED_INSTALLERS
    ): Boolean {
        val installer = getInstallerPackageName(context)?.trim()
        return installer.isNullOrBlank() || installer !in trustedInstallers
    }

    fun hasHooksInMemory(): Boolean {
        return scanProcMapsForTerms(SUSPICIOUS_MAP_TERMS)
    }

    fun hasSuspiciousNativeLibraries(): Boolean {
        return scanProcMapsForTerms(SUSPICIOUS_LIBRARY_TERMS)
    }

    fun hasSuspiciousSystemProperties(): Boolean {
        val debuggable = getSystemProperty("ro.debuggable", "0")
        val secure = getSystemProperty("ro.secure", "1")
        val buildType = getSystemProperty("ro.build.type", "")

        return debuggable == "1" || secure == "0" || buildType.equals("userdebug", ignoreCase = true) || buildType.equals("eng", ignoreCase = true)
    }

    fun isProxyEnabled(): Boolean {
        val host = System.getProperty("http.proxyHost").orEmpty().trim()
        val port = System.getProperty("http.proxyPort").orEmpty().trim()
        val socksHost = System.getProperty("socksProxyHost").orEmpty().trim()
        val socksPort = System.getProperty("socksProxyPort").orEmpty().trim()

        return host.isNotEmpty() || port.isNotEmpty() || socksHost.isNotEmpty() || socksPort.isNotEmpty()
    }

    fun hasSuspiciousRuntimePath(context: Context): Boolean {
        val dataDir = runCatching { context.applicationInfo.dataDir.orEmpty() }.getOrDefault("")
        val sourceDir = runCatching { context.applicationInfo.sourceDir.orEmpty() }.getOrDefault("")
        val packageName = context.packageName

        val abnormalDataDir =
            dataDir.isNotBlank() &&
                !dataDir.startsWith("/data/") &&
                !dataDir.contains(packageName)

        val abnormalSourceDir =
            sourceDir.isNotBlank() &&
                !sourceDir.startsWith("/data/app/") &&
                !sourceDir.startsWith("/data/user/") &&
                !sourceDir.startsWith("/mnt/")

        return abnormalDataDir || abnormalSourceDir
    }

    fun isApkSizeSuspicious(
        context: Context,
        expectedApkSizeBytes: Long,
        toleranceBytes: Long = 0L
    ): Boolean {
        val actual = getInstalledApkSizeBytes(context) ?: return true
        return kotlin.math.abs(actual - expectedApkSizeBytes) > toleranceBytes
    }

    fun isDexSizeSuspicious(
        context: Context,
        expectedDexSizeBytes: Long,
        toleranceBytes: Long = 0L,
        dexEntryName: String = "classes.dex"
    ): Boolean {
        val actual = getInstalledDexSizeBytes(context, dexEntryName) ?: return true
        return kotlin.math.abs(actual - expectedDexSizeBytes) > toleranceBytes
    }

    fun getInstalledApkSizeBytes(context: Context): Long? {
        val apkPath = runCatching { context.applicationInfo.sourceDir }.getOrNull() ?: return null
        return runCatching { File(apkPath).length() }
            .getOrNull()
            ?.takeIf { it > 0L }
    }

    fun getInstalledDexSizeBytes(
        context: Context,
        dexEntryName: String = "classes.dex"
    ): Long? {
        val apkPath = runCatching { context.applicationInfo.sourceDir }.getOrNull() ?: return null

        return runCatching {
            java.util.zip.ZipFile(apkPath).use { zipFile ->
                zipFile.getEntry(dexEntryName)?.size
            }
        }.getOrNull()?.takeIf { it > 0L }
    }

    fun shouldRestrictSensitiveOperations(
        context: Context,
        expectedApkSizeBytes: Long? = null,
        expectedDexSizeBytes: Long? = null
    ): Boolean {
        return inspect(
            context = context,
            expectedApkSizeBytes = expectedApkSizeBytes,
            expectedDexSizeBytes = expectedDexSizeBytes
        ).compromised
    }

    fun getRiskReasons(
        context: Context,
        expectedApkSizeBytes: Long? = null,
        expectedDexSizeBytes: Long? = null
    ): List<String> {
        return inspect(
            context = context,
            expectedApkSizeBytes = expectedApkSizeBytes,
            expectedDexSizeBytes = expectedDexSizeBytes
        ).signals.map { it.name }
    }

    private fun getInstallerPackageName(context: Context): String? {
        return runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getInstallerPackageName(context.packageName)
        }.getOrNull()
    }

    private fun scanProcMapsForTerms(terms: List<String>): Boolean {
        return runCatching {
            File("/proc/self/maps").useLines { lines ->
                lines.any { line ->
                    terms.any { term -> line.contains(term, ignoreCase = true) }
                }
            }
        }.getOrDefault(false)
    }

    private fun getSystemProperty(key: String, defaultValue: String = ""): String {
        return runCatching {
            val clazz = Class.forName("android.os.SystemProperties")
            val method = clazz.getMethod("get", String::class.java, String::class.java)
            method.invoke(null, key, defaultValue) as String
        }.getOrDefault(defaultValue)
    }
}