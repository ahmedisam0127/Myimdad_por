package com.myimdad_por.core.security.app.hardening

import android.content.Context
import com.myimdad_por.core.security.RootDetector
import java.io.File

/**
 * Final enforcement layer for rooted or tampered environments.
 *
 * RootDetector reports the risk.
 * RootGuard enforces the policy.
 */
object RootGuard {

    enum class Signal {
        ROOT_DETECTED,
        ROOT_BINARY_FOUND,
        MAGISK_ARTIFACT_FOUND,
        BUSYBOX_FOUND,
        SYSTEM_MOUNT_RW,
        VENDOR_MOUNT_RW,
        PROD_MOUNT_RW,
        SUSPICIOUS_MOUNT_POINT,
        TEST_KEYS
    }

    data class SecurityStatus(
        val safe: Boolean,
        val signals: List<Signal>
    ) {
        val riskScore: Int get() = signals.size
    }

    private val rootPaths = listOf(
        "/system/app/Superuser.apk",
        "/sbin/su",
        "/system/bin/su",
        "/system/xbin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/su",
        "/system/xbin/mu"
    )

    private val magiskPaths = listOf(
        "/sbin/.magisk",
        "/data/adb/magisk",
        "/data/adb/modules",
        "/data/adb/zygisk",
        "/data/adb/ksu",
        "/cache/.magisk",
        "/dev/.magisk",
        "/debug_ramdisk",
        "/sbin/.core/img",
        "/system/bin/magisk",
        "/system/xbin/magisk"
    )

    private val busyBoxPaths = listOf(
        "/system/xbin/busybox",
        "/system/bin/busybox",
        "/system/sbin/busybox",
        "/sbin/busybox",
        "/vendor/bin/busybox",
        "/data/local/busybox"
    )

    private val suspiciousMountMarkers = listOf(
        "magisk",
        "zygisk",
        "overlay",
        "tmpfs",
        "rw,"
    )

    /**
     * Final assessment for sensitive flows.
     */
    fun inspect(context: Context): SecurityStatus {
        val signals = linkedSetOf<Signal>()

        if (RootDetector.isSystemCompromised(context)) {
            signals += Signal.ROOT_DETECTED
        }

        if (checkRootFiles()) {
            signals += Signal.ROOT_BINARY_FOUND
        }

        if (checkMagiskArtifacts()) {
            signals += Signal.MAGISK_ARTIFACT_FOUND
        }

        if (checkBusyBoxArtifacts()) {
            signals += Signal.BUSYBOX_FOUND
        }

        if (hasReadWriteSystemMount()) {
            signals += Signal.SYSTEM_MOUNT_RW
        }

        if (hasReadWriteVendorMount()) {
            signals += Signal.VENDOR_MOUNT_RW
        }

        if (hasReadWriteProductMount()) {
            signals += Signal.PROD_MOUNT_RW
        }

        if (hasSuspiciousMountPoints()) {
            signals += Signal.SUSPICIOUS_MOUNT_POINT
        }

        if (hasTestKeys()) {
            signals += Signal.TEST_KEYS
        }

        return SecurityStatus(
            safe = signals.isEmpty(),
            signals = signals.toList()
        )
    }

    /**
     * Main policy check for high-value operations.
     */
    fun isEnvironmentSafe(context: Context): Boolean {
        return inspect(context).safe
    }

    fun shouldBlockSensitiveOperations(context: Context): Boolean {
        return !isEnvironmentSafe(context)
    }

    fun getRiskLevel(context: Context): Int {
        return inspect(context).riskScore
    }

    fun getRiskReasons(context: Context): List<String> {
        return inspect(context).signals.map { it.name }
    }

    fun hasRootFiles(): Boolean {
        return checkRootFiles()
    }

    fun hasMagiskArtifacts(): Boolean {
        return checkMagiskArtifacts()
    }

    fun hasBusyBox(): Boolean {
        return checkBusyBoxArtifacts()
    }

    fun hasReadWriteSystem(): Boolean {
        return hasReadWriteSystemMount()
    }

    private fun checkRootFiles(): Boolean {
        return rootPaths.any { path ->
            runCatching { File(path).exists() }.getOrDefault(false)
        }
    }

    private fun checkMagiskArtifacts(): Boolean {
        return magiskPaths.any { path ->
            runCatching { File(path).exists() }.getOrDefault(false)
        } || runCatching {
            File("/proc/self/mounts").useLines { lines ->
                lines.any { line ->
                    line.contains("magisk", ignoreCase = true) ||
                        line.contains("zygisk", ignoreCase = true) ||
                        line.contains("overlay", ignoreCase = true)
                }
            }
        }.getOrDefault(false)
    }

    private fun checkBusyBoxArtifacts(): Boolean {
        return busyBoxPaths.any { path ->
            runCatching { File(path).exists() }.getOrDefault(false)
        } || runCatching {
            File("/system/bin").listFiles()?.any { file ->
                file.name.equals("busybox", ignoreCase = true)
            } == true
        }.getOrDefault(false)
    }

    private fun hasReadWriteSystemMount(): Boolean {
        return hasReadWriteMountFor("/system")
    }

    private fun hasReadWriteVendorMount(): Boolean {
        return hasReadWriteMountFor("/vendor")
    }

    private fun hasReadWriteProductMount(): Boolean {
        return hasReadWriteMountFor("/product")
    }

    private fun hasReadWriteMountFor(prefix: String): Boolean {
        return runCatching {
            File("/proc/mounts").useLines { lines ->
                lines.any { line ->
                    val parts = line.split(Regex("\\s+"))
                    val mountPoint = parts.getOrNull(1).orEmpty()
                    val options = parts.getOrNull(3).orEmpty()

                    mountPoint.startsWith(prefix) && options.contains("rw")
                }
            }
        }.getOrDefault(false)
    }

    private fun hasSuspiciousMountPoints(): Boolean {
        return runCatching {
            File("/proc/mounts").useLines { lines ->
                lines.any { line ->
                    suspiciousMountMarkers.any { marker ->
                        line.contains(marker, ignoreCase = true)
                    }
                }
            }
        }.getOrDefault(false)
    }

    private fun hasTestKeys(): Boolean {
        return android.os.Build.TAGS?.contains("test-keys", ignoreCase = true) == true
    }
}