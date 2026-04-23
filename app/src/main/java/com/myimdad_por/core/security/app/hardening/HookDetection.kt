package com.myimdad_por.core.security.app.hardening

import java.io.File
import java.lang.reflect.Method
import java.net.InetSocketAddress
import java.net.Socket

object HookDetection {

    private val SUSPICIOUS_STACK_CLASS_MARKERS = listOf(
        "de.robv.android.xposed",
        "me.weishu.exposed",
        "com.saurik.substrate",
        "lsposed",
        "zygisk",
        "frida"
    )

    private val SUSPICIOUS_MAP_MARKERS = listOf(
        "frida",
        "xposed",
        "substrate",
        "lsposed",
        "zygisk",
        "inject",
        "hook",
        "gum-js-loop",
        "re.frida.server"
    )

    private val SUSPICIOUS_NATIVE_PORTS = intArrayOf(27042, 27043)

    enum class Signal {
        STACK_TRACE_HOOKED,
        FRIDA_SERVER_RUNNING,
        HOOKING_LIBRARIES_LOADED,
        SUSPICIOUS_NATIVE_METHOD,
        SUSPICIOUS_LOCAL_PORT_OPEN
    }

    data class SecurityStatus(
        val compromised: Boolean,
        val signals: List<Signal>
    ) {
        val riskScore: Int get() = signals.size
    }

    /**
     * فحص شامل يجمع عدة مؤشرات لهجوم hooking / instrumentation.
     */
    fun inspect(methodsToVerify: Collection<Method> = emptyList()): SecurityStatus {
        val signals = linkedSetOf<Signal>()

        if (isStackTraceHooked()) {
            signals += Signal.STACK_TRACE_HOOKED
        }
        if (isFridaServerRunning()) {
            signals += Signal.FRIDA_SERVER_RUNNING
        }
        if (hasHookingLibraries()) {
            signals += Signal.HOOKING_LIBRARIES_LOADED
        }
        if (hasUnexpectedNativeMethods(methodsToVerify)) {
            signals += Signal.SUSPICIOUS_NATIVE_METHOD
        }
        if (hasSuspiciousLocalPort()) {
            signals += Signal.SUSPICIOUS_LOCAL_PORT_OPEN
        }

        return SecurityStatus(
            compromised = signals.isNotEmpty(),
            signals = signals.toList()
        )
    }

    /**
     * يفحص StackTrace بحثًا عن بصمات Xposed / Frida / Substrate.
     */
    fun isStackTraceHooked(): Boolean {
        return runCatching {
            val trace = Exception("HookDetection").stackTrace
            trace.any { element ->
                val className = element.className.lowercase()
                SUSPICIOUS_STACK_CLASS_MARKERS.any { marker ->
                    className.contains(marker.lowercase())
                }
            }
        }.getOrDefault(false)
    }

    /**
     * كشف Frida عبر المنافذ الافتراضية المعروفة.
     */
    fun isFridaServerRunning(): Boolean {
        return SUSPICIOUS_NATIVE_PORTS.any { port ->
            isPortOpen("127.0.0.1", port)
        }
    }

    /**
     * فحص /proc/self/maps للبحث عن مكتبات أو بصمات حقن مشبوهة.
     */
    fun hasHookingLibraries(): Boolean {
        return runCatching {
            File("/proc/self/maps").useLines { lines ->
                lines.any { line ->
                    val lower = line.lowercase()
                    SUSPICIOUS_MAP_MARKERS.any { marker -> lower.contains(marker) }
                }
            }
        }.getOrDefault(false)
    }

    /**
     * يكشف إن كانت دوال تطبيقك الحساسة تحولت فجأة إلى native.
     * مرر هنا methods التي تتوقع أن تبقى Kotlin/Java عادية.
     */
    fun hasUnexpectedNativeMethods(methodsToVerify: Collection<Method>): Boolean {
        if (methodsToVerify.isEmpty()) return false

        return methodsToVerify.any { method ->
            isSuspiciousNativeMethod(method)
        }
    }

    /**
     * يكشف وجود منفذ محلي مفتوح بشكل مريب.
     * مفيد كإشارة إضافية، وليس كحكم نهائي.
     */
    fun hasSuspiciousLocalPort(): Boolean {
        return runCatching {
            val commonPorts = intArrayOf(27042, 27043, 23946)
            commonPorts.any { port -> isPortOpen("127.0.0.1", port) }
        }.getOrDefault(false)
    }

    fun getRiskReasons(methodsToVerify: Collection<Method> = emptyList()): List<String> {
        return inspect(methodsToVerify).signals.map { it.name }
    }

    fun shouldRestrictSensitiveOperations(methodsToVerify: Collection<Method> = emptyList()): Boolean {
        return inspect(methodsToVerify).compromised
    }

    private fun isSuspiciousNativeMethod(method: Method): Boolean {
        val declaringClassName = method.declaringClass.name.lowercase()

        val appPackageHint = declaringClassName.startsWith("com.myimdad_por")
        val sensitiveNameHint = method.name.lowercase().contains("premium") ||
            method.name.lowercase().contains("auth") ||
            method.name.lowercase().contains("token") ||
            method.name.lowercase().contains("password") ||
            method.name.lowercase().contains("integrity") ||
            method.name.lowercase().contains("subscribe")

        return appPackageHint && sensitiveNameHint && method.modifiers.let { java.lang.reflect.Modifier.isNative(it) }
    }

    private fun isPortOpen(host: String, port: Int): Boolean {
        return runCatching {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), 300)
                true
            }
        }.getOrDefault(false)
    }
}