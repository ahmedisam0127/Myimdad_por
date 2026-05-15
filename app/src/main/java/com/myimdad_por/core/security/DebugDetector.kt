package com.myimdad_por.core.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Debug
import android.provider.Settings
import java.io.File

/**
 * Debugging and tracing detector.
 *
 * This is a local defense layer, not a final proof.
 * Combine it with server-side integrity checks for stronger protection.
 */
object DebugDetector {

    enum class Signal {
        APP_DEBUGGABLE,
        DEBUGGER_CONNECTED,
        WAITING_FOR_DEBUGGER,
        TRACER_PID,
        USB_DEBUGGING_ENABLED,
        EMULATOR_LIKE_BUILD
    }

    data class SecurityStatus(
        val compromised: Boolean,
        val signals: List<Signal>
    ) {
        val riskScore: Int get() = signals.size
    }

    fun inspect(context: Context): SecurityStatus {
        val signals = linkedSetOf<Signal>()

        if (isAppDebuggable(context)) {
            signals += Signal.APP_DEBUGGABLE
        }

        if (isDebuggerAttached()) {
            signals += Signal.DEBUGGER_CONNECTED
        }

        if (isWaitingForDebugger()) {
            signals += Signal.WAITING_FOR_DEBUGGER
        }

        if (isBeingTraced()) {
            signals += Signal.TRACER_PID
        }

        if (isUsbDebuggingEnabled(context)) {
            signals += Signal.USB_DEBUGGING_ENABLED
        }

        if (hasEmulatorLikeBuildProfile()) {
            signals += Signal.EMULATOR_LIKE_BUILD
        }

        return SecurityStatus(
            compromised = signals.isNotEmpty(),
            signals = signals.toList()
        )
    }

    fun isDebuggerAttached(): Boolean {
        return Debug.isDebuggerConnected()
    }

    fun isWaitingForDebugger(): Boolean {
        return Debug.waitingForDebugger()
    }

    fun isAppDebuggable(context: Context): Boolean {
        return (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    fun isBeingTraced(): Boolean {
        return runCatching {
            File("/proc/self/status").useLines { lines ->
                lines.firstOrNull { it.startsWith("TracerPid:") }
                    ?.substringAfter("TracerPid:")
                    ?.trim()
                    ?.toIntOrNull()
                    ?.let { it > 0 }
                    ?: false
            }
        }.getOrDefault(false)
    }

    fun isUsbDebuggingEnabled(context: Context): Boolean {
        return runCatching {
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.ADB_ENABLED,
                0
            ) == 1
        }.getOrDefault(false)
    }

    fun hasEmulatorLikeBuildProfile(): Boolean {
        val fingerprint = android.os.Build.FINGERPRINT.lowercase()
        val model = android.os.Build.MODEL.lowercase()
        val manufacturer = android.os.Build.MANUFACTURER.lowercase()
        val brand = android.os.Build.BRAND.lowercase()
        val device = android.os.Build.DEVICE.lowercase()
        val product = android.os.Build.PRODUCT.lowercase()
        val hardware = android.os.Build.HARDWARE.lowercase()

        return fingerprint.contains("generic") ||
            fingerprint.contains("test-keys") ||
            model.contains("google_sdk") ||
            model.contains("emulator") ||
            model.contains("android sdk built for x86") ||
            manufacturer.contains("genymotion") ||
            brand.startsWith("generic") && device.startsWith("generic") ||
            product.contains("sdk") ||
            product.contains("emulator") ||
            hardware.contains("goldfish") ||
            hardware.contains("ranchu")
    }

    fun shouldRestrictSensitiveOperations(context: Context): Boolean {
        val status = inspect(context)
        return status.signals.contains(Signal.DEBUGGER_CONNECTED) ||
            status.signals.contains(Signal.TRACER_PID) ||
            status.signals.contains(Signal.APP_DEBUGGABLE)
    }

    fun getRiskReasons(context: Context): List<String> {
        return inspect(context).signals.map { it.name }
    }
}