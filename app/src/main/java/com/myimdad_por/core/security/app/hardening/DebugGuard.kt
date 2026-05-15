package com.myimdad_por.core.security.app.hardening

import android.app.Activity
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Debug
import android.provider.Settings
import android.view.WindowManager
import com.myimdad_por.core.security.DebugDetector
import com.myimdad_por.core.security.RootDetector
import java.io.File

/**
 * Active defense layer against debugging and reverse-engineering.
 *
 * This class is stricter than the detector layer:
 * - detector: reports signals
 * - guard: enforces policy
 */
object DebugGuard {

    enum class Signal {
        DEBUGGER_CONNECTED,
        APP_DEBUGGABLE,
        TRACER_PID,
        WAITING_FOR_DEBUGGER,
        USB_DEBUGGING_ENABLED,
        ROOTED_ENVIRONMENT,
        EMULATOR_LIKE_BUILD,
        TEST_KEYS,
        JDWP_SUSPICIOUS_THREAD
    }

    data class SecurityStatus(
        val blocked: Boolean,
        val signals: List<Signal>
    ) {
        val riskScore: Int get() = signals.size
    }

    /**
     * Full guard check that combines debugger, root, and device signals.
     */
    fun inspect(context: Context): SecurityStatus {
        val signals = linkedSetOf<Signal>()

        if (Debug.isDebuggerConnected()) {
            signals += Signal.DEBUGGER_CONNECTED
        }

        if (Debug.waitingForDebugger()) {
            signals += Signal.WAITING_FOR_DEBUGGER
        }

        if (isManifestDebuggable(context)) {
            signals += Signal.APP_DEBUGGABLE
        }

        if (isTracerPresent()) {
            signals += Signal.TRACER_PID
        }

        if (isUsbDebuggingEnabled(context)) {
            signals += Signal.USB_DEBUGGING_ENABLED
        }

        if (RootDetector.isSystemCompromised(context)) {
            signals += Signal.ROOTED_ENVIRONMENT
        }

        if (DebugDetector.hasEmulatorLikeBuildProfile()) {
            signals += Signal.EMULATOR_LIKE_BUILD
        }

        if (hasTestKeys()) {
            signals += Signal.TEST_KEYS
        }

        if (hasSuspiciousJdwpThreads()) {
            signals += Signal.JDWP_SUSPICIOUS_THREAD
        }

        return SecurityStatus(
            blocked = signals.isNotEmpty(),
            signals = signals.toList()
        )
    }

    /**
     * Main policy decision.
     */
    fun isDebuggerActive(context: Context): Boolean {
        return inspect(context).blocked
    }

    /**
     * Use this before sensitive actions such as:
     * - login
     * - payment
     * - decrypting secrets
     * - exporting data
     */
    fun shouldBlockSensitiveOperation(context: Context): Boolean {
        val status = inspect(context)
        return status.blocked
    }

    /**
     * Prevent screen capture and screen recording.
     */
    fun applyWindowProtection(activity: Activity) {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    /**
     * Remove screen protection if needed.
     */
    fun clearWindowProtection(activity: Activity) {
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    /**
     * Provides human-readable reasons for blocking.
     */
    fun getRiskReasons(context: Context): List<String> {
        return inspect(context).signals.map { it.name }
    }

    /**
     * Returns a compact risk score.
     */
    fun getRiskLevel(context: Context): Int {
        return inspect(context).riskScore
    }

    /**
     * Optional hard stop for highly sensitive flows.
     * Keep the decision at the call site; this method only helps centralize it.
     */
    fun terminateIfCompromised(context: Context): Boolean {
        return isDebuggerActive(context)
    }

    private fun isManifestDebuggable(context: Context): Boolean {
        return (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    private fun isTracerPresent(): Boolean {
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

    private fun isUsbDebuggingEnabled(context: Context): Boolean {
        return runCatching {
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.ADB_ENABLED,
                0
            ) == 1
        }.getOrDefault(false)
    }

    private fun hasTestKeys(): Boolean {
        return Build.TAGS?.contains("test-keys", ignoreCase = true) == true
    }

    private fun hasSuspiciousJdwpThreads(): Boolean {
        return runCatching {
            Thread.getAllStackTraces().keys.any { thread ->
                val name = thread.name.lowercase()
                name.contains("jdwp") || name.contains("signal catcher")
            }
        }.getOrDefault(false)
    }
}